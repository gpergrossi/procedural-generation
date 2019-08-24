package com.gpergrossi.view;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;

public class Example extends View {

	public static void main(String[] args) {
		// When you copy paste, make sure you are updating which view class you are using
		Example example = new Example();
		ViewerFrame frame = new ViewerFrame(example);
		frame.setVisible(true);
	}
	
	double runningTime;
	String textBuffer;
	
	boolean textCursorVisible;
	double textCursorFlashTimer;
	double textCursorFlashInterval;
	
	private static final int NUMBER_OF_BALLS = 100;
	
	Ball centralAnchoredBall;
	List<Ball> balls;
	
	public double anchorX = 0;
	public double anchorY = 0;
	public boolean vacuumMode = false;
	
	Random random = new Random();
	
	public Example() {
		super(0, 0, 1024, 768);
	}

	
	@Override
	public void init() {
		// Called once when the view is created
	}

	@Override
	public void start() {
		// Called each time the view is started, (ViewerFrame only calls this once on creation, so it served an almost identical purpose to init())
		runningTime = 0;
		textBuffer = "";
		
		textCursorVisible = true;
		textCursorFlashTimer = 0;
		textCursorFlashInterval = 0.5;

		balls = new ArrayList<>();
		
		centralAnchoredBall = new Ball(0, 0, 50, 50f);
		balls.add(centralAnchoredBall);
		
		for (int i = 0; i < NUMBER_OF_BALLS-1; i++) {
			Ball newBall = new Ball();
			newBall.setDensity(10.0f);
			respawnBall(newBall);
			balls.add(newBall);
		}
	}

	private void respawnBall(Ball ball) {
		float size = (float) ((random.nextFloat() * 0.40 + 0.10) * centralAnchoredBall.getSize());
		ball.setSize(size);
		
		float angle = random.nextFloat() * 3.14159f * 2.0f;
		float dist = (float) Math.sqrt(random.nextDouble()) * centralAnchoredBall.getSize()*60 + centralAnchoredBall.getSize() + size;
		float x = (float)(centralAnchoredBall.getX() + Math.cos(angle)*dist);
		float y = (float)(centralAnchoredBall.getY() + Math.sin(angle)*dist);
		ball.setX(x);
		ball.setY(y);
		
		float vx = (float) (-Math.sin(angle) * Math.sqrt(4000.0 * centralAnchoredBall.getMass() / dist));
		float vy = (float) (Math.cos(angle) * Math.sqrt(4000.0 * centralAnchoredBall.getMass() / dist));
		ball.setVelocityX(vx);
		ball.setVelocityY(vy);
		
		float h = random.nextFloat() * 0.33f + 0.33f;
		float s = random.nextFloat() * 0.5f + 0.5f;
		float b = 1.0f;
		ball.color = new Color(Color.HSBtoRGB(h, s, b));
		
		ball.life = 0;
	}
	
	@Override
	public void stop() {
		// Called each time the view is stopped, (ViewerFrame only calls this once when the window is closed)
	}

