package com.gpergrossi.voronoi.shoreline;

public class RemoveResult {
	
	public final Arc oldArc;
	public final Breakpoint oldLeftBreakpoint;
	public final Breakpoint oldRightBreakpoint;
	public final Breakpoint newBreakpoint;
	
	public RemoveResult(Arc oldArc, Breakpoint oldLeftBreakpoint, Breakpoint oldRightBreakpoint, Breakpoint newBreakpoint) {
		this.oldArc = oldArc;
		this.oldLeftBreakpoint = oldLeftBreakpoint;
		this.oldRightBreakpoint = oldRightBreakpoint;
		this.newBreakpoint = newBreakpoint;
	}
	
}
