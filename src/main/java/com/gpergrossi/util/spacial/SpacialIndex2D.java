package com.gpergrossi.util.spacial;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.gpergrossi.util.geom.ranges.Double2DRange;
import com.gpergrossi.util.geom.ranges.Int2DRange;
import com.gpergrossi.util.geom.vectors.Double2D;
import com.gpergrossi.util.geom.vectors.Int2D;

public class SpacialIndex2D {

	private int size;
	private double gridSize;
	private Double2DRange bounds;
	private Map<Int2D, List<Double2D>> entries;
	
	public SpacialIndex2D(double gridSize) {
		this.gridSize = gridSize;
		this.entries = new HashMap<>();
		this.bounds = null;
		this.size = 0;
	}
	
	public void clear() {
		this.entries.clear();
		this.bounds = null;
		this.size = 0;
	}
	
	public int getNearbyPoints(Double2D queryPoint, double queryRadius, Optional<List<Double2D>> nearbyPointsOut) {
		double queryRadiusSquared = queryRadius * queryRadius;
		Int2D cellRangeMin = toGridCell(queryPoint.x() - queryRadius, queryPoint.y() - queryRadius);
		Int2D cellRangeMax = toGridCell(queryPoint.x() + queryRadius, queryPoint.y() + queryRadius);
		Int2DRange cellRange = new Int2DRange(cellRangeMin, cellRangeMax);

		int nearbyCount = 0;
		for (Int2D cell : cellRange.getAllMutable()) {
			List<Double2D> points = entries.get(cell);
			if (points == null) continue;
			
			for (Double2D point : points) {
				if (point.distanceSquaredTo(queryPoint) <= queryRadiusSquared) {
					nearbyCount++;
					if (nearbyPointsOut.isPresent()) nearbyPointsOut.get().add(point);
				}
			}
		}
		
		return nearbyCount;
	}
	
	public void add(Double2D point) {
		Int2D gridCell = toGridCell(point.x(), point.y());
		
		// 1. Add entry to list
		List<Double2D> list = entries.get(gridCell);
		if (list == null) {
			list = new ArrayList<>();
			list.add(point);
			entries.put(gridCell, list);
		} else {
			list.add(point);
		}
		
		// 2. Expand bounds
		if (bounds == null) {
			bounds = new Double2DRange(point, point);
		} else if (!bounds.contains(point)) {
			bounds = bounds.union(new Double2DRange(point, point));
		}
		
		// 3. Increment size
		this.size++;
	}
	
	public boolean remove(Double2D point) {
		Int2D gridCell = toGridCell(point.x(), point.y());
		
		// 1. Remove entry from list
		List<Double2D> list = entries.get(gridCell);
		if (list == null || !list.contains(point)) {
			return false;
		} else {
			list.remove(point);
		}

		// 2. Unless empty, do nothing with bounds
		boolean empty = list.isEmpty();
		if (empty) {
			entries.remove(gridCell);
			bounds = null;
		}
		
		// 3. Decrement size
		this.size--;
		
		return true;
	}
	
	public int getSize() {
		return size;
	}
	
	public Double2DRange getBounds() {
		return bounds.copy();
	}
	
	private Int2D toGridCell(double x, double y) {
		final int cx = (int) Math.floor(x / gridSize);
		final int cy = (int) Math.floor(y / gridSize);
		return new Int2D(cx, cy);
	}

	
	
}
