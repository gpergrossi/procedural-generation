package com.gpergrossi.util.math;

/**
 * A speed-improved simplex noise algorithm for 2D, 3D and 4D in Java. Version
 * 2012-03-09
 * 
 * This code was placed in the public domain by its original author, Stefan
 * Gustavson. You may use it as you see fit, but attribution is appreciated.
 * 
 * @author Stefan Gustavson (stegu@itn.liu.se)
 * @author Peter Eastman (peastman@drizzle.stanford.edu)
 * 
 */
public abstract class SimplexNoise {
	
	private static Grad grad3[] = { new Grad(1, 1, 0), new Grad(-1, 1, 0),
			new Grad(1, -1, 0), new Grad(-1, -1, 0), new Grad(1, 0, 1),
			new Grad(-1, 0, 1), new Grad(1, 0, -1), new Grad(-1, 0, -1),
			new Grad(0, 1, 1), new Grad(0, -1, 1), new Grad(0, 1, -1),
			new Grad(0, -1, -1) 
	};

	private static Grad grad4[] = { new Grad(0, 1, 1, 1),
			new Grad(0, 1, 1, -1), new Grad(0, 1, -1, 1),
			new Grad(0, 1, -1, -1), new Grad(0, -1, 1, 1),
			new Grad(0, -1, 1, -1), new Grad(0, -1, -1, 1),
			new Grad(0, -1, -1, -1), new Grad(1, 0, 1, 1),
			new Grad(1, 0, 1, -1), new Grad(1, 0, -1, 1),
			new Grad(1, 0, -1, -1), new Grad(-1, 0, 1, 1),
			new Grad(-1, 0, 1, -1), new Grad(-1, 0, -1, 1),
			new Grad(-1, 0, -1, -1), new Grad(1, 1, 0, 1),
			new Grad(1, 1, 0, -1), new Grad(1, -1, 0, 1),
			new Grad(1, -1, 0, -1), new Grad(-1, 1, 0, 1),
			new Grad(-1, 1, 0, -1), new Grad(-1, -1, 0, 1),
			new Grad(-1, -1, 0, -1), new Grad(1, 1, 1, 0),
			new Grad(1, 1, -1, 0), new Grad(1, -1, 1, 0),
			new Grad(1, -1, -1, 0), new Grad(-1, 1, 1, 0),
			new Grad(-1, 1, -1, 0), new Grad(-1, -1, 1, 0),
			new Grad(-1, -1, -1, 0) 
	};

	private static short p[] = { 151, 160, 137, 91, 90, 15, 131, 13, 201, 95,
			96, 53, 194, 233, 7, 225, 140, 36, 103, 30, 69, 142, 8, 99, 37,
			240, 21, 10, 23, 190, 6, 148, 247, 120, 234, 75, 0, 26, 197, 62,
			94, 252, 219, 203, 117, 35, 11, 32, 57, 177, 33, 88, 237, 149, 56,
			87, 174, 20, 125, 136, 171, 168, 68, 175, 74, 165, 71, 134, 139,
			48, 27, 166, 77, 146, 158, 231, 83, 111, 229, 122, 60, 211, 133,
			230, 220, 105, 92, 41, 55, 46, 245, 40, 244, 102, 143, 54, 65, 25,
			63, 161, 1, 216, 80, 73, 209, 76, 132, 187, 208, 89, 18, 169, 200,
			196, 135, 130, 116, 188, 159, 86, 164, 100, 109, 198, 173, 186, 3,
			64, 52, 217, 226, 250, 124, 123, 5, 202, 38, 147, 118, 126, 255,
			82, 85, 212, 207, 206, 59, 227, 47, 16, 58, 17, 182, 189, 28, 42,
			223, 183, 170, 213, 119, 248, 152, 2, 44, 154, 163, 70, 221, 153,
			101, 155, 167, 43, 172, 9, 129, 22, 39, 253, 19, 98, 108, 110, 79,
			113, 224, 232, 178, 185, 112, 104, 218, 246, 97, 228, 251, 34, 242,
			193, 238, 210, 144, 12, 191, 179, 162, 241, 81, 51, 145, 235, 249,
			14, 239, 107, 49, 192, 214, 31, 181, 199, 106, 157, 184, 84, 204,
			176, 115, 121, 50, 45, 127, 4, 150, 254, 138, 236, 205, 93, 222,
			114, 67, 29, 24, 72, 243, 141, 128, 195, 78, 66, 215, 61, 156, 180 
	};
	
