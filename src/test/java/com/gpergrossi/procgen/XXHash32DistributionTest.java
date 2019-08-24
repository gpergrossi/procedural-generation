package com.gpergrossi.procgen;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.Random;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.gpergrossi.util.hash.XXHash32;

public class XXHash32DistributionTest {

	public static void main(String[] args) {
		new XXHash32DistributionTest().testSpread();
		//new XXHash32DistributionTest().profile();
	}
	
	public void testSpread() {
		HashDistributionPanel hdp = new HashDistributionPanel();
		
		JFrame f = new JFrame("Distribution");
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.add(hdp);
		f.pack();
		f.setVisible(true);
		
		final int[] i = new int[] {0};
		
		Timer t = new Timer();
		t.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				System.out.println("Render");
				hdp.drawHash(i[0]++);
				hdp.repaint();
			}
		}, 0, 100);
	}
	
	public void profile() {
		int seed = 0;
		float width = 1000;
		float height = 1000;
		float depth = 1;
		double averageActionsPerMillisecond = 0;
		
		System.out.println("Wait for input");
		Scanner sc = new Scanner(System.in);
		sc.nextLine();
		sc.close();
		
		while (true) {
			seed++;
			long time = System.nanoTime();
			for (float z = 0; z < depth; z++) {
				for (float y = 0; y < height; y++) {
					for (float x = 0; x < width; x++) {
						XXHash32.hashInts(seed, Float.floatToRawIntBits(x), Float.floatToRawIntBits(y));
					}
				}
			}
			long duration = System.nanoTime() - time;
			double actionsPerMillisecond = (width*height*depth) / (duration * 0.000001);
			
			averageActionsPerMillisecond = (averageActionsPerMillisecond * 19.0 + actionsPerMillisecond) / 20.0;

			System.out.println("Finished another "+(width*height*depth)+" hashes in "+duration+" nanoseconds \t("+actionsPerMillisecond+" actions per millisecond, \tsmoothed "+averageActionsPerMillisecond+")");
		}
		
		// 2 ints:    2765874
		// 2 longs:   2741952
		// 3 ints:    2741823
		// 3 longs:   2786060
		// 3 doubles:  643091 (to raw long bits)
		// 2 doubles:  655607 (to raw long bits)
		// 3 floats:   645552 (to raw int bits)
		// 2 floats:   649292 (to raw int bits)
	}
	
	private static class HashDistributionPanel extends JPanel {
		private static final long serialVersionUID = -8334468788506726559L;
		
		final int width = 2048;
		final int height = 1024;
		
		BufferedImage image;
		int[] argb;
		
		public HashDistributionPanel() {
			image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
			argb = new int[width*height];
			this.setMinimumSize(new Dimension(width, height));
		}
		
		public void drawHash(int seed) {
			double norm = Math.pow(2, 32);
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					int value = XXHash32.hashInts(seed, x, y);
					double valD = ((double) value - (double) Integer.MIN_VALUE) / norm;
					int brightness = ((int) Math.floor(valD * 256.0)) % 256;
					if (brightness < 255) brightness = 0;
					argb[y*width+x] = (255 << 24) | (brightness << 16) | (brightness << 8) | (brightness);
				}
			}
			image.setRGB(0, 0, width, height, argb, 0, width);
		}
		
		@Override
		public void paint(Graphics g) {
			switch ((int)(Math.random()*4)) {
				case 0: g.setColor(Color.RED); break;
				case 1: g.setColor(Color.GREEN); break;
				case 2: g.setColor(Color.YELLOW); break;
				case 3: g.setColor(Color.BLUE); break;
			}
			
			g.fillRect(0, 0, width, height);
			g.drawImage(image, 0, 0, this);
		}
		
		@Override
		public Dimension getPreferredSize() {
			return new Dimension(width, height);
		}
	}
	
}