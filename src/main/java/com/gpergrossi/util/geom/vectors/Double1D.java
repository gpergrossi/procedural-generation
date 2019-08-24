package com.gpergrossi.util.geom.vectors;

public class Double1D implements Vector<Double1D> {

	protected double x;
	
	public Double1D() {
		this(0);
	}
	
	public Double1D(double x) {
		this.x = x;
	}
	
	public double x() {
		return x;
	}

	public Double1D redefine(double x) {
		return new Double1D(x);
	}
	
	@Override
	public Double1D copy() {
		return new Double1D(x);
	}
	
	@Override
	public Double1D immutable() {
		return this;
	}
	
	@Override
	public Mutable mutable() {
		return new Mutable(this.x);
	}

	@Override
	public Double1D multiply(double scalar) {
		this.x *= scalar;
		return this;
	}

	@Override
	public Double1D divide(double scalar) {
		this.x /= scalar;
		return this;
	}

	@Override
	public double dot(Double1D vector) {
		return this.x * vector.x;
	}

	@Override
	public Double1D add(Double1D vector) {
		this.x += vector.x;
		return this;
	}

	@Override
	public Double1D subtract(Double1D vector) {
		this.x -= vector.x;
		return this;
	}

	@Override
	public double length() {
		return Math.abs(this.x);
	}

	@Override
	public double lengthSquared() {
		double dx = this.x;
		return dx*dx;
	}
	
	@Override
	public Double1D normalize() {
		return new Double1D(Math.signum(this.x));
	}

	@Override
	public double distanceTo(Double1D vector) {
		return Math.abs(this.x - vector.x);
	}

	@Override
	public double distanceSquaredTo(Double1D vector) {
		double dist = distanceTo(vector);
		return dist*dist;
	}

	@Override
	public int compareTo(Double1D other) {
		int dx = (int) Math.signum(this.x - other.x);
		if (dx != 0) return dx;
		
		return Integer.compare(this.hashCode(), other.hashCode());
	}

	@Override
   public boolean equals(Object obj) {
      if (!(obj instanceof Double1D)) return false;
      return this.equals((Double1D) obj);
   }

   @Override
   public boolean equals(Double1D other) {
      return this.distanceTo(other) < EPSILON;
   }	
	
	public static class Mutable extends Double1D {
		
		public Mutable() {
			super();
		}
		
		public Mutable(double x) {
			super(x);
		}
		
		public void x(double x) {
			this.x = x;
		}
		
		@Override
		public Mutable redefine(double x) {
			this.x = x;
			return this;
		}
		
		@Override
		public Double1D immutable() {
			return new Double1D(x);
		}

		@Override
		public Mutable mutable() {
			return copy();
		}

		@Override
		public Mutable copy() {
			return new Mutable(x);
		}

		@Override
		public Mutable multiply(double scalar) {
			this.x *= scalar;
			return this;
		}

		@Override
		public Mutable divide(double scalar) {
			this.x /= scalar;
			return this;
		}

		@Override
		public Mutable add(Double1D vector) {
			this.x += vector.x;
			return this;
		}

		@Override
		public Mutable subtract(Double1D vector) {
			this.x -= vector.x;
			return this;
		}
		
		@Override
		public Mutable normalize() {
			this.x = Math.signum(this.x);
			return this;
		}
		
	}
	
}
