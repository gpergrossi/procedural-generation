package com.gpergrossi.voronoi.shoreline;

import java.util.Optional;

import com.gpergrossi.util.geom.vectors.Double2D;
import com.gpergrossi.voronoi.event.CircleEvent;

public class PartialEdge {

	public final Breakpoint breakpointA;
	public final Breakpoint breakpointB;
	
	protected Double2D endpointA;
	protected Double2D endpointB;
	
	public PartialEdge(CircleEvent start, Breakpoint breakpoint) {
		this.breakpointA = null;
		this.breakpointB = breakpoint;

		this.endpointA = start.center;
	}
	
	public PartialEdge(Breakpoint left, Breakpoint right) {
		this.breakpointA = left;
		this.breakpointB = right;
	}
	
	public void addEndpoint(CircleEvent circleEvent) {
		Optional<Breakpoint> left = circleEvent.arc.getLeftBreakpoint();
		Optional<Breakpoint> right = circleEvent.arc.getRightBreakpoint();
		
		// Make sure circle event matches one of the breakpoints in this edge
		boolean circleEventMatchesA = false;
		boolean circleEventMatchesB = false;
		if (left.isPresent()) {
			if (left.get() == this.breakpointA) circleEventMatchesA = true;
			else if (left.get() == this.breakpointB) circleEventMatchesB = true;
		}
		if (right.isPresent()) {
			if (right.get() == this.breakpointA) circleEventMatchesA = true;
			else if (right.get() == this.breakpointB) circleEventMatchesB = true;
		}

		// Close out the correct endpoint
		if (circleEventMatchesA) {
			if (this.endpointA != null) throw new IllegalStateException("Endpoint A is already set!");
			this.endpointA = circleEvent.center;
		} else if (circleEventMatchesB) {
			if (this.endpointB != null) throw new IllegalStateException("Endpoint B is already set!");
			this.endpointB = circleEvent.center;
		} else {
			throw new IllegalArgumentException("Circle event is not related to this edge!");
		}
	}
	
	
}
