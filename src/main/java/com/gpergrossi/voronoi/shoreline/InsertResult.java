package com.gpergrossi.voronoi.shoreline;

public class InsertResult {
	
	private final boolean error;
	private final String errorMessage;
	
	private final boolean firstArc;
	
	public final Arc leftArc;
	public final Breakpoint leftBreakpoint;
	public final Arc newArc;
	public final Breakpoint rightBreakpoint;
	public final Arc rightArc;
	
	public final Arc oldArc;
	
	/**
	 * Construct an insert result for the first arc added to the shoreline
	 * @param arc
	 * @return
	 */
	public static InsertResult createFirstArcResult(Arc arc) {
		return new InsertResult(arc);
	}
	
	public static InsertResult createNormalResult(Arc oldArc, Arc leftArc, Breakpoint leftBreakpoint, Arc newArc, Breakpoint rightBreakpoint, Arc rightArc) {
		return new InsertResult(oldArc, leftArc, leftBreakpoint, newArc, rightBreakpoint, rightArc);
	}
	
	public static InsertResult createErrorResult(String message) {
		return new InsertResult(message);
	}

	private InsertResult(String errorMessage) {
		this.error = true;
		this.errorMessage = errorMessage;
		
		this.leftArc = null;
		this.leftBreakpoint = null;
		this.newArc = null;
		this.rightBreakpoint = null;
		this.rightArc = null;
		
		this.oldArc = null;
		this.firstArc = false;
	}
	
	private InsertResult(Arc firstArc) {
		this.error = false;
		this.errorMessage = null;
		
		this.firstArc = true;
		
		this.leftArc = null;
		this.leftBreakpoint = null;
		this.newArc = firstArc;
		this.rightBreakpoint = null;
		this.rightArc = null;
		
		this.oldArc = null;
	}
	
	private InsertResult(Arc oldArc, Arc leftArc, Breakpoint leftBreakpoint, Arc newArc, Breakpoint rightBreakpoint, Arc rightArc) {
		this.error = false;
		this.errorMessage = null;
		
		this.firstArc = false;
		
		this.leftArc = leftArc;
		this.leftBreakpoint = leftBreakpoint;
		this.newArc = newArc;
		this.rightBreakpoint = rightBreakpoint;
		this.rightArc = rightArc;
		
		this.oldArc = oldArc;
	}
	
	public boolean isError() {
		return error;
	}
	
	public String getErrorMessage() {
		return errorMessage;
	}

	public boolean isFirstArc() {
		return firstArc;
	}
	
}
