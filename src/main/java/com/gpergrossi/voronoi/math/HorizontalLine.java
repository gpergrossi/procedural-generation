package com.gpergrossi.voronoi.math;

public class HorizontalLine extends Quadratic {
	
	public static final HorizontalLine ZERO_LINE = new HorizontalLine(0);
	
	public HorizontalLine(double y) {
		super(0, 0, y);
	}
	
	@Override
	public IntersectionResult zeros() {
		if (VoronoiUtils.nearlyEqual(this.c, 0.0)) {
			return IntersectionResult.infiniteZeros(this);
		} else {
			return IntersectionResult.emptyZeros(this);
		}
	}
	
	@Override
	public double apply(double x) {
		return this.c;
	}
	
}
