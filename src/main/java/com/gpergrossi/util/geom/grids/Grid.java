package com.gpergrossi.util.geom.grids;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import com.gpergrossi.util.data.Hashable;
import com.gpergrossi.util.geom.ranges.Double2DRange;
import com.gpergrossi.util.geom.vectors.Double2D;
import com.gpergrossi.util.geom.vectors.Int2D;

/**
 * Represents a 2d grid. Grids are very flexible and will support any system of 
 * interconnected shapes. The important concepts of a grid are:
 * 1. A Grid has Cells
 * 2. For any point in 2d space there is at most one cell (no overlap)
 * 3. Cells have a fixed location in 2d space which is query-able by a Double2DRange
 * 4. Cells have a unique coordinate object which can be used to lookup a single cell in the grid
 * 5. Cells have neighbor cells
 * 
 * I believe this set of requirements provides a balance between the flexibility and usefulness of
 * the Grid interface.
 * 
 * @author Gregary Pergrossi
 *
 * @param <T> Type of GridCells in a Grid / Neighbors of this GridCell
 * @param <C> Type of Coordinate object used to identify unique cells
 */
public interface Grid<T extends GridCell<T, C>, C extends Hashable> {

	public Optional<T> getCell(C coord);
	
	public Optional<T> findCell(Double2D point);
	public void findCell(Double2D point, Consumer<T> consumer);
	public void findCells(Double2DRange range, List<T> outputCells);
	public void findCells(Double2DRange range, Consumer<T> consumer);
	
}
