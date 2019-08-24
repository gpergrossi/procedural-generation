package com.gpergrossi.util.geom.ranges;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.TreeMap;

import com.gpergrossi.util.data.queue.PriorityMultiQueue;
import com.gpergrossi.util.data.queue.ReadOnlyQueue;

import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.Random;

public class Int1DMultiRange {

	private static final Comparator<Entry<Integer, Int1DRange>> KEY_COMPARATOR = (ca, cb) -> (ca.getKey() - cb.getKey());
	
	public static Int1DMultiRange union(Int1DMultiRange a, Int1DMultiRange b) {
		if (a.isEmpty()) return new Int1DMultiRange(b);
		if (b.isEmpty()) return new Int1DMultiRange(a);
		
		PriorityMultiQueue<Entry<Integer, Int1DRange>> queue = new PriorityMultiQueue<>(KEY_COMPARATOR);
		queue.addQueue(new ReadOnlyQueue<>(a.rangesMap.entrySet()));
		queue.addQueue(new ReadOnlyQueue<>(b.rangesMap.entrySet()));
		
		Int1DMultiRange result = new Int1DMultiRange();
		queue.forEach(t -> result.addRange(t.getValue()));
		
		return result;
	}
	
	public static Int1DMultiRange intersection(Int1DMultiRange a, Int1DMultiRange b) {
		if (a.isEmpty() || b.isEmpty()) return new Int1DMultiRange(Int1DRange.EMPTY);
		
		Int1DMultiRange result = new Int1DMultiRange(a);
		result.removeRanges(b.compliment());
		
		return result;
	}
	
	/**
	 * <p>This operation is similar to 1-dimensional convolution in mathematics with a few differences.</p>
	 * 
	 * <p><h1>Difference 1: Multiranges are not functions</h1>
	 * Instead, consider any multirange as being a discreet function (I.E. defined only on integer values)
	 * which has a boolean value at any integer 'x'. The discreet, boolean-valued function associated with
	 * an Int1DMultiRange is defined as:
	 * <pre>boolean f(int x) = multirange.contains(x)</pre></p>
	 * 
	 * <p><h1>Difference 2: The functions are boolean values rather than scalar</h1>
	 * A typical convolution on discreet functions can be thought of as a "sliding dot product" in which
	 * one of the two functions has been reversed. However, since the input functions and output function 
	 * of this operation are boolean-valued, this operation uses a "<i>boolean dot product</i>".<br/><br/>
	 * 
	 * The <i>boolean dot product</i> This is like a regular dot product except that same-indexed values
	 * are ANDed together (instead of multiplied) and the intermediate results are ORed together (instead
	 * of added). The result ends up being the same as checking for an intersection between the two sets 
	 * of integers (again, sets of integers and discreet, boolean-valued functions are being treated as 
	 * interchangeable)</p>
	 * 
	 * <p><h1>Difference 3: Int1DMultiRanges have a finite domain intended to represent an infinite domain</h1>
	 * Int1DMultiRanges are limited to the finite domain between Integer.MIN_VALUE and Integer.MAX_VALUE.
	 * However, they are most useful when we pretend that they represent an infinite domain (any possible
	 * integer value, not just the ones Java allows).<br/><br/>
	 * 
	 * Therefore, shifting an Int1DMultiRange follows a "carry-in" policy. If an Int1DMultiRange containing
	 * Integer.MIN_VALUE is shifted to the right, the undefined values less than Integer.MIN_VALUE are 
	 * assumed to also be in the set. The same is true for values greater than Integer.MAX_VALUE which 
	 * are left-shifted in on a set containing Integer.MAX_VALUE. Finally, reversing an Int1DMultiRange
	 * containing Integer.MAX_VALUE (2^31 - 1) carries in Integer.MIN_VALUE (-2^31).</p>
	 * 
	 * <br/><p>
	 * This operation is commutative, associative, and distributive (wherein addition is equivalent to a union).<br/>
	 * Time scales with N*M where N and M are the number of continuous ranges in A and B, respectively.
	 * </p>
	 * 
	 * @return
	 */
	public static Int1DMultiRange convolve(Int1DMultiRange a, Int1DMultiRange b) {
		if (a.isEmpty() || b.isEmpty()) return new Int1DMultiRange(Int1DRange.EMPTY);
		Int1DMultiRange result = new Int1DMultiRange();
		for (Int1DRange rangeA : a.getRanges()) {
			for (Int1DRange rangeB : b.getRanges()) {
				result.addRange(convolve(rangeA, rangeB));
			}
		}
		return result;
	}
	
