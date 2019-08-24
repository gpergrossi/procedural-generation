package com.gpergrossi.voronoi.math;

public abstract class Function {
	
	public abstract double apply(double x);
	
	public abstract IntersectionResult intersect(Function other);
	public abstract IntersectionResult zeros();
	
}
