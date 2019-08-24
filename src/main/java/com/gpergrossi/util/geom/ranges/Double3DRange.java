package com.gpergrossi.util.geom.ranges;

import java.util.Random;

import com.gpergrossi.util.geom.vectors.Double3D;

public class Double3DRange {
	
	public final double minX, maxX;
	public final double minY, maxY;
	public final double minZ, maxZ;
	public final double width, height, depth;
	
	public Double3DRange(Double3DRange range) {
		this(range.minX, range.minY, range.minZ, range.maxX, range.maxY, range.maxZ);
	}
	
	public Double3DRange(Double3D start, Double3D end) {
		this(start.x(), start.y(), start.z(), end.x(), end.y(), end.z());
	}
	
	public Double3DRange(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
		this.minX = minX;
		this.minY = minY;
		this.minZ = minZ;
		this.maxX = maxX;
		this.maxY = maxY;
		this.maxZ = maxZ;
		this.width = Math.max(maxX - minX, 0);
		this.height = Math.max(maxY - minY, 0);
		this.depth = Math.max(maxZ - minZ, 0);
	}

	public Double3DRange resize(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
		return new Double3DRange(minX, minY, minZ, maxX, maxY, maxZ);
	}
	
	public double size() {
		return width*height*depth;
	}
	
	public boolean isEmpty() {
		if (Double.isNaN(width)) return true;
		if (Double.isNaN(height)) return true;
		if (Double.isNaN(depth)) return true;
		return size() <= 0;
	}
	
	public boolean contains(double x, double y, double z) {
		return (x >= minX && y >= minY && z >= minZ && x <= maxX && y <= maxY && z <= maxZ);
	}

	public boolean contains(Double3D double3d) {
		return contains(double3d.x(), double3d.y(), double3d.z());
	}
	
	public boolean onBorder(double x, double y, double z, double borderPadding) {
		return contains(x, y, z) && (
			   (x < minX + borderPadding || y < minY + borderPadding || z < minZ + borderPadding) 
			|| (x > maxX - borderPadding || y > maxY - borderPadding || y > maxZ - borderPadding)
		);
	}
	
	public boolean onBorder(Double3D pt, double borderPadding) {
		return onBorder(pt.x(), pt.y(), pt.z(), borderPadding);
	}
	
	public Double3DRange grow(double padXMin, double padYMin, double padZMin, double padXMax, double padYMax, double padZMax) {
		return resize(minX-padXMin, minY-padYMin, minZ-padZMin, maxX+padXMax, maxY+padYMax, maxZ+padZMax);
	}
	
	public Double3DRange grow(double padX, double padY, double padZ) {
		return grow(padX, padY, padZ, padX, padY, padZ);
	}

	public Double3DRange grow(double pad) {
		return grow(pad, pad, pad);
	}
	
	public Double3DRange shrink(double insetX, double insetY, double insetZ) {
		return grow(-insetX, -insetY, -insetZ);
	}
	
	public Double3DRange shrink(double inset) {
		return grow(-inset, -inset, -inset);
	}
	
	public Double3DRange intersect(Double3DRange other) {
		double minX = Math.max(this.minX, other.minX);
		double minY = Math.max(this.minY, other.minY);
		double minZ = Math.max(this.minZ, other.minZ);
		double maxX = Math.min(this.maxX, other.maxX);
		double maxY = Math.min(this.maxY, other.maxY);
		double maxZ = Math.min(this.maxZ, other.maxZ);
		return resize(minX, minY, minZ, maxX, maxY, maxZ);
	}

	public Double3DRange offset(double offsetX, double offsetY, double offsetZ) {
		return resize(minX+offsetX, minY+offsetY, minZ+offsetZ, maxX+offsetX, maxY+offsetY, maxZ+offsetZ);
	}
	
	public Double3DRange scale(double scaleUp) {
		return resize(minX*scaleUp, minY*scaleUp, minZ*scaleUp, maxX*scaleUp, maxY*scaleUp, maxZ*scaleUp);
	}

	public Double3D random(Random random) {
		return new Double3D(randomX(random), randomY(random), randomZ(random));
	}

	public double randomX(Random random) {
		return random.nextDouble()*width+minX;
	}
	
	public double randomY(Random random) {
		return random.nextDouble()*height+minY;
	}
	
	public double randomZ(Random random) {
		return random.nextDouble()*depth+minZ;
	}
	
	@Override
	public String toString() {
		return "("+minX+", "+minY+", "+minZ+") to ("+maxX+", "+maxY+", "+maxZ+")";
	}

	public Double3D getCenter() {
		return new Double3D((minX/2+maxX/2), (minY/2+maxY/2), (minZ/2+maxZ/2));
	}
	
}