	// To remove the need for index wrapping, double the permutation table length
	private static short perm[] = new short[512];
	private static short permMod12[] = new short[512];
	static {
		for (int i = 0; i < 512; i++) {
			perm[i] = p[i & 255];
			permMod12[i] = (short) (perm[i] % 12);
		}
	}

	// Skewing and unskewing factors for 2, 3, and 4 dimensions
	private static final double scewFactor2D = 0.5 * (Math.sqrt(3.0) - 1.0);
	private static final double unscewFactor2D = (3.0 - Math.sqrt(3.0)) / 6.0;
	private static final double scewFactor3D = 1.0 / 3.0;
	private static final double unscewFactor3D = 1.0 / 6.0;
	private static final double scewFactor4D = (Math.sqrt(5.0) - 1.0) / 4.0;
	private static final double unscewFactor4D = (5.0 - Math.sqrt(5.0)) / 20.0;

	private static int floor(double x) {
		int xi = (int) x;
		return x < xi ? xi - 1 : xi;
	}

	private static double dot(Grad g, double x, double y) {
		return g.x * x + g.y * y;
	}

	private static double dot(Grad g, double x, double y, double z) {
		return g.x * x + g.y * y + g.z * z;
	}

	private static double dot(Grad g, double x, double y, double z, double w) {
		return g.x * x + g.y * y + g.z * z + g.w * w;
	}

	/**
	 * This method returns the value of a 2D simplex noise function at (x, y).
	 * @param x - coordinate in the noise (any double)
	 * @param y - coordinate in the noise (any double)
	 * @return double - value of the noise at the coordinate specified [-1, 1]
	 */
	public static double noise(double x, double y) {
		// Noise contributions from the three corners
		double n0, n1, n2;
		
		// Skew the input space to determine which simplex cell we're in
		double scew = (x + y) * scewFactor2D;
		int i = floor(x + scew);
		int j = floor(y + scew);
		
		// Unskew the cell origin back to (x,y) space
		double unscew = (i + j) * unscewFactor2D;
		double X0 = i - unscew; 
		double Y0 = j - unscew;
		
		// The x,y distances from the cell origin
		double x0 = x - X0; 
		double y0 = y - Y0;
		
		// Determine which simplex we are in.
		// Offsets for second (middle) corner of simplex in (i,j) coordinates
		int i1, j1; 
		if (x0 > y0) {
			i1 = 1;
			j1 = 0;
		} else {
			i1 = 0;
			j1 = 1;
		}
		
		// A step of (1,0) in (i,j) means a step of (1-c,-c) in (x,y), and
		// a step of (0,1) in (i,j) means a step of (-c,1-c) in (x,y), where
		// c = (3-sqrt(3))/6
		
		// Offsets for middle corner in (x,y) unskewed coordinates
		double x1 = x0 - i1 + unscewFactor2D; 
		double y1 = y0 - j1 + unscewFactor2D;
		
		// Offsets for last corner in (x,y) unskewed coordinates
		double x2 = x0 - 1.0 + 2.0 * unscewFactor2D; 
		double y2 = y0 - 1.0 + 2.0 * unscewFactor2D;
		
		// Work out the hashed gradient indices of the three simplex corners
		int ii = i & 255;
		int jj = j & 255;
		int gi0 = permMod12[ii + perm[jj]];
		int gi1 = permMod12[ii + i1 + perm[jj + j1]];
		int gi2 = permMod12[ii + 1 + perm[jj + 1]];
		
		// Calculate the contribution from the first corner
		double t0 = 0.5 - x0 * x0 - y0 * y0;
		if (t0 < 0) {
			n0 = 0.0;
		} else {
			t0 *= t0;
			n0 = t0 * t0 * dot(grad3[gi0], x0, y0);
		}
		
		// Calculate the contribution from the second corner
		double t1 = 0.5 - x1 * x1 - y1 * y1;
		if (t1 < 0) {
			n1 = 0.0;
		} else {
			t1 *= t1;
			n1 = t1 * t1 * dot(grad3[gi1], x1, y1);
		}
		
		// Calculate the contribution from the third corner
		double t2 = 0.5 - x2 * x2 - y2 * y2;
		if (t2 < 0)
			n2 = 0.0;
		else {
			t2 *= t2;
			n2 = t2 * t2 * dot(grad3[gi2], x2, y2);
		}
		
		// Add contributions from each corner to get the final noise value.
		// The result is scaled to return values in the interval [-1,1].
		return 70.0 * (n0 + n1 + n2);
	}