	/**
	 * Called repeatedly with information about how long it has been since the last update
	 */
	@Override
	public void update(double secondsPassed) {
		
		runningTime += secondsPassed;

		// Make a cursor flash in and out
		textCursorFlashTimer += secondsPassed;
		while (textCursorFlashTimer > textCursorFlashInterval) {
			textCursorFlashTimer -= textCursorFlashInterval;
			textCursorVisible = !textCursorVisible;
		}

		// If vacuumMode is on, reduce everything's velocity and move toward centralAnchor
		if (vacuumMode) {
			for (Ball ball : balls) {
				if (ball != centralAnchoredBall) {
					ball.setVelocityX(ball.getVelocityX() * 0.9f);
					ball.setVelocityY(ball.getVelocityY() * 0.9f);
					
					float dx = centralAnchoredBall.getX() - ball.getX();
					float dy = centralAnchoredBall.getY() - ball.getY();
					float dist = (float) Math.sqrt(dx*dx + dy*dy);
					dx /= dist;
					dy /= dist;
					
					ball.setVelocityX(ball.getVelocityX() + dx * 500);
					ball.setVelocityY(ball.getVelocityY() + dy * 500);
				}
			}
		}
		
		// Pull "anchored" ball back towards anchor point
		{
			final float distX = (float) (anchorX - Ball.getX(0));
			final float distY = (float) (anchorY - Ball.getY(0));
			final float dist = (float) Math.sqrt(distX*distX + distY*distY);
			if (dist > 0.1) {
				final float dirX = distX/dist;
				final float dirY = distY/dist;
				final float accel = 100000 * dist*dist / (dist+1);
				centralAnchoredBall.setAccelerationX(centralAnchoredBall.getAccelerationX() + dirX * accel);
				centralAnchoredBall.setAccelerationY(centralAnchoredBall.getAccelerationY() + dirY * accel);
				centralAnchoredBall.setVelocityX(centralAnchoredBall.getVelocityX() * 0.9f);
				centralAnchoredBall.setVelocityY(centralAnchoredBall.getVelocityY() * 0.9f);
			}
		}
		
		// Do physics simulation steps
		for (int stepsPerFrame = 0; stepsPerFrame < 100; stepsPerFrame++) {
			Ball.simulate();
		}
		
		for (Ball ball : balls) {
			if (ball != centralAnchoredBall) {
				if (ball.distanceTo(centralAnchoredBall) > centralAnchoredBall.getSize() * 128.0) {
					respawnBall(ball);
				}
				
				if (ball.isTouching(centralAnchoredBall)) {
					ball.life += (vacuumMode ? 1001.0 : 100.0);
				} else {
					ball.life *= 0.99;
				}
				
				if (ball.life > 1000.0) {
					double destroyedBallMass = ball.getMass();
					
					respawnBall(ball);
					
					// Add mass to central ball
					double combinedMass = destroyedBallMass + centralAnchoredBall.getMass();
					centralAnchoredBall.setMass((float) combinedMass);
				}
			}
		}
	}

	/**
	 * Draws the UI layer. All draw operations happen on top of the world and use a transform
	 * in which 0,0 is the top left of the screen and width,height is the bottom right.
	 */
	@Override
	public void drawOverlayUI(Graphics2D g2d) {
		g2d.setColor(Color.WHITE);

		// Draw a string in the upper left corner of the screen indicating total run time
		g2d.drawString("Elapsed time: "+getFormatedTimeString(runningTime), 8, 16);
		
		// Split text buffer into lines (drawString doesn't do this for you)
		String[] lines = textBuffer.split("\n", -1);
		
		// Append the cursor character to the end of the last line (where you are typing)
		if (textCursorVisible) {
			lines[lines.length-1] += "_";
		}
		
		// Prepend the "Text buffer: " label to the first line
		lines[0] = "Text buffer: " + lines[0];

		// Draw text buffer
		int lineY = 32;
		for (String line : lines) {
			g2d.drawString(line, 8, lineY);
			lineY += 14;
		}
	}
	
	/**
	 * Draws the World layer. All draw operations use a transform corresponding to
	 * the current zoom and view coordinates. (Middle mouse click/drag, and scrolling)
	 */
	@Override
	public void drawWorld(Graphics2D g2d) {
		g2d.setColor(Color.WHITE);
		
		g2d.drawLine(-10, -10, 10, 10);
		g2d.drawLine(-10, 10, 10, -10);

		// Draw balls
		Ellipse2D ellipse = new Ellipse2D.Double();
		for (Ball ball : balls) {
			g2d.setColor(ball.getColor());
			ellipse.setFrame(ball.getX() - ball.getSize(), ball.getY() - ball.getSize(), ball.getSize()*2, ball.getSize()*2);
			g2d.fill(ellipse);

			if (ball.isTouching(centralAnchoredBall)) {
				g2d.setColor(Color.WHITE);
				g2d.draw(ellipse);
			}
			
//			g2d.setColor(Color.ORANGE);
//			g2d.draw(new Line2D.Double(ball.getX(), ball.getY(), ball.getX() + ball.getVelocityX()*0.5, ball.getY() + ball.getVelocityY()*0.5));
//			
//			g2d.setColor(Color.MAGENTA);
//			g2d.draw(new Line2D.Double(ball.getX(), ball.getY(), ball.getX() + ball.getAccelerationX()*0.125, ball.getY() + ball.getAccelerationY()*0.125));
		}

		// Draw edge of universe
		g2d.setColor(Color.YELLOW);
		g2d.draw(new Line2D.Double(anchorX, anchorY, centralAnchoredBall.getX(), centralAnchoredBall.getY()));

		// Draw edge of universe
		g2d.setColor(Color.WHITE);
		ellipse.setFrame(centralAnchoredBall.getX() - centralAnchoredBall.getSize()*128, centralAnchoredBall.getY() - centralAnchoredBall.getSize()*128, centralAnchoredBall.getSize()*256, centralAnchoredBall.getSize()*256);
		g2d.draw(ellipse);
	}

