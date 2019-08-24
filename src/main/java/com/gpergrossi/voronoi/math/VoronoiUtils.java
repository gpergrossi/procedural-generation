package com.gpergrossi.voronoi.math;

import java.util.List;
import java.util.Optional;

import com.gpergrossi.util.geom.vectors.Double2D;
import com.gpergrossi.voronoi.shoreline.Arc;

public class VoronoiUtils {

	public static final double EPSILON = 0.000000001;
	
	public static boolean nearlyEqual(double a, double b) {
		return nearlyEqual(a, b, EPSILON);
	}
	
	public static boolean nearlyEqual(double a, double b, double epsilon) {
		final double absA = Math.abs(a);
		final double absB = Math.abs(b);
		final double diff = Math.abs(a - b);

		if (a == b) { 
			// shortcut, handles infinities
			return true;
		} else if (a == 0 || b == 0 || diff < Double.MIN_NORMAL) {
			// a or b is zero or both are extremely close to it
			// relative error is less meaningful here
			return diff < (epsilon * Double.MIN_NORMAL);
		} else { 
			// use relative error
			return diff / Math.min((absA + absB), Double.MAX_VALUE) < epsilon;
		}
	}
	
	public static boolean nearlyEqual(Double2D a, Double2D b) {
		return nearlyEqual(a.x(), b.x(), EPSILON) || nearlyEqual(a.y(), b.y(), EPSILON);
	}
	
	public static boolean nearlyEqual(Double2D a, Double2D b, double epsilon) {
		return nearlyEqual(a.x(), b.x(), epsilon) || nearlyEqual(a.y(), b.y(), epsilon);
	}
	
	public static Optional<Double2D> computeBreakpoint(double sweeplineY, Arc leftArc, Arc rightArc) {
		final Function leftArcFunction = leftArc.computeParabola(sweeplineY);
		final Function rightArcFunction = rightArc.computeParabola(sweeplineY);
		final IntersectionResult result = leftArcFunction.intersect(rightArcFunction);
		
		// Zero or Infinite overlaps handled trivially:
		if (result.hasInfiniteOverlap()) {
			throw new IllegalStateException("Infinite overlap! Arcs identical?");
		} else if (!result.hasIntersections()) {
			return Optional.empty();
		}
		
		final List<Double2D> intersections = result.getIntersections();
		final boolean aIsVertical = (result.getFunctionA() instanceof VerticalLine);
		final boolean bIsVertical = (result.getFunctionB() instanceof VerticalLine);
		
		// Special case: vertical line
		// Verify that only one function was vertical and there is only one intersection
		// Accept the intersection regardless of which arc is left or right
		if (aIsVertical || bIsVertical) {
			if (aIsVertical && bIsVertical) throw new IllegalStateException("Only one function should be a vertical line!");
			if (intersections.size() != 1) throw new IllegalStateException("Expected a single intersection!");
			return Optional.of(intersections.get(0));
		}
		
		// Special case: single intersection
		// Verify sites have same y coordinate
		// The intersection is only a valid breakpoint if the left and right arcs are in the correct order
		if (intersections.size() == 1) {
			final double leftY = leftArc.getSite().y();
			final double rightY = rightArc.getSite().y();
			if (!nearlyEqual(leftY, rightY)) throw new IllegalStateException("Expected sites at same Y coordinate.");
			
			final double leftX = leftArc.getSite().x();
			final double rightX = rightArc.getSite().x();
			if (leftX < rightX) {
				return Optional.of(intersections.get(0));
			} else {
				return Optional.empty();
			}
		}
		
		// Normal case: two intersections
		// Select intersection corresponding to the requested breakpoint (left arc on the left, right on the right)
		// This depends on which arc is closer to the sweepline
		final double leftDeltaY = Math.abs(leftArc.getSite().y() - sweeplineY);
		final double rightDeltaY = Math.abs(rightArc.getSite().y() - sweeplineY);
		if (nearlyEqual(leftDeltaY, rightDeltaY)) {
			throw new IllegalStateException("Parabola intersect twice, but sites have same Y coordinate!");
		} else if (leftDeltaY > rightDeltaY) {
			return Optional.of(intersections.get(0));
		} else {
			return Optional.of(intersections.get(1));
		}
	}
	
	public static boolean areConvergent(Double2D left, Double2D middle, Double2D right) {
		final double dx0 = right.x() - middle.x();
		final double dy0 = right.y() - middle.y();
		
		final double dx1 = left.x() - middle.x();
		final double dy1 = left.y() - middle.y();
		
		final double cross = (dx0*dy1) - (dy0*dx1);
		
		// Not convergent if cross product is nearly 0 or negative
		if (cross < 0 || nearlyEqual(cross, 0)) {
			return false;
		}
		
		return true;
	}
	
}
