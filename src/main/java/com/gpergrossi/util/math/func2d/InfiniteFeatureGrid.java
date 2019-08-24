package com.gpergrossi.util.math.func2d;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class InfiniteFeatureGrid implements Function2D {

	List<FiniteFeature> features;
	List<Integer> weights;
	int totalWeight;
	
	long seed;
	int tileSizeX;
	int tileSizeY;
	
	float maxRadius = 0; //TODO: replace with extents left, top, right, bottom (including after rotations)
	
	boolean rotation = false;
	float minAngle = 0, maxAngle = 0;
	
	boolean scaling = false;
	float minScale = 1, maxScale = 1;
	
	boolean translation = false;
	float offsetX = 0, offsetY = 0;
	
	public InfiniteFeatureGrid(long seed, int tileSizeX, int tileSizeY) {
		this.seed = seed;
		this.tileSizeX = tileSizeX;
		this.tileSizeY = tileSizeY;
		this.features = new ArrayList<>();
		this.weights = new ArrayList<>();
	}
	
	public InfiniteFeatureGrid addFeature(int weight, FiniteFeature feature) {
		if (weight <= 0) throw new IllegalArgumentException();
		maxRadius = Math.max(maxRadius, feature.getRadius());
		this.features.add(feature);
		this.weights.add(weight);
		this.totalWeight += weight;
		return this;
	}
	
	public InfiniteFeatureGrid allowRotation(float minAngle, float maxAngle) {
		this.rotation = true;
		this.minAngle = minAngle;
		this.maxAngle = maxAngle;
		return this;
	}
	
	public InfiniteFeatureGrid allowScaling(float minScale, float maxScale) {
		this.scaling = true;
		this.minScale = minScale;
		this.maxScale = maxScale;
		return this;
	}

	public InfiniteFeatureGrid allowTranslation(float offsetX, float offsetY) {
		this.translation = true;
		this.offsetX = offsetX;
		this.offsetY = offsetY;
		return this;
	}
	
	protected FiniteFeature getRandomFeature(Random random, double x, double y) {
		double roll = random.nextInt(totalWeight);
		for (int i = 0; i < features.size(); i++) {
			roll -= weights.get(i);
			if (roll < 0) return features.get(i);
		}
		return null;
	}
	
	protected void createSeed(Random random, int x, int y) {
		random.setSeed(seed);
		long seedX = random.nextInt(Integer.MAX_VALUE);
		long seedY = random.nextInt(Integer.MAX_VALUE);
		
		long rx = (x * seedX);
		long ry = (y * seedY);
		random.setSeed(rx + ry);
	}
	
	@Override
	public double getValue(double x, double y) {
		int tileX = (int) Math.floor(x / tileSizeX);
		int tileY = (int) Math.floor(y / tileSizeY);
		
		double safeRadius = maxRadius*maxScale + Math.sqrt(offsetX*offsetX + offsetY*offsetY);
		double safeRadius2 = safeRadius * safeRadius;
		
		int tileRangeX = (int) Math.ceil(safeRadius / tileSizeX);
		int tileRangeY = (int) Math.ceil(safeRadius / tileSizeY);
		double val = 0;
		
		Random random = new Random();
		float fx = (float) x;
		float fy = (float) y;
		
		for (int i = -tileRangeX; i <= tileRangeX; i++) {
			double xDist = i * tileSizeX;
			
			for (int j = -tileRangeY; j <= tileRangeY; j++) {
				double yDist = j * tileSizeY;
				
				if (xDist*xDist + yDist*yDist > safeRadius2) continue;
				
				double tileVal = getTileVal(random, tileX+i, tileY+j, fx, fy);
				val = (val > tileVal) ? val : tileVal;
			}
		}
		
		return val;
	}

	private double getTileVal(Random random, int tileX, int tileY, float x, float y) {
		createSeed(random, tileX, tileY);
		
		// Get the current tile's center plus possible translation
		float centerX, centerY;
		if (translation) {
			centerX = (tileX+0.5f)*tileSizeX + (random.nextFloat()-0.5f)*offsetX;
			centerY = (tileY+0.5f)*tileSizeY + (random.nextFloat()-0.5f)*offsetY;
		} else {
			centerX = (tileX+0.5f)*tileSizeX;
			centerY = (tileY+0.5f)*tileSizeY;
		}

		// Get a feature for this coordinate
		FiniteFeature feature = getRandomFeature(random, centerX, centerY);
		if (feature == null) return 0;
		
		// Translate the input point
		x -= centerX;
		y -= centerY;
		
		// Rotate around origin (opposite direction as feature would be rotated)
		if (rotation) {
		float angle = random.nextFloat() * (maxAngle - minAngle) + minAngle;
			float sin = (float) Math.sin(-angle);
			float cos = (float) Math.cos(-angle);
			float xr = cos*x - sin*y;
			float yr = sin*x + cos*y;
			x = xr; y = yr;
		}
		
		// Scale (divide by scale by which feature would be multiplied)
		if (scaling) {
			float scale = random.nextFloat() * (maxScale - minScale) + minScale;
			x /= scale;
			y /= scale;
		}
		
		return feature.getValue(x, y);
	}

}
