package com.gpergrossi.voronoi.shoreline;

import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.util.List;
import java.util.Optional;

import com.gpergrossi.util.geom.vectors.Double2D;
import com.gpergrossi.voronoi.graph.Site;
import com.gpergrossi.voronoi.math.Function;
import com.gpergrossi.voronoi.math.VerticalLine;
import com.gpergrossi.voronoi.shoreline.Shoreline.Entry;

public class ShorelineRenderer {

	private Sweepline swpl;
	private Shoreline shrl;
	private List<PartialEdge> edges;
	
	public ShorelineRenderer(Sweepline sweepline, Shoreline shoreline, List<PartialEdge> edges) {
		this.swpl = sweepline;
		this.shrl = shoreline;
		this.edges = edges;
	}

	public void draw(Graphics2D g2d) {
		Shoreline.Entry entry = shrl.root;
		if (entry != null) {
			entry.forEach(en -> {
				if (en.isBreakpoint) {
					try {
						drawBreakpoint(g2d, en);
					} catch (Exception e) {
						// Do nothing
					}
				} else {
					try {
						drawArc(g2d, en);
					} catch (Exception e) {
						// Do nothing
					}
				}
			});
		}
		
		for (PartialEdge edge : edges) {
			try {
				drawEdge(g2d, edge);
			} catch (Exception e) {
				// Do nothing
			}
		}
	}

	private void drawBreakpoint(Graphics2D g2d, Shoreline.Entry en) {
		Double2D bp = en.breakpoint.computeLocation(swpl);
		g2d.fill(new Ellipse2D.Double(bp.x()-2, bp.y()-2, 4, 4));
		
		Double2D leftPt = en.breakpoint.getLeftSite().getPoint();
		Double2D rightPt = en.breakpoint.getRightSite().getPoint();
		
		if (leftPt.y() < rightPt.y()) {
			g2d.drawString("["+en.breakpoint.getLeftSite().getID()+","+en.breakpoint.getRightSite().getID()+"]", (int)bp.x()-30, (int)bp.y());
		} else {
			g2d.drawString("["+en.breakpoint.getLeftSite().getID()+","+en.breakpoint.getRightSite().getID()+"]", (int)bp.x()+5, (int)bp.y());
		}
	}

	private void drawArc(Graphics2D g2d, Entry en) {
		Arc arc = en.arc;
		Optional<Breakpoint> leftBreakpoint = arc.getLeftBreakpoint();
		Optional<Breakpoint> rightBreakpoint = arc.getRightBreakpoint();
		Site site = arc.getSite();
		Double2D sitePt = site.getPoint();

		double leftBreakpointY = site.y(), rightBreakpointY = site.y();
		double xMin, xMax;
			
		if (leftBreakpoint.isPresent() && rightBreakpoint.isPresent()) {
			Double2D bpLeft = leftBreakpoint.get().computeLocation(swpl);
			Double2D bpRight = rightBreakpoint.get().computeLocation(swpl);
			leftBreakpointY = bpLeft.y();
			rightBreakpointY = bpRight.y();
			xMin = bpLeft.x();
			xMax = bpRight.x();
		} else if (leftBreakpoint.isPresent()) {
			Double2D bpLeft = leftBreakpoint.get().computeLocation(swpl);
			leftBreakpointY = bpLeft.y();
			xMin = bpLeft.x();
			xMax = Math.max(xMin + 2000, sitePt.x() + 2000);
		} else if (rightBreakpoint.isPresent()) {
			Double2D bpRight = rightBreakpoint.get().computeLocation(swpl);
			rightBreakpointY = bpRight.y();
			xMax = bpRight.x();
			xMin = Math.min(xMax - 2000, sitePt.x() - 2000);
		} else {
			xMin = sitePt.x()-2000;
			xMax = sitePt.x()+2000;
		}
			
		int steps = (int)((xMax - xMin) * 0.2);
		double stepSize = (xMax - xMin) / steps;
		
		Function func = arc.computeParabola(swpl.getProgress());
		
		if (func instanceof VerticalLine) {
			g2d.draw(new Line2D.Double(site.x(), swpl.getProgress(), site.x(), leftBreakpointY));
			g2d.draw(new Line2D.Double(site.x(), swpl.getProgress(), site.x(), rightBreakpointY));
		} else {
			for (int i = 0; i < steps; i++) {
				double x0 = xMin + stepSize * i;
				double x1 = xMin + stepSize * (i+1);
				if (i == steps) x1 = xMax;
				
				final double y0 = func.apply(x0);
				final double y1 = func.apply(x1);
					
				if (x0 < sitePt.x() && x1 > sitePt.x()) {
					// make sure to include point x coord exactly
					final double xm = sitePt.x();
					final double ym = func.apply(xm);
					g2d.draw(new Line2D.Double(x0, y0, xm, ym));
					g2d.draw(new Line2D.Double(xm, ym, x1, y1));
				} else {
					g2d.draw(new Line2D.Double(x0, y0, x1, y1));
				}
			}
		}
	}

	private void drawEdge(Graphics2D g2d, PartialEdge edge) {
		Double2D leftPt = null;
		if (edge.endpointA != null) leftPt = edge.endpointA;
		else if (edge.breakpointA != null) leftPt = edge.breakpointA.computeLocation(swpl);
		
		Double2D rightPt = null;
		if (edge.endpointB != null) rightPt = edge.endpointB;
		else if (edge.breakpointB != null) rightPt = edge.breakpointB.computeLocation(swpl);
		
		if (leftPt != null && rightPt != null) {
			g2d.draw(new Line2D.Double(leftPt.x(), leftPt.y(), rightPt.x(), rightPt.y()));
		}
	}
	
}
