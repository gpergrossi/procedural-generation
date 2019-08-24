package com.gpergrossi.procgen;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import com.gpergrossi.view.View;
import com.gpergrossi.view.ViewerFrame;
import de.alsclo.voronoi.Voronoi;
import de.alsclo.voronoi.graph.Graph;
import de.alsclo.voronoi.graph.Point;

public class TestView extends View {

	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				try {
					System.setProperty("sun.java2d.opengl", "true");
					ViewerFrame frame = new ViewerFrame(new TestView());
					frame.setVisible(true);
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	
	
	List<Point> points;
	Graph graph;
	
	public TestView() {
		super(0, 0, 1024, 768);
	}
	
	@Override
	public void init() {
		points = new ArrayList<>();
		graph = null;
	}

	@Override
	public void start() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void update(double secondsPassed) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void drawWorld(Graphics2D g2d) {
		g2d.setColor(Color.WHITE);
		g2d.drawOval(-100, -100, 200, 200);
		
		for (Point p : points) {
			g2d.fill(new Ellipse2D.Double(p.x-5, p.y-5, 10, 10));
		}
		
		if (graph != null) {
			graph.edgeStream().forEach(edge -> {
				Point sa = edge.getSite1();
				Point sb = edge.getSite2();
				if (sa != null && sb != null) {
					g2d.draw(new Line2D.Double(sa.x, sa.y, sb.x, sb.y));
				}
			});
			
			Point site = graph.getSitePoints().iterator().next();
			g2d.setColor(Color.RED);
			g2d.fill(new Ellipse2D.Double(site.x-3, site.y-3, 6, 6));

			g2d.setColor(Color.BLACK);
			graph.edgeStream()
				.filter(edge -> (edge.getSite1() == site || edge.getSite2() == site))
				.map(edge -> (edge.getSite1() == site) ? edge.getSite2() : edge.getSite1())
				.forEach(neighbor -> {
					g2d.fill(new Ellipse2D.Double(neighbor.x-3, neighbor.y-3, 6, 6));
				});
		}
	}

	@Override
	public void drawOverlayUI(Graphics2D g2d) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mousePressed() {
		boolean changed = false;
		Point mousePoint = new Point(getMouseWorldX(), getMouseWorldY());
		
		if (getMouseClick() == LEFT_CLICK) {
			points.add(mousePoint);
			changed = true;
		} else if (getMouseClick() == RIGHT_CLICK) {
			Iterator<Point> iter = points.iterator();
			while (iter.hasNext()) {
				Point p = iter.next();
				double dx = mousePoint.x - p.x;
				double dy = mousePoint.y - p.y;
				if (dx*dx + dy*dy < 100) {
					iter.remove();
					changed = true;
				}
			}
		}
		
		if (changed) {
			if (points.size() > 0) {
				Voronoi vor = new Voronoi(points);
				graph = vor.getGraph();
			} else {
				graph = null;
			}
		}
		
	}

	@Override
	public void mouseDragged() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseReleased() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseMoved() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseScrolled() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void keyPressed() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void keyReleased() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void keyTyped() {
		// TODO Auto-generated method stub
		
	}
	
	
	
}
