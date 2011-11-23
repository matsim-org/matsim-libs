package playground.gregor.sim2d_v2.simulation.floor.forces.deliberative.velocityobstacle;

import java.util.Map.Entry;
import java.util.TreeMap;

import com.vividsolutions.jts.geom.Coordinate;

public class Algorithms {


	private static final TreeMap<Double,Double> atans = new TreeMap<Double,Double>();
	private static final double ATANS_LOOKUP_TABLE_RES = Math.PI/100.;

	/**
	 * tests whether then polar angle of vector s0s1 is bigger than the polar angle of vector t0t1
	 * @param s0
	 * @param s1
	 * @param t0
	 * @param t1
	 * @return
	 */
	public static int isAngleBigger(Coordinate s0, Coordinate s1, Coordinate t0, Coordinate t1) {
		double x0 = s1.x - s0.x;
		double y0 = s1.y - s0.y;
		int q0 = getQuadrant(x0,y0);
		double x1 = t1.x - t0.x;
		double y1 = t1.y - t0.y;
		int q1 = getQuadrant(x1,y1);
		if (q0 != q1) {
			return q0 > q1 ? 1 : -1;
		}

		if (y0/x0 == y1/x1) {
			return 0;
		}

		return y0/x0 > y1/x1 ? 1 : -1;
	}

	private static int getQuadrant(double x1, double y1) {
		if (x1 >= 0){
			if (y1 >= 0) {
				return 1;
			} else {
				return 4;
			}
		} else {
			if (y1 >= 0) {
				return 2;
			} else {
				return 3;
			}
		}
	}

	/**
	 * calculates the polar angle for coordinate (x,y)
	 * @param x
	 * @param y
	 * @return the polar angle
	 */
	public static double getPolarAngle(double x, double y) {

		double ret;
		if (x > 0) {

			ret = lazyLookupAtan(y/x);
		} else if (y >= 0 && x < 0) {
			ret = lazyLookupAtan(y/x) + Math.PI;
		} else if (y < 0 && x <0) {
			ret =  lazyLookupAtan(y/x) - Math.PI;
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

	private static double lazyLookupAtan(double d) {
		Entry<Double, Double> c = atans.ceilingEntry(d);
		double ret;
		if (c == null){
			ret = Math.atan(d);
			atans.put(d, ret);
			return ret;
		}

		Entry<Double, Double> f = atans.floorEntry(d);
		if (f == null) {
			ret = Math.atan(d);
			atans.put(d, ret);
			return ret;
		}

		if (Math.abs(c.getValue()-f.getValue()) > ATANS_LOOKUP_TABLE_RES) {
			ret = Math.atan(d);
			atans.put(d, ret);
			return ret;
		}



		return (c.getValue()+f.getValue())/2;
	}

	/**
	 * translates all coordinates in the array by dx, dy
	 * @param dx
	 * @param dy
	 * @param coords
	 */
	public static void translate(double dx, double dy, Coordinate [] coords) {
		int last = coords.length -1;

		for (int i = 0; i < last; i++) {
			coords[i].x += dx;
			coords[i].y += dy;
		}
		if (coords[0] != coords[last]){
			coords[last].x += dx;
			coords[last].y += dy;
		}

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


	private static double getYSquareOfEllipse(double x, double a, double b) {
		double ySquare = (1 - (x*x)/(a*a))*b*b;
		return ySquare;
	}

	public static Coordinate[] getEllipse(double a, double b) {
		//create agentGeometry
		int numOfParts = 8;
		Coordinate[] c = new Coordinate[numOfParts + 1];
		double x = -a;
		double incr = 4*a/numOfParts;
		int pos=0;
		while (x <= a) {
			double ySqr = Algorithms.getYSquareOfEllipse(x,a,b);
			double y = Math.sqrt(ySqr);
			c[pos++] = new Coordinate(x,y);
			x += incr;
		}
		x -= incr;
		x -= incr;
		while (x >= -a) {
			double ySqr = Algorithms.getYSquareOfEllipse(x,a,b);
			double y = -Math.sqrt(ySqr);
			c[pos++] = new Coordinate(x,y);
			x -= incr;
		}
		c[numOfParts] = c[0];
		return c;
	}

	public static void rotate(double alpha, Coordinate [] ring) {
		Coordinate refCoord = new Coordinate(0,0);
		double cos = Math.cos(alpha);
		double sin = Math.sin(alpha);
		for (int i = 0; i < ring.length -1; i++) {
			rotateCoordinate(ring[i],cos,sin, refCoord);
		}
	}

	private static void rotateCoordinate(Coordinate coord, double cos, double sin,
			Coordinate refCoord) {
		double x = refCoord.x + (coord.x - refCoord.x) * cos - (coord.y - refCoord.y)*sin;
		double y = refCoord.y + (coord.x - refCoord.x) * sin + (coord.y - refCoord.y)*cos;
		coord.x = x;
		coord.y = y;
	}
}