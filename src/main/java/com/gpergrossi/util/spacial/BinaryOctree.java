package com.gpergrossi.util.spacial;

import java.util.ArrayList;
import java.util.List;

import com.gpergrossi.util.geom.ranges.Int3DRange;
import com.gpergrossi.util.geom.vectors.Double3D;
import com.gpergrossi.util.spacial.BinaryOctree.Entry;

/**
 * <p>This class implements an Octree which can store and query Entry objects.
 * The Entry interface is an internal interface of this class.</p>
 * 
 * <p>The octree is implemented as a binary tree with each node in the tree 
 * dividing its sub-space in half on the largest of the X, Y or Z dimensions.</p>
 * 
 * @author Mortus
 *
 * @param <T> The class objects to be stored in this Octree
 */
public class BinaryOctree<T extends Entry> {
	
	/**
	 * An Octree.Entry requires a minimal description of objects
	 * which will be inserted into the Octree. This includes a
	 * 3D bounding box and a getDistanceTo() method.
	 *
	 */
	public static interface Entry {
		
		/**
		 * Get the bounding box of this Entry
		 */
		public Int3DRange getRange();
		
		/**
		 * <p>Get the distance from an arbitrary 3D point
		 * to the nearest point in this Entry.</p>
		 * 
		 * <p>This value is allowed to be inexact, but will
		 * result in an inexact result for any of the Octree</p>
		 * query methods.
		 * 
		 * <p>A negative value should be returned if the 3D
		 * point lies inside this Entry</p>
		 * 
		 * @param pt
		 * @return
		 */
		public double getDistanceTo(Double3D pt);
		
	}
	
	public static class QueryResult<T extends Entry> {
		
		private final Double3D queryPoint;
		private double distanceToQueryPoint;
		private T entry;
		
		public QueryResult(Double3D queryPoint, double distanceToQueryPoint, T entry) {
			this.queryPoint = queryPoint.immutable();
			this.distanceToQueryPoint = distanceToQueryPoint;
			this.entry = entry;
		}
		
		public Double3D getQueryPoint() {
			return queryPoint;
		}
		
		public double getDistanceToQueryPoint() {
			return distanceToQueryPoint;
		}
		
		public T getEntry() {
			return entry;
		}
		
	}
	
	
	
	/**
	 * The NodeType of an Octree object defines how the node's children
	 * are partitioned compared to itself. Possible values are LEAF node (no children),
	 * X_SPLIT (divided equally in half along the X dimension), Y_SPLIT, and Z_SPLIT.
	 */
	protected static enum NodeType {
		LEAF {
			@Override
			public int getSplitValue(Int3DRange range) {
				return 0;
			}
			@Override
			public int compare(Double3D elem, int splitValue) {
				return 0;
			}
			@Override
			public int compare(Int3DRange elem, int splitValue) {
				return 0;
			}
			@Override
			public Int3DRange chop(Int3DRange range, boolean greater) {
				return null;
			}
		}, 
		
		X_SPLIT {
			@Override
			public int getSplitValue(Int3DRange range) {
				return Math.floorDiv(range.minX + range.maxX, 2);
			}
			@Override
			public int compare(Double3D elem, int splitValue) {
				return (int) Math.signum(elem.x() - splitValue);
			}
			@Override
			public int compare(Int3DRange elem, int splitValue) {
				return (elem.maxX <= splitValue) ? -1 : ((elem.minX > splitValue) ? 1 : 0);
			}
			@Override
			public Int3DRange chop(Int3DRange range, boolean greater) {
				int splitValue = this.getSplitValue(range);
				if (greater) {
					return range.resize(splitValue+1, range.minY, range.minZ, range.maxX, range.maxY, range.maxZ);
				} else {
					return range.resize(range.minX, range.minY, range.minZ, splitValue, range.maxY, range.maxZ);
				}
			}
		}, 
		
		Y_SPLIT {
			@Override
			public int getSplitValue(Int3DRange range) {
				return Math.floorDiv(range.minY + range.maxY, 2);
			}
			@Override
			public int compare(Double3D elem, int splitValue) {
				return (int) Math.signum(elem.y() - splitValue);
			}
			@Override
			public int compare(Int3DRange elem, int splitValue) {
				return (elem.maxY <= splitValue) ? -1 : ((elem.minY > splitValue) ? 1 : 0);
			}
			@Override
			public Int3DRange chop(Int3DRange range, boolean greater) {
				int splitValue = this.getSplitValue(range);
				if (greater) {
					return range.resize(range.minX, splitValue+1, range.minZ, range.maxX, range.maxY, range.maxZ);
				} else {
					return range.resize(range.minX, range.minY, range.minZ, range.maxX, splitValue, range.maxZ);
				}
			}
		},  
		