	/**
	 * Called when a mouse button begins being pressed
	 */
	@Override
	public void mousePressed() {
		if ((getMouseClick() & LEFT_CLICK) != 0) {
			anchorX = getMouseWorldX();
			anchorY = getMouseWorldY();
			System.out.println("Set anchor to ("+anchorX+", "+anchorY+")");
		}
		
		if ((getMouseClick() & RIGHT_CLICK) != 0) {
			vacuumMode = true;
		}
	}

	/**
	 * Called when the mouse is moved while at least one button is held
	 */
	@Override
	public void mouseDragged() {
		if ((getMouseClick() & LEFT_CLICK) != 0) {
			anchorX = getMouseWorldX();
			anchorY = getMouseWorldY();
			System.out.println("Set anchor to ("+anchorX+", "+anchorY+")");
		}
	}

	/**
	 * Called when a mouse button is released
	 */
	@Override
	public void mouseReleased() {
		if ((getMouseClick() & LEFT_CLICK) != 0) {
			anchorX = getMouseWorldX();
			anchorY = getMouseWorldY();
			System.out.println("Set anchor to ("+anchorX+", "+anchorY+")");
		}
		
		if ((getMouseClick() & RIGHT_CLICK) != 0) {
			vacuumMode = false;
		}
	}

	/**
	 * Called when the mouse is moved while no buttons are held
	 */
	@Override
	public void mouseMoved() {
		// getMouseWorldDX();
		// getMouseWorldDY();
	}

	/**
	 * Called each time the scroll wheel moves
	 */
	@Override
	public void mouseScrolled() {
		//getMouseScroll();
	}

	/**
	 * Called each time any key is pressed
	 */
	@Override
	public void keyPressed() {
		KeyEvent e = getKeyEvent();
		if (e.getKeyCode() == KeyEvent.VK_UP) {
			// etc
		}
		
		// You may not use getKeyChar() in a keyPressed event
	}

	/**
	 * Called each time any key is release
	 */
	@Override
	public void keyReleased() {
		KeyEvent e = getKeyEvent();
		if (e.getKeyCode() == KeyEvent.VK_UP) {
			// etc
		}
		
		// You may not use getKeyChar() in keyReleased event
	}

	/**
	 * Called each time certain keys are typed (once when pressed, repeating after some delay at some rate until release)
	 */
	@Override
	public void keyTyped() {
		KeyEvent e = getKeyEvent();
				
		if (e.getKeyChar() == '\b') {
			if (textBuffer.length() > 0) {
				textBuffer = textBuffer.substring(0, textBuffer.length()-1);
			}
		} else if (e.getKeyChar() == '\n') {
			textBuffer += "\n";
		} else {
			textBuffer += e.getKeyChar();
		}
		
		textCursorVisible = true;
		textCursorFlashTimer = 0;

		// You may not use getKeyCode() in a keyTyped event
	}
	

	
	private String getFormatedTimeString(double timeSeconds) {
		
		int millis = (int) Math.floor(timeSeconds * 1000);
		int seconds = Math.floorDiv(millis, 1000);
		int minutes = Math.floorDiv(seconds, 60);
		int hours = Math.floorDiv(minutes, 60);
		
		millis -= seconds * 1000;
		seconds -= minutes * 60;
		minutes -= hours * 60;
		
		StringBuilder timeString = new StringBuilder();
		
		if (hours == 1) {
			timeString.append(hours);
			timeString.append(" hour ");
		} else if (hours > 1) {
			timeString.append(hours);
			timeString.append(" hours ");
		}
		
		if (minutes == 1) {
			timeString.append(minutes);
			timeString.append(" minute ");
		} else if (minutes > 1) {
			timeString.append(minutes);
			timeString.append(" minutes ");
		}
		
		String milliseconds = ("000" + millis);
		milliseconds = milliseconds.substring(milliseconds.length() - 3);
		
		timeString.append(seconds).append('.').append(milliseconds);
		if (millis == 0 && seconds == 1) {
			timeString.append(" second");
		} else {
			timeString.append(" seconds");
		}
		
		return timeString.toString();
	}
	
	
	/**
	 * This class is organized in a strange way that forces all of the information for each instance object
	 * to exist in a large float array that java will hopefully put in the same place in memory. Keeping this
	 * data close together in memory is supposed to make the physics computations faster.
	 * 
	 * You should probably not write classes like this. Especially since it limits the max number of objects
	 * you can create.
	 */
	public static class Ball {
		private static final float SIMULATION_TIME_STEP = 0.0001f;
		
