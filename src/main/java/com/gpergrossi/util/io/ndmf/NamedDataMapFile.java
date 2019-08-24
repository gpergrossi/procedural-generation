package com.gpergrossi.util.io.ndmf;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableSet;
import java.util.TreeSet;

import com.gpergrossi.util.io.IStreamHandler;
import com.gpergrossi.util.io.IStreamHandlerFixedSize;
import com.gpergrossi.util.io.MD5Hash;
import com.gpergrossi.util.io.IStreamHandler.Reader;
import com.gpergrossi.util.io.IStreamHandler.Writer;

/** 
 * <p>A NamedDataMapFile mimics a small file system within a single OS file. It is ideal for 
 * saving and loading a large quantity of very small "files" (E.G. less than 256 KB each).</p>
 * 
 * <p>Each "file" will have a unique {@link Name} and some {@link Data}. The classes used to represent
 * these concepts are parameterized but should follow some basic guidelines based on the parameters of
 * the NamedDataMapFile in which they will be stored. There a two important parameters to consider:
 * <ol>
 * <li><b>SIZE_NAME</b> specifies the fixed size that each and every Name entry will take up.
 *    If a Name object fails to fit inside SIZE_NAME bytes an exception will be thrown.
 *    SIZE_NAME is read from the IStreamHandler.FixedSize provided for names.</li>
 *    
 * <li><b>SIZE_BLOCK</b> sets the size of each block in the file. Blocks are used to navigate the
 *     file and organize the Data entries written to it. A good block size should be large enough
 *     to hold the smallest Data entries (or multiple index entries) and small enough to use space 
 *     efficiently. (An index entry is SIZE_INDEX_ID + SIZE_NAME bytes)</li></ol></p>
 * 
 * <p><b>The save file (which contains our pseudo "files") has the following structure:</b></p>
 * 
 * <p>The file is divided into blocks, each of which is  SIZE_BLOCK bytes large. These blocks are
 * addressed by their "block index", which is a block offset from the beginning of the file. Therefore,
 * the first block in the file has a block index of 0 and the next, exactly SIZE_BLOCK bytes later, 
 * has an index of 1.</p>
 * 
 * <p>Data blocks are filled by segments of information spanning one or more blocks. Segment lengths are
 * allowed to be a non-perfect multiple of the SIZE_BLOCK. To assist in reading/skipping segments as well 
 * as allocating new segments, each segment starts with a single big-endian, signed integer (SIZE_SEGMENT_HEADER bytes)
 * describing the exact number of bytes in the segment that follows (including SIZE_SEGMENT_HEADER).
 * Positive sizes indicate that a segment is allocated and has valid data, while a non-positive size indicates
 * a block that can be claimed by new allocations. The start of a segment is always aligned to the start
 * of a block. Thus, to skip over a segment, a reader should skip a number of bytes equal to the segment's 
 * size rounded up to the nearest SIZE_BLOCK. Empty blocks (non-positive size, e.g. negative or 0) can be
 * skipped by skipping one SIZE_BLOCK.</p>
 * 
 * <p>There are only two types of segment: 'index' segments, and 'data' segments.</p>
 * 
 * <p>The body of an 'index' segment is an array of index entries. A single index entry consists of an integer 
 * (4 bytes, signed, big-endian) block index pointing to the entry's data and a Name entry. A block index of 
 * exactly 0 indicates an empty index entry slot. If the index segment runs out of space for more index entries, 
 * then the last index entry that fits in the index segment will have a negative offset value. The block found 
 * at the abs(offset) block index is the start of the next index segment. The first block (index 0) of the save 
 * file will always be the start of the first index segment.</p>
 * 
 * <p>The body of a 'data' segment is written directly by the Data IStreamHandler's Writer.<p>
 * 
 * @param <Name> - This type parameter will be used as the key in a TreeMap. The class used for this type parameter 
 * 		should implement hashCode() and all objects intended to equal should return the same hashCode().
 * @param <Data> - This is the class that represents the data of each "file" stored in this map.
 */
