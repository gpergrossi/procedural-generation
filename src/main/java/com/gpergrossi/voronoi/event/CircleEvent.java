package com.gpergrossi.voronoi.event;

import com.gpergrossi.util.geom.vectors.Double2D;
import com.gpergrossi.voronoi.shoreline.Arc;

public class CircleEvent implements VoronoiEvent {

	public final Arc arc;
	public final Double2D center;
	public final double radius;
	public final Double2D eventPoint;
	private boolean valid;
	
	public CircleEvent(Arc arc, Double2D center, double radius) {
		this.arc = arc;
		this.radius = radius;
		this.center = center;
		this.eventPoint = new Double2D(center.x(), center.y() + radius);
		this.valid = true;
	}
	
	@Override
	public VoronoiEventType getType() {
		return VoronoiEventType.CIRCLE;
	}
	
	@Override
	public Double2D getEventPoint() {
		return eventPoint;
	}
	
	public void markInvalid() {
		this.valid = false;
	}
	
	public boolean isValid() {
		return valid;
	}

	@Override
	public String toString() {
		return "CircleEvent[LeftBP=" + arc.getLeftBreakpoint().orElse(null) + ", RightBP=" + arc.getRightBreakpoint().orElse(null) + "]";
	}
	
}