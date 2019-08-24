package com.gpergrossi.util.math.func2d;

import java.util.Random;

import com.gpergrossi.util.math.SimplexNoise;

public class SimplexNoise2D implements Function2D {

	public static double MAX_OFFSET = 16777216.0f; //2^24
	
	//Manipulation of noise to allow seeded noise generation
	double frequency = 1.0f / 64.0f;
	
	double xOff = 0;
	double yOff = 0;
	double xScale, yScale;
	
	public SimplexNoise2D(long seed, double frequency) {
		Random random = new Random(seed);
		
		int xDir = random.nextInt(2);
		if(xDir == 0) xDir = 1;
		int yDir = random.nextInt(2);
		if(yDir == 0) yDir = 1;
		
		this.xOff = random.nextDouble()*MAX_OFFSET*xDir;
		this.yOff = random.nextDouble()*MAX_OFFSET*yDir;
		
		this.xScale = random.nextInt(2);
		if(this.xScale == 0) this.xScale = 1;
		this.yScale = random.nextInt(2);
		if(this.yScale == 0) this.yScale = 1;

		this.frequency = frequency;
		this.xScale *= frequency;
		this.yScale *= frequency;
	}

	@Override
	public double getValue(double x, double y) {
		return SimplexNoise.noise(x*xScale+xOff, y*yScale+yOff);
	}

}
