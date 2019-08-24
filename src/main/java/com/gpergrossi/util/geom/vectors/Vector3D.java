package com.gpergrossi.util.geom.vectors;

public interface Vector3D<T extends Vector3D<T>> extends Vector<T> {

	public T cross(T vector);
	
	public T rotate(T axis, double angle);
	
}
