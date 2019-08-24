package com.gpergrossi.util.hash;

public class XXHash32 {

	private static int PRIME_1 = -1640531535;
	private static int PRIME_2 = -2048144777;
	private static int PRIME_3 = -1028477379;
	private static int PRIME_4 =   668265263;
	private static int PRIME_5 =   374761393;

	/**
	 * Unsafe, only used within this class. Reads bytes from a byte array beginning at offset.
	 * Combines into Little Endian integer.
	 * @param bytes
	 * @param offset
	 * @return
	 */
	private static int readInt(byte[] bytes, int offset) {
		return bytes[offset] + (bytes[offset+1] << 8) + (bytes[offset+2] << 16) + (bytes[offset+3] << 24);
	}
	
	/**
	 * Adds a lane value to an accumulator. This operation comes in 3 flavors.
	 * Pre-multiplies the lane value by PRIME_2, Rotates left by 13 bits, Post-multiplies the accumulator by PRIME_1.
	 * @param accumulator
	 * @param lane
	 * @return new accumulator value
	 */
	public static int accumulateA(int accumulator, final int lane) {
		accumulator += lane * PRIME_2;
		accumulator = Integer.rotateLeft(accumulator, 13);
		accumulator *= PRIME_1;
		return accumulator;
	}
	
	/**
	 * Adds a lane value to an accumulator. This operation comes in 3 flavors.
	 * Pre-multiplies the lane value by PRIME_3, Rotates left by 17 bits, Post-multiplies the accumulator by PRIME_4.
	 * @param accumulator
	 * @param lane
	 * @return new accumulator value
	 */
	public static int accumulateB(int accumulator, final int lane) {
		accumulator += lane * PRIME_3;
		accumulator = Integer.rotateLeft(accumulator, 17);
		accumulator *= PRIME_4;
		return accumulator;
	}
	
	/**
	 * Adds a lane value to an accumulator. This operation comes in 3 flavors.
	 * Pre-multiplies the lane value by PRIME_5, Rotates left by 11 bits, Post-multiplies the accumulator by PRIME_1.
	 * @param accumulator
	 * @param lane
	 * @return new accumulator value
	 */
	public static int accumulateC(int accumulator, final int lane) {
		accumulator += lane * PRIME_5;
		accumulator = Integer.rotateLeft(accumulator, 11);
		accumulator *= PRIME_1;
		return accumulator;
	}

	/**
	 * Converge 4 32-bit accumulators into a single 32-bit accumulator
	 * @param acc0
	 * @param acc1
	 * @param acc2
	 * @param acc3
	 * @return A combined accumulator value
	 */
	public static int converge(int acc0, int acc1, int acc2, int acc3) {
		return Integer.rotateLeft(acc0, 1) + Integer.rotateLeft(acc1, 7) + Integer.rotateLeft(acc2, 12) + Integer.rotateLeft(acc3, 18);
	}
	
	/**
	 * Stirs up the bits in an accumulator so that each of the 4 bytes has a presence in the other 3 bytes.
	 * @param acc
	 * @return An avalanched accumulator value
	 */
	public static int avalanche(int acc) {
		acc ^= (acc >>> 15);
		acc *= PRIME_2;
		acc ^= (acc >>> 13);
		acc *= PRIME_3;
		acc ^= (acc >>> 16);
		return acc;
	}
	
	public static int hashBytes(final int seed, final byte[] bytes, final int offset, final int length) {
		final int end = offset + length;
		int readIndex = offset;
		int accumulator;
		
		if (length < 16) {
			// The input data is fewer than 16 bytes: use a simple formula to initialize acc
			
			// Step 1. Initialize internal accumulators (Skip to step 4)
			accumulator = seed + PRIME_5;
			
		} else {
			// When the input data is large enough, it is processed in 16-byte stripes.
			
			// Step 1. Initialize internal accumulators
			int accumulatorLane0 = seed + PRIME_1 + PRIME_2;
			int accumulatorLane1 = seed + PRIME_2;
			int accumulatorLane2 = seed;
			int accumulatorLane3 = seed - PRIME_1;
			
			// Step 2. Process stripes
			//   A stripe of 16 bytes is consumed each round until the remaining data is smaller than 16 bytes.
			//   During this phase, 4 accumulators are used to collect and mix data from each of 4 lanes (4 bytes each).
			final int endStripes = end - 16;
			for (; readIndex <= endStripes; readIndex += 16) {
				accumulatorLane0 = accumulateA(accumulatorLane0, readInt(bytes, readIndex));
				accumulatorLane1 = accumulateA(accumulatorLane1, readInt(bytes, readIndex+4));
				accumulatorLane2 = accumulateA(accumulatorLane2, readInt(bytes, readIndex+8));
				accumulatorLane3 = accumulateA(accumulatorLane3, readInt(bytes, readIndex+16));
			}
			
			// Step 3. Accumulator convergence
			accumulator = converge(accumulatorLane0, accumulatorLane1, accumulatorLane2, accumulatorLane3);
		}
		
		// Step 4. Add input length
		accumulator += length;
		
		// Step 5. Consume remaining input
		final int endInts = end - 4;
		for (; readIndex <= endInts; readIndex += 4) {
		    accumulator = accumulateB(accumulator, readInt(bytes, readIndex));
		}
		if (readIndex < length) {
		    accumulator = accumulateC(accumulator, (int) bytes[readIndex]);
		}
		
		// Step 6. Final mix (avalanche)
		accumulator = avalanche(accumulator);
		
		// Result is a 32 bit hash
		return accumulator;
	}
	