public class NamedDataMapFile<Name, Data> {

	/**
	 * If true, all garbage values written will be zeros. This will introduce 
	 * overhead to every padding operation and should be false unless debugging.
	 */
	protected final boolean ZERO_GARBAGE = true;
	
	protected final int SIZE_NAME;
	protected final int SIZE_BLOCK;
	/**
	 * <b>SIZE_INDEX_SEGMENT</b> sets the size in bytes of a index segment. An index segment is a 
	 * segment whose body is an array of index entries, each consisting a (blockID, Name) pair.
	 * If (SIZE_INDEX_SEGMENT - SIZE_SEGMENT_HEADER) is not a multiple of (SIZE_BLOCK_ID + SIZE_NAME),
	 * or SIZE_INDEX_SEGMENT is not a multiple of SIZE_BLOCK, then some bytes at the end of the 
	 * segment body will be unused.
	 */
	protected final int SIZE_INDEX_SEGMENT;
	
	protected final int SIZE_SEGMENT_HEADER = 4;
	protected final int SIZE_DATA_HEADER = 1;
	protected final int SIZE_BLOCK_ID = 4;
	protected final int BUFFER_SIZE = 8192;
	protected final int SIZE_INDEX_ENTRY;
	protected final int MAX_INDEX_ENTRY_SLOTS;

	private byte[] buffer;
	
	private Reader<Name> nameReader;
	private Writer<Name> nameWriter;
	private Reader<Data> dataReader;
	private Writer<Data> dataWriter;
	
	private Map<Name, Integer> storedNames;
	private Map<Name, DataSegment<Name, Data>> storedData;
	private TreeSet<Integer> freeBlocks;
	
	private RandomAccessFile randomAccessFile;
	private boolean isOpen;

	public boolean debug = false;
	public int debugVerbosity = 1; // Currently goes up to 3
	public boolean debugVerifyOnLoad = false;
	
	public NamedDataMapFile(IStreamHandlerFixedSize<Name> nameStreamHandler, IStreamHandler<Data> dataStreamHandler, int blockSize) {
		this.nameReader = nameStreamHandler.getReader();
		this.nameWriter = nameStreamHandler.getWriter();
		this.dataReader = dataStreamHandler.getReader();
		this.dataWriter = dataStreamHandler.getWriter();
				
		this.SIZE_NAME = nameStreamHandler.getMaxSize();
		this.SIZE_BLOCK = blockSize;
		this.SIZE_INDEX_SEGMENT = blockSize;
		
		this.SIZE_INDEX_ENTRY = (SIZE_NAME + SIZE_BLOCK_ID);
		this.MAX_INDEX_ENTRY_SLOTS = (SIZE_INDEX_SEGMENT - SIZE_SEGMENT_HEADER) / SIZE_INDEX_ENTRY;

		this.buffer = new byte[BUFFER_SIZE];
	}

	public boolean isOpen() {
		return isOpen;
	}
	