	/**
	 * The effective convolution of A and B ends up being:
	 * <pre>result.min = (A.min + B.min); 
	 * result.max = (A.max + B.max);</pre>
	 * with some special cases for Integer.MIN_VALUE and Integer.MAX_VALUE.
	 * @param a
	 * @param b
	 * @return
	 */
	private static Int1DRange convolve(Int1DRange a, Int1DRange b) {
		long min = (long) a.min + b.min;
		long max = (long) a.max + b.max;
		if (min < Integer.MIN_VALUE || a.min == Integer.MIN_VALUE || b.min == Integer.MIN_VALUE) min = Integer.MIN_VALUE;
		if (max > Integer.MAX_VALUE || a.max == Integer.MAX_VALUE || b.max == Integer.MAX_VALUE) max = Integer.MAX_VALUE;
		return new Int1DRange((int) min, (int) max);
	}
	
	private static Int1DRange reverse(Int1DRange range) {
		int resultMin = -range.max;
		int resultMax = -range.min;
		if (range.max == Integer.MAX_VALUE) {
			resultMin = Integer.MIN_VALUE;
		}
		if (range.min == Integer.MIN_VALUE) {
			resultMax = Integer.MAX_VALUE;
		}
		return new Int1DRange(resultMin, resultMax);
	}
	
	
	private long size;
	private TreeMap<Integer, Int1DRange> rangesMap;
	
	public Int1DMultiRange() {
		this.size = 0;
		this.rangesMap = new TreeMap<>();
	}
	
	public Int1DMultiRange(Int1DMultiRange multirange) {
		this();
		this.size = multirange.size;
		this.rangesMap = new TreeMap<>(multirange.rangesMap);
	}
	
	public Int1DMultiRange(int min, int max) {
		this(new Int1DRange(min, max));
	}
	
	public Int1DMultiRange(Int1DRange... ranges) {
		this();
		for (Int1DRange range : ranges) {
			addRange(range);
		}
	}
	
	public Collection<Int1DRange> getRanges() {
		return rangesMap.values();
	}
	
	public Optional<Int1DRange> asSimpleRange() {
		if (rangesMap.size() == 1) {
			return Optional.of(rangesMap.firstEntry().getValue());
		} else {
			return Optional.empty();
		}
	}

	public void addRange(int min, int max) {
		addRange(new Int1DRange(min, max));
	}
	
	public void addRange(Int1DRange range) {
		if (range.isEmpty()) return;
		
		// Extend range to include ranges overlapping/adjacent
		final Int1DRange extended = this.getExtendedRange(range);

		// Remove all ranges in the extended range
		removeRange(extended);
		
		// Add the extended range back
		rangesMap.put(extended.min, extended);
		this.size += extended.size();
	}
	
	public void removeRange(int min, int max) {
		removeRange(new Int1DRange(min, max));
	}
	
	public void removeRange(Int1DRange range) {
		if (range.isEmpty() || this.isEmpty()) return;
		
		// Extend range to include ranges overlapping/adjacent
		final Int1DRange extended = this.getExtendedRange(range);

		// Remove all ranges in the extended range
		final NavigableMap<Integer, Int1DRange> removeSubMap = rangesMap.subMap(extended.min, true, extended.max, true);
		final Iterator<Entry<Integer, Int1DRange>> removeIterator = removeSubMap.entrySet().iterator();
		while (removeIterator.hasNext()) {
			final Entry<Integer, Int1DRange> entry = removeIterator.next();
			this.size -= entry.getValue().size();
			removeIterator.remove();
		}
		
		// Add the cut-offs back
		if (extended.min < range.min) addRange(new Int1DRange(extended.min, range.min-1));
		if (extended.max > range.max) addRange(new Int1DRange(range.max+1, extended.max));
	}
	
	public void removeRanges(Int1DMultiRange b) {
		for (Int1DRange range : b.getRanges()) {
			this.removeRange(range);
		}
	}
	
	/**
	 * Returns an extended range which include ranges in this multirange object
	 * that may be inclusive-of or adjacent-to the min and max of the provided range. 
	 * @param range - range to extend
	 * @return a new range including overlapping/adjacent ranges from this multirange object
	 */
	private Int1DRange getExtendedRange(Int1DRange range) {
		int min = range.min;
		int max = range.max;
		
		// Look for an overlapping/adjacent range to min
		if (min > Integer.MIN_VALUE) {
			Entry<Integer, Int1DRange> lowEntry = rangesMap.floorEntry(min-1); // -1 gets adjacency
			if (lowEntry != null) {
				final Int1DRange low = lowEntry.getValue();
				if (low.contains(min-1)) min = low.min;
			}
		}

		// Look for an overlapping/adjacent range to max
		if (max < Integer.MAX_VALUE) {
			Entry<Integer, Int1DRange> highEntry = rangesMap.floorEntry(max+1); // +1 gets adjacency
			if (highEntry != null) {
				final Int1DRange high = highEntry.getValue();
				if (high.contains(max+1)) max = high.max;
			}
		}
		
		return new Int1DRange(min, max);
	}
	
	/**
	 * Returns a multirange containing all integers NOT contained by this multirange.
	 */
	public Int1DMultiRange compliment() {
		Int1DMultiRange result = new Int1DMultiRange(Int1DRange.ALL);
		for (Int1DRange range : this.getRanges()) {
			result.removeRange(range);
		}
		return result;
	}
	