		Z_SPLIT {
			@Override
			public int getSplitValue(Int3DRange range) {
				return Math.floorDiv(range.minZ + range.maxZ, 2);
			}
			@Override
			public int compare(Double3D elem, int splitValue) {
				return (int) Math.signum(elem.z() - splitValue);
			}
			@Override
			public int compare(Int3DRange elem, int splitValue) {
				return (elem.maxZ <= splitValue) ? -1 : ((elem.minZ > splitValue) ? 1 : 0);
			}
			@Override
			public Int3DRange chop(Int3DRange range, boolean greater) {
				int splitValue = this.getSplitValue(range);
				if (greater) {
					return range.resize(range.minX, range.minY, splitValue+1, range.maxX, range.maxY, range.maxZ);
				} else {
					return range.resize(range.minX, range.minY, range.minZ, range.maxX, range.maxY, splitValue);
				}
			}
		};

		/**
		 * Chooses the splitting dimension for given Int3DRange.
		 * The split dimension will be whichever dimension (X, Y, or Z) has the largest size.
		 * If the given range is 1 by 1 by 1 then the NodeType returned will be LEAF,
		 * indicating that this range cannot be divided any further.
		 * 
		 * @param range - Int3DRange to be considered
		 * @return NodeType selected for the given range
		 */
		public static NodeType choose(Int3DRange range) {
			if (range.width == 1 && range.height == 1 && range.depth == 1) return NodeType.LEAF;
			if (range.width > range.height) {
				if (range.width > range.depth) {
					return NodeType.X_SPLIT;
				} else {
					return NodeType.Z_SPLIT;
				}
			} else {
				if (range.height > range.depth) {
					return NodeType.Y_SPLIT;
				} else {
					return NodeType.Z_SPLIT;
				}
			}
		}
		
		/**
		 * Returns the X, Y, or Z value of the middle of the range provided, depending on this NodeType's division dimension.
		 * @param range the range to be divided
		 * @return the center of this NodeType's division dimension (X, Y, or Z)
		 */
		public abstract int getSplitValue(Int3DRange range);
		
		/**
		 * Compares a 3D point to this NodeType's partitioning plane.
		 * @param elem - 3D coordinate of the element being considered
		 * @param splitValue - the "Split Value" for the node being considered
		 * @return A value of -1 if this element is less than the given splitValue
		 *   or 1 if it is greater on whichever dimension this NodeType uses
		 */
		public abstract int compare(Double3D elem, int splitValue);
		
		/**
		 * Compares a 3D point to this NodeType's partitioning plane.
		 * @param elem - 3D coordinate of the element being considered
		 * @param splitValue - the "Split Value" for the node being considered
		 * @return A value of -1 if this element is less than the given splitValue
		 *   or 1 if it is greater on whichever dimension this NodeType uses
		 */
		public abstract int compare(Int3DRange elem, int splitValue);
		
		/**
		 * Divide the given range in half according to this NodeType's preferential dimension.
		 * @param range - range to be chopped in half
		 * @param greater - whether to return the greater side (else lesser side)
		 * @return half the range, depending on the {@code greater} parameter
		 */
		public abstract Int3DRange chop(Int3DRange range, boolean greater);
		
	}

	
	
	
	
	protected BinaryOctree<T> parent = null;
	
	protected Int3DRange range;
	protected NodeType split;
	protected int splitValue;
	
	protected List<T> items;
	protected BinaryOctree<T> lesserChild;
	protected BinaryOctree<T> greaterChild;
	
	/**
	 * Construct an integer Octree with bounds defined by the provided Int3DRange.
	 */
	public BinaryOctree(Int3DRange range) {
		this.range = range.copy();
		this.items = new ArrayList<T>();
		this.split = NodeType.choose(this.range);
		this.splitValue = split.getSplitValue(this.range);
	}
	
	/**
	 * Construct an integer Octree child with bounds defined by the 
	 * provided Int3DRange and a parent Octree
	 */
	protected BinaryOctree(BinaryOctree<T> parent, Int3DRange range) {
		this(range);
		this.parent = parent;
	}
	
	/**
	 * Computes the number of items stored in this Octree
	 * Only considers this node and all descendants (children, grand children, etc.)
	 * @return
	 */
	public int size() {
		int size = this.items.size();
		if (hasLesserChild()) size += getLesserChild().size();
		if (hasGreaterChild()) size += getGreaterChild().size();
		return size;
	}
	
