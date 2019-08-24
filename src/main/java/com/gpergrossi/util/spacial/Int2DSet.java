package com.gpergrossi.util.spacial;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;

import com.google.common.collect.MapMaker;
import com.google.common.util.concurrent.Striped;
import com.gpergrossi.util.data.Iterators;
import com.gpergrossi.util.geom.vectors.Int2D;

public class Int2DSet extends AbstractSet<Int2D> {
	
	private class Chunk implements Iterable<Int2D> {
		
		private final Int2D chunkCoord;
		private AtomicInteger value = new AtomicInteger(0);
		
		private Chunk(Int2D chunkCoord) {
			this.chunkCoord = chunkCoord;
		}
		
		/**
		 * Sets this chunk's bit at {@code index = ((y & 3) << 3) | (x & 7)} to 1. 
		 * Returns true if the bit's previous value was 0.
		 */
		protected boolean add(final int x, final int y) {
			final int index = ((y & 3) << 3) | (x & 7);
			final int bitOfInterest = (1 << index);
			return (value.getAndUpdate(o -> o | bitOfInterest) & bitOfInterest) == 0;
		}
		
		/**
		 * Checks this chunk's bit at {@code index = ((y & 3) << 3) | (x & 7)}. 
		 * Returns true if the bit's value is 1.
		 */
		protected boolean contains(final int x, final int y) {
			final int index = ((y & 3) << 3) | (x & 7);
			final int bitOfInterest = (1 << index);
			return (value.get() & bitOfInterest) != 0;
		}
		
		/**
		 * Sets this chunk's bit at {@code index = ((y & 3) << 3) | (x & 7)} to 0. 
		 * Returns true if the bit's previous value was 1.
		 */
		protected boolean remove(final int x, final int y) {
			final int index = ((y & 3) << 3) | (x & 7);
			final int bitOfInterest = (1 << index);
			return (value.getAndUpdate(o -> o & ~bitOfInterest) & bitOfInterest) != 0;
		}
		
		@Override
		public Iterator<Int2D> iterator() {
			return new Iterator<Int2D>() {
				final int snapshot = value.get();
				
				Int2D lastReturned = null;
				int index = 0;
				
				@Override
				public boolean hasNext() {
					for ( ; index < 32 && !getBit(index); index++);
					return (index < 32);
				}

				@Override
				public Int2D next() {
					if (!hasNext()) throw new NoSuchElementException();
					
					lastReturned = new Int2D(chunkCoord.x() | (index & 7), chunkCoord.y() | (index >> 3));
					index++;
					
					return lastReturned;
				}
				
				@Override
				public void remove() {
					if (lastReturned == null) throw new IllegalStateException();
					Int2DSet.this.remove(lastReturned);
				}

				private boolean getBit(int index) {
					return (snapshot & (1 << index)) != 0;
				}
			};
		}
		
		public int size() {
			return Integer.bitCount(value.get());
		}
	}
	
	Striped<Lock> stripedLock;
	ConcurrentMap<Int2D, Chunk> chunks;
	
	public Int2DSet(int concurrencyLevel) {
		if (concurrencyLevel < 1) concurrencyLevel = 1;
		stripedLock = Striped.lock(concurrencyLevel*4);
		chunks = new MapMaker().concurrencyLevel(concurrencyLevel).initialCapacity(128).makeMap();
	}
	
	public boolean add(final int x, final int y) {
		final Int2D chunkKey = new Int2D(x & ~7, y & ~3);
		
		final Lock lock = stripedLock.get(chunkKey);
		lock.lock();
		
		try {
			Chunk chunk = chunks.get(chunkKey);
			
			if (chunk == null) {
				chunk = new Chunk(chunkKey);
				chunks.put(chunkKey, chunk);
			}
			
			return chunk.add(x, y);
		} finally {
			lock.unlock();
		}
	}
	
	@Override
	public boolean add(final Int2D coord) {
		return add(coord.x(), coord.y());
	}
	
	public boolean remove(final int x, final int y) {
		final Int2D chunkKey = new Int2D(x & ~7, y & ~3);
		
		final Lock lock = stripedLock.get(chunkKey);
		lock.lock();
		
		try {
			Chunk chunk = chunks.get(chunkKey);
			if (chunk == null) return false;
			
			boolean result = chunk.remove(x, y);
			
			if (chunk.value.get() == 0) {
				chunks.remove(chunkKey);
			}
			
			return result;
		} finally {
			lock.unlock();
		}
	}
	
	@Override
	public boolean remove(final Object o) {
		Int2D coord = (Int2D) o;
		return remove(coord.x(), coord.y());
	}
	
	public boolean contains(final int x, final int y) {
		final Int2D chunkKey = new Int2D(x & ~7, y & ~3);
		
		final Lock lock = stripedLock.get(chunkKey);
		lock.lock();
		
		try {
			Chunk chunk = chunks.get(chunkKey);
			if (chunk == null) return false;
			
			return chunk.contains(x, y);
		} finally {
			lock.unlock();
		}
	}
	
	@Override
	public boolean contains(Object o) {
		Int2D coord = (Int2D) o;
		return contains(coord.x(), coord.y());
	}
	
	@Override
	public void clear() {
		chunks.clear();
	}
	
	public Iterator<Int2D> chunkIterator() {
		return Iterators.cast(chunks.values().iterator(), chunk -> chunk.chunkCoord);
	}
	
	@Override
	public Iterator<Int2D> iterator() {
		return Iterators.unwrap(chunks.values().iterator());
	}

	@Override
	public int size() {
		return chunks.values().stream().mapToInt(chunk -> chunk.size()).sum();
	}

	public int getChunk(Int2D coord) {
		return getChunk(coord.x(), coord.y());
	}
	
	public int getChunk(final int x, final int y) {
		final Int2D chunkKey = new Int2D(x & ~7, y & ~3);
		
		final Lock lock = stripedLock.get(chunkKey);
		lock.lock();
		try {			
			final Chunk chunk = chunks.get(chunkKey);
			if (chunk == null) return 0;
			return chunk.value.get();
		} finally {
			lock.unlock();
		}
	}
	
	public int numChunks() {
		return chunks.size();
	}

}
