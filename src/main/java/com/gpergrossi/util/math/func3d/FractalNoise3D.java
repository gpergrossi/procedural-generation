package com.gpergrossi.util.math.func3d;

import java.util.Random;

public class FractalNoise3D implements Function3D {

	public static Builder builder() {
		return new Builder();
	};
	
	public static class Builder {
		private long seed = 0L;
		private double persistence = 0.5;
		private double frequency = 1;
		private double min = -1, max = 1;
		private int octaves = 1;
		
		public Builder() {}
		
		public Builder withSeed(long seed) { this.seed = seed; return this; }
		public Builder withFrequency(double frequency) { this.frequency = frequency; return this; }
		public Builder withPeriod(double period) { this.frequency = 1.0/period; return this; }
		public Builder withMin(double min) { this.min = min; return this; }
		public Builder withMax(double max) { this.max = max; return this; }
		public Builder withRange(double min, double max) { this.min = min; this.max = max; return this; }
		public Builder withOctaves(int numOctaves) { this.octaves = numOctaves; return this; }
		public Builder withPersistence(double persistence) { this.persistence = persistence; return this; }
		public Builder withOctaves(int numOctaves, double persistence) { this.octaves = numOctaves; this.persistence = persistence; return this; }
		
		public FractalNoise3D build() {
			return new FractalNoise3D(seed, frequency, octaves, min, max, persistence);
		}
		
	}
	
	private final long seed;
	private final double persistence;
	private final double frequency;
	private final double scale, offset;
	private final int octaves;
	private final SimplexNoise3D[] generators;
	
	private FractalNoise3D(long seed, double frequency, int octaves, double min, double max, double persistence) {
		this.seed = seed;
		this.persistence = persistence;
		this.frequency = frequency;
		this.octaves = octaves;
		
		// Output normally on range (-1, 1), Re-map to (min, max).
		this.scale = (max - min) / 2.0;
		this.offset = min + scale;
		
		// Create generators
		Random r = new Random(this.seed);
		generators = new SimplexNoise3D[octaves];
		double power = 1.0;
		for(int i = 0; i < octaves; i++) {
			generators[i] = new SimplexNoise3D(r.nextLong(), this.frequency*power);
			power *= 2.0;
		}
	}
	
//	public FractalNoise2D(long seed, double frequency, int octaves) {
//		this(seed, frequency, octaves, -1.0, 1.0, 0.5);
//	}
	
	/**
	 * Returns a value between -1.0 and 1.0. More octaves
	 * make values more likely to be in the middle.
	 */
	@Override
	public double getValue(double x, double y, double z) {
		double value = 0;
		double dividend = 0;
		double multiple = 1;
		for(int i = 0; i < octaves; i++) {
			value += generators[i].getValue(x, y, z)*multiple;
			dividend += multiple;
			multiple *= this.persistence;
		}
		value /= dividend;
		return value * scale + offset;
	}

}
