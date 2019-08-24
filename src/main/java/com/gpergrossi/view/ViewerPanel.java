package com.gpergrossi.view;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.JPanel;

public class ViewerPanel extends JPanel implements Runnable, ComponentListener, MouseListener, MouseMotionListener, MouseWheelListener, KeyListener {

	/**
	 * All of the javax.swing stuff is "serializable". Java wants all serializable
	 * classes to have this serialVersionUID. Eclipse knows this and warns you about
	 * it unless you define something. In this case it is a random long value.
	 */
	private static final long serialVersionUID = 5969712076925495090L;

	// Constants
	private static final long NANOS_PER_MILLI = 1000000;
	private static final long NANOS_PER_SECOND = 1000000000;
	
	/**
	 *  When enabled, the view.update method will receive a fixed time step, regardless of frame rate.
	 *  Otherwise, the received update delta will be equal to the amount of time elapsed since last update.
	 */
	private boolean useFixedUpdate = false;
	
	/**
	 * Fixed time step to be used when useFixedUpdate is true.
	 */
	private double fixedUpdateDeltaSeconds = 1.0 / 60.0;
	
	public double targetFPS = 60;
	public boolean showFPS = false;
	public double averageFPS = Double.NaN;

	long lastDrawTime = Long.MAX_VALUE;
	long lastLoopNanos = System.nanoTime();
	long currentLoopNanos = System.nanoTime();
	long frameDeltaNanos = 1;
	long sleepThreshold = 6 * NANOS_PER_MILLI;
	long yieldThreshold = 2 * NANOS_PER_MILLI;
	
	private AffineTransform freshTransform = new AffineTransform();
	private AffineTransform screenToWorld = new AffineTransform();
	private AffineTransform worldToScreen = new AffineTransform();
	private BufferedImage backBufferImage, frontBufferImage;
	private Graphics2D backBufferGraphics, frontBufferGraphics;
	private boolean running = false;
	private View view;
	private Thread thread;
	
	// Input event handlers
	private int mouseClick;
	
	Lock graphicsBufferLock = new ReentrantLock();
	
	public ViewerPanel(View view) {
		this.view = view;
		
		addComponentListener(this);
		addMouseListener(this);
		addMouseMotionListener(this);
		addMouseWheelListener(this);
		addKeyListener(this);
		
		Dimension preferredSize = new Dimension((int) view.getViewWidth(), (int) view.getViewHeight());
		buildBuffers(preferredSize);
		this.setPreferredSize(preferredSize);
		
		this.setDoubleBuffered(true);
	}
	
	@Override
	public void componentHidden(ComponentEvent arg0) {
	}

	@Override
	public void componentMoved(ComponentEvent arg0) {		
	}

	@Override
	public void componentResized(ComponentEvent e) {
		Dimension size = e.getComponent().getSize();
		
		// Has the screen size actually changed?
		if (size.width != backBufferImage.getWidth() || size.height != backBufferImage.getHeight()) {
			buildBuffers(size);
			view.onResize(size.getWidth(), size.getHeight());
			updateViewTransform();
		}
	}

	@Override
	public void componentShown(ComponentEvent arg0) {		
	}
	
	@Override
	public void mouseReleased(MouseEvent e) {
		Point2D pt = screenToWorld(e.getPoint());
		view.internalMouseReleased(mouseClick, pt.getX(), pt.getY(), e.getX(), e.getY());
		mouseClick &= ~(1 << (e.getButton()-1));
	}
	
	@Override
	public void mousePressed(MouseEvent e) {
		Point2D pt = screenToWorld(e.getPoint());
		mouseClick |= (1 << (e.getButton()-1));
		view.internalMousePressed(mouseClick, pt.getX(), pt.getY(), e.getX(), e.getY());
	}
	
	@Override
	public void mouseExited(MouseEvent e) {
	}
	
	@Override
	public void mouseEntered(MouseEvent e) {
	}
	
	@Override
	public void mouseClicked(MouseEvent e) {
	}
	
