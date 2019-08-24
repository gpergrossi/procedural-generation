package com.gpergrossi.util.spacial;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.gpergrossi.util.geom.ranges.Int2DRange;
import com.gpergrossi.util.geom.vectors.Double2D;
import com.gpergrossi.util.spacial.BinaryQuadtree.Entry;

/**
 * <p>This class implements an QuadTree which can store and query Entry objects.
 * The Entry interface is an internal interface of this class.</p>
 * 
 * <p>The QuadTree is implemented as a binary tree with each node in the tree 
 * dividing its sub-space in half on the largest of the X, or Y dimensions.</p>
 * 
 * @author Mortus
 *
 * @param <T> The class objects to be stored in this QuadTree
 */
public class BinaryQuadtree<T extends Entry> {
	
	/**
	 * An QuadTree.Entry requires a minimal description of objects
	 * which will be inserted into the QuadTree. This includes a
	 * 2D bounding box and a getDistanceTo() method.
	 *
	 */
	public static interface Entry {
		
		/**
		 * Get the bounding box of this QuadTree.Entry
		 */
		public Int2DRange getRange();
		
		/**
		 * <p>Get the distance from an arbitrary 2D point
		 * to the nearest point in this QuadTree.Entry.</p>
		 * 
		 * <p>This value is allowed to be inexact, but will
		 * result in an inexact result for any of the QuadTree</p>
		 * query methods.
		 * 
		 * @param pt
		 * @return
		 */
		public double getDistanceTo(Double2D pt);
		
	}
	
	public static class QueryResult<T extends Entry> {
		
		private final Double2D queryPoint;
		private double distanceToQueryPoint;
		private Optional<T> entry;
		
		public QueryResult(Double2D queryPoint, double distanceToQueryPoint, Optional<T> entry) {
			this.queryPoint = queryPoint.immutable();
			this.distanceToQueryPoint = distanceToQueryPoint;
			this.entry = entry;
		}
		
		public Double2D getQueryPoint() {
			return queryPoint;
		}
		
		public double getDistanceToQueryPoint() {
			return distanceToQueryPoint;
		}
		
		public Optional<T> getEntry() {
			return entry;
		}
		
	}
	
	/**
	 * The NodeType of an QuadTree object defines how the node's children
	 * are partitioned compared to itself. Possible values are LEAF node (no children),
	 * X_SPLIT (divided equally in half along the X dimension), Y_SPLIT, and Z_SPLIT.
	 */
	protected static enum NodeType {
		LEAF {
			@Override
			public int getSplitValue(Int2DRange range) {
				return 0;
			}
			@Override
			public int compare(Double2D elem, int splitValue) {
				return 0;
			}
			@Override
			public int compare(Int2DRange elem, int splitValue) {
				return 0;
			}
			@Override
			public Int2DRange chop(Int2DRange range, boolean greater) {
				return null;
			}
		}, 
		
		X_SPLIT {
			@Override
			public int getSplitValue(Int2DRange range) {
				return Math.floorDiv(range.minX + range.maxX, 2);
			}
			@Override
			public int compare(Double2D elem, int splitValue) {
				return (int) Math.signum(elem.x() - splitValue);
			}
			@Override
			public int compare(Int2DRange elem, int splitValue) {
				return (elem.maxX <= splitValue) ? -1 : ((elem.minX > splitValue) ? 1 : 0);
			}
			@Override
			public Int2DRange chop(Int2DRange range, boolean greater) {
				int splitValue = this.getSplitValue(range);
				if (greater) {
					return range.resize(splitValue+1, range.minY, range.maxX, range.maxY);
				} else {
					return range.resize(range.minX, range.minY, splitValue, range.maxY);
				}
			}
		}, 
		
		Y_SPLIT {
			@Override
			public int getSplitValue(Int2DRange range) {
				return Math.floorDiv(range.minY + range.maxY, 2);
			}
			@Override
			public int compare(Double2D elem, int splitValue) {
				return (int) Math.signum(elem.y() - splitValue);
			}
			@Override
			public int compare(Int2DRange elem, int splitValue) {
				return (elem.maxY <= splitValue) ? -1 : ((elem.minY > splitValue) ? 1 : 0);
			}
			@Override
			public Int2DRange chop(Int2DRange range, boolean greater) {
				int splitValue = this.getSplitValue(range);
				if (greater) {
					return range.resize(range.minX, splitValue+1, range.maxX, range.maxY);
				} else {
					return range.resize(range.minX, range.minY, range.maxX, splitValue);
				}
			}
		};

