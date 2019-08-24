package com.gpergrossi.procgen;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.gpergrossi.util.geom.vectors.Double2D;
import com.gpergrossi.voronoi.math.VoronoiUtils;

public class VoronoiMathTest {

	@Test
	public void testComputeBreakpointSameYCorrectXOrder() {
		Optional<Double2D> bp; 
		
		bp = VoronoiUtils.computeBreakpoint(10, new Double2D(-5, 5), new Double2D(5, 5));
		assertTrue(bp.isPresent());
		assertTrue(VoronoiUtils.nearlyEqual(new Double2D(0, 5), bp.get()));
		
		bp = VoronoiUtils.computeBreakpoint(0, new Double2D(-5, 5), new Double2D(5, 5));
		assertTrue(bp.isPresent());
		assertTrue(VoronoiUtils.nearlyEqual(new Double2D(0, 5), bp.get()));
	}
	
	@Test
	public void testComputeBreakpointSameYIncorrectXOrder() {
		Optional<Double2D> bp; 
		
		bp = VoronoiUtils.computeBreakpoint(10, new Double2D(5, 5), new Double2D(-5, 5));
		assertFalse(bp.isPresent());
		
		bp = VoronoiUtils.computeBreakpoint(0, new Double2D(5, 5), new Double2D(-5, 5));
		assertFalse(bp.isPresent());
	}
	
	@Test
	public void testComputeBreakpointLeftPointy() {
		Optional<Double2D> bp; 
		
		bp = VoronoiUtils.computeBreakpoint(0, new Double2D(0, 0), new Double2D(0, 5));
		assertTrue(bp.isPresent());
		assertTrue(VoronoiUtils.nearlyEqual(new Double2D(0, 2.5), bp.get()));
		
		bp = VoronoiUtils.computeBreakpoint(0, new Double2D(0, 0), new Double2D(0, -5));
		assertTrue(bp.isPresent());
		assertTrue(VoronoiUtils.nearlyEqual(new Double2D(0, -2.5), bp.get()));
	}
	
    @Test
	public void testComputeBreakpointRightPointy() {
		Optional<Double2D> bp; 
		
		bp = VoronoiUtils.computeBreakpoint(0, new Double2D(0, 5), new Double2D(0, 0));
		assertTrue(bp.isPresent());
		assertTrue(VoronoiUtils.nearlyEqual(new Double2D(0, 2.5), bp.get()));
		
		bp = VoronoiUtils.computeBreakpoint(0, new Double2D(0, -5), new Double2D(0, 0));
		assertTrue(bp.isPresent());
		assertTrue(VoronoiUtils.nearlyEqual(new Double2D(0, -2.5), bp.get()));
	}

}