	/**
	 * This method returns the value of a 3D simplex noise function at (x, y, z).
	 * @param x - coordinate in the noise (any double)
	 * @param y - coordinate in the noise (any double)
	 * @param z - coordinate in the noise (any double)
	 * @return double - value of the noise at the coordinate specified [-1, 1]
	 */
	public static double noise(double x, double y, double z) {
		// Noise contributions from the four corners
		double n0, n1, n2, n3; 
		
		// Skew the input space to determine which simplex cell we're in
		double s = (x + y + z) * scewFactor3D;
		int i = floor(x + s);
		int j = floor(y + s);
		int k = floor(z + s);
		
		// Unskew the cell origin back to (x,y,z) space
		double t = (i + j + k) * unscewFactor3D;
		double X0 = i - t; 
		double Y0 = j - t;
		double Z0 = k - t;
		
		// The x,y,z distances from the cell origin
		double x0 = x - X0; 
		double y0 = y - Y0;
		double z0 = z - Z0;
		
		// Determine which simplex we are in.
		int i1, j1, k1; // Offsets for second corner of simplex in (i,j,k) coordinates
		int i2, j2, k2; // Offsets for third corner of simplex in (i,j,k) coordinates
		if (x0 >= y0) {
			if (y0 >= z0) {
				i1 = 1;
				j1 = 0;
				k1 = 0;
				i2 = 1;
				j2 = 1;
				k2 = 0;
			} else if (x0 >= z0) {
				i1 = 1;
				j1 = 0;
				k1 = 0;
				i2 = 1;
				j2 = 0;
				k2 = 1;
			} else {
				i1 = 0;
				j1 = 0;
				k1 = 1;
				i2 = 1;
				j2 = 0;
				k2 = 1;
			}
		} else {
			if (y0 < z0) {
				i1 = 0;
				j1 = 0;
				k1 = 1;
				i2 = 0;
				j2 = 1;
				k2 = 1;
			} else if (x0 < z0) {
				i1 = 0;
				j1 = 1;
				k1 = 0;
				i2 = 0;
				j2 = 1;
				k2 = 1;
			} else {
				i1 = 0;
				j1 = 1;
				k1 = 0;
				i2 = 1;
				j2 = 1;
				k2 = 0;
			}
		}
		
		// A step of (1,0,0) in (i,j,k) means a step of (1-c,-c,-c) in (x,y,z), 
		// a step of (0,1,0) in (i,j,k) means a step of (-c,1-c,-c) in (x,y,z), and
		// a step of (0,0,1) in (i,j,k) means a step of (-c,-c,1-c) in (x,y,z), where
		// c = 1/6.
		
		// Offsets for second corner in (x,y,z) coordinates
		double x1 = x0 - i1 + unscewFactor3D; 
		double y1 = y0 - j1 + unscewFactor3D;
		double z1 = z0 - k1 + unscewFactor3D;
		
		// Offsets for third corner in (x,y,z) coordinates
		double x2 = x0 - i2 + 2.0 * unscewFactor3D;
		double y2 = y0 - j2 + 2.0 * unscewFactor3D;
		double z2 = z0 - k2 + 2.0 * unscewFactor3D;
		
		// Offsets for last corner in (x,y,z) coordinates
		double x3 = x0 - 1.0 + 3.0 * unscewFactor3D;
		double y3 = y0 - 1.0 + 3.0 * unscewFactor3D;
		double z3 = z0 - 1.0 + 3.0 * unscewFactor3D;
		
		// Work out the hashed gradient indices of the four simplex corners
		int ii = i & 255;
		int jj = j & 255;
		int kk = k & 255;
		int gi0 = permMod12[ii + perm[jj + perm[kk]]];
		int gi1 = permMod12[ii + i1 + perm[jj + j1 + perm[kk + k1]]];
		int gi2 = permMod12[ii + i2 + perm[jj + j2 + perm[kk + k2]]];
		int gi3 = permMod12[ii + 1 + perm[jj + 1 + perm[kk + 1]]];
		
		// Calculate the contribution from the first corner
		double t0 = 0.6 - x0 * x0 - y0 * y0 - z0 * z0;
		if (t0 < 0) {
			n0 = 0.0;
		} else {
			t0 *= t0;
			n0 = t0 * t0 * dot(grad3[gi0], x0, y0, z0);
		}
		
		// Calculate the contribution from the second corner
		double t1 = 0.6 - x1 * x1 - y1 * y1 - z1 * z1;
		if (t1 < 0) {
			n1 = 0.0;
		} else {
			t1 *= t1;
			n1 = t1 * t1 * dot(grad3[gi1], x1, y1, z1);
		}
		
		// Calculate the contribution from the third corner
		double t2 = 0.6 - x2 * x2 - y2 * y2 - z2 * z2;
		if (t2 < 0) {
			n2 = 0.0;
		} else {
			t2 *= t2;
			n2 = t2 * t2 * dot(grad3[gi2], x2, y2, z2);
		}
		
		// Calculate the contribution from the fourth corner
		double t3 = 0.6 - x3 * x3 - y3 * y3 - z3 * z3;
		if (t3 < 0) {
			n3 = 0.0;
		} else {
			t3 *= t3;
			n3 = t3 * t3 * dot(grad3[gi3], x3, y3, z3);
		}
		
		// Add contributions from each corner to get the final noise value.
		// The result is scaled to stay just inside [-1,1]
		return 32.0 * (n0 + n1 + n2 + n3);
	}

