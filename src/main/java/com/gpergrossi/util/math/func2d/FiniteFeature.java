package com.gpergrossi.util.math.func2d;

import com.gpergrossi.util.geom.ranges.Int2DRange;

public class FiniteFeature implements Function2D {

	Int2DRange.Floats feature;
	
	float radius;

	public FiniteFeature(Int2DRange.Floats details) {
		this.feature = details;
		
		float dx = details.width / 2.0f;
		float dy = details.height / 2.0f;
		this.radius = (float) Math.sqrt(dx*dx + dy*dy);
	}
	
	@Override
	public double getValue(double x, double y) {
		return feature.lerp((float) x, (float) y, 0);
	}
	
	public float getCenterX() {
		return (feature.maxX + feature.minX) / 2.0f;
	}
	
	public float getCenterY() {
		return (feature.maxY + feature.minY) / 2.0f;
	}
	
	public int getWidth() {
		return feature.width;
	}
	
	public int getHeight() {
		return feature.height;
	}
	
	public float getRadius() {
		return radius;
	}
	
}
