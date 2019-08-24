package com.gpergrossi.util.math.func2d;

public class CombineOperation implements Function2D {

	public static interface Operation {
		public double combine(double a, double b);
	}
	
	Function2D noiseA, noiseB;
	Operation operation;
	
	public CombineOperation(Function2D a, Function2D b, Operation op) {
		this.noiseA = a;
		this.noiseB = b;
		this.operation = op;
	}
	
	@Override
	public double getValue(double x, double y) {
		double a = 0, b = 0;
		
		if (noiseA != null) a = noiseA.getValue(x, y);
		if (noiseB != null) b = noiseB.getValue(x, y);
		return operation.combine(a, b);
	}

}
