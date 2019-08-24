package com.gpergrossi.util.geom.grids;

import java.util.List;
import java.util.function.Consumer;

import com.gpergrossi.util.geom.shapes.Shape;
import com.gpergrossi.util.geom.shapes.Rect;
import com.gpergrossi.util.geom.vectors.Double2D;
import com.gpergrossi.util.geom.vectors.Int2D;

public class SquareGridCell implements GridCell<SquareGridCell, Int2D> {

	private SquareGrid grid;
	private Int2D coord;
	private Rect shape;
	private Double2D center;
	
	public SquareGridCell(SquareGrid grid, Int2D coord) {
		this.grid = grid;
		this.coord = coord;
		
		double minX = coord.x() * grid.gridSize + grid.gridOffset.x();
		double minY = coord.y() * grid.gridSize + grid.gridOffset.y();
		this.shape = new Rect(minX, minY, grid.gridSize, grid.gridSize);
		this.center = shape.getCentroid();
	}

	@Override
	public Grid<SquareGridCell, Int2D> getGrid() {
		return grid;
	}
	
	@Override
	public Int2D getCoord() {
		return coord;
	}

	@Override
	public Double2D getCenter() {
		return center;
	}

	@Override
	public Shape getShape() {
		return shape;
	}

	@Override
	public void getNeighbors(List<Int2D> output) {
		getNeighbors(neighbor -> output.add(neighbor));
	}
	
	@Override
	public void getNeighbors(Consumer<Int2D> consumer) {
		consumer.accept( new Int2D(coord.x()-1, coord.y()  ) );
		consumer.accept( new Int2D(coord.x(),   coord.y()-1) );
		consumer.accept( new Int2D(coord.x()+1, coord.y()  ) );
		consumer.accept( new Int2D(coord.x(),   coord.y()+1) );
	}
	
}
