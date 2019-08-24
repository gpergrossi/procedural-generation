package com.gpergrossi.util.io.ndmf;

import java.io.IOException;
import java.util.Comparator;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;

import com.gpergrossi.util.data.queue.PriorityMultiQueue;
import com.gpergrossi.util.data.queue.ReadOnlyQueue;
import com.gpergrossi.util.geom.vectors.Int2D;

public class NDMFVerifier {

	public static <Name, Data> boolean verifyFormat(NamedDataMapFile<Name, Data> ndmf, boolean verbose) throws IOException {
		System.out.println("----- VERIFYING FORMAT -----");
		
		Map<Integer, String> blockMessages = new TreeMap<>();
		Map<Integer, String> blockErrors = new TreeMap<>();
		
		// Map from expected index blockID to the indexBlock that referenced them
		Map<Integer, Integer> indexSegments = new TreeMap<>();
		indexSegments.put(0, null);
		int numIndexBlocks = 0;
		int highestExpected = 0;
		
		// Map from expected data blockIDs to the (indexBlock, slot) pair that referenced them
		Map<Integer, Int2D> dataSegments = new TreeMap<>();
		int numDataBlocks = 0;

		Integer blockOn = 0, previousBlock = null;
		while (blockOn != null) {

			final long offset = ndmf.blockOffset(blockOn);
			if (offset >= ndmf.getFileLength()) {
				String error = "Unexpected EOF! Expected block "+blockOn+" to exist\n"
						+ "  Index block "+indexSegments.get(highestExpected)+" links to next block at "+highestExpected;
				String previousError = blockErrors.get(blockOn);
				error = (previousError == null) ? error : previousError+"\n"+error; 
				blockErrors.put(blockOn, error);
				break;
			}
			
			try {
				ndmf.seekBlock(blockOn, 0);
			} catch (IOException e) {
				throw new IOException("Failed to seek to block "+blockOn+". Linked from index block "+previousBlock, e);
			}
			int size = ndmf.readSegmentHeader();
			
			if (size < 0) {
				String error = "Invalid Size! Block "+blockOn+" has a size of "+size+"!";
				String previousError = blockErrors.get(blockOn);
				error = (previousError == null) ? error : previousError+"\n"+error; 
				blockErrors.put(blockOn, error);
				break;
			}
			if (size == 0) {
				String error = "Missing Block! Block "+blockOn+" has a size of 0, but is supposed to be an index block!\n"
						+ "  Index block "+previousBlock+" links to next block at "+blockOn;
				String previousError = blockErrors.get(blockOn);
				error = (previousError == null) ? error : previousError+"\n"+error; 
				blockErrors.put(blockOn, error);
				break;
			}
			
			Integer link = null;
			int numSlots = (size - ndmf.SIZE_SEGMENT_HEADER) / ndmf.SIZE_INDEX_ENTRY;
			int numUsed = 0, numEmpty = 0;
			for (int slot = 0; slot < numSlots; slot++) {
				int blockReference = ndmf.readBlockID();
				ndmf.skipName();
				
				if (blockReference == 0) {
					numEmpty++;
					continue;
				}
				
				if (blockReference < 0) {
					if (slot == ndmf.MAX_INDEX_ENTRY_SLOTS-1) {
						highestExpected = Math.max(highestExpected, blockReference);
						link = -blockReference;
						indexSegments.put(link, blockOn);
					} else {
						String error = "Bad Index Entry! Block "+blockOn+" slot "+slot+" has a negative reference ("+blockReference+") but is not the last slot!";
						String previousError = blockErrors.get(blockOn);
						error = (previousError == null) ? error : previousError+"\n"+error; 
						blockErrors.put(blockOn, error);
					}
					continue;
				}
				
				numUsed++;
				dataSegments.put(blockReference, new Int2D(blockOn, slot));
				highestExpected = Math.max(highestExpected, blockReference);
			}
			
			int numBlocks = ndmf.numBlocks(size);
			String description;
			if (numBlocks == 1) {
				description = "Block  "+blockOn+": Index block. "+numSlots+" entries: "+numUsed+" used, "+numEmpty+" empty";
				if (link != null) description += ", link="+link;
				description += ". Next block: "+(blockOn+numBlocks);
			} else {
				description = "Blocks "+blockOn+"-"+(blockOn+numBlocks-1)+": Index block. "+numSlots+" entries: "+numUsed+" used, "+numEmpty+" empty";
				if (link != null) description += ", link="+link;
				description += ". Next block: "+(blockOn+numBlocks);
			}
			blockMessages.put(blockOn, description);
			
			numIndexBlocks += numBlocks;
			previousBlock = blockOn;
			blockOn = link;
		}
		
		int numDataSegments = 0;
		int numOrphanBlocks = 0;
		Set<Integer> freeBlocks = new TreeSet<>();
		
		Integer previousBlockID = 0;
		Integer previousBlockSize = null;
		
		blockOn = 0;
		while (blockOn <= highestExpected) {
			
			final long offset = ndmf.blockOffset(blockOn);
			if (offset >= ndmf.getFileLength()) {
				String error = "Unexpected EOF! Expected block "+highestExpected+" to exist\n";
				if (indexSegments.containsKey(highestExpected)) {
					error += "  Index block "+indexSegments.get(highestExpected)+" links to next block at "+highestExpected;
				} else if (dataSegments.containsKey(highestExpected)) {
					Int2D reference = dataSegments.get(highestExpected);
					error += "  Index block "+reference.x()+" slot "+reference.y()+" references data block at "+highestExpected;
				}
				String previousError = blockErrors.get(blockOn);
				error = (previousError == null) ? error : previousError+"\n"+error; 
				blockErrors.put(blockOn, error);
				break;
			}
			
			ndmf.seekBlock(blockOn, 0);
			int size = ndmf.readSegmentHeader();
			previousBlockID = blockOn;
			previousBlockSize = size;
			
			if (indexSegments.containsKey(blockOn)) {				
				blockOn = blockOn + ndmf.numBlocks(size);
				continue;
			}
			
			if (size < 0) {
				String error = "Negative Block Size! Block "+blockOn+" has a size of "+size;
				String previousError = blockErrors.get(blockOn);
				error = (previousError == null) ? error : previousError+"\n"+error; 
				blockErrors.put(blockOn, error);
				blockOn++;
				continue;
			}
			
			if (dataSegments.containsKey(blockOn)) {
				Int2D reference = dataSegments.get(blockOn);
				dataSegments.remove(blockOn);
				
				if (size == 0) {
					String error = "Missing Block! Block "+blockOn+" has a size of 0, but is supposed to be a data block!\n"
							+ "  Index block "+reference.x()+" slot "+reference.y()+" references a data block at "+blockOn;
					String previousError = blockErrors.get(blockOn);
					error = (previousError == null) ? error : previousError+"\n"+error; 
					blockErrors.put(blockOn, error);
					blockOn++;
					continue;
				}
				
				int numBlocks = ndmf.numBlocks(size);
				String description;
				if (numBlocks == 1) {
					description = "Block  "+blockOn+": Data block. "+size+" bytes. Next block: "+(blockOn+numBlocks);
				} else {
					description = "Blocks "+blockOn+"-"+(blockOn+numBlocks-1)+": Data block. "+size+" bytes. Next block: "+(blockOn+numBlocks);
				}
				blockMessages.put(blockOn, description);

				numDataSegments++;
				numDataBlocks += numBlocks;
				
				blockOn = blockOn+numBlocks;
				continue;
			}
			
			// Neither an expected index block, nor an expected data block
			if (size != 0) {
				String error = "Orphan Block! Block "+blockOn+" is not referenced by any index segment!";
				String previousError = blockErrors.get(blockOn);
				error = (previousError == null) ? error : previousError+"\n"+error; 
				blockErrors.put(blockOn, error);
				numOrphanBlocks++;
				
				blockOn = blockOn + ndmf.numBlocks(size);
				continue;
			}

			// Size = 0			
			freeBlocks.add(blockOn);
			blockOn++;
		}

		final long offset = ndmf.blockOffset(blockOn);
		if (offset > ndmf.getFileLength()) {
			String error = ("Unexpected EOF! Last block in file was Block "+previousBlockID+" with a size of "+previousBlockSize);
			blockErrors.put(blockOn, error);
		}
		
		for (Entry<Integer, Int2D> entry : dataSegments.entrySet()) {
			String error = "Missing Block! Block "+entry.getKey()+" is referenced but not found!\n"
					+ "  Index block "+entry.getValue().x()+" slot "+entry.getValue().y()+" references a data block at "+entry.getKey();
			String previousError = blockErrors.get(blockOn);
			error = (previousError == null) ? error : previousError+"\n"+error; 
			blockErrors.put(entry.getKey(), error);
		}

		int totalBlocks = freeBlocks.size() + numDataBlocks + numIndexBlocks;
		double percentFree = (freeBlocks.size() * 100.0) / totalBlocks;
		String percent = String.format("%.2f", percentFree);
		
		System.out.println("OVERVIEW:");
		System.out.println(indexSegments.size()+" index segments ("+numIndexBlocks+" blocks) found");
		System.out.println(numDataSegments+" data segments ("+numDataBlocks+" blocks) found");
		System.out.println(freeBlocks.size()+" free blocks found ("+percent+"%)");
		System.out.println("---");
		System.out.println(blockErrors.size()+" total errors");
		System.out.println(dataSegments.size()+" missing data blocks ");
		System.out.println(numOrphanBlocks+" orphans");
		
		if (blockErrors.size() > 0 || verbose) {
			System.out.println();
			System.out.println("=========== MAP ============");
			Queue<Entry<Integer, String>> qMessages = new ReadOnlyQueue<>(blockMessages.entrySet());
			Queue<Entry<Integer, String>> qErrors = new ReadOnlyQueue<>(blockErrors.entrySet());
			PriorityMultiQueue<Entry<Integer, String>> qAll = new PriorityMultiQueue<>(new Comparator<Entry<Integer, String>>() {
				@Override
				public int compare(Entry<Integer, String> o1, Entry<Integer, String> o2) {
					return o1.getKey() - o2.getKey();
				}
			});
			qAll.addQueue(qMessages);
			qAll.addQueue(qErrors);
			
			blockOn = 0;
			
			Integer rangeStart = null;
			Integer lastBlock = null;
			
			while (!qAll.isEmpty()) {
				int entryBlock = qAll.peek().getKey();
				
				if (lastBlock != blockOn) {
					if (freeBlocks.contains(blockOn)) {
						if (rangeStart == null) {
							rangeStart = blockOn;
						}
						freeBlocks.remove(blockOn);
					} else {
						if (rangeStart != null) {
							if (blockOn - rangeStart == 1) {
								System.out.println("Block  "+rangeStart+": Free");
							} else {
								System.out.println("Blocks "+rangeStart+"-"+(blockOn-1)+": Free");
							}
							rangeStart = null;
						}
					}
				}
	
				if (freeBlocks.isEmpty()) {
					blockOn = entryBlock;
				}
				
				lastBlock = blockOn;
						
				if (entryBlock == blockOn) {
					System.out.println(qAll.poll().getValue());
				} else {
					blockOn++;
				}
			}
			
			System.out.println("============================");
		}
		System.out.println("----- END VERIFICATION -----");
		System.out.println();
		
		return blockErrors.size() == 0;
	}
	
}
