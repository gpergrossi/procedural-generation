package com.gpergrossi.util.geom.grids;

import java.util.List;
import java.util.function.Consumer;

import com.gpergrossi.util.data.Hashable;
import com.gpergrossi.util.geom.shapes.Shape;
import com.gpergrossi.util.geom.vectors.Double2D;
import com.gpergrossi.util.geom.vectors.Int2D;

/**
 * Represents a cell in a 2d grid. Grids are very flexible and will support any system
 * of interconnected shapes. The important concepts of a grid are that it has cells
 * and that cells have neighboring cells. Cells should have a fixed location in
 * 2d space which is query-able by a Double2DRange. Cells must also have a unique
 * coordinate object which can be used to lookup a single cell in the grid.
 * 
 * @author Mortus
 *
 * @param <T> Type of GridCells in a Grid / Neighbors of this GridCell
 * @param <C> Type of Coordinate object used to identify unique cells
 */
public interface GridCell<T extends GridCell<T, C>, C extends Hashable> {

	Grid<T, C> getGrid();
	
	C getCoord();
	Double2D getCenter();
	
	Shape getShape();
	void getNeighbors(List<C> output);
	void getNeighbors(Consumer<C> consumer);
	
}
