package com.gpergrossi.util.math.func3d;

import java.util.Random;

import com.gpergrossi.util.math.SimplexNoise;

public class SimplexNoise3D implements Function3D {

	public static double MAX_OFFSET = 16777216.0f; //2^24
	
	double frequency = 1.0f / 64.0f;

	// Manipulation of noise to allow seeded noise generation
	double xOff, yOff, zOff;
	double xDir, yDir, zDir;
	
	public SimplexNoise3D(long seed, double frequency) {
		Random random = new Random(seed);
		
		int xDir = random.nextInt(2);
		if(xDir == 0) xDir = 1;
		int yDir = random.nextInt(2);
		if(yDir == 0) yDir = 1;
		int zDir = random.nextInt(2);
		if(zDir == 0) zDir = 1;
		
		this.xOff = random.nextDouble()*MAX_OFFSET*xDir;
		this.yOff = random.nextDouble()*MAX_OFFSET*yDir;
		this.zOff = random.nextDouble()*MAX_OFFSET*zDir;
		
		this.xDir = random.nextInt(2);
		if(this.xDir == 0) this.xDir = 1;
		this.yDir = random.nextInt(2);
		if(this.yDir == 0) this.yDir = 1;
		this.zDir = random.nextInt(2);
		if(this.zDir == 0) this.zDir = 1;

		this.frequency = frequency;
		this.xDir *= frequency;
		this.yDir *= frequency;
		this.zDir *= frequency;
	}

	@Override
	public double getValue(double x, double y, double z) {
		return SimplexNoise.noise(x*xDir+xOff, y*yDir+yOff, z*zDir+zOff);
	}

}
