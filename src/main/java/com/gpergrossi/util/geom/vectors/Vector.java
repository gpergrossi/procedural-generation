package com.gpergrossi.util.geom.vectors;

/**
 * This interface defines a standard for vector classes.
 * It's extending interfaces, IVecotor2D and IVector3D
 * are meant to be implemented by all Vector classes.
 * 
 * This interface, IVector2D, and IVector3D are mostly
 * intended to ensure that vector classes have all of the
 * necessary methods to be used as a vector. Making
 * use of the interface to store multiple TYPES of vectors
 * would be cumbersome but do-able. (i.e. List<IVector<?>> vectors)
 * 
 * @author Mortus
 *
 * @param <T> the vector class that implements this interface. 
 * (e.g. Double2D implements IVector2D<'Double2D'>. Quotes because otherwise javadoc comments eat brackets)
 */
public interface Vector<T extends Vector<T>> extends Comparable<T> {

	public static final double EPSILON = 0.001;
	public static final double EPSILON2 = Double2D.EPSILON * Double2D.EPSILON;
	
	/**
	 * The only method that returns a new vector.
	 * @return A new vector that is a copy of this one
	 */
	public T copy();
	
	/**
	 * Should return an immutable copy of this instance.
	 * In order for this to be implemented easily, the base
	 * class of all Vector types should be immutable and extend
	 * this interface. Then there should be a mutable class 
	 * extending the base class.
	 * @return
	 */
	public T immutable();
	
	/**
	 * Should return a mutable copy of this instance that is distinct from the current instance.
	 * Changes to the returned copy should not be seen by the current instance.
	 * @return
	 */
	public T mutable();
	
	public T multiply(double scalar);
	public T divide(double scalar);

	public double dot(T vector);
	
	public T add(T vector);
	public T subtract(T vector);
	
	public double length();
	public double lengthSquared();
	public T normalize();
	
	public double distanceTo(T vector);
	public double distanceSquaredTo(T vector);
	
	@Override
	public int compareTo(T other);
	public boolean equals(T other);
	
}
