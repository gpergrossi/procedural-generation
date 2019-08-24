package com.gpergrossi.voronoi.shoreline;

import java.util.Optional;

import com.gpergrossi.util.geom.shapes.Circle;
import com.gpergrossi.util.geom.vectors.Double2D;
import com.gpergrossi.voronoi.event.CircleEvent;
import com.gpergrossi.voronoi.graph.Site;
import com.gpergrossi.voronoi.math.Function;
import com.gpergrossi.voronoi.math.HorizontalLine;
import com.gpergrossi.voronoi.math.Quadratic;
import com.gpergrossi.voronoi.math.VoronoiUtils;

public class Arc {

	private final Site site;
	private Optional<Breakpoint> left;
	private Optional<Breakpoint> right;
	private Optional<CircleEvent> circleEvent;

	protected Shoreline.Entry shorelineEntry;
	
	private Function lastParabolaFunction;
	private double lastSweeplineY;
	
	public Arc(Site site) {
		this.site = site;
		this.left = Optional.empty();
		this.right = Optional.empty();
		this.circleEvent = Optional.empty();
	}

	@Override
	public String toString() {
		return "Arc[LeftBP=" + getLeftBreakpoint().orElse(null) + ", RightBP=" + getRightBreakpoint().orElse(null) + ", Site=" + this.site + "]" + (circleEvent.isPresent() ? " *"+circleEvent.get().toString() : "");
	}
	
	public Site getSite() {
		return site;
	}

	protected void setLeftBreakpoint(Optional<Breakpoint> left) {
		this.left = left;
	}
	
	public Optional<Breakpoint> getLeftBreakpoint() {
		return left;
	}

	protected void setRightBreakpoint(Optional<Breakpoint> right) {
		this.right = right;
	}
	
	public Optional<Breakpoint> getRightBreakpoint() {
		return right;
	}
	
	public Optional<Circle> computeEventCircle() {		
		final Optional<Breakpoint> leftBP = getLeftBreakpoint();
		final Optional<Breakpoint> rightBP = getRightBreakpoint();
		
		// Event circle only exists if both breakpoints exist 
		if (!left.isPresent() || !right.isPresent()) {
			return Optional.empty();
		}

		final Site leftSite = leftBP.get().getLeftSite();
		final Site middleSite = this.site;
		final Site rightSite = rightBP.get().getRightSite();
		
		final boolean leftIsProxy = (leftSite instanceof ProxySite);
		final boolean middleIsProxy = (middleSite instanceof ProxySite);
		final boolean rightIsProxy = (rightSite instanceof ProxySite);
		
		final Double2D leftSitePoint = leftSite.getPoint();
		final Double2D middleSitePoint = middleSite.getPoint();
		final Double2D rightSitePoint = rightSite.getPoint();
		
		// Special case: proxy sites are handled very differently
		if (leftIsProxy || middleIsProxy || rightIsProxy) {
			if (middleIsProxy && !leftIsProxy && !rightIsProxy) {
				return ProxySite.computeEventCircle(leftSitePoint, (ProxySite) middleSite, rightSitePoint);
			} else if (leftIsProxy && !middleIsProxy && !rightIsProxy) {
				return ProxySite.computeEventCircle((ProxySite) leftSite, middleSitePoint, rightSitePoint);
			} else if (rightIsProxy && !leftIsProxy && !middleIsProxy) {
				return ProxySite.computeEventCircle(leftSitePoint, middleSitePoint, (ProxySite) rightSite);
			} else if (leftIsProxy && rightIsProxy && !middleIsProxy) {
				return Optional.empty();
			} else {
				throw new IllegalStateException("Two proxy site arcs should never be adjacent!");
			}
		}		
		
		// Event circle is useless if breakpoints don't converge
		if (!VoronoiUtils.areConvergent(leftSitePoint, middleSitePoint, rightSitePoint)) {
			return Optional.empty();
		}
		
		return Optional.ofNullable(Circle.fromPoints(leftSitePoint, middleSitePoint, rightSitePoint)); 
	}

	public void setCircleEvent(Optional<CircleEvent> event) {
		this.circleEvent = event;
	}
	
	public Optional<CircleEvent> getCircleEvent() {
		return circleEvent;
	}
	
	public Function computeParabola(double sweeplineY) {
		if (lastParabolaFunction == null || sweeplineY != lastSweeplineY) {
			lastSweeplineY = sweeplineY;
			if (this.site instanceof ProxySite) {
				lastParabolaFunction = new HorizontalLine(this.site.y());
			} else {
				lastParabolaFunction = Quadratic.fromPointAndLine(this.site.x(), this.site.y(), sweeplineY);
			}
		}
		return lastParabolaFunction;
	}
	
}
