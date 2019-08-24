package com.gpergrossi.voronoi.graph;

import com.gpergrossi.util.geom.vectors.Double2D;

public class Site {

	protected final int siteID;
	protected final Double2D point;
	
	public Site(int siteID, Double2D point) {
		this.siteID = siteID;
		this.point = point.immutable();
	}
	
	public int getID() {
		return siteID;
	}

	public double x() {
		return point.x();
	}
	
	public double y() {
		return point.y();
	}
	
	public Double2D getPoint() {
		return point;
	}
	
	@Override
	public String toString() {
		return "Site[x=" + point.x() + ", y=" + point.y() + "]";
	}
	
}
