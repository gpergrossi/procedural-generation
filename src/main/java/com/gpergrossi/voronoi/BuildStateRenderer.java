package com.gpergrossi.voronoi;

import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.Iterator;

import com.gpergrossi.util.geom.vectors.Double2D;
import com.gpergrossi.voronoi.event.CircleEvent;
import com.gpergrossi.voronoi.event.VoronoiEvent;
import com.gpergrossi.voronoi.event.VoronoiEventType;
import com.gpergrossi.voronoi.graph.Site;
import com.gpergrossi.voronoi.shoreline.ShorelineRenderer;

public class BuildStateRenderer {

	private VoronoiBuildState vbs;
	private ShorelineRenderer sr;
	
	public BuildStateRenderer(VoronoiBuildState vbs) {
		this.vbs = vbs;
	}
	
	public void draw(Graphics2D g2d) {
		if (vbs.bounds != null) {
			g2d.draw(new Rectangle2D.Double(vbs.bounds.minX, vbs.bounds.minY, vbs.bounds.width, vbs.bounds.height));
		}
		 
		if (vbs.sites != null) {
			for (Site site : vbs.sites) {
				Double2D pt = site.getPoint();
				g2d.drawString("" + site.getID(), (int)pt.x(), (int)pt.y());
			}
		}
		
		if (vbs.multiQueue != null) {
			Iterator<VoronoiEvent> events = vbs.multiQueue.iterator();
			while (events.hasNext()) {
				VoronoiEvent event = events.next();
				if (event.getType() == VoronoiEventType.SITE) {
					Double2D pt = event.getEventPoint();
					g2d.fill(new Ellipse2D.Double(pt.x()-2, pt.y()-2, 5, 5));
				}
			}
		}
		
		if (vbs.completedEvents != null) {
			Iterator<VoronoiEvent> events = vbs.completedEvents.iterator();
			while (events.hasNext()) {
				VoronoiEvent event = events.next();
				if (event.getType() == VoronoiEventType.SITE) {
					Double2D pt = event.getEventPoint();
					g2d.draw(new Line2D.Double(pt.x()-2, pt.y()-2, pt.x()+2, pt.y()+2));
					g2d.draw(new Line2D.Double(pt.x()-2, pt.y()+2, pt.x()+2, pt.y()-2));
				}
			}
		}
		
		if (vbs.circleEventsQueue != null) {
			for (CircleEvent event : vbs.circleEventsQueue) {
				if (event.isValid()) {
					Double2D center = event.center;
					double radius = event.radius;
					g2d.draw(new Ellipse2D.Double(center.x()-radius, center.y()-radius, radius*2, radius*2));
					g2d.drawString("<"+event.arc.toString()+">", (int)center.x(), (int)center.y());
				}
			}
		}
		
		if (vbs.sweepline.isInitialized()) {
			Double2D direction = new Double2D(0, 1);
			Double2D origin = direction.multiply(vbs.sweepline.getProgress());
			Double2D lineStart = origin.add(direction.perpendicular().multiply(30000));
			Double2D lineEnd = origin.add(direction.perpendicular().multiply(-30000));
			g2d.draw(new Line2D.Double(lineStart.x(), lineStart.y(), lineEnd.x(), lineEnd.y()));
			g2d.drawString(Double.toString(vbs.sweepline.getProgress()), (int)origin.x()-50, (int)origin.y()-2);
			
			Double2D arrowOrigin = origin.add(direction.multiply(-20.0));
			Double2D arrowEnd = arrowOrigin.add(direction.rotate(Math.PI/2).multiply(10));
			Double2D arrowPoint = arrowEnd.add(direction.rotate(-3*Math.PI/4).multiply(3));
			
			g2d.draw(new Line2D.Double(arrowOrigin.x(), arrowOrigin.y(), -arrowEnd.x(), arrowEnd.y()));
			g2d.draw(new Line2D.Double(-arrowEnd.x(), arrowEnd.y(), -arrowPoint.x(), arrowPoint.y()));
		}
		
		if (vbs.shoreline != null && vbs.sweepline.isInitialized() && vbs.partialEdges != null) {
			if (sr == null) {
				sr = new ShorelineRenderer(vbs.sweepline, vbs.shoreline, vbs.partialEdges);
			}
			sr.draw(g2d);
		}
	}
}
