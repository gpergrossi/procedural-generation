package com.gpergrossi.voronoi.math;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.gpergrossi.util.geom.vectors.Double2D;

public class IntersectionResult {
	
	private static final List<Double2D> EMPTY_LIST = Collections.unmodifiableList(new ArrayList<>(0));
	
	private static List<Double2D> coordsToDouble2DList(double[] coords) {
		if (coords.length % 2 != 0) throw new IllegalArgumentException("Expected even number of coords!");
		final int numIntersections = (coords.length / 2);
		List<Double2D> list;
		if (numIntersections == 0) {
			list = EMPTY_LIST;
		} else {
			list = new ArrayList<>(numIntersections);
			for (int i = 0; i < coords.length; i += 2) {
				final Double2D intersection = new Double2D(coords[i + 0], coords[i + 1]);
				list.add(intersection);
			}
			list = Collections.unmodifiableList(list);
		}
		return list;
	}
	
	private static List<Double2D> zerosToDouble2DList(double[] xCoords) {
		final int numIntersections = xCoords.length;
		List<Double2D> list;
		if (numIntersections == 0) {
			list = EMPTY_LIST;
		} else {
			list = new ArrayList<>(numIntersections);
			for (int i = 0; i < xCoords.length; i++) {
				final Double2D intersection = new Double2D(xCoords[i], 0);
				list.add(intersection);
			}
			list = Collections.unmodifiableList(list);
		}
		return list;
	}
	
	public static IntersectionResult infinite(Function functionA, Function functionB) {
		return new IntersectionResult(functionA, functionB, true, EMPTY_LIST);
	}
	
	public static IntersectionResult empty(Function functionA, Function functionB) {
		return new IntersectionResult(functionA, functionB, false, EMPTY_LIST);
	}
	
	public static IntersectionResult of(Function functionA, Function functionB, double... coords) {
		return new IntersectionResult(functionA, functionB, false, coordsToDouble2DList(coords));
	}
	
	public static IntersectionResult infiniteZeros(Function function) {
		return new IntersectionResult(function, HorizontalLine.ZERO_LINE, true, EMPTY_LIST);	
	}
	
	public static IntersectionResult emptyZeros(Function function, double... xCoords) {
		return new IntersectionResult(function, HorizontalLine.ZERO_LINE, false, EMPTY_LIST);
	}
	
	public static IntersectionResult zeros(Function function, double... xCoords) {
		return new IntersectionResult(function, HorizontalLine.ZERO_LINE, false, zerosToDouble2DList(xCoords));
	}
	
	private final Function functionA;
	private final Function functionB;
	
	private final boolean hasInfiniteOverlap;
	private final List<Double2D> intersections;
	
	private IntersectionResult(Function functionA, Function functionB, boolean identical, List<Double2D> immutablePts) {
		this.functionA = functionA;
		this.functionB = functionB;
		if (identical) {
			this.hasInfiniteOverlap = true;
			this.intersections = null;
		} else {
			this.hasInfiniteOverlap = false;
			this.intersections = immutablePts;
		}
	}

	public Function getFunctionA() {
		return functionA;
	}
	
	public Function getFunctionB() {
		return functionB;
	}
	
	public boolean hasIntersections() {
		return (this.intersections == null) || (this.intersections.size() > 0);
	}
	
	public boolean hasInfiniteOverlap() {
		return hasInfiniteOverlap;
	}
	
	public List<Double2D> getIntersections() {
		if (hasInfiniteOverlap) throw new UnsupportedOperationException("Method not supported on results with infinite overlap!");
		return this.intersections;
	}

	public IntersectionResult convertZerosToIntersections(Function functionA, Function functionB) {
		if (this.functionB != HorizontalLine.ZERO_LINE) throw new IllegalStateException("This IntersectionResult does not represent zeros!");
		if (this.hasInfiniteOverlap) {
			return new IntersectionResult(functionA, functionB, true, EMPTY_LIST);
		} else {
			List<Double2D> newList = new ArrayList<>(this.intersections.size());
			for (Double2D pt : this.intersections) {
				final double x = pt.x();
				final double y = functionA.apply(x);
				Double2D newPt = new Double2D(x, y);
				newList.add(newPt);
			}
			newList = Collections.unmodifiableList(newList);
			return new IntersectionResult(functionA, functionB, false, newList);
		}
	}

}
