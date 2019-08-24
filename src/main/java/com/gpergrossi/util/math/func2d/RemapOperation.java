package com.gpergrossi.util.math.func2d;

public class RemapOperation implements Function2D {

	@FunctionalInterface
	public static interface Operation {
		public double remap(double a);
	}
	
	Function2D noiseA;
	Operation operation;
	
	public RemapOperation(Function2D a, Operation op) {
		this.noiseA = a;
		this.operation = op;
	}
	
	@Override
	public double getValue(double x, double y) {
		return operation.remap(noiseA.getValue(x, y));
	}
	
}
