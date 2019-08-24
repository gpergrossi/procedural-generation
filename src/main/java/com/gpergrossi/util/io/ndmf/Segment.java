package com.gpergrossi.util.io.ndmf;

import java.io.IOException;

public class Segment<Name, Data> {

	protected static final int UNALLOCATED = -1;
	
	protected final NamedDataMapFile<Name, Data> ndmFile;
	protected int blockIDStart;
	protected int size;
	protected boolean copyOnResize = true;
	
	public Segment(NamedDataMapFile<Name, Data> ndmFile, int blockIDStart) {
		this.ndmFile = ndmFile;
		this.blockIDStart = blockIDStart;
		this.size = 4;
	}
	
	protected final void readSegment() {
		try {
			ndmFile.seekBlock(blockIDStart, 0);
			this.size = ndmFile.readSegmentHeader();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	protected final void writeSegment() {
		try {
			ndmFile.seekBlock(blockIDStart, 0);
			ndmFile.writeSegmentHeader(size);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Resizes this segment to the provided {@link newSize} in bytes. The new size is immediately written 
	 * to the disk. If this segment no longer fits in its current blocks and {@link reallocate} is true,
	 * the segment will allocate new block(s) and may be copied to a new location. If {@link reallocate} 
	 * is false and the new size cannot fit, the operation fails, no changes are made, and false is returned.
	 * @param newSize - the size in bytes this segment should change to
	 * @param allowReallocate - whether or not the operation should allow the allocation of new
	 *  blocks and a potential relocation of this segment's data.
	 * @return true if successful, false otherwise.
	 * @throws IOException 
	 */
	public boolean resize(int newSize, boolean allowReallocate) throws IOException {
		if (blockIDStart == UNALLOCATED) return allocate(newSize, allowReallocate);
		if (!copyOnResize && allowReallocate) return freeReallocate(newSize);
		if (newSize < size) return shrink(newSize);
		if (newSize > size) return grow(newSize, allowReallocate);
		return true; // newSize == size
	}

	/**
	 * Shrinks this segment to the provided {@link newSize} in bytes. 
	 * The new size is immediately written to the disk.
	 * @param newSize - the size in bytes this segment should shrink to.
	 * @throws IOException 
	 */
	private boolean shrink(int newSize) throws IOException {
		if (size < ndmFile.SIZE_SEGMENT_HEADER) throw new RuntimeException("Cannot shrink to a size smaller than SIZE_SEGMENT_HEADER!");
		
		int numBlocks = ndmFile.numBlocks(size);
		int newNumBlocks = ndmFile.numBlocks(newSize);
		
		if (newNumBlocks == numBlocks) {
			this.size = newSize;
			writeSegment();
			return true;
		}
		
		final int numBlocksFreed = numBlocks - newNumBlocks;
		final int startBlocksFreed = this.blockIDStart + numBlocks;
		final int endBlocksFreed = startBlocksFreed + numBlocksFreed - 1;
		ndmFile.markBlocksFree(startBlocksFreed, endBlocksFreed);
		return true;
	}

	private boolean grow(int newSize, boolean allowReallocate) throws IOException {
		if (size < ndmFile.SIZE_SEGMENT_HEADER) throw new RuntimeException("Cannot grow to a size smaller than SIZE_SEGMENT_HEADER!");
		
		int numBlocks = ndmFile.numBlocks(size);
		int newNumBlocks = ndmFile.numBlocks(newSize);

		if (ndmFile.debug) System.out.println("Resize grow "+size+" ("+numBlocks+") --> "+newSize+" ("+newNumBlocks+")");

		if (newNumBlocks == numBlocks) {
			this.size = newSize;
			writeSegment();
			return true;
		}
	
		if (!allowReallocate) return false;
	
		// Try to grow in place
		final int numBlocksClaimed = (newNumBlocks - numBlocks);
		final int startBlocksClaimed = this.blockIDStart + numBlocks;
		final int endBlocksClaimed = startBlocksClaimed + numBlocksClaimed - 1;
		boolean success = ndmFile.tryClaim(startBlocksClaimed, endBlocksClaimed);
		if (success) {
			this.size = newSize;
			writeSegment();
			return true;
		}
		
		// Reallocate
		int newClaim = ndmFile.getClaim(newNumBlocks);
		if (ndmFile.debug) System.out.println("Reallocate from blocks "+blockIDStart+"-"+(blockIDStart+numBlocks-1)+" to blocks "+newClaim+"-"+(newClaim+newNumBlocks-1));
		
		if (copyOnResize) ndmFile.copyBlock(this.blockIDStart, newClaim, this.size);
		this.free();
		this.blockIDStart = newClaim;
		this.size = newSize;
		writeSegment();
		return true;
	}
	
	private boolean freeReallocate(int newSize) throws IOException {
		int newNumBlocks = ndmFile.numBlocks(newSize);
		
		// Free
		this.free();
		
		// Re-allocate
		int newClaim = ndmFile.getClaim(newNumBlocks);
		this.blockIDStart = newClaim;
		this.size = newSize;
		writeSegment();
		return true;
	}
	
	private boolean allocate(int newSize, boolean reallocate) throws IOException {
		if (!reallocate) throw new RuntimeException("Current segment is unallocated and cannot be resize with allocation.");
		
		int newNumBlocks = ndmFile.numBlocks(newSize);
		int newClaim = ndmFile.getClaim(newNumBlocks);
		this.blockIDStart = newClaim;
		this.size = newSize;
		writeSegment();
		
		return true;
	}

	public void free() {
		try {
			int numBlocks = ndmFile.numBlocks(size);
			ndmFile.markBlocksFree(this.blockIDStart, this.blockIDStart+numBlocks-1);
			this.blockIDStart = UNALLOCATED;
			this.size = -1;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	protected int getAllocateBlock(int size) throws IOException {
		// Need to at least have space for the size header
		if (size < ndmFile.SIZE_SEGMENT_HEADER) throw new IllegalArgumentException("Cannot allocate a size smaller than SIZE_SEGMENT_HEADER!");
		final int numBlocks = ndmFile.numBlocks(size);
		return ndmFile.getClaim(numBlocks);
	}

}
