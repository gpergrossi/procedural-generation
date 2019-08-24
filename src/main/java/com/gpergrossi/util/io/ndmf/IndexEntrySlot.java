package com.gpergrossi.util.io.ndmf;

import java.io.IOException;
import java.util.Map;

public class IndexEntrySlot<Name> implements Map.Entry<Name, Integer> {
	private final IndexSegment<Name, ?> segment;
	private final int slotID;
	private int dataBlockID;
	private Name name;
	
	public IndexEntrySlot(IndexSegment<Name, ?> segment, int slotID) {
		if (segment == null) throw new IllegalArgumentException("Null segment!");
		if (slotID < 0 || slotID >= segment.ndmFile.MAX_INDEX_ENTRY_SLOTS) throw new IllegalArgumentException("slotID out of bounds");
		
		this.segment = segment;
		this.slotID = slotID;
	}

	public IndexSegment<Name, ?> getSegment() {
		return segment;
	}
	
	public int getSlotID() {
		return slotID;
	}
	
	public void set(Name name, int dataBlockID) {
		if (dataBlockID < 0) throw new RuntimeException("Cannot update a negative offset segment!");
		if (dataBlockID == 0) {
			this.clear();
			return;
		}

		if (!name.equals(this.name)) {
			segment.usedSlots.remove(this.name);
			segment.usedSlots.put(name, this);
		}
		
		this.name = name;
		this.dataBlockID = dataBlockID;
		this.write();

		if (segment.ndmFile.debug && segment.ndmFile.debugVerbosity >= 2) {
			System.out.println("Slot "+segment.blockIDStart+":"+slotID+" set to ("+name+"->"+dataBlockID+")");
		}
	}

	public void clear() {
		// Remove entry (null/zero value)
		if (segment.usedSlots.get(this.name) == null) {
			if (segment.ndmFile.debug && segment.ndmFile.debugVerbosity >= 2) {
				System.out.println("Slot "+segment.blockIDStart+":"+slotID+" already empty");
			}
			return;
		}
		segment.usedSlots.remove(this.name);
		segment.emptySlots.offer(this);
		
		this.name = null;
		this.dataBlockID = 0;
		this.write();

		if (segment.ndmFile.debug && segment.ndmFile.debugVerbosity >= 2) {
			System.out.println("Slot "+segment.blockIDStart+":"+slotID+" cleared");
		}
	}

	public int getDataBlockID() {
		return dataBlockID;
	}
	
	public boolean isEmpty() {
		return (this.dataBlockID == 0);
	}

	public boolean isUsed() {
		return (this.dataBlockID > 0);
	}

	public boolean isLast() {
		return this.slotID == segment.ndmFile.MAX_INDEX_ENTRY_SLOTS-1;
	}
	
	public boolean isSegmentPointer() {
		return (this.dataBlockID < 0);
	}

	public int getSegmentPointerBlockIndex() {
		if (!isSegmentPointer()) throw new RuntimeException("Segment pointer invalid!");
		if (this.slotID != segment.ndmFile.MAX_INDEX_ENTRY_SLOTS-1) throw new RuntimeException("Found segment pointer before end of block!");
		return -this.dataBlockID;
	}

	public void setSegmentPointerBlockIndex(int segmentBlockID) {
		this.dataBlockID = -segmentBlockID;
		this.name = null;
		this.write();
	}
	
	public Name getName() {
		return name;
	}	
	
	@Override
	public Name getKey() {
		return name;
	}

	@Override
	public Integer getValue() {
		return dataBlockID;
	}

	@Override
	public Integer setValue(Integer value) {
		if (value == null) value = 0;
		int oldValue = this.dataBlockID;
		this.set(name, value);
		return oldValue;
	}
	
	public void read() {
		try {
			this.dataBlockID = segment.ndmFile.readBlockID();
			if (dataBlockID > 0) {
				this.name = segment.ndmFile.readName();
			} else {
				this.name = null;
				segment.ndmFile.skipName();
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Writes this slot's offset and name to the disk.
	 * @throws IOException 
	 */
	public void write() {
		try {
			final int offset = segment.ndmFile.SIZE_SEGMENT_HEADER + slotID*segment.ndmFile.SIZE_INDEX_ENTRY;
			segment.ndmFile.seekBlock(segment.blockIDStart, offset);
			segment.ndmFile.writeBlockID(dataBlockID);
			if (name != null) {
				segment.ndmFile.writeName(name);
			} else {
				segment.ndmFile.writeBlankName();
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Moves the data from this slot to the {@link newSlot}.
	 * All changes are written to the disk.
	 * @param newSlot - the new slot, must be empty (dataBlockID == 0).
	 * @return the newSlot, for convenience
	 * @throws IOException 
	 */
	public IndexEntrySlot<Name> migrate(IndexEntrySlot<Name> newSlot) {
		if (newSlot.dataBlockID != 0) throw new RuntimeException("Cannot migrate to non-empty slot!");
		
		newSlot.set(this.name, this.dataBlockID);
		this.clear();
		
		return newSlot;
	}
	
	@Override
	public String toString() {
		return "IndexEntrySlot[block="+segment.blockIDStart+", slot="+slotID+", name="+name+", dataBlock="+dataBlockID+"]";
	}
	
}