	public synchronized void open(File file) throws IOException {
		if (isOpen) throw new IllegalStateException("NamedDataMapFile is already open!");
		
		if (!file.exists()) {
			try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(file))) {
				dos.writeInt(SIZE_BLOCK);
			}
			randomAccessFile = new RandomAccessFile(file, "rws");
			randomAccessFile.setLength(SIZE_BLOCK);
		} else {
			randomAccessFile = new RandomAccessFile(file, "rws");
		}
		load();
		this.isOpen = true;
	}
	
	private void load() throws IOException {
		if (debugVerifyOnLoad) {
			boolean success = NDMFVerifier.verifyFormat(this, false);
			if (!success) throw new IOException("Bad format!");
		}
		
		IndexSegment<Name, Data> indexSegment = new IndexSegment<>(this, 0);
		indexSegment.readIndex();
		
		this.storedNames = indexSegment;
		
		this.freeBlocks = new TreeSet<>();
		int block = 0;
		while (true) {
			final long pos = blockOffset(block);
			
			if (pos >= randomAccessFile.length()) break;
			randomAccessFile.seek(pos);
			
			final int size = randomAccessFile.readInt();
			if (size >= SIZE_SEGMENT_HEADER) {
				block += numBlocks(size);
			} else if (size <= 0) {
				freeBlocks.add(block);
				block++;
			} else {
				throw new RuntimeException("Invalid block size: "+size);
			}
		}
		
		this.storedData = new HashMap<>();
	}
	
	public synchronized void close() throws IOException {
		this.storedNames = null;
		this.storedData = null;
		this.freeBlocks = null;
		if (randomAccessFile != null) randomAccessFile.close();
		randomAccessFile = null;
		this.isOpen = false;
	}

	private synchronized DataSegment<Name, Data> internalGetDataSegment(Name name, boolean readDataBody) {		
		DataSegment<Name, Data> stored = storedData.get(name);
		if (stored != null) return stored;
	
		Integer blockID = storedNames.get(name);
		if (blockID == null) return null;
		
		DataSegment<Name, Data> newSeg = new DataSegment<>(this, blockID);
		if (readDataBody) {
			newSeg.readData(); // Read full data body
			storedData.put(name, newSeg);
		} else {
			newSeg.readSegment(); // Read only size information
		}
		return newSeg;
	}
	
	private synchronized Data internalPut(Name name, Data data, boolean returnOldValue) {
		DataSegment<Name, Data> seg = internalGetDataSegment(name, returnOldValue);

		// No action needed: null->null
		if (seg == null && data == null) return null;
		
		// Create new
		if (seg == null) {
			seg = new DataSegment<>(this, Segment.UNALLOCATED);
			seg.dataObject = data;
			seg.writeData(); // Will assign a new block ID
			storedNames.put(name, seg.blockIDStart);
			storedData.put(name, seg);
			return null;
		}
		
		Data oldData = seg.dataObject;

		if (data == null) {
			// Remove existing
			seg.free();
			storedNames.remove(name);
			storedData.remove(name);
		} else {
			// Edit existing
			seg.dataObject = data;
			seg.writeData();
			storedNames.put(name, seg.blockIDStart);
			storedData.put(name, seg);
		}
		
		if (returnOldValue)	return oldData;
		else return null;
	}
	
	public boolean has(Name name) {
		DataSegment<Name, Data> seg = internalGetDataSegment(name, false);
		return seg != null;
	}
	
	public Data get(Name name) {
		DataSegment<Name, Data> seg = internalGetDataSegment(name, true);
		if (seg == null) return null;
		return seg.dataObject;
	}
	
	public Data put(Name name, Data data) {
		return internalPut(name, data, true);
	}
	
	public void set(Name name, Data data) {
		internalPut(name, data, false);
	}
	
	public Map<Name, Integer> debugGetStoredNames() {
		return storedNames;
	}

	protected long seekBlock(int blockID, int offset) throws IOException {
		final long pos = blockOffset(blockID) + offset;
	
		// No seek needed
		final long old = randomAccessFile.getFilePointer();
		if (pos == old) return old;

		if (pos > randomAccessFile.length()) {
			throw new RuntimeException("WARNING: seek beyond file length!");
//			System.out.println("WARNING: seek beyond file length!");
//			StackTraceElement[] elements = Thread.currentThread().getStackTrace();
//			for (int i = 1; i < elements.length; i++) {
//				System.out.println("   at "+elements[i]);
//			}
		}
		//System.out.println("Seek "+pos+"["+Long.toHexString(pos)+"] (block="+blockID+" offset="+offset+")");
		
		// Regular seek
		randomAccessFile.seek(pos);
		return old;
	}
	
	public static void fillArray(byte[] array, byte value, int length) {
		if (length <= 0) return;
		if (length > array.length) length = array.length;
		
		int upTo16 = Math.min(length, 16);
		
		int i;
		for (i = 0; i < upTo16; i++) {
			array[i] = value;
		}
		for (; i < length; i += i) {
			System.arraycopy(array, 0, array, i, (length - i < i) ? (length - i) : i);
		}
	}

	protected int numBlocks(long bytes) {
		long numBlocks = ((bytes - 1) / SIZE_BLOCK) + 1;
		if (numBlocks > Integer.MAX_VALUE) throw new RuntimeException("Block offset too large!");
		return (int) numBlocks;
	}
	
	protected int numBlocks(int bytes) {
		return ((bytes - 1) / SIZE_BLOCK) + 1;
	}
	
	protected long blockOffset(int blockID) {
		return (long) blockID * SIZE_BLOCK;
	}
	
	protected Name readName() throws IOException {
		randomAccessFile.read(buffer, 0, SIZE_NAME);
		return nameReader.read(new ByteArrayInputStream(buffer));
	}

	protected void skipName() throws IOException {
		randomAccessFile.skipBytes(SIZE_NAME);
	}
	
	protected void writeName(Name name) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream(SIZE_NAME);
		nameWriter.write(baos, name);
		if (baos.size() > SIZE_NAME) throw new RuntimeException("nameWriter wrote name that is larger than SIZE_NAME! ("+name+" -> "+Arrays.toString(baos.toByteArray())+")");
		randomAccessFile.write(baos.toByteArray(), 0, baos.size());
	}

	protected void writeBlankName() throws IOException {
		for (int i = 0; i < SIZE_NAME; i++) {
			randomAccessFile.write(0);
		}
	}

	protected int readSegmentHeader() throws IOException {
		return randomAccessFile.readInt();
	}
	
	protected void writeSegmentHeader(int size) throws IOException {
		randomAccessFile.writeInt(size);
	}

	protected int readBlockID() throws IOException {
		return randomAccessFile.readInt();
	}
	
	protected void writeBlockID(int size) throws IOException {
		randomAccessFile.writeInt(size);
	}
	
	protected void writeDataHeader(byte b) throws IOException {
		randomAccessFile.writeByte(b);
	}

	public byte readDataHeader() throws IOException {
		return randomAccessFile.readByte();
	}

	protected byte[] getDataArray(Data data, CompressionMethod compression) throws IOException {
		final ByteArrayOutputStream baos = new ByteArrayOutputStream(SIZE_BLOCK);
		try (final BufferedOutputStream bos = new BufferedOutputStream(compression.getCompressionStream(baos))) {
			dataWriter.write(bos, data);
		}
		
		final byte[] bytes = baos.toByteArray();
		if (debug && debugVerbosity >= 2) {
			final String md5 = MD5Hash.hash(bytes);
			System.out.println("Wrote "+bytes.length+" bytes of data (MD5="+md5+")");
		}
		
		return bytes;
	}

	public void writeDataArray(byte[] bytes) throws IOException {
		randomAccessFile.write(bytes, 0, bytes.length);
	}

	protected Data readData(int size, CompressionMethod compression) throws IOException {
		final ByteArrayOutputStream baos = new ByteArrayOutputStream(size);
		
		int copied = 0;
		while (copied < size) {
			int copySize = Math.min(BUFFER_SIZE, size - copied);
			randomAccessFile.read(buffer, 0, copySize);
			baos.write(buffer, 0, copySize);
			copied += copySize;
		}

		final byte[] bytes = baos.toByteArray();
		if (debug && debugVerbosity >= 2) {
			final String md5 = MD5Hash.hash(bytes);
			System.out.println("Read "+bytes.length+" bytes of data (MD5="+md5+")");
		}
		
		final ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
		final InputStream decompressed = compression.getDecompressionStream(bais);
		final BufferedInputStream bis = new BufferedInputStream(decompressed);
		
		return dataReader.read(bis);
	}

	protected void markBlocksFree(int startBlocksFreed, int endBlocksFreed) throws IOException {
		if (debug) {
			System.out.println("Freed blocks "+startBlocksFreed+"-"+endBlocksFreed);
		}
		
		for (int block = startBlocksFreed; block <= endBlocksFreed; block++) {
			seekBlock(block, 0);
			randomAccessFile.writeInt(0);
			freeBlocks.add(block);
		}
	}
	
	/**
	 * <p>Attempts to claim the blocks from start to end (inclusive).</p>
	 * <p>If successful, true is returned and it is assumed that these
	 * blocks will be immediately consumed. They will be removed from
	 * the free blocks set.</p>
	 * <p>If unsuccessful, false is returned and no changes are made.</p>
	 * @param start
	 * @param end
	 * @return
	 * @throws IOException 
	 */
	protected boolean tryClaim(int start, int end) throws IOException {
		if (freeBlocks == null) {
			grow(blockOffset(end) + SIZE_BLOCK);
			if (debug && debugVerbosity >= 0) System.out.println("Claimed blocks "+start+"-"+end);
			return true;
		}
		
		final int lastBlock = numBlocks(randomAccessFile.length()) - 1;
		
		// We do not really need to claim block past the end
		if (start > lastBlock) {
			if (start > lastBlock+1) throw new RuntimeException("Attempted claim is more than one SIZE_BLOCK past the end of the file!");
			grow(blockOffset(end) + SIZE_BLOCK);
			if (debug && debugVerbosity >= 0) System.out.println("Claimed blocks "+start+"-"+end);
			return true; // Padding will allow writing starting at block 'start'
		}

		// Regular claim: remove blocks from free block set
		int searchEnd = (end > lastBlock) ? lastBlock : end;
		NavigableSet<Integer> subset = freeBlocks.subSet(start, true, searchEnd, true);
		if (subset.size() < searchEnd-start+1) return false;
		subset.clear();
		grow(blockOffset(end) + SIZE_BLOCK);
		if (debug && debugVerbosity >= 0) System.out.println("Claimed blocks "+start+"-"+end);
		return true;
	}

	private void grow(long minSize) throws IOException {
		if (randomAccessFile.length() >= minSize) return;
		randomAccessFile.setLength(minSize);
	}

	/**
	 * Searches for and returns the block index at which a new claim
	 * of {@link blockCount} blocks can be made. All blocks included
	 * in the claim will be removed from the free blocks set.
	 * @throws IOException 
	 */
	protected int getClaim(int blockCount) throws IOException {
		if (debug && debugVerbosity >= 0) System.out.println("Asking for claim of "+blockCount+" blocks");
		
		if (freeBlocks == null) {
			tryClaim(0, blockCount-1);
			return 0;
		}
		
		// Find a range that's big enough
		Iterator<Integer> iter = freeBlocks.iterator();
		int rangeStart = 0, rangeLength = -1, expected = 0;
		while (iter.hasNext()) {
			final int freeBlock = iter.next();
			if (freeBlock == expected) {
				rangeLength++;
				expected++;
			} else {
				rangeStart = freeBlock;
				rangeLength = 1;
				expected = freeBlock+1;
			}
			
			// Big enough range found?
			if (rangeLength >= blockCount) break;
		}
		
		if (rangeLength < blockCount) {
			// If there were no free blocks, rangeStart is at end of file
			rangeStart = numBlocks(randomAccessFile.length());
			if (debug && debugVerbosity >= 0) System.out.println("Allocated at end of file");
		}

		boolean success = tryClaim(rangeStart, rangeStart+blockCount-1);
		if (!success) throw new RuntimeException("Could not claim allocated region: "+rangeStart+"-"+(rangeStart+blockCount-1));
		return rangeStart;
		
	}

	/**
	 * @param blockIDStart
	 * @param newClaim
	 * @param size
	 * @throws IOException 
	 */
	protected void copyBlock(int blockFrom, int blockTo, int size) throws IOException {
		int copied = 0;
		while (copied < size) {
			final int copySize = Math.min(BUFFER_SIZE, size-copied);
			seekBlock(blockFrom, copied);
			randomAccessFile.read(buffer, 0, copySize);
			seekBlock(blockTo, copied);
			randomAccessFile.write(buffer, 0, copySize);
			copied += copySize;
		}
	}

	public long getFileLength() throws IOException {
		return randomAccessFile.length();
	}
	
}
