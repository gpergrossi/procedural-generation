package com.gpergrossi.util.geom.vectors;

public interface Vector2D<T extends Vector2D<T>> extends Vector<T> {
	
	public double cross(T vector);
	public double angle();
	
	/**
	 * Effective rotation of 90 degrees
	 * @return
	 */
	public T perpendicular();
	public T rotate(double angle);
	
}