	@Override
	public void mouseMoved(MouseEvent e) {
		Point2D pt = screenToWorld(e.getPoint());
		mouseClick = 0;
		view.internalMouseMoved(pt.getX(), pt.getY(), e.getX(), e.getY());
	}
	
	@Override
	public void mouseDragged(MouseEvent e) {
		Point2D pt = screenToWorld(e.getPoint());
		view.internalMouseDragged(mouseClick, pt.getX(), pt.getY(), e.getX(), e.getY());
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {
		view.internalMouseScrolled(e.getPreciseWheelRotation());			
	}
	
	@Override
	public void keyTyped(KeyEvent e) {
		view.internalKeyTyped(e);
	}
	
	@Override
	public void keyReleased(KeyEvent e) {
		view.internalKeyReleased(e);
	}
	
	@Override
	public void keyPressed(KeyEvent e) {
		view.internalKeyPressed(e);
	}
	
	protected void onFrameComplete(BufferedImage frame) {
		view.onFrameComplete(frame);
	}
	
	private void buildBuffers(Dimension size) {
		// Create new buffer images
		BufferedImage newBackBufferImage = new BufferedImage((int) size.getWidth(), (int) size.getHeight(), BufferedImage.TYPE_INT_ARGB);
		BufferedImage newFrontBufferImage = new BufferedImage((int) size.getWidth(), (int) size.getHeight(), BufferedImage.TYPE_INT_ARGB);
		
		// Create new buffer graphics
		Graphics2D newBackBufferGraphics = newBackBufferImage.createGraphics();
		Graphics2D newFrontBufferGraphics = newFrontBufferImage.createGraphics();
		
		// Apply settings to new graphics objects
		view.applyRenderSettings(newBackBufferGraphics);
		view.applyRenderSettings(newFrontBufferGraphics);
		
		// Save old graphics object for later disposal
		Graphics2D oldBackBufferGraphics = this.backBufferGraphics;
		Graphics2D oldFrontBufferGraphics = this.frontBufferGraphics;
		
		// Lock the graphics buffer lock and quickly swap in the new buffers
		graphicsBufferLock.lock();
		backBufferImage = newBackBufferImage;
		frontBufferImage = newFrontBufferImage;
		backBufferGraphics = newBackBufferGraphics;
		frontBufferGraphics = newFrontBufferGraphics;
		freshTransform.setTransform(backBufferGraphics.getTransform());
		graphicsBufferLock.unlock();
		
		// After swap is complete, dispose old graphics
		if (oldBackBufferGraphics != null) oldBackBufferGraphics.dispose();
		if (oldFrontBufferGraphics != null) oldFrontBufferGraphics.dispose();
	}

	private void swapBuffers() {
		BufferedImage swapImage = frontBufferImage;
		frontBufferImage = backBufferImage;
		backBufferImage = swapImage;
		
		Graphics2D swapGraphics = frontBufferGraphics;
		frontBufferGraphics = backBufferGraphics;
		backBufferGraphics = swapGraphics;
	}
	
	@Override
	public void paint(Graphics g) {
		graphicsBufferLock.lock();
		
		swapBuffers();
		
		// Show old buffer
		g.drawImage(frontBufferImage, 0, 0, this);
		
		// Recording
		onFrameComplete(frontBufferImage);
		
		// Render new buffer
		drawFrame(backBufferGraphics);
		
		graphicsBufferLock.unlock();
		
		if (lastDrawTime != Long.MAX_VALUE) {
			long drawDelta = System.nanoTime() - lastDrawTime;
			double FPS = (double) NANOS_PER_SECOND/drawDelta;
			if (FPS < targetFPS*2) {
				if (Double.isNaN(averageFPS)) {
					this.averageFPS = FPS;
				} else {
					this.averageFPS = (averageFPS * 59.0 + FPS) / 60.0;
				}
			}
		}
		lastDrawTime = System.nanoTime();
	}

	public void start() {
		running = true;
		view.internalStart();
		thread = new Thread(this);
		thread.setPriority(Thread.MAX_PRIORITY);
		thread.start();
	}
	
	public void stop() {
		if (running == false) return;
		running = false;
		try {
			thread.join();
		} catch(InterruptedException e) {
			e.printStackTrace();
		}
		view.internalStop();
	}
	
	private void sync(double fps) {
		long nanosPerFrame = (long) (NANOS_PER_SECOND/fps);
		do {
			long timeLeft = lastLoopNanos + nanosPerFrame - currentLoopNanos;
			if (timeLeft > sleepThreshold) {
				long sleepTime = timeLeft-sleepThreshold;
				sleepTime /= NANOS_PER_MILLI;
				try {
					Thread.sleep(sleepTime);
				} catch(InterruptedException e) {
					e.printStackTrace();
				}
			} else if (timeLeft > yieldThreshold) {
				Thread.yield();
			}
			currentLoopNanos = System.nanoTime();
		} while (currentLoopNanos - lastLoopNanos < nanosPerFrame);
		frameDeltaNanos = currentLoopNanos - lastLoopNanos;
		if (frameDeltaNanos == 0) frameDeltaNanos = 1;
		lastLoopNanos = currentLoopNanos;
	}
	
	@Override
	public void run() {
		while (running) {
			double updateDeltaSeconds = (double) frameDeltaNanos/NANOS_PER_SECOND;
			if (useFixedUpdate) updateDeltaSeconds = fixedUpdateDeltaSeconds;
			
			view.internalUpdate(updateDeltaSeconds);
			
			this.repaint();
			sync(targetFPS);
		}
	}
	
	public double getFPS() {
		if (Double.isNaN(averageFPS)) return 0;
		return this.averageFPS;
	}
	
	public void setFPS(double fps) {
		this.targetFPS = fps;
	}
	
	private void drawFrame(Graphics2D g2d) {
		// Clear frame
		resetTransform(g2d);
		g2d.setColor(view.getBackgroundColor());
		g2d.fillRect(0, 0, getWidth(), getHeight());

		// Request draw from view object
		view.internalDrawFrame(g2d, worldToScreen);
	}
	
	private void resetTransform(Graphics2D g2d) {
		g2d.setTransform(freshTransform);
	}
	
	protected void updateViewTransform() {
		final AffineTransform newWorldToScreenTransform = new AffineTransform(freshTransform);
		newWorldToScreenTransform.translate(getWidth()/2.0, getHeight()/2.0);
		newWorldToScreenTransform.scale(view.getViewZoom(), view.getViewZoom());
		newWorldToScreenTransform.translate(-view.getViewX(), -view.getViewY());
		
		final AffineTransform newScreenToWorldTransform = new AffineTransform(freshTransform);
		newScreenToWorldTransform.translate(view.getViewX(), view.getViewY());
		newScreenToWorldTransform.scale(1.0/view.getViewZoom(), 1.0/view.getViewZoom());
		newScreenToWorldTransform.translate(-getWidth()/2.0, -getHeight()/2.0);
		
		worldToScreen = newWorldToScreenTransform;
		screenToWorld = newScreenToWorldTransform;
	}
	
	public Point2D worldToScreen(Point2D pt) {
		return worldToScreen.transform(pt, pt);
	}
	
	public Point2D worldToScreenVelocity(Point2D pt) {
		pt.setLocation(pt.getX()*(getWidth()/view.getViewWidth())*view.getViewZoom(), pt.getY()*(getHeight()/view.getViewHeight())*view.getViewZoom());
		return pt;
	}
	
	public Point2D screenToWorld(Point2D pt) {
		return screenToWorld.transform(pt, pt);
	}
	
	public Point2D screenToWorldVelocity(Point2D pt) {
		pt.setLocation(pt.getX()*(view.getViewWidth()/getWidth())/view.getViewZoom(), pt.getY()*(view.getViewHeight()/getHeight())/view.getViewZoom());
		return pt;
	}
	
	public void useFixedUpdate(double updateDeltaSeconds) {
		this.useFixedUpdate = true;
		this.fixedUpdateDeltaSeconds = updateDeltaSeconds;
	}
	
	public void useVariableUpdate() {
		this.useFixedUpdate = false;
	}
	
}