	public static int hashInts(final int seed, final int[] integers, final int offset, final int length) {
		final int end = offset + length;
		int readIndex = offset;
		int accumulator;
		
		if (length < 4) {
			// The input data is fewer than 16 bytes: use a simple formula to initialize acc
			
			// Step 1. Initialize internal accumulators (Skip to step 4)
			accumulator = seed + PRIME_5;
			
		} else {
			// When the input data is large enough, it is processed in 16-byte stripes.
			
			// Step 1. Initialize internal accumulators
			int accumulatorLane0 = seed + PRIME_1 + PRIME_2;
			int accumulatorLane1 = seed + PRIME_2;
			int accumulatorLane2 = seed;
			int accumulatorLane3 = seed - PRIME_1;
			
			// Step 2. Process stripes
			//   A stripe of 16 bytes is consumed each round until the remaining data is smaller than 16 bytes.
			//   During this phase, 4 accumulators are used to collect and mix data from each of 4 lanes (4 bytes each).
			final int endStripes = end - 4;
			for (; readIndex <= endStripes; readIndex += 4) {	
				accumulatorLane0 = accumulateA(accumulatorLane0, integers[readIndex]);
				accumulatorLane1 = accumulateA(accumulatorLane1, integers[readIndex+1]);
				accumulatorLane2 = accumulateA(accumulatorLane2, integers[readIndex+2]);
				accumulatorLane3 = accumulateA(accumulatorLane3, integers[readIndex+3]);
			}
			
			// Step 3. Accumulator convergence
			//   Once the remaining data has been reduce to fewer than 16 bytes, a different approach is used to consume the rest.
			//   The new approach uses only 1 accumulator, so the previous 4 accumulators need to be combined.
			accumulator = converge(accumulatorLane0, accumulatorLane1, accumulatorLane2, accumulatorLane3);
		}
		
		// Step 4. Add input length
		accumulator += length*4;
		
		// Step 5. Consume remaining input
		for (; readIndex < end; readIndex++) {
		    accumulator = accumulateB(accumulator, integers[readIndex]);
		}
		
		// Step 6. Final mix (avalanche)
		accumulator = avalanche(accumulator);
		
		// Result is a 32 bit hash
		return accumulator;
	}
	
	public static int hashInts(final int seed, final int int0, final int int1, final int int2, final int int3, final int int4, final int int5) {
		int accumulator;
					
		// Step 1. Initialize internal accumulators
		int accumulatorLane0 = seed + PRIME_1 + PRIME_2;
		int accumulatorLane1 = seed + PRIME_2;
		int accumulatorLane2 = seed;
		int accumulatorLane3 = seed - PRIME_1;
			
		// Step 2. Process stripes
		accumulatorLane0 += int0 * PRIME_2;
		accumulatorLane0 = Integer.rotateLeft(accumulatorLane0, 13);
		accumulatorLane0 *= PRIME_1;
		
		accumulatorLane1 += int1 * PRIME_2;
		accumulatorLane1 = Integer.rotateLeft(accumulatorLane1, 13);
		accumulatorLane1 *= PRIME_1;
		
		accumulatorLane2 += int2 * PRIME_2;
		accumulatorLane2 = Integer.rotateLeft(accumulatorLane2, 13);
		accumulatorLane2 *= PRIME_1;
		
		accumulatorLane3 += int3 * PRIME_2;
		accumulatorLane3 = Integer.rotateLeft(accumulatorLane3, 13);
		accumulatorLane3 *= PRIME_1;
		
		// Step 3. Accumulator convergence
		accumulator = Integer.rotateLeft(accumulatorLane0, 1) + Integer.rotateLeft(accumulatorLane1, 7) + Integer.rotateLeft(accumulatorLane2, 12) + Integer.rotateLeft(accumulatorLane3, 18);
		
		// Step 4. Add input length
		accumulator += 16;

		// Step 5. Consume remaining input
		accumulator += int4 * PRIME_3;
		accumulator = Integer.rotateLeft(accumulator, 17);
		accumulator *= PRIME_4;
		
		accumulator += int5 * PRIME_3;
		accumulator = Integer.rotateLeft(accumulator, 17);
		accumulator *= PRIME_4;
		
		// Step 6. Final mix (avalanche)
		accumulator ^= (accumulator >>> 15);
		accumulator *= PRIME_2;
		accumulator ^= (accumulator >>> 13);
		accumulator *= PRIME_3;
		accumulator ^= (accumulator >>> 16);
		
		// Result is a 32 bit hash
		return accumulator;
	}
	
