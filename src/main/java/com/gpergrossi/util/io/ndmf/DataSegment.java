package com.gpergrossi.util.io.ndmf;

import java.io.IOException;

public class DataSegment<Name, Data> extends Segment<Name, Data> {

	protected Data dataObject;
	
	public DataSegment(NamedDataMapFile<Name, Data> ndmFile, int blockIDStart) {
		super(ndmFile, blockIDStart);
		this.copyOnResize = false;
	}

	public void writeData() {
		try {
			if (dataObject == null) throw new RuntimeException("Cannot write null data!");
			
			final CompressionMethod compression = CompressionMethod.ZLIB;
			final byte[] bytes = ndmFile.getDataArray(dataObject, compression);
			
			// Reallocate if necessary
			final int newSize = bytes.length + ndmFile.SIZE_SEGMENT_HEADER + ndmFile.SIZE_DATA_HEADER;
			this.resize(newSize, true);
			
			// Write data
			ndmFile.seekBlock(this.blockIDStart, 0);
			ndmFile.writeSegmentHeader(newSize);
			ndmFile.writeDataHeader(compression.getCompressionID());
			ndmFile.writeDataArray(bytes);
			
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public void readData() {
		try {
			
			ndmFile.seekBlock(this.blockIDStart, 0);
			this.size = ndmFile.readSegmentHeader();
			final byte compressionID = ndmFile.readDataHeader();
			final CompressionMethod compression = CompressionMethod.fromID(compressionID);
			
			final int readSize = this.size - ndmFile.SIZE_SEGMENT_HEADER - ndmFile.SIZE_DATA_HEADER;
			this.dataObject = ndmFile.readData(readSize, compression);
			
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
}
