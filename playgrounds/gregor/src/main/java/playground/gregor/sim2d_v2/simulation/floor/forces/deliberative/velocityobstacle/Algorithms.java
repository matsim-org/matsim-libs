package playground.gregor.sim2d_v2.simulation.floor.forces.deliberative.velocityobstacle;

import com.vividsolutions.jts.geom.Coordinate;

public class Algorithms {

	/**
	 * calculates the polar angle for vector (c1,c2)
	 * @param c1 first coordinate
	 * @param c2 second coordinate
	 * @return the polar angle
	 */
	public static double getPolarAngle(Coordinate c1, Coordinate c2) {
		double x = c2.x - c1.x;
		double y = c2.y - c1.y;

		double ret;
		if (x > 0) {
			ret = Math.atan(y/x);
		} else if (y >= 0 && x < 0) {
			ret = Math.atan(y/x) + Math.PI;
		} else if (y < 0 && x <0) {
			ret =  Math.atan(y/x) - Math.PI;
		} else if (y > 0 && x == 0) {
			ret =  Math.PI/2;
		} else if (y < 0 && x == 0) {
			ret =  -Math.PI/2;
		} else {
			ret = Double.NaN;
		}
		if (ret < 0) {
			return ret + 2*Math.PI;
		}
		return ret;
	}

	/**
	 * tests whether coordinate c0 is located left of the infinite vector that runs through c1 and c2
	 * @param c0 the coordinate to test
	 * @param c1 one coordinate of the vector
	 * @param c2 another coordinate of the same vector
	 * @return >0 if c0 is left of the vector
	 * 		  ==0 if c0 is on the vector
	 * 		   <0 if c0 is right of the vector
	 */
	public static double isLeftOfLine(Coordinate c0, Coordinate c1, Coordinate c2) {
		return (c2.x - c1.x)*(c0.y - c1.y) - (c0.x - c1.x) * (c2.y - c1.y);
	}

	/**
	 * test whether polygon vertex c0 is above polygon vertex c1 relative to c2
	 * @param c0
	 * @param c1
	 * @param c2
	 * @return true is c0 is above c1
	 */
	public static boolean isAbove(Coordinate c0, Coordinate c1, Coordinate c2) {
		return isLeftOfLine(c0,c2,c1) < 0;
	}

	/**
	 * test whether polygon vertex c0 is below polygon vertex c1 relative to c2
	 * @param c0
	 * @param c1
	 * @param c2
	 * @return true is c0 is below c1
	 */
	public static boolean isBelow(Coordinate c0, Coordinate c1, Coordinate c2) {
		return isLeftOfLine(c0,c2,c1) > 0;
	}

	/**
	 * Calculates the tangent indices from coordinate c to polygon p
	 * @param c
	 * @param p
	 * @return array with indices
	 */
	public static int[] getTangentIndices(Coordinate c, Coordinate [] ccw) {
		int [] ret = new int[2];
		ret[0] = getRightTangentIndex(c, ccw);
		ret[1] = getLeftTangentIndex(c, ccw);
		//		ret[1] = ret[0];
		return ret;
	}


	private static int getLeftTangentIndex(Coordinate c, Coordinate[] ccwRing) {
		int n = ccwRing.length-1;
		if (isAbove(ccwRing[n-1],ccwRing[0],c) && !isBelow(ccwRing[1],ccwRing[0],c)) {
			return 0;
		}
		int a =0;
		int b = n;
		for (int i = 0; i <=n; i++) {
			int m = (a+b)/2;
			boolean downM = isBelow(ccwRing[m+1],ccwRing[m],c);
			if (isAbove(ccwRing[m-1],ccwRing[m],c) && !downM) {
				return m;
			}

			boolean downA = isBelow(ccwRing[a+1],ccwRing[a],c);
			if (downA) {
				if (!downM) {
					b = m;
				} else {
					if (isBelow(ccwRing[a],ccwRing[m],c)) {
						b = m;
					} else {
						a = m;
					}
				}
			} else {
				if (downM) {
					a = m;
				} else {
					if (isAbove(ccwRing[a],ccwRing[m],c)) {
						b = m;
					} else {
						a = m;
					}
				}
			}
		}
		return (a+b)/2-1;//we have already penetrated the obstacle
	}
	private static int getRightTangentIndex(Coordinate c, Coordinate[] ccwRing) {
		int n = ccwRing.length-1;
		if (isBelow(ccwRing[1],ccwRing[0],c) && !isAbove(ccwRing[n-1],ccwRing[0],c)) {
			boolean tt = !isAbove(ccwRing[n-1],ccwRing[0],c);
			tt = !tt;
			return 0;
		}

		int a = 0;
		int b = n;
		for (int i = 0; i<=n; i++) {
			int m = (a+b)/2;
			boolean downM = isBelow(ccwRing[m+1],ccwRing[m],c);
			if (downM && !isAbove(ccwRing[m-1],ccwRing[m],c)) {
				return m;
			}

			boolean upA = isAbove(ccwRing[a+1],ccwRing[a],c);
			if (upA) {
				if (downM) {
					b = m;
				} else {
					if (isAbove(ccwRing[a],ccwRing[m],c)) {
						b = m;
					} else {
						a = m;
					}
				}
			} else {
				if (!downM) {
					a = m;
				} else {
					if (isBelow(ccwRing[a],ccwRing[m],c)) {
						b = m;
					} else {
						a = m;
					}
				}
			}
		}
		return(a+b)/2;//we have already penetrated the obstacle
	}


}