		/**
		 * Chooses the splitting dimension for given Int2DRange.
		 * The split dimension will be whichever dimension (X, Y, or Z) has the largest size.
		 * If the given range is 1 by 1 by 1 then the NodeType returned will be LEAF,
		 * indicating that this range cannot be divided any further.
		 * 
		 * @param range - Int2DRange to be considered
		 * @return NodeType selected for the given range
		 */
		public static NodeType choose(Int2DRange range) {
			if (range.width == 1 && range.height == 1) return NodeType.LEAF;
			if (range.width > range.height) {
				return NodeType.X_SPLIT;
			} else {
				return NodeType.Y_SPLIT;
			}
		}
		
		/**
		 * Returns the X, Y, or Z value of the middle of the range provided, depending on this NodeType's division dimension.
		 * @param range the range to be divided
		 * @return the center of this NodeType's division dimension (X, Y, or Z)
		 */
		public abstract int getSplitValue(Int2DRange range);
		
		/**
		 * Compares a 2D point to this NodeType's partitioning plane.
		 * @param elem - 2D coordinate of the element being considered
		 * @param splitValue - the "Split Value" for the node being considered
		 * @return A value of -1 if this element is less than the given splitValue
		 *   or 1 if it is greater on whichever dimension this NodeType uses
		 */
		public abstract int compare(Double2D elem, int splitValue);
		
		/**
		 * Compares a 2D point to this NodeType's partitioning plane.
		 * @param elem - 2D coordinate of the element being considered
		 * @param splitValue - the "Split Value" for the node being considered
		 * @return A value of -1 if this element is less than the given splitValue
		 *   or 1 if it is greater on whichever dimension this NodeType uses
		 */
		public abstract int compare(Int2DRange elem, int splitValue);
		
		/**
		 * Divide the given range in half according to this NodeType's preferential dimension.
		 * @param range - range to be chopped in half
		 * @param greater - whether to return the greater side (else lesser side)
		 * @return half the range, depending on the {@code greater} parameter
		 */
		public abstract Int2DRange chop(Int2DRange range, boolean greater);
		
	}

	
	
	
	
	protected BinaryQuadtree<T> parent = null;
	
	protected Int2DRange range;
	protected NodeType split;
	protected int splitValue;
	
	protected List<T> items;
	protected BinaryQuadtree<T> lesserChild;
	protected BinaryQuadtree<T> greaterChild;
	
	/**
	 * Construct an integer QuadTree with bounds defined by the provided Int2DRange.
	 */
	public BinaryQuadtree(Int2DRange range) {
		this.range = range.copy();
		this.items = new ArrayList<T>();
		this.split = NodeType.choose(this.range);
		this.splitValue = split.getSplitValue(this.range);
	}
	
	/**
	 * Construct an integer QuadTree child with bounds defined by the 
	 * provided Int2DRange and a parent QuadTree
	 */
	protected BinaryQuadtree(BinaryQuadtree<T> parent, Int2DRange range) {
		this(range);
		this.parent = parent;
	}
	
	/**
	 * Computes the number of items stored in this QuadTree
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
	 * Computes the number of nodes in this QuadTree.
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
	 * Inserts a compatible object into this QuadTree.
	 * The object will be added to the deepest child node
	 * that fully contains the object's range.
	 * @param object
	 */
	public void insert(T object) {
		Int2DRange objRange = object.getRange();
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

	public QueryResult<T> getClosest(Double2D point) {
		DEBUG_NODES_SEARCHED = 0;
		QueryResult<T> result = this.getClosest(point, new QueryResult<T>(point, Double.POSITIVE_INFINITY, Optional.empty()));
		return result;
	}
	
	public QueryResult<T> getClosestWithin(Double2D point, double maxDist) {
		DEBUG_NODES_SEARCHED = 0;
		QueryResult<T> result = this.getClosest(point, new QueryResult<T>(point, maxDist, Optional.empty()));
		return result;
	}
	
	protected QueryResult<T> getClosest(Double2D point, QueryResult<T> currentBest) {
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
				currentBest.entry = Optional.of(item);
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
	public List<QueryResult<T>> getIntersects(Double2D point) {
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
	public boolean getIntersects(Double2D point, List<QueryResult<T>> output) {
		DEBUG_NODES_SEARCHED = 0;
		return getIntersectsInternal(point, output);
	}
	
	private boolean getIntersectsInternal(Double2D point, List<QueryResult<T>> output) {
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
				output.add(new QueryResult<T>(point, dist, Optional.of(item)));
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
	
	protected BinaryQuadtree<T> getLesserChild() {
		if (lesserChild == null) lesserChild = new BinaryQuadtree<>(this, split.chop(range, false));
		return lesserChild;
	}

	protected boolean hasGreaterChild() {
		return (greaterChild != null);
	}
	
	protected BinaryQuadtree<T> getGreaterChild() {
		if (greaterChild == null) greaterChild = new BinaryQuadtree<>(this, split.chop(range, true));
		return greaterChild;
	}
	
}