		private static final int MAX_NUMBER_OF_BALLS = 10000;
		private static final float[] BallData = new float[MAX_NUMBER_OF_BALLS * 8];
		
		public static final float getX(int index) 						 { return BallData[(index << 3) | 0]; }
		public static final float getY(int index) 						 { return BallData[(index << 3) | 1]; }
		public static final float getVelocityX(int index) 				 { return BallData[(index << 3) | 2]; }
		public static final float getVelocityY(int index) 				 { return BallData[(index << 3) | 3]; }
		public static final float getAccelerationX(int index) 			 { return BallData[(index << 3) | 4]; }
		public static final float getAccelerationY(int index) 			 { return BallData[(index << 3) | 5]; }
		public static final float getSize(int index) 					 { return BallData[(index << 3) | 6]; }
		public static final float getMass(int index) 					 { return BallData[(index << 3) | 7]; }
		
		public static final void setX(int index, float x) 				 { BallData[(index << 3) | 0] = x; }
		public static final void setY(int index, float y)			 	 { BallData[(index << 3) | 1] = y; }
		public static final void setVelocityX(int index, float vx) 		 { BallData[(index << 3) | 2] = vx; }
		public static final void setVelocityY(int index, float vy) 		 { BallData[(index << 3) | 3] = vy; }
		public static final void setAccelerationX(int index, float ax) 	 { BallData[(index << 3) | 4] = ax; }
		public static final void setAccelerationY(int index, float ay) 	 { BallData[(index << 3) | 5] = ay; }
		public static final void setSize(int index, float size) 		 { BallData[(index << 3) | 6] = size; }
		public static final void setMass(int index, float mass) 		 { BallData[(index << 3) | 7] = mass; }
		
		public static final void addX(int index, float x)				 { BallData[(index << 3) | 0] += x; }
		public static final void addY(int index, float y) 				 { BallData[(index << 3) | 1] += y; }
		public static final void addVelocityX(int index, float vx) 		 { BallData[(index << 3) | 2] += vx; }
		public static final void addVelocityY(int index, float vy) 		 { BallData[(index << 3) | 3] += vy; }
		public static final void addAccelerationX(int index, float ax) 	 { BallData[(index << 3) | 4] += ax; }
		public static final void addAccelerationY(int index, float ay) 	 { BallData[(index << 3) | 5] += ay; }
		public static final void addSize(int index, float size) 		 { BallData[(index << 3) | 6] += size; }
		public static final void addMass(int index, float mass) 		 { BallData[(index << 3) | 7] += mass; }
		
		public static final void scaleX(int index, float x) 			 { BallData[(index << 3) | 0] *= x; }
		public static final void scaleY(int index, float y) 			 { BallData[(index << 3) | 1] *= y; }
		public static final void scaleVelocityX(int index, float vx) 	 { BallData[(index << 3) | 2] *= vx; }
		public static final void scaleVelocityY(int index, float vy) 	 { BallData[(index << 3) | 3] *= vy; }
		public static final void scaleAccelerationX(int index, float ax) { BallData[(index << 3) | 4] *= ax; }
		public static final void scaleAccelerationY(int index, float ay) { BallData[(index << 3) | 5] *= ay; }
		public static final void scaleSize(int index, float size) 		 { BallData[(index << 3) | 6] *= size; }
		public static final void scaleMass(int index, float mass) 		 { BallData[(index << 3) | 7] *= mass; }

