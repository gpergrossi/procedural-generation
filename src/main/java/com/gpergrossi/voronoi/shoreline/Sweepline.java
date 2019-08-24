package com.gpergrossi.voronoi.shoreline;

import java.util.Comparator;

import com.gpergrossi.util.geom.vectors.Double2D;

/**
 * <p>
 * A sweepline is an infinite line that moves in a direction perpendicular to its extent. It divides the plane
 * of sites into points that have been incorporated into the voronoi diagram, and points that have not. It will 
 * move forward through the sites and circle events one by one, including more points into the diagram each step 
 * until all points have been included and the diagram is complete. It has three important features:
 * </p><ol>
 * <li>  Ordering points according to its direction of movement (always Y+)</li>
 * <li>  Keeping track of its own forward progress</li></ol>
 * <p>
 * Imagine a horizontal line that moves up (Y+) selecting points at lower Y coordinates first.
 * </p>
 */
public class Sweepline implements Comparator<Double2D> {

	private boolean initialized;
	private double progress;
	
	/**
	 * Creates a sweepline that moves in the Y+ direction.
	 */
	public Sweepline() {
		Comparator.comparing(Double2D::y).thenComparing(Double2D::x);
		this.progress = Double.NEGATIVE_INFINITY;
		this.initialized = false;
	}

	public boolean isInitialized() {
		return initialized;
	}
	
	/**
	 * Advances progress to the next point. Fails if the point provided is already behind the sweepline.
	 * @param point - next (or first) point to which the sweepline should advance
	 */
	public void advance(Double2D point) {
		this.initialized = true;
		
		// Progress is represented as a Y coordinate moving up
		if (this.progress <= point.y()) {
			this.progress = point.y();
		} else {
			throw new IllegalStateException("Cannot advance backwards");
		}
	}
	
	public double getProgress() {
		return progress;
	}

	@Override
	public int compare(Double2D a, Double2D b) {
		return (int) Math.signum(a.y() - b.y());
	}
	
}