	/**
	 * This method returns the value of a 4D simplex noise function at (x, y, z, w).
	 * @param x - coordinate in the noise (any double)
	 * @param y - coordinate in the noise (any double)
	 * @param z - coordinate in the noise (any double)
	 * @param w - coordinate in the noise (any double)
	 * @return double - value of the noise at the coordinate specified [-1, 1]
	 */
	public static double noise(double x, double y, double z, double w) {

		// Skew the (x,y,z,w) space to determine which cell of 24 simplices we're in
		double n0, n1, n2, n3, n4;
		double s = (x + y + z + w) * scewFactor4D;
		int i = floor(x + s);
		int j = floor(y + s);
		int k = floor(z + s);
		int l = floor(w + s);
		
		// Unskew the cell origin back to (x, y, z, w) space
		double t = (i + j + k + l) * unscewFactor4D;		
		double X0 = i - t;
		double Y0 = j - t;
		double Z0 = k - t;
		double W0 = l - t;
		
		// The (x, y, z, w) distances from the cell origin
		double x0 = x - X0;
		double y0 = y - Y0;
		double z0 = z - Z0;
		double w0 = w - W0;
		
		// To find out which of the 24 possible simplices we're in, we need to
		// determine the magnitude ordering of x0, y0, z0 and w0.
		// Six pair-wise comparisons are performed between each possible pair
		// of the four coordinates, and the results are used to rank the
		// numbers.
		int rankx = 0;
		int ranky = 0;
		int rankz = 0;
		int rankw = 0;
		if (x0 > y0) {
			rankx++;
		} else {
			ranky++;
		}
		if (x0 > z0) {
			rankx++;
		} else {
			rankz++;
		}
		if (x0 > w0) {
			rankx++;
		} else {
			rankw++;
		}
		if (y0 > z0) {
			ranky++;
		} else {
			rankz++;
		}
		if (y0 > w0) {
			ranky++;
		} else {
			rankw++;
		}
		if (z0 > w0) {
			rankz++;
		} else {
			rankw++;
		}
		
		int i1, j1, k1, l1; // The integer offsets for the second simplex corner
		int i2, j2, k2, l2; // The integer offsets for the third simplex corner
		int i3, j3, k3, l3; // The integer offsets for the fourth simplex corner
		
		// simplex[c] is a 4-vector with the numbers 0, 1, 2 and 3 in some order.
		// Many values of c will never occur, since e.g. x>y>z>w makes x<z, y<w and x<w
		// impossible. Only the 24 indices which have non-zero entries make any sense.
		// We use a thresholding to set the coordinates in turn from the largest magnitude.
		
		// Rank 3 denotes the largest coordinate.
		i1 = rankx >= 3 ? 1 : 0;
		j1 = ranky >= 3 ? 1 : 0;
		k1 = rankz >= 3 ? 1 : 0;
		l1 = rankw >= 3 ? 1 : 0;
		
		// Rank 2 denotes the second largest coordinate.
		i2 = rankx >= 2 ? 1 : 0;
		j2 = ranky >= 2 ? 1 : 0;
		k2 = rankz >= 2 ? 1 : 0;
		l2 = rankw >= 2 ? 1 : 0;
		
		// Rank 1 denotes the second smallest coordinate.
		i3 = rankx >= 1 ? 1 : 0;
		j3 = ranky >= 1 ? 1 : 0;
		k3 = rankz >= 1 ? 1 : 0;
		l3 = rankw >= 1 ? 1 : 0;
		
		// Offsets for second corner in (x,y,z,w) coordinates
		double x1 = x0 - i1 + unscewFactor4D; 
		double y1 = y0 - j1 + unscewFactor4D;
		double z1 = z0 - k1 + unscewFactor4D;
		double w1 = w0 - l1 + unscewFactor4D;
		
		// Offsets for third corner in (x,y,z,w) coordinates
		double x2 = x0 - i2 + 2.0 * unscewFactor4D;
		double y2 = y0 - j2 + 2.0 * unscewFactor4D;
		double z2 = z0 - k2 + 2.0 * unscewFactor4D;
		double w2 = w0 - l2 + 2.0 * unscewFactor4D;
		
		// Offsets for fourth corner in (x,y,z,w) coordinates
		double x3 = x0 - i3 + 3.0 * unscewFactor4D; 
		double y3 = y0 - j3 + 3.0 * unscewFactor4D;
		double z3 = z0 - k3 + 3.0 * unscewFactor4D;
		double w3 = w0 - l3 + 3.0 * unscewFactor4D;
		
		 // Offsets for last corner in (x,y,z,w) coordinates
		double x4 = x0 - 1.0 + 4.0 * unscewFactor4D;
		double y4 = y0 - 1.0 + 4.0 * unscewFactor4D;
		double z4 = z0 - 1.0 + 4.0 * unscewFactor4D;
		double w4 = w0 - 1.0 + 4.0 * unscewFactor4D;
		
		// Work out the hashed gradient indices of the five simplex corners
		int ii = i & 255;
		int jj = j & 255;
		int kk = k & 255;
		int ll = l & 255;
		int gi0 = perm[ii + perm[jj + perm[kk + perm[ll]]]] % 32;
		int gi1 = perm[ii + i1 + perm[jj + j1 + perm[kk + k1 + perm[ll + l1]]]] % 32;
		int gi2 = perm[ii + i2 + perm[jj + j2 + perm[kk + k2 + perm[ll + l2]]]] % 32;
		int gi3 = perm[ii + i3 + perm[jj + j3 + perm[kk + k3 + perm[ll + l3]]]] % 32;
		int gi4 = perm[ii + 1 + perm[jj + 1 + perm[kk + 1 + perm[ll + 1]]]] % 32;
		
		// Calculate the contribution from the first corner
		double t0 = 0.6 - x0 * x0 - y0 * y0 - z0 * z0 - w0 * w0;
		if (t0 < 0) {
			n0 = 0.0;
		} else {
			t0 *= t0;
			n0 = t0 * t0 * dot(grad4[gi0], x0, y0, z0, w0);
		}
		
		// Calculate the contribution from the second corner
		double t1 = 0.6 - x1 * x1 - y1 * y1 - z1 * z1 - w1 * w1;
		if (t1 < 0) {
			n1 = 0.0;
		} else {
			t1 *= t1;
			n1 = t1 * t1 * dot(grad4[gi1], x1, y1, z1, w1);
		}
		
		// Calculate the contribution from the third corner
		double t2 = 0.6 - x2 * x2 - y2 * y2 - z2 * z2 - w2 * w2;
		if (t2 < 0) {
			n2 = 0.0;
		} else { 
			t2 *= t2;
			n2 = t2 * t2 * dot(grad4[gi2], x2, y2, z2, w2);
		}
		
		// Calculate the contribution from the fourth corner
		double t3 = 0.6 - x3 * x3 - y3 * y3 - z3 * z3 - w3 * w3;
		if (t3 < 0) {
			n3 = 0.0;
		} else {
			t3 *= t3;
			n3 = t3 * t3 * dot(grad4[gi3], x3, y3, z3, w3);
		}
		
		// Calculate the contribution from the fifth corner
		double t4 = 0.6 - x4 * x4 - y4 * y4 - z4 * z4 - w4 * w4;
		if (t4 < 0) {
			n4 = 0.0;
		} else {
			t4 *= t4;
			n4 = t4 * t4 * dot(grad4[gi4], x4, y4, z4, w4);
		}
		
		// Sum up and scale the result to cover the range [-1,1]
		return 27.0 * (n0 + n1 + n2 + n3 + n4);
	}

	/**
	 * This inner class represents a gradient
	 * Member access is faster in Java than array access
	 */
	private static final class Grad {
		final double x, y, z, w;

		Grad(double x, double y, double z) {
			this.x = x;
			this.y = y;
			this.z = z;
			this.w = 0;
		}

		Grad(double x, double y, double z, double w) {
			this.x = x;
			this.y = y;
			this.z = z;
			this.w = w;
		}
	}
	
}