		private static int NextBallIndex = 0;
		
		public final int index;
		
		public Color color = Color.WHITE;
		public double life = 0;
		
		private double density;
		
		public Ball() {
			this(0, 0, 10, 1);
		}
		
		public Ball(float x, float y, float size, float density) {
			this.index = NextBallIndex;
			NextBallIndex++;
			
			this.density = density;
			
			this.setX(x);
			this.setY(y);
			this.setSize(size);
			this.setVelocityX(0);
			this.setVelocityY(0);
			this.setAccelerationX(0);
			this.setAccelerationY(0);
		}
		
		public boolean isTouching(Ball other) {
			final double combinedSize = this.getSize() + other.getSize() + 20;			
			return (this.distanceTo(other) <= combinedSize);
		}
		
		public double distanceTo(Ball other) {			
			final double distX = this.getX() - other.getX();
			final double distY = this.getY() - other.getY();
			final double dist2 = distX*distX + distY*distY;
			return Math.sqrt(dist2);
		}
		
		public double getDensity() {
			return density;
		}
		
		public void setDensity(double density) {
			this.density = density;
			this.setMass(3.14159f * this.getSize() * this.getSize() * (float) density);
		}
		
		public Color getColor() {
			float[] hsb = new float[3];
			Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), hsb);
			
			hsb[1] *= (1.0 - Math.max(Math.min(this.life / 1000.0, 1.0f), 0.0f));
			