	/**
	 * Returns a new multirange which has the values in this multirange mirrored over zero.
	 * This can be though of as "multiply all values by -1". There is one distinction though:
	 * If this set contains Integer.MAX_VALUE (2^31-1) the reversed set will "carry-in"
	 * Integer.MIN_VALUE (-2^31).
	 * @return
	 */
	public Int1DMultiRange reverse() {
		Int1DMultiRange result = new Int1DMultiRange(Int1DRange.EMPTY);
		for (Int1DRange range : this.getRanges()) {
			result.addRange(reverse(range));
		}
		return result;
	}
	
	public Int1DMultiRange union(Int1DMultiRange other) {
		return union(this, other);
	}
	
	public Int1DMultiRange intersect(Int1DMultiRange other) {
		return intersection(this, other);
	}

	public Int1DMultiRange convolve(Int1DMultiRange other) {
		return convolve(this, other);
	}
	
	public long size() {
		return size;
	}
	
	public boolean isEmpty() {
		return size == 0;
	}
	
	public boolean contains(int value) {
		final Entry<Integer, Int1DRange> entry = rangesMap.floorEntry(value);
		if (entry == null) return false;
		final Int1DRange range = entry.getValue();
		return range.contains(value);
	}
	
	/**
	 * Checks if this multirange contains the value provided.<br/>
	 * If the value provided is less than Integer.MIN_VALUE, it will be looked up as Integer.MIN_VALUE.<br/>
	 * If the value provided is greater than Integer.MAX_VALUE, it will be looked up as Integer.MAX_VALUE.<br/>
	 * In this way, the Int1DMultiRange will behave as if it contains all values below Integer.MIN_VALUE if it
	 * contains Integer.MIN_VALUE and all values above Integer.MAX_VALUE if it contains Integer.MAX_VALUE.
	 * @param value
	 * @return
	 */
	public boolean contains(long value) {
		return contains((int) Math.min(Math.max(value, Integer.MIN_VALUE), Integer.MAX_VALUE));
	}
	
	public int valueFor(long index) {
		for (Int1DRange range : rangesMap.values()) {
			if (index >= range.size()) {
				index -= range.size();
				continue;
			}
			return range.valueFor(index);
		}
		throw new IndexOutOfBoundsException();
	}
	
	public int random(Random random) {
		long index;
		if (this.size < Integer.MAX_VALUE) {
			index = random.nextInt((int) size);
		} else {
			long rollLong = random.nextLong() & Long.MAX_VALUE;
			index = rollLong % size;
		}
		return valueFor(index);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Int1DMultiRange)) return false;
		Int1DMultiRange other = (Int1DMultiRange) obj;
		return this.rangesMap.equals(other.rangesMap);
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append('{');
		Iterator<Int1DRange> iter = getRanges().iterator();
		while (iter.hasNext()) {
			sb.append(iter.next());
			if (iter.hasNext()) sb.append(", ");
		}
		sb.append('}');
		return sb.toString();
	}
	
//	public Int1DMultiRange intersect(Int1DMultiRange other) {
//		int min = Math.max(this.min, other.min);
//		int max = Math.min(this.max, other.max);
//		return resize(min, max);
//	}
//	
//	public int indexFor(int value) {
//		return value-min;
//	}
//
//	public int random(Random random) {
//		return random.nextInt(max-min+1)+min;
//	}
//	
//	public Iterable<Int1D.WithIndex> getAllMutable() {
//		return new Iterable<Int1D.WithIndex>() {
//			@Override
//			public Iterator<Int1D.WithIndex> iterator() {
//				return new Iterator<Int1D.WithIndex>() {
//					private Int1D.WithIndex mutable = new Int1D.WithIndex(Int1DMultiRange.this, 0, 0);
//					private int index = 0;
//					
//					@Override
//					public boolean hasNext() {
//						return index < size();
//					}
//
//					@Override
//					public Int1D.WithIndex next() {
//						if (!hasNext()) throw new NoSuchElementException();
//						mutable.x(index+min);
//						mutable.index = index;
//						index++;
//						return mutable;
//					}
//				};	
//			}
//		};
//	}
//	
//	public Iterable<Int1D.WithIndex> getAll() {
//		return new Iterable<Int1D.WithIndex>() {
//			@Override
//			public Iterator<Int1D.WithIndex> iterator() {
//				return new Iterator<Int1D.WithIndex>() {
//					private int index = 0;
//					
//					@Override
//					public boolean hasNext() {
//						return index < size();
//					}
//
//					@Override
//					public Int1D.WithIndex next() {
//						if (!hasNext()) throw new NoSuchElementException();
//						Int1D.WithIndex result = new Int1D.WithIndex(Int1DMultiRange.this, index+min, index);
//						index++;
//						return result;
//					}
//				};	
//			}
//		};
//	}
	
}
