package com.gpergrossi.util.geom.ranges;

public class DistanceTransform {

	private static interface FloatFunction2I {
		public float get(int x, int y);
	}
	
	private static interface IntFunction2I {
		public int get(int x, int y);
	}
	
	/**
	 * Distance Transform algorithm from:
	 * 
	 * <br/><br/>
	 * <b>A General Algorithm for Computing Distance Transforms in Linear Time</b><br/>
	 * <i>A. Meijster, J.B.T.M. Roerdink, & W.H. Hesselink</i><br/>
	 * <i>University of Groningen</i><br/>
	 * 
	 * @param shape
	 * @return
	 */
	public static DistanceTransform transform(Int2DRange.Bits shape) {
		final float[] yDist = new float[shape.size()];
		final int width = shape.width;
		final int height = shape.height;
		
		// Initialize + First Scan + Second Scan
		for (int x = 0; x < width; x++) {
			int index = x;
			
			yDist[index] = shape.get(x+shape.minX, 0+shape.minY) ? 0 : Float.POSITIVE_INFINITY;
			
			// Initialize + First scan: cell[x,y] = 0 or cell[x,y-1]+1
			for (int y = 1; y < height; y++) {
				index += width;
				if (shape.get(x+shape.minX, y+shape.minY)) yDist[index] = 0; // init
				else yDist[index] = yDist[index - width] + 1; // scan down
			}
			
			// Second scan: cell[x,y] = min(cell[x,y], cell[x,y+1]+1)
			for (int y = height-2; y >= 0; y--) {
				index -= width;
				yDist[index] = Math.min(yDist[index], yDist[index + width] + 1);
			}
		}

		final Int2DRange.Floats result = shape.createFloats();
		final int[] mins = new int[width];
		final int[] partition = new int[width];
		float maxDist = 0;
			
		// Third Scan + Fourth Scan
		for (int y = 0; y < height; y++) {

			final int rowIndex = y*width;

			// computes (x-i)^2 + g(i)^2
			final FloatFunction2I distFunc = new FloatFunction2I() {
				@Override
				public float get(int x, int i) {
					final int d = x - i;
					final float gi = yDist[rowIndex + i];
					return d*d + gi*gi;
				}
			};
			
			final IntFunction2I seperator = new IntFunction2I() {
				@Override
				public int get(int i, int u) {
					final float gu = yDist[rowIndex + u];
					final float gi = yDist[rowIndex + i];
					return (int) Math.floor((u*u - i*i + gu*gu - gi*gi) / (2*(u-i)));
				}
			};

			int scout = 0;
			
			for (int x = 1; x < width; x++) {				
				for (; scout >= 0; scout--) {
					final float currentMin = distFunc.get(partition[scout], mins[scout]);
					final float newMin = distFunc.get(partition[scout], x);
					if (newMin >= currentMin) break;
				}
				
				if (scout < 0) {
					scout = 0;
					mins[0] = x;
				} else {
					int intersection = seperator.get(mins[scout], x) + 1;
					if (intersection < width && intersection != Integer.MIN_VALUE) {
						scout++;
						mins[scout] = x;
						partition[scout] = intersection;
					}
				}
			}
			
			for (int x = width-1; x >= 0; x--) {
				final float distance = (float) Math.sqrt(distFunc.get(x, mins[scout]));
				maxDist = Math.max(maxDist, distance);
				result.set(rowIndex+x, distance);
				if (x == partition[scout]) scout--;
			}
		}
		
		return new DistanceTransform(result, maxDist);
	}
	
	private Int2DRange.Floats result;
	private double maxDistance;
	
	public DistanceTransform(Int2DRange.Floats result, double maxDistance) {
		this.result = result;
		this.maxDistance = maxDistance;
	}
	
	public Int2DRange.Floats getResult() {
		return result;
	}
	
	public double getMaxDistance() {
		return maxDistance;
	}
	
}