			return new Color(Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]));
		}

		public float getX() 						{ return Ball.getX(index); }
		public float getY() 						{ return Ball.getY(index); }
		public float getVelocityX() 				{ return Ball.getVelocityX(index); }
		public float getVelocityY() 				{ return Ball.getVelocityY(index); }
		public float getAccelerationX() 			{ return Ball.getAccelerationX(index); }
		public float getAccelerationY() 			{ return Ball.getAccelerationY(index); }
		public float getSize() 						{ return Ball.getSize(index); }
		public float getMass() 						{ return Ball.getMass(index); }

		public void setX(float x) 					{ Ball.setX(index, x); }
		public void setY(float y) 					{ Ball.setY(index, y); }
		public void setVelocityX(float vx) 			{ Ball.setVelocityX(index, vx); }
		public void setVelocityY(float vy) 			{ Ball.setVelocityY(index, vy); }
		public void setAccelerationX(float ax) 		{ Ball.setAccelerationX(index, ax); }
		public void setAccelerationY(float ay) 		{ Ball.setAccelerationY(index, ay); }
		public void setSize(float size) 			{ Ball.setSize(index, size); Ball.setMass(index, 3.14159f * Ball.getSize(index) * Ball.getSize(index) * (float) density); }
		public void setMass(float mass) 			{ Ball.setMass(index, mass); Ball.setSize(index, (float) Math.sqrt(Ball.getMass(index) / (3.14159f * this.getDensity()))); }
		
		public static void simulate() {
			float timeStep = SIMULATION_TIME_STEP;
			
			// Next, calculate new ball accelerations without moving any balls around
			for (int i = 0; i < NextBallIndex; i++) {
				for (int j = i+1; j < NextBallIndex; j++) {
					final float distX = Ball.getX(j) - Ball.getX(i);
					final float distY = Ball.getY(j) - Ball.getY(i);
					final float combinedSize = Ball.getSize(i) + Ball.getSize(j);
					final float combinedSize2 = combinedSize*combinedSize;
					
					float dist2 = distX*distX + distY*distY;
					if (!Float.isFinite(dist2)) {
						dist2 = 10000000.0f;
					}
					
					dist2 = Math.max(dist2, combinedSize2);
					
					final float massA = Ball.getMass(i);
					final float massB = Ball.getMass(j);
					
					Ball.addAccelerationX(i, distX * massB / dist2);
					Ball.addAccelerationY(i, distY * massB / dist2);
					Ball.addAccelerationX(j, -distX * massA / dist2);
					Ball.addAccelerationY(j, -distY * massA / dist2);
				}
			}
			
			// Then, update velocities and positions
			for (int i = 0; i < NextBallIndex; i++) {
				Ball.addX(i, Ball.getVelocityX(i) * timeStep);
				Ball.addY(i, Ball.getVelocityY(i) * timeStep);
				Ball.addVelocityX(i, Ball.getAccelerationX(i) * timeStep);
				Ball.addVelocityY(i, Ball.getAccelerationY(i) * timeStep);
				Ball.scaleVelocityX(i, 0.999998f);
				Ball.scaleVelocityY(i, 0.999998f);
			}
				
			// Then, adjust velocities of colliding objects without moving them
			for (int i = 0; i < NextBallIndex; i++) {
				for (int j = i+1; j < NextBallIndex; j++) {
					final float distX = Ball.getX(j) - Ball.getX(i);
					final float distY = Ball.getY(j) - Ball.getY(i);
					final float dist2 = distX*distX + distY*distY;
					final float combinedSize = Ball.getSize(i) + Ball.getSize(j);
					final float combinedSize2 = combinedSize*combinedSize;

					if (Float.isFinite(dist2) && dist2 < combinedSize2) {
						final float dist = (float) Math.sqrt(dist2);
						final float directionX = distX / dist;
						final float directionY = distY / dist;
						
						final float dotA = directionX * Ball.getVelocityX(i) + directionY * Ball.getVelocityY(i);
						final float dotB = directionX * Ball.getVelocityX(j) + directionY * Ball.getVelocityY(j);
						final float together = dotA - dotB;
						
						// Can't collide if not moving together
						if (together >= 0) {
							final float crossA = directionX * Ball.getVelocityY(i) - directionY * Ball.getVelocityX(i);
							final float crossB = directionX * Ball.getVelocityY(j) - directionY * Ball.getVelocityX(j);
							
							final float totalMass = Ball.getMass(i) + Ball.getMass(j);
							final float massPortionA = Ball.getMass(i) / totalMass;
							final float massPortionB = Ball.getMass(j) / totalMass;
							
							final float averageSpeed = (massPortionA * dotA) + (massPortionB * dotB);
							final float afterCollisionDotA = averageSpeed - ((dotA - dotB) * massPortionB * 0.9f);
							final float afterCollisionDotB = averageSpeed + ((dotA - dotB) * massPortionA * 0.9f);
							
							// Reflect velocities off collision vector
							Ball.setVelocityX(i, afterCollisionDotA * directionX - (0.97f * crossA * directionY));
							Ball.setVelocityY(i, afterCollisionDotA * directionY + (0.97f * crossA * directionX));
							Ball.setVelocityX(j, afterCollisionDotB * directionX - (0.97f * crossB * directionY));
							Ball.setVelocityY(j, afterCollisionDotB * directionY + (0.97f * crossB * directionX));
						}
					}
				}
			}
			
			// Finally, try to fix collisions
			for (int x = 0; x < 10; x++) {
				for (int i = 0; i < NextBallIndex; i++) {
					for (int j = i+1; j < NextBallIndex; j++) {
						final float distX = Ball.getX(j) - Ball.getX(i);
						final float distY = Ball.getY(j) - Ball.getY(i);
						final float dist2 = distX*distX + distY*distY;
						final float combinedSize = Ball.getSize(i) + Ball.getSize(j);
						final float combinedSize2 = combinedSize*combinedSize;
						
						if (Float.isFinite(dist2) && dist2 < combinedSize2) {
							final float dist = (float) Math.sqrt(dist2);
							final float overlap = combinedSize - dist;
							
							final float directionX = distX / dist;
							final float directionY = distY / dist;

							final float totalMass = Ball.getMass(i) + Ball.getMass(j);
							final float massPortionA = Ball.getMass(i) / totalMass;
							final float massPortionB = Ball.getMass(j) / totalMass;
							
							// Set ball positions apart
							Ball.addX(i, -0.9f * directionX * overlap * massPortionB);
							Ball.addY(i, -0.9f * directionY * overlap * massPortionB);
							Ball.addX(j,  0.9f * directionX * overlap * massPortionA);
							Ball.addY(j,  0.9f * directionY * overlap * massPortionA);
						}
					}
				}
			}
			
			// Reset accelerations
			for (int i = 0; i < NextBallIndex; i++) {
				Ball.setAccelerationX(i, 0);
				Ball.setAccelerationY(i, 0);
			}
		}
		
	}
	
}
