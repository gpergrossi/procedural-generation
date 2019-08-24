package com.gpergrossi.procgen;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;

import com.gpergrossi.util.geom.shapes.Circle;
import com.gpergrossi.util.geom.shapes.Convex;
import com.gpergrossi.util.geom.vectors.Double2D;
import com.gpergrossi.view.View;
import com.gpergrossi.view.ViewerFrame;
import com.gpergrossi.voronoi.BuildStateRenderer;
import com.gpergrossi.voronoi.VoronoiBuildState;
import com.gpergrossi.voronoi.VoronoiBuilder;

public class GUIVoronoiTestView extends View {
	
	public static ViewerFrame frame;
	
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				try {
					System.setProperty("sun.java2d.opengl", "true");
					frame = new ViewerFrame(new GUIVoronoiTestView(0, 0, 1024, 768));
					frame.setVisible(true);
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	@Override
	protected double getMinZoom() {
		return 0.25;
	}
	
	@Override
	protected void applyRenderSettings(Graphics2D g2d) {
		// Background Black
		g2d.setBackground(new Color(0,0,0,255));
	}
	
	double seconds;
	double printTime;
	double radiansPerDegree = (Math.PI/180.0);
	
	Convex poly = new Circle(0, 200, 100).toPolygon(5);
	
	VoronoiBuilder builder = new VoronoiBuilder();
	VoronoiBuildState buildState = null;
    BuildStateRenderer bsRenderer = null;
	
	public GUIVoronoiTestView (double x, double y, double width, double height) {
		super (x, y, width, height);
	}

	@Override
	public void init() {
	}
	
	@Override
	public void start() {
	}

	@Override
	public void stop() {
	}

	@Override
	public void update(double secondsPassed) {
		seconds += secondsPassed;

		printTime += secondsPassed;
		if (printTime > 1) {
			printTime -= 1;
			frame.setTitle("FPS = "+String.format("%.2f", getFPS()));
		}
	}
	
	@Override
	public void drawWorld(Graphics2D g2d) {		
		// Clock dots
		g2d.setColor(Color.WHITE);
		for (int i = 0; i < 60; i++) {
			double an = i * (Math.PI / 30.0);
			double ew = 5, eh = 5;
			double ex = Math.cos(an)*100-ew/2;
			double ey = Math.sin(an)*100-eh/2;
			Ellipse2D ellipse = new Ellipse2D.Double(ex, ey, ew, eh);
	
			if (ellipse.contains(mX, mY)) g2d.setColor(Color.YELLOW);
			g2d.fill(ellipse);
			g2d.setColor(Color.WHITE);
		}
		
		// Clock center
		Ellipse2D.Double ellipse2 = new Ellipse2D.Double(-5.0, -5.0, 10.0, 10.0);
		if (ellipse2.contains(mX, mY)) g2d.setColor(Color.YELLOW);
		g2d.fill(ellipse2);
		g2d.setColor(Color.WHITE);
		
		// Clock hand
		AffineTransform before = g2d.getTransform();
		Path2D.Double path = new Path2D.Double();
		path.moveTo(100, 0);
		path.lineTo(0, 2);
		path.lineTo(0, -2);
		path.closePath();
		g2d.rotate(seconds * (Math.PI / 30.0));
		g2d.fill(path);
		g2d.setTransform(before);
		
//		// Draw Polygon and highlight on mouseover
//		Double2D mouse = new Double2D(this.getMouseWorldX(), this.getMouseWorldY());
//		if (poly.contains(mouse)) g2d.setColor(Color.ORANGE); 
//		else g2d.setColor(Color.WHITE);
//		g2d.fill(poly.asAWTShape());
		
//		// Extended clockhand intersect
//		g2d.setColor(Color.BLUE);
//		double a = seconds*(Math.PI / 30.0);
//		Ray ray = new Ray(0, 0, Math.cos(a), Math.sin(a));
//		LineSeg seg = poly.clip(ray);
//		if (seg != null) g2d.draw(seg.asAWTShape());
		
//		// Reflection
//		Double2D dir = mouse.perpendicular().normalize();
//		Line line = new LineSeg(mouse.x(), mouse.y(), mouse.x()+dir.x(), mouse.y()+dir.y());
//		LineSeg mirror = line.toSegment(100, 1000);
//		Convex refl = poly.reflect(mirror);
//		Double2D reflC = refl.getCentroid();
//		
//		g2d.draw(mirror.asAWTShape());
//		g2d.drawOval((int)reflC.x()-20, (int)reflC.y()-20, 40, 40);
//		g2d.draw(refl.asAWTShape());
		
		
		
		if (buildState != null) {
			if (bsRenderer == null) {
				bsRenderer = new BuildStateRenderer(buildState);
			}
			bsRenderer.draw(g2d);
		} else {
			Iterator<Double2D> sites = builder.getSites();
			while (sites.hasNext()) {
				Double2D site = sites.next();
				Ellipse2D ellipse = new Ellipse2D.Double(site.x()-1, site.y()-1, 2, 2);
				g2d.fill(ellipse);
			}
		}
		
		// Mouse velocity trail
		g2d.setColor(Color.WHITE);
		Line2D mVel = new Line2D.Double(mX, mY, mX+mVelX, mY+mVelY);
		g2d.draw(mVel);
	}

	@Override
	public void drawOverlayUI(Graphics2D g2d) {
	}

	double startPX, startPY;
	double startViewX, startViewY;
	boolean panning = false;
	
	double mX, mY;
	double mDX, mDY;
	double mVelX, mVelY;

	@Override
	public void mousePressed() {}

	@Override
	public void mouseDragged() {}

	@Override
	public void mouseReleased() {
		int click = this.getMouseClick();
		double px = this.getMouseWorldX();
		double py = this.getMouseWorldY();
		
		if (click == View.RIGHT_CLICK || click == View.LEFT_CLICK) {
			Point2D clickP = new Point2D.Double(px, py);
			int x = (int) Math.floor((clickP.getX()+2) / 4);
			int y = (int) Math.floor((clickP.getY()+2) / 4);
			clickP = new Point2D.Double(x*4, y*4);

			if (click == View.RIGHT_CLICK) {
				Iterator<Double2D> sites = builder.getSites();
				while (sites.hasNext()) {
					Double2D site = sites.next();
					Point2D point = site.toPoint2D();
					if (clickP.distance(point) < 4) {
						System.out.println("Removing site");
						builder.removeSite(site);
						break;
					}
				}
			}
			
			if (click == View.LEFT_CLICK) {
				builder.addSite(new Double2D(clickP.getX(), clickP.getY()));
				System.out.println("Adding site");
			}
			
			buildState = null;
			bsRenderer = null;
		}
	}

	@Override
	public void mouseMoved() {
	}

	@Override
	public void mouseScrolled() {}

	@Override
	public void keyPressed() {
		KeyEvent e = getKeyEvent();
		if (e.getKeyCode() == KeyEvent.VK_SPACE) {
			if (buildState == null) buildState = builder.createBuildState();
			else buildState.step();
		} else if (e.getKeyCode() == KeyEvent.VK_ENTER) {

		} else if (e.getKeyCode() == KeyEvent.VK_UP) {

		} else if (e.getKeyCode() == KeyEvent.VK_DOWN) {

		} else if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {

		} else if (e.getKeyCode() == KeyEvent.VK_S) {
			try {
				BufferedWriter bw = new BufferedWriter(new FileWriter(new File("points.txt")));
				Iterator<Double2D> sites = builder.getSites();
				while (sites.hasNext()) {
					Double2D site = sites.next();
					bw.write(site.x() + ", " + site.y() + "\n");
				}
				bw.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		} else if (e.getKeyCode() == KeyEvent.VK_L) {
			try {
				BufferedReader br = new BufferedReader(new FileReader(new File("points.txt")));
				String line;
				while ((line = br.readLine()) != null) {
					String[] parts = line.split(",");
					if (parts.length == 2) {
						double x = Double.parseDouble(parts[0].trim());
						double y = Double.parseDouble(parts[1].trim());
						builder.addSite(new Double2D(x*5, y*5));
					}
					else
					{
						System.out.println("WARNING: Skipping input line from points.txt due to bad format. \"" + line + "\"");
					}
				}
				br.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		} else if (e.getKeyCode() == KeyEvent.VK_C) {
			System.out.println("Clearing");
			builder.clear();
		} else if (e.getKeyCode() == KeyEvent.VK_PAGE_UP) {
			this.setSlowZoom(9.0/14.0);
		} else if (e.getKeyCode() == KeyEvent.VK_PAGE_DOWN) {
			this.setSlowZoom(14.0/9.0);
		} else if (e.getKeyCode() == KeyEvent.VK_R) {
			if (!this.isRecording()) {
				System.out.println("Recording started");
				this.startRecording();
			} else {
				System.out.println("Recording finished");
				this.stopRecording();
			}
		} else if (e.getKeyCode() == KeyEvent.VK_SLASH) {
			for (double d = Math.PI/2.0; d < Math.PI*1.0; d += Math.PI/17.3) {
				builder.addSite(new Double2D(Math.cos(d)*d*100, Math.sin(d)*d*100));
				builder.addSite(new Double2D(Math.cos(d+Math.PI*2.0/3.0)*d*100, Math.sin(d+Math.PI*2.0/3.0)*d*100));
				builder.addSite(new Double2D(Math.cos(d-Math.PI*2.0/3.0)*d*100, Math.sin(d-Math.PI*2.0/3.0)*d*100));
			}
		} else if (e.getKeyCode() == KeyEvent.VK_SEMICOLON) {

		}
	}

	@Override
	public void keyReleased() {
		KeyEvent e = getKeyEvent();
		if (e.getKeyCode() == KeyEvent.VK_PAGE_UP) {
			setSlowZoom(1.0);
		} else if (e.getKeyCode() == KeyEvent.VK_PAGE_DOWN) {
			setSlowZoom(1.0);
		}
	}
	
	@Override
	public void keyTyped() { }
	
}