	public static int hashInts(final int seed, final int int0, final int int1, final int int2, final int int3) {
		int accumulator;
					
		// Step 1. Initialize internal accumulators
		int accumulatorLane0 = seed + PRIME_1 + PRIME_2;
		int accumulatorLane1 = seed + PRIME_2;
		int accumulatorLane2 = seed;
		int accumulatorLane3 = seed - PRIME_1;
			
		// Step 2. Process stripes
		accumulatorLane0 += int0 * PRIME_2;
		accumulatorLane0 = Integer.rotateLeft(accumulatorLane0, 13);
		accumulatorLane0 *= PRIME_1;
		
		accumulatorLane1 += int1 * PRIME_2;
		accumulatorLane1 = Integer.rotateLeft(accumulatorLane1, 13);
		accumulatorLane1 *= PRIME_1;
		
		accumulatorLane2 += int2 * PRIME_2;
		accumulatorLane2 = Integer.rotateLeft(accumulatorLane2, 13);
		accumulatorLane2 *= PRIME_1;
		
		accumulatorLane3 += int3 * PRIME_2;
		accumulatorLane3 = Integer.rotateLeft(accumulatorLane3, 13);
		accumulatorLane3 *= PRIME_1;
		
		// Step 3. Accumulator convergence
		accumulator = Integer.rotateLeft(accumulatorLane0, 1) + Integer.rotateLeft(accumulatorLane1, 7) + Integer.rotateLeft(accumulatorLane2, 12) + Integer.rotateLeft(accumulatorLane3, 18);
		
		// Step 4. Add input length
		accumulator += 16;

		// Step 5. Consume remaining input
		// None
		
		// Step 6. Final mix (avalanche)
		accumulator ^= (accumulator >>> 15);
		accumulator *= PRIME_2;
		accumulator ^= (accumulator >>> 13);
		accumulator *= PRIME_3;
		accumulator ^= (accumulator >>> 16);
		
		// Result is a 32 bit hash
		return accumulator;
	}
	
	public static int hashInts(final int seed, final int int0, final int int1, final int int2) {
		// Step 1. Initialize internal accumulators (Skip to step 4)
		int accumulator = seed + PRIME_5;
		
		// Step 4. Add input length
		accumulator += 12;
		
		// Step 5. Consume remaining input
		accumulator += int0 * PRIME_5;
		accumulator = Integer.rotateLeft(accumulator, 11);
		accumulator *= PRIME_1;
		
		accumulator += int1 * PRIME_5;
		accumulator = Integer.rotateLeft(accumulator, 11);
		accumulator *= PRIME_1;
		
		accumulator += int2 * PRIME_5;
		accumulator = Integer.rotateLeft(accumulator, 11);
		accumulator *= PRIME_1;
		
		// Step 6. Final mix (avalanche)
		accumulator ^= (accumulator >>> 15);
		accumulator *= PRIME_2;
		accumulator ^= (accumulator >>> 13);
		accumulator *= PRIME_3;
		accumulator ^= (accumulator >>> 16);
		
		// Result is a 32 bit hash
		return accumulator;
	}
	
	public static int hashInts(final int seed, final int int0, final int int1) {
		// Step 1. Initialize internal accumulators (Skip to step 4)
		int accumulator = seed + PRIME_5;
		
		// Step 4. Add input length
		accumulator += 12;
		
		// Step 5. Consume remaining input
		accumulator += int0 * PRIME_5;
		accumulator = Integer.rotateLeft(accumulator, 11);
		accumulator *= PRIME_1;
		
		accumulator += int1 * PRIME_5;
		accumulator = Integer.rotateLeft(accumulator, 11);
		accumulator *= PRIME_1;
		
		// Step 6. Final mix (avalanche)
		accumulator ^= (accumulator >>> 15);
		accumulator *= PRIME_2;
		accumulator ^= (accumulator >>> 13);
		accumulator *= PRIME_3;
		accumulator ^= (accumulator >>> 16);
		
		// Result is a 32 bit hash
		return accumulator;
	}
	
	public static int hashInts(final int seed, final int int0) {
		// Step 1. Initialize internal accumulators (Skip to step 4)
		int accumulator = seed + PRIME_5;
		
		// Step 4. Add input length
		accumulator += 12;
		
		// Step 5. Consume remaining input
		accumulator += int0 * PRIME_5;
		accumulator = Integer.rotateLeft(accumulator, 11);
		accumulator *= PRIME_1;
		
		// Step 6. Final mix (avalanche)
		accumulator ^= (accumulator >>> 15);
		accumulator *= PRIME_2;
		accumulator ^= (accumulator >>> 13);
		accumulator *= PRIME_3;
		accumulator ^= (accumulator >>> 16);
		
		// Result is a 32 bit hash
		return accumulator;
	}
	
	public static int hashLongs(final int seed, final long long0, final long long1) {
		return hashInts(seed, (int) (long0 >> 32), (int) long0, (int) (long1 >> 32), (int) long1);
	}
	
	public static int hashLongs(final int seed, final long long0, final long long1, final long long2) {
		return hashInts(seed, (int) (long0 >> 32), (int) long0, (int) (long1 >> 32), (int) long1, (int) (long2 >> 32), (int) long2);
	}
	
}