	/**
	 * Computes the number of nodes in this Octree.
	 * Only considers this node and all descendants (children, grand children, etc.)
	 * @return
	 */
	public int numNodes() {
		int numNodes = 1;
		if (hasLesserChild()) numNodes += getLesserChild().numNodes();
		if (hasGreaterChild()) numNodes += getGreaterChild().numNodes();
		return numNodes;
	}
	
	/**
	 * Inserts a compatible object into this Octree.
	 * The object will be added to the deepest child node
	 * that fully contains the object's range.
	 * @param object
	 */
	public void insert(T object) {
		Int3DRange objRange = object.getRange();
		int compare = split.compare(objRange, splitValue);
		
		if (compare == 0) {
			items.add(object);
		} else if (compare > 0) {
			getGreaterChild().insert(object);
		} else if (compare < 0) {
			getLesserChild().insert(object);
		}
	}

	public static int DEBUG_NODES_SEARCHED = 0;

	public QueryResult<T> getClosest(Double3D point) {
		DEBUG_NODES_SEARCHED = 0;
		QueryResult<T> result = this.getClosest(point, new QueryResult<T>(point, Double.POSITIVE_INFINITY, null));
		return result;
	}
	
	protected QueryResult<T> getClosest(Double3D point, QueryResult<T> currentBest) {
		DEBUG_NODES_SEARCHED++;
		
		int compare = split.compare(point, splitValue);
		if (compare <= 0) {
			if (hasLesserChild()) currentBest = getLesserChild().getClosest(point, currentBest);
			if (-compare < currentBest.distanceToQueryPoint) {
				if (hasGreaterChild()) currentBest = getGreaterChild().getClosest(point, currentBest);
			}
		} else {
			if (hasGreaterChild()) currentBest = getGreaterChild().getClosest(point, currentBest);
			if (compare-1 < currentBest.distanceToQueryPoint) {
				if (hasLesserChild()) currentBest = getLesserChild().getClosest(point, currentBest);
			}
		}
		
		for (T item : items) {
			double dist = item.getDistanceTo(point);
			if (dist < currentBest.getDistanceToQueryPoint()) {
				currentBest.entry = item;
				currentBest.distanceToQueryPoint = dist;
			}
		}
		
		return currentBest;
	}

	/**
	 * Get the objects in the OctTree that intersect with the given input point.
	 * @param point - point on which to check for intersects
	 * @return List of intersecting objects
	 * @see getIntersects(Int3D, List&lt;QueryResult&lt;T&gt;&gt;) if you wish to provide a list object or do not need the list results.
	 */
	public List<QueryResult<T>> getIntersects(Double3D point) {
		DEBUG_NODES_SEARCHED = 0;
		List<QueryResult<T>> intersects = new ArrayList<>();
		this.getIntersectsInternal(point, intersects);
		return intersects;
	}
	
	/**
	 * Get the objects in the OctTree that intersect with the given input point.
	 * @param point - point on which to check for intersects
	 * @param output - the list to add hits to, or null if you don't need the list. (A list will NOT be created)
	 * @return true if there was an intersect, false otherwise
	 */
	public boolean getIntersects(Double3D point, List<QueryResult<T>> output) {
		DEBUG_NODES_SEARCHED = 0;
		return getIntersectsInternal(point, output);
	}
	
	public boolean getIntersectsInternal(Double3D point, List<QueryResult<T>> output) {
		DEBUG_NODES_SEARCHED++;
		boolean added = false;
		
		int compare = split.compare(point, splitValue);
		if (compare <= 0) {
			if (hasLesserChild()) added |= getLesserChild().getIntersects(point, output);
		} else {
			if (hasGreaterChild()) added |= getGreaterChild().getIntersects(point, output);
		}
		
		// No output list requested. Since something was intersected, return true
		if (output == null && added == true) return true;
		
		for (T item : items) {
			double dist = item.getDistanceTo(point);
			if (dist > 0) continue;
			
			if (output != null) {
				added = true;
				output.add(new QueryResult<T>(point, dist, item));
			} else {
				// No output list requested. Since something was intersected, return true
				return true;
			}
		}
		
		return added;
	}
	
	protected boolean hasLesserChild() {
		return (lesserChild != null);
	}
	
	protected BinaryOctree<T> getLesserChild() {
		if (lesserChild == null) lesserChild = new BinaryOctree<>(this, split.chop(range, false));
		return lesserChild;
	}

	protected boolean hasGreaterChild() {
		return (greaterChild != null);
	}
	
	protected BinaryOctree<T> getGreaterChild() {
		if (greaterChild == null) greaterChild = new BinaryOctree<>(this, split.chop(range, true));
		return greaterChild;
	}
	
}
