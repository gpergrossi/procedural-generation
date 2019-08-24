package com.gpergrossi.voronoi.math;

public class Linear extends Quadratic {

	protected Linear(double b, double c) {
		super(0.0, b, c);
	}
	
	@Override
	public IntersectionResult zeros() {
		return IntersectionResult.zeros(this, -this.c / this.b);
	}
	
	@Override
	public double apply(double x) {
		return this.b * x + this.c;
	}

}
