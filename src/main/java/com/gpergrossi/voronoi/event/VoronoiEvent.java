package com.gpergrossi.voronoi.event;

import com.gpergrossi.util.geom.vectors.Double2D;

public interface VoronoiEvent {
	
	public VoronoiEventType getType();
	
	public Double2D getEventPoint();
	
}
