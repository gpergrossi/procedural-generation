package com.gpergrossi.voronoi.math;

public class Quadratic extends Function {
	
	public static Function fromCoefficients(final double a, final double b, final double c) {
		if (VoronoiUtils.nearlyEqual(a, 0.0)) {
			if (VoronoiUtils.nearlyEqual(b, 0.0)) {
				return new HorizontalLine(c);
			} else {
				return new Linear(b, c);
			}
		} else {
			return new Quadratic(a, b, c);
		}
	}
	
	public static Function fromPointAndLine(final double pointX, final double pointY, final double horizontalLineY) {
		final double deltaY = pointY - horizontalLineY;
		if (VoronoiUtils.nearlyEqual(deltaY, 0.0)) {
			return new VerticalLine(pointX);
		} else {
			final double reciprocal = 1.0 / deltaY;
			final double a = 0.5 * reciprocal;
			final double b = -pointX * reciprocal;
			final double c = 0.5 * (pointX * pointX + pointY * pointY - horizontalLineY * horizontalLineY) * reciprocal;
			return Quadratic.fromCoefficients(a, b, c);
			
		}
	}

	protected final double a, b, c;
	
	protected Quadratic(double a, double b, double c) {
		this.a = a;
		this.b = b;
		this.c = c;
	}

	@Override
	public IntersectionResult intersect(Function other) {
		if (other instanceof Quadratic) {
			return this.intersect((Quadratic) other);
		} else if (other instanceof VerticalLine) {
			return this.intersect((VerticalLine) other);
		}
		throw new UnsupportedOperationException();
	}
	
	public IntersectionResult intersect(Quadratic other) {
		// Intersections are computed by subtracting one parabola from the other and finding the zeros.
		// To keep things simple, we want the A coefficient of the difference Parabola to be positive:
		final double diffA, diffB, diffC;
		if (this.a > other.a) {
			diffA = this.a - other.a;
			diffB = this.b - other.b;
			diffC = this.c - other.c;
		} else {
			diffA = other.a - this.a;
			diffB = other.b - this.b;
			diffC = other.c - this.c;
		}
		
		IntersectionResult zeros = Quadratic.fromCoefficients(diffA, diffB, diffC).zeros();
		return zeros.convertZerosToIntersections(this, other);
	}
	
	public IntersectionResult intersect(VerticalLine other) {
		final double x = other.x();
		final double y = this.apply(x);
		return IntersectionResult.of(this, other, x, y);
	}
	
	@Override
	public IntersectionResult zeros() {
		// The quadratic formula is:  
		//   -b +/- sqrt(b*b - 4*a*c) 
		//   ------------------------ 
		//             2*a            
		final double bSquaredMinus4AC = b*b - 4.0*a*c;
		if (VoronoiUtils.nearlyEqual(bSquaredMinus4AC, 0)) {
			// If bSquaredMinus4AC is nearly zero, then return a single zero:
			final double x = -b / (2.0*a);
			return IntersectionResult.zeros(this, x);
		} else if (bSquaredMinus4AC < 0) {
			// If bSquaredMinus4AC is negative, there are no zeros:
			return IntersectionResult.emptyZeros(this);
		} else {
			// Otherwise, there are two zeros:
			final double sqrtBSqrMinus4AC = Math.sqrt(bSquaredMinus4AC);
			final double leftX = (-b - sqrtBSqrMinus4AC) / (2.0*a);
			final double rightX = (-b + sqrtBSqrMinus4AC) / (2.0*a);
			return IntersectionResult.zeros(this, leftX, rightX);
		}
	}

	@Override
	public double apply(double x) {
		return this.a * x * x + this.b * x + this.c;
	}
	
}
