package com.gpergrossi.util.geom.ranges;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Random;

import com.gpergrossi.util.geom.vectors.Double1D;

public class Double1DRange {

	public static final Double1DRange EMPTY = new Double1DRange(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY);
	public static final Double1DRange ALL = new Double1DRange(-Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
	
	public final double min;
	public final double max;
	public final double range;

	public Double1DRange(Double1DRange range) {
		this(range.min, range.max);
	}
	
	public Double1DRange(double min, double max) {
		this.min = min;
		this.max = max;
		this.range = Math.max(max-min, 0);
	}

	protected Double1DRange resize(double min, double max) {
		return new Double1DRange(min, max);
	}
	
	public double range() {
		return range;
	}
	
	public boolean isEmpty() {
		if (Double.isNaN(range)) return true;
		return range <= 0;
	}
	
	public boolean contains(double value) {
		return (value >= min && value <= max);
	}
	
	public boolean onBorder(double value, double borderPadding) {
		return contains(value) && (value < min+borderPadding || value > max-borderPadding);
	}
	
	public Double1DRange grow(double padMin, double padMax) {
		return resize(min-padMin, max+padMax);
	}
	
	public Double1DRange grow(double padding) {
		return grow(padding, padding);
	}
	
	public Double1DRange shrink(double insetMin, double insetMax) {
		return grow(-insetMin, -insetMax);
	}
	
	public Double1DRange shrink(double inset) {
		return grow(-inset, -inset);
	}
	
	public Double1DRange intersect(Double1DRange other) {
		double min = Math.max(this.min, other.min);
		double max = Math.min(this.max, other.max);
		return resize(min, max);
	}

	public Double1DRange offset(double offset) {
		return resize(min+offset, max+offset);
	}

	public double random(Random random) {
		return random.nextDouble()*range+min;
	}
	
	@Override
	public String toString() {
		if (this.isEmpty()) return "Empty";
		if (min == max) return String.valueOf(min);
		return min+" to "+max;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Double1DRange)) return false;
		Double1DRange range = (Double1DRange) obj;
		if (range.isEmpty() && this.isEmpty()) return true;
		if (range.min != this.min) return false;
		if (range.max != this.max) return false;
		return true;
	}
	
	public Iterable<Double1D.Mutable> iterableMutable(double step) {
		if (!Double.isFinite(range)) throw new RuntimeException("Range is non-finite! Cannot iterate!");
		return new Iterable<Double1D.Mutable>() {
			@Override
			public Iterator<Double1D.Mutable> iterator() {
				return new Iterator<Double1D.Mutable>() {
					private Double1D.Mutable mutable = new Double1D.Mutable(min);
					
					@Override
					public boolean hasNext() {
						return mutable.x()+step <= max;
					}

					@Override
					public Double1D.Mutable next() {
						if (!hasNext()) throw new NoSuchElementException();
						mutable.x(mutable.x() + step);
						return mutable;
					}
				};	
			}
		};
	}
	
	public Iterable<Double1D> iterable(double step) {
		if (!Double.isFinite(range)) throw new RuntimeException("Range is non-finite! Cannot iterate!");
		return new Iterable<Double1D>() {
			@Override
			public Iterator<Double1D> iterator() {
				return new Iterator<Double1D>() {
					private double value = min;
					
					@Override
					public boolean hasNext() {
						return value+step <= max;
					}

					@Override
					public Double1D next() {
						if (!hasNext()) throw new NoSuchElementException();
						value += step;
						return new Double1D(value);
					}
				};	
			}
		};
	}
	
}
