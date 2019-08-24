package com.gpergrossi.voronoi.shoreline;

import java.util.Optional;

import com.gpergrossi.util.geom.vectors.Double2D;
import com.gpergrossi.voronoi.graph.Site;
import com.gpergrossi.voronoi.math.VoronoiUtils;

public class Breakpoint {

	private Arc left;
	private Arc right;
	
	private double lastLocationProgress = Double.NaN;
	private Double2D lastLocation = null;

	protected Shoreline.Entry shorelineEntry;
	protected PartialEdge partialEdge;
	
	public Breakpoint(Arc left, Arc right) {
		this.left = left;
		this.right = right;
	}
	
	protected void setLeftArc(Arc left) {
		this.left = left;
	}
	
	public Arc getLeftArc() {
		return left;
	}
	
	public Site getLeftSite() {
		return left.getSite();
	}

	protected void setRightArc(Arc right) {
		this.right = right;
	}
	
	public Arc getRightArc() {
		return right;
	}
	
	public Site getRightSite() {
		return right.getSite();
	}
	
	public void setEdge(PartialEdge edge) {
		if (edge == null) throw new IllegalArgumentException("Cannot assign a null edge!");
		if (this.partialEdge != null) throw new IllegalStateException("This breakpoint is already assigned to a different edge!");
		this.partialEdge = edge;
	}
	
	public PartialEdge getEdge() {
		return this.partialEdge;
	}
	
	@Override
	public String toString() {
		return "Breakpoint[" + left.getSite().getID() + "," + right.getSite().getID() + "]";
	}
	
	/** 
	 * Computes the location of the breakpoint as the intersection between two parabolas.
	 * Each parabola is defined to be equidistant from one of the sites and the sweepline.
	 * 
	 * Contains lots of checks for rounding errors and edge cases.
	 * 
	 * @param sweepline - sweepline used to generate parabolas
	 * @return location of the breakpoint
	 */
	public Double2D computeLocation(Sweepline sweepline) {
		// Save cached result
		if (Double.isNaN(lastLocationProgress) || lastLocationProgress != sweepline.getProgress()) {
			lastLocationProgress = sweepline.getProgress();
			lastLocation = this.computeLocationInternal(sweepline);
		}
		return lastLocation;
	}
	
	private Double2D computeLocationInternal(Sweepline sweepline) {
		final boolean leftIsProxy = (this.left.getSite() instanceof ProxySite);
		final boolean rightIsProxy = (this.right.getSite() instanceof ProxySite);
		if (leftIsProxy && rightIsProxy) {
			throw new IllegalStateException("A proxy site arc cannot intersect another proxy site arc!");
		}
		
		Optional<Double2D> breakpointLocation = VoronoiUtils.computeBreakpoint(sweepline.getProgress(), this.left, this.right);
		if (!breakpointLocation.isPresent()) {
			throw new IllegalStateException("Request breakpoint does not exist!");
		}
		return breakpointLocation.get();
	}
	
	
}
