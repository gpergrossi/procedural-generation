package com.gpergrossi.util.geom.ranges;

import java.util.Random;
import com.gpergrossi.util.geom.shapes.Rect;
import com.gpergrossi.util.geom.vectors.Double2D;
import com.gpergrossi.util.geom.vectors.Int2D;

public class Double2DRange {
	
	public final double minX, maxX;
	public final double minY, maxY;
	public final double width, height;
	
	public static Double2DRange fromRect(Rect rect) {
		double minX = rect.minX();
		double minY = rect.minY();
		double maxX = rect.maxX();
		double maxY = rect.maxY();
		return new Double2DRange(minX, minY, maxX, maxY);
	}

	public Double2DRange(Double2DRange range) {
		this(range.minX, range.minY, range.maxX, range.maxY);
	}
	
	public Double2DRange(Double2D start, Double2D end) {
		this(start.x(), start.y(), end.x(), end.y());
	}
	
	public Double2DRange copy() {
		return this.resize(minX, minY, maxX, maxY);
	}
	
	public Double2DRange(double minX, double minY, double maxX, double maxY) {
		this.minX = minX;
		this.minY = minY;
		this.maxX = maxX;
		this.maxY = maxY;
		this.width = Math.max(maxX - minX, 0);
		this.height = Math.max(maxY - minY, 0);
	}

	protected Double2DRange resize(double minX, double minY, double maxX, double maxY) {
		return new Double2DRange(minX, minY, maxX, maxY);
	}
	
	public double area() {
		return width*height;
	}
	
	public boolean isEmpty() {
		if (Double.isNaN(width)) return true;
		if (Double.isNaN(height)) return true;
		return area() <= 0;
	}
	
	public boolean contains(double x, double y) {
		return (x >= minX && y >= minY && x <= maxX && y <= maxY);
	}
	
	public boolean contains(Double2D pt) {
		return (pt.x() >= minX && pt.y() >= minY && pt.x() <= maxX && pt.y() <= maxY);
	}
	
	public boolean contains(Int2D pt) {
		return contains(pt.x(), pt.y());
	}
	
	public boolean onBorder(double x, double y, double borderPadding) {
		return contains(x, y) && (x < minX+borderPadding || y < minY+borderPadding || x > maxX-borderPadding || y > maxY-borderPadding);
	}
	
	public boolean onBorder(Double2D pt, double borderPadding) {
		return onBorder(pt.x(), pt.y(), borderPadding);
	}
	
	public Double2DRange grow(double padXMin, double padYMin, double padXMax, double padYMax) {
		return resize(minX-padXMin, minY-padYMin, maxX+padXMax, maxY+padYMax);
	}
	
	public Double2DRange grow(double padX, double padY) {
		return grow(padX, padY, padX, padY);
	}

	public Double2DRange grow(double pad) {
		return grow(pad, pad, pad, pad);
	}
	
	public Double2DRange shrink(double insetX, double insetY) {
		return grow(-insetX, -insetY, -insetX, -insetY);
	}
	
	public Double2DRange shrink(double inset) {
		return grow(-inset, -inset, -inset, -inset);
	}
	
	public Double2DRange scale(double scaleUp) {
		return resize(minX*scaleUp, minY*scaleUp, maxX*scaleUp, maxY*scaleUp);
	}
	
	public Double2DRange intersect(Double2DRange other) {
		double minX = Math.max(this.minX, other.minX);
		double minY = Math.max(this.minY, other.minY);
		double maxX = Math.min(this.maxX, other.maxX);
		double maxY = Math.min(this.maxY, other.maxY);
		return resize(minX, minY, maxX, maxY);
	}
	
	public Double2DRange union(Double2DRange other) {
		double minX = Math.min(this.minX, other.minX);
		double minY = Math.min(this.minY, other.minY);
		double maxX = Math.max(this.maxX, other.maxX);
		double maxY = Math.max(this.maxY, other.maxY);
		return resize(minX, minY, maxX, maxY);
	}

	public Double2DRange offset(double offsetX, double offsetY) {
		return resize(minX+offsetX, minY+offsetY, maxX+offsetX, maxY+offsetY);
	}

	public double randomX(Random random) {
		return random.nextDouble()*width+minX;
	}
	
	public double randomY(Random random) {
		return random.nextDouble()*height+minY;
	}
	
	public Double2D random(Random random) {
		return new Double2D(randomX(random), randomY(random));
	}
	
	public Int2DRange toInt() {
		return new Int2DRange((int)Math.floor(minX), (int)Math.floor(minY), (int)Math.ceil(maxX), (int)Math.ceil(maxY));
	}
	
	@Override
	public String toString() {
		return "("+minX+", "+minY+") to ("+maxX+", "+maxY+")";
	}
	
}
