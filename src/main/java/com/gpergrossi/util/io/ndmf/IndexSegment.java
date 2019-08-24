package com.gpergrossi.util.io.ndmf;

import java.io.IOException;
import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Set;

import com.gpergrossi.util.data.Iterators;

public class IndexSegment<Name, Data> extends Segment<Name, Data> implements Map<Name, Integer> {
	
	protected Map<Name, IndexEntrySlot<Name>> usedSlots;
	protected Queue<IndexEntrySlot<Name>> emptySlots;
	
	protected IndexSegment<Name, Data> nextSegment;
	
	public IndexSegment(NamedDataMapFile<Name, Data> ndmFile, int blockIDStart) {
		super(ndmFile, blockIDStart);
		this.usedSlots = new HashMap<>(ndmFile.MAX_INDEX_ENTRY_SLOTS);
		this.emptySlots = new LinkedList<>();
	}
	
	public void readIndex() {
		try {
			IndexSegment<Name, Data> previousSegment = this;
			int nextBlockID = readInternal();
			while (nextBlockID > 0) {
				IndexSegment<Name, Data> nextSegment = new IndexSegment<>(ndmFile, nextBlockID);
				nextBlockID = nextSegment.readInternal();
				previousSegment.nextSegment = nextSegment;
				previousSegment = nextSegment;
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Reads this segment's information
	 * @return the block index of the next index segment, or -1 if there isn't one
	 * @throws IOException
	 */
	private int readInternal() throws IOException {
		super.readSegment();
		final int numSlots = getNumSlots();
		for (int slotID = 0; slotID < numSlots; slotID++) {
			final IndexEntrySlot<Name> slot = new IndexEntrySlot<Name>(this, slotID);
			slot.read();
			
			if (slot.isEmpty()) {
				if (!slot.isLast())	emptySlots.offer(slot);
			} else if (slot.isUsed()) {
				usedSlots.put(slot.getName(), slot);
			} else {
				return slot.getSegmentPointerBlockIndex();
			}
		}
		
		return -1; // There is no next index segment
	}
	
	/**
	 * Allocates the next index segment and writes the link to the end of this one
	 * @throws IOException 
	 */
	private void allocateNext(IndexEntrySlot<Name> lastSlot) {
		if (!lastSlot.isLast()) throw new IllegalArgumentException("lastSlot is not last slot!");
		if (this.nextSegment != null) throw new RuntimeException("Cannot allocate next index segment because it already exists!");
				
		// Attempt allocate
		final int allocateSize = ndmFile.SIZE_SEGMENT_HEADER + ndmFile.MAX_INDEX_ENTRY_SLOTS * ndmFile.SIZE_INDEX_ENTRY;
		final int allocateBlock;
		try {
			allocateBlock = super.getAllocateBlock(allocateSize);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		// Write link to lastSlot
		lastSlot.setSegmentPointerBlockIndex(allocateBlock);
		
		// Create new segment
		this.nextSegment = new IndexSegment<>(ndmFile, allocateBlock);
		this.nextSegment.size = allocateSize;
		final int numSlots = this.nextSegment.getNumSlots();
		for (int slotID = 0; slotID < numSlots; slotID++) {
			final IndexEntrySlot<Name> slot = new IndexEntrySlot<Name>(this.nextSegment, slotID);
			slot.write();
			if (!slot.isLast())	this.nextSegment.emptySlots.offer(slot);
		}
		this.nextSegment.writeSegment();
	}
	
	private int getNumSlots() {
		// Size of this segment divided by the size of a index entry
		return (this.size - ndmFile.SIZE_SEGMENT_HEADER) / ndmFile.SIZE_INDEX_ENTRY;
	}
	
	/**
	 * Adds a slot if there is room and returns the new slot index.
	 * @return
	 */
	private IndexEntrySlot<Name> internalAddSlot() {
		final int numSlots = getNumSlots();
		try {
			boolean success = this.resize(ndmFile.SIZE_SEGMENT_HEADER + (numSlots+1)*ndmFile.SIZE_INDEX_ENTRY, false);
			if (!success) throw new RuntimeException("Not enough space for new slot!");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return new IndexEntrySlot<>(this, numSlots);
	}
	
	private boolean canGrow() {
		final int numSlots = getNumSlots();
		
		if (numSlots < ndmFile.MAX_INDEX_ENTRY_SLOTS-1) return true;
		
		if (numSlots > ndmFile.MAX_INDEX_ENTRY_SLOTS) {
			if (this.size < ndmFile.SIZE_SEGMENT_HEADER + ndmFile.SIZE_INDEX_ENTRY) {
				throw new RuntimeException("Could not grow because SIZE_INDEX_SEGMENT is too small!");
			}
			return false;
		}
		
		// Only room for one more slot
		// We need to allocate a new index segment
		if (this.nextSegment == null) {
			if (numSlots == ndmFile.MAX_INDEX_ENTRY_SLOTS) this.allocateNext(new IndexEntrySlot<>(this, numSlots-1));
			else this.allocateNext(internalAddSlot());
		}
		return false;
	}

	/**
	 * Checks for an available empty slot. If there isn't one in the emptySlot queue,
	 * this IndexSegment will try to grow to make room for a new slot. If a slot
	 * is available by the end of this method, it will be in the emptySlots queue.
	 * @return true if a slot is available in this IndexSegment, false if not
	 */
	private boolean hasEmptyEntry() {
		if (emptySlots.size() > 0) return true;
		return canGrow();
	}
	
	/**
	 * Returns an empty slot from this IndexSegment if there is one.
	 * @return an empty slot, or null
	 */
	private IndexEntrySlot<Name> internalGetEmptyEntry() {
		if (!hasEmptyEntry()) return null;
		if (emptySlots.size() > 0) return emptySlots.poll();
		return internalAddSlot();
	}
	
	/**
	 * Gets an empty slot from this segment or any of the next segments following the segment links.
	 * If there is no more room in any existing index segments, and {@link canCreate} is true, a new
	 * segment will be created.
	 */
	private IndexEntrySlot<Name> getSlot(Name name, boolean canCreate) {
		IndexSegment<Name, Data> segment = this;
		IndexSegment<Name, Data> firstOpenSegment = null;
		
		if (ndmFile.debug && ndmFile.debugVerbosity >= 2) {
			System.out.println("Looking for slot for "+name+" (canCreate="+canCreate+")");
		}
		
		while (segment != null) {
			segment.printSlots();
			
			final IndexEntrySlot<Name> slot = segment.usedSlots.get(name);
			if (slot != null) {
				if (ndmFile.debug && ndmFile.debugVerbosity >= 2) {
					System.out.println("Found "+name+" in slot "+segment.blockIDStart+":"+slot.getSlotID());
				}
				if (firstOpenSegment != null) {
					final IndexEntrySlot<Name> openSlot = firstOpenSegment.internalGetEmptyEntry();
					if (ndmFile.debug && ndmFile.debugVerbosity >= 2) {
						System.out.println("Migrating "+name+" from slot "+segment.blockIDStart+":"+slot.getSlotID()
							+" to slot "+firstOpenSegment.blockIDStart+":"+openSlot.getSlotID());
					}
					return slot.migrate(openSlot);
				} else {
					return slot;
				}
			}
			
			if (firstOpenSegment == null && segment.hasEmptyEntry()) {
				firstOpenSegment = segment;
			}
			
			segment = segment.nextSegment;
		}
		
		if (canCreate) return firstOpenSegment.internalGetEmptyEntry();
		else return null;
	}

	protected void printSlots() {
		if (!ndmFile.debug) return;
		if (ndmFile.debugVerbosity >= 3) {
			System.out.println("  Segment "+this.blockIDStart+" has "+usedSlots.size()+" filled entries:");
			List<Entry<Name, IndexEntrySlot<Name>>> entries = new ArrayList<>(this.usedSlots.entrySet());
			Collections.sort(entries, (a, b) -> a.getValue().getSlotID() - b.getValue().getSlotID());
			for (Entry<Name, IndexEntrySlot<Name>> entry : entries) {
				System.out.println("  > "+entry.getKey()+" -> "+entry.getValue());
			}
			System.out.println("  + "+this.emptySlots.size()+" free slots");
		} else if (ndmFile.debugVerbosity >= 2) {
			System.out.println("  Segment "+this.blockIDStart+" has "+usedSlots.size()+" used and "+emptySlots.size()+" empty entries.");
		}
		
	}

	@Override
	public int size() {
		IndexSegment<Name, Data> segment = this;
		int numEntries = 0;
		while (segment != null) {
			numEntries += segment.usedSlots.size();
			segment = segment.nextSegment;
		}
		return numEntries;
	}

	@Override
	public boolean isEmpty() {
		return size() == 0;
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean containsKey(Object key) {
		return getSlot((Name) key, false) != null;
	}

	@Override
	public boolean containsValue(Object value) {
		throw new UnsupportedOperationException();
	}

	@Override
	@SuppressWarnings("unchecked")
	public Integer get(Object key) {
		Name name = (Name) key;
		IndexEntrySlot<Name> slot = getSlot(name, false);
		if (slot == null) return null;
		return slot.getDataBlockID();
	}

	@Override
	public Integer put(Name key, Integer value) {
		if (value == null) value = 0;
		else if (value < 0) throw new IllegalArgumentException("Cannot set a negative offset!");
		
		IndexEntrySlot<Name> slot = getSlot(key, value != 0);
		if (value == 0 && slot == null) return null;
		
		int oldValue = slot.getDataBlockID(); 
		if (value != oldValue) slot.set(key, value);
		return ((oldValue == 0) ? null : oldValue);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Integer remove(Object key) {
		Name name = (Name) key;
		return put(name, 0);
	}

	@Override
	public void putAll(Map<? extends Name, ? extends Integer> m) {
		for (Entry<? extends Name, ? extends Integer> entry : m.entrySet()) {
			this.put(entry.getKey(), entry.getValue());
		}
	}

	@Override
	public void clear() {
		Iterator<Entry<Name, Integer>> iter = this.entrySet().iterator();
		while (iter.hasNext()) {
			iter.next();
			iter.remove();
		}
	}

	@Override
	public Set<Name> keySet() {
		return new AbstractSet<Name>() {
			@Override
			public int size() {
				return IndexSegment.this.size();
			}

			@Override
			@SuppressWarnings("unchecked")
			public boolean contains(Object o) {
				Name name = (Name) o;
				return IndexSegment.this.get(name) != null;
			}
			
			@Override
			public Iterator<Name> iterator() {
				return Iterators.cast(IndexSegment.this.entrySet().iterator(), entry -> entry.getKey());
			}
			
			@Override
			@SuppressWarnings("unchecked")
			public boolean remove(Object o) {
				Name name = (Name) o;
				return IndexSegment.this.remove(name) != null;
			}
			
		};
	}

	@Override
	public Collection<Integer> values() { 
		return new AbstractCollection<Integer>() {
			@Override
			public int size() {
				return IndexSegment.this.size();
			}
			
			@Override
			public Iterator<Integer> iterator() {
				return Iterators.cast(IndexSegment.this.entrySet().iterator(), entry -> entry.getValue());
			}
		};
	}

	@Override
	public Set<Entry<Name, Integer>> entrySet() {			
		return new AbstractSet<Entry<Name, Integer>>() {
			@Override
			public int size() {
				return IndexSegment.this.size();
			}

			@Override
			@SuppressWarnings("unchecked")
			public boolean contains(Object o) {
				Entry<Name, Integer> entry = (Entry<Name, Integer>) o;
				return IndexSegment.this.get(entry.getKey()) == entry.getValue();
			}
			
			@Override
			public Iterator<Entry<Name, Integer>> iterator() {
				return new Iterator<Entry<Name, Integer>>() {
					IndexSegment<Name, Data> currentSegment = IndexSegment.this;
					Iterator<Entry<Name, IndexEntrySlot<Name>>> currentIterator = currentSegment.usedSlots.entrySet().iterator();
					Iterator<Entry<Name, IndexEntrySlot<Name>>> lastIterator = null;
					
					@Override
					public boolean hasNext() {
						while (true) {
							if (currentIterator.hasNext()) return true;
							if (currentSegment.nextSegment == null) return false;
							
							currentSegment = currentSegment.nextSegment;
							currentIterator = currentSegment.usedSlots.entrySet().iterator();
						}
					}

					@Override
					public Entry<Name, Integer> next() {
						if (!hasNext()) throw new NoSuchElementException();
						lastIterator = currentIterator;
						return currentIterator.next().getValue();
					}
					
					@Override
					public void remove() {
						if (lastIterator == null) throw new IllegalStateException();
						lastIterator.remove();
						lastIterator = null;
					}
				};
			}

			@Override
			public boolean add(Entry<Name, Integer> e) {
				return IndexSegment.this.put(e.getKey(), e.getValue()) != e.getValue();
			}
			
			@Override
			@SuppressWarnings("unchecked")
			public boolean remove(Object o) {
				Entry<Name, Integer> entry = (Entry<Name, Integer>) o;
				if (!this.contains(entry)) return false;
				return IndexSegment.this.remove(entry.getKey()) != null;
			}
		};
	}
}
