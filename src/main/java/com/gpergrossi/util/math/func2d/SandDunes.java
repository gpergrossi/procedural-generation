package com.gpergrossi.util.math.func2d;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.gpergrossi.util.geom.ranges.Int2DRange;
import com.gpergrossi.util.geom.vectors.Int2D;

public class SandDunes extends InfiniteFeatureGrid {

	Function2D flatPatchLayer;
	Function2D baseLayer;
	
	float scale;
	
	List<List<FiniteFeature>> featuresBySize;
	
	public SandDunes(long seed) {
		super(seed, 48, 64);
		Random rand = new Random(seed);
		
		allowTranslation(48, 32);
		
		featuresBySize = new ArrayList<>();
		maxRadius = 0;
		for (int i = 1; i <= 16; i++) {
			List<FiniteFeature> features = new ArrayList<>();
			for (int j = 0; j < 8; j++) {
				FiniteFeature dune = createDune(rand.nextLong(), i);
				features.add(dune);
				maxRadius = Math.max(maxRadius, dune.getRadius());
			}
			featuresBySize.add(features);
		}
		
		baseLayer = FractalNoise2D.builder().withSeed(seed).withPeriod(128).withOctaves(2).withRange(0, 0.25).build();		
		flatPatchLayer = FractalNoise2D.builder().withSeed(seed).withPeriod(1024).withOctaves(3).withRange(-0.3, 1.0).build();
	}
	
	private static FiniteFeature createDune(long seed, float height) {
		int sizeX = 64;
		int offsetX = 16;
		int sizeY = 64;
		
		Int2DRange.Floats details = new Int2DRange.Floats(-sizeX, -sizeY, sizeX, sizeY);
		
		float[] duneX = new float[sizeY*2+1];
		float[] duneZ = new float[sizeY*2+1];
		
		Random rand = new Random(seed);
		
		float lastX = 0;
		float period = 16f;
		float amplitude = 8f;
		float currentX = (rand.nextFloat()-0.5f)*2.0f*amplitude;
		float i = period;
		
		for (int y = -sizeY; y <= sizeY; y++) {
			i++;
			if (i > period) {
				lastX = currentX;
				currentX += (rand.nextFloat()-0.5f)*2.0f*amplitude;
				currentX *= 0.9;
				i = 0;
			}
			
			float val = i / period;
			val = cosInterp(val, lastX, currentX);
			
			float attenuate = (float) Math.max(0, 1.0 - ((float) Math.abs(y)) / sizeY);
			attenuate = (float) Math.pow(attenuate, 0.33);
			
			duneX[y+sizeY] = val;
			duneZ[y+sizeY] = Math.min(1.0f, 0.9f + val*0.1f) * height * attenuate * attenuate;
		}
		
		for (Int2D.StoredFloat tile : details.getAllFloats()) {			
			float arc = duneX[tile.y()+sizeY];
			float arcHeight = duneZ[tile.y()+sizeY];
			
			float effectiveX = tile.x()+offsetX;
			float arcDist = Math.abs(arc - effectiveX);
			
			float val = 0;
			if (effectiveX < arc) {
				val = arcHeight - 0.97f*arcDist;
			} else {
				float smoothing = (float) Math.cos((1.0f - 1.0f/(1.0f + 0.07f*arcDist))*3.14159f*1.1f - 3.14159f*0.1f) * 0.5f + 0.52f;
				float constant = -0.05f * arcDist;
				val = arcHeight*smoothing + constant;
			}
		
			val /= 16.0f;
			if (val < 0) val = 0;
			if (val > 1) val = 1;
			
			tile.setValue(val);
//			if (tile.x() == details.minX || tile.x() == details.maxX || tile.y() == details.minY || tile.y() == details.maxY) tile.setValue(1);
		}
		
		Int2DRange range = details.getTrimmedRange(v -> v <= 0.0f);
		details = range.copyFloats(details, 0, 0);
		
//		for (Int2D.StoredFloat tile : details.getAllFloatsMutable()) {			
//			if (tile.x() == details.minX || tile.x() == details.maxX || tile.y() == details.minY || tile.y() == details.maxY) tile.setValue(1);
//		}
		
		return new FiniteFeature(details);
	}
	
	@Override
	protected FiniteFeature getRandomFeature(Random random, double x, double y) {
		double value = flatPatchLayer.getValue(x, y);
		if (value < 0) return null;
		
		int size = (int) Math.ceil(value * 16.0);
		size = Math.max(random.nextInt(size), random.nextInt(size));
		size = Math.max(0, Math.min(15, size));
		List<FiniteFeature> featuresOfSize = featuresBySize.get(size);
		
		return featuresOfSize.get(random.nextInt(featuresOfSize.size()));
	}
	
	private static float PI = (float) Math.PI;
	
	private static float cosInterp(float val, float lastX, float currentX) {
		val = 0.5f - (float) Math.cos(val * PI) * 0.5f;
		val = (1f - val)*lastX + val*currentX;
		return val;
	}
	
	public boolean isFlat(double x, double y) {		
		double flatPatch = flatPatchLayer.getValue(x, y);
		return (flatPatch < 0);
	}

	@Override
	public double getValue(double x, double y) {		
		double flatness = flatPatchLayer.getValue(x, y);
		if (flatness < 0) return 0;
		
		double base = baseLayer.getValue(x, y);
		return Math.max(super.getValue(x, y), base*flatness);
	}

}
