package com.gpergrossi.util.geom.grids;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import com.gpergrossi.util.geom.ranges.Double2DRange;
import com.gpergrossi.util.geom.ranges.Int2DRange;
import com.gpergrossi.util.geom.vectors.Double2D;
import com.gpergrossi.util.geom.vectors.Int2D;

public class SquareGrid implements Grid<SquareGridCell, Int2D>{

	double gridSize;
	Double2D gridOffset;
	
	@Override
	public Optional<SquareGridCell> getCell(Int2D coord) {
		return Optional.of(new SquareGridCell(this, coord));
	}

	@Override
	public Optional<SquareGridCell> findCell(Double2D point) {
		Double2D pointMutable = point.mutable();
		pointMutable.subtract(gridOffset);
		pointMutable.divide(gridSize);
		
		return getCell(pointMutable.floor());
	}
	
	@Override
	public void findCell(Double2D point, Consumer<SquareGridCell> consumer) {
		Double2D pointMutable = point.mutable();
		pointMutable.subtract(gridOffset);
		pointMutable.divide(gridSize);
		
		Optional<SquareGridCell> cell = getCell(pointMutable.floor());
		if (cell.isPresent()) consumer.accept(cell.get());
	}

	@Override
	public void findCells(Double2DRange range, List<SquareGridCell> outputCells) {
		findCells(range, cell -> outputCells.add(cell));
	}
	
	@Override
	public void findCells(Double2DRange range, Consumer<SquareGridCell> consumer) {
		Double2D minMutable = new Double2D.Mutable(range.minX, range.minY);
		minMutable.subtract(gridOffset);
		minMutable.divide(gridSize);
		Int2D min = minMutable.floor();
		
		Double2D maxMutable = new Double2D.Mutable(range.maxX, range.maxY);		
		maxMutable.subtract(gridOffset);
		maxMutable.divide(gridSize);
		Int2D max = maxMutable.ceil();
		
		Int2DRange cellsRange = new Int2DRange(min, max);
		
		for (Int2D cellCoord : cellsRange.getAllMutable()) {
			Optional<SquareGridCell> cell = getCell(cellCoord);
			if (cell.isPresent()) consumer.accept(cell.get());
		}
	}
	

	
}
