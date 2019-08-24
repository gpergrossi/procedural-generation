package com.gpergrossi.voronoi.math;

public class VerticalLine extends Function {

	private double x;
	
	public VerticalLine(double x) {
		this.x = x;
	}

	public double x() {
		return x;
	}

	@Override
	public IntersectionResult intersect(Function other) {
		if (other instanceof Quadratic) {
			return ((Quadratic) other).intersect(this);
		} else if (other instanceof VerticalLine) {
			return this.intersect((VerticalLine) other);
		}
		return null;
	}
	
	public IntersectionResult intersect(VerticalLine other) {
		if (VoronoiUtils.nearlyEqual(this.x, other.x)) {
			return IntersectionResult.infinite(this, other);
		} else {
			return IntersectionResult.empty(this, other);
		}
	}
	
	@Override
	public IntersectionResult zeros() {
		return IntersectionResult.zeros(this, x);
	}

	@Override
	public double apply(double x) {
		return Double.NaN;
	}
	
}
