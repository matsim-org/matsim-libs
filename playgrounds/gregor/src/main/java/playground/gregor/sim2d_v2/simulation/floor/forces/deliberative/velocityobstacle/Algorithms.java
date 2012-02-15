package playground.gregor.sim2d_v2.simulation.floor.forces.deliberative.velocityobstacle;

import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.vividsolutions.jts.geom.Coordinate;


//this class provides basic implementations of geometric algorithms
public abstract class Algorithms {

	private final static double epsilon = 0.00001;

	private static final TreeMap<Double,Double> atans = new TreeMap<Double,Double>();
	private static final double ATANS_LOOKUP_TABLE_RES = Math.PI/100.;

	/**
	 * Computes both tangents of a circle running through a given point. Computation uses Thales' theorem.
	 * Note the point has to be outside the circle. This requirement, however, will not be checked!
	 * @param circleCenter
	 * @param radius
	 * @param point
	 * @return tangentsCoordinates
	 */
	public static Coordinate[] computeTangentsThroughPoint(Coordinate circleCenter, double radius, Coordinate point) {
		Coordinate[] ret = new Coordinate[3];
		
		double dx = (point.x - circleCenter.x)/2;
		double dy = (point.y - circleCenter.y)/2;
		
//		double d = Math.hypot(dx, dy); // hypot avoids underflow/overflow  ... but it is to slow
		double d = Math.sqrt(dx*dx + dy*dy); // hypot avoids underflow/overflow  ... but it is to slow
		
		double a = (radius*radius) / (2.0 *d);
		
		double x2 = circleCenter.x + (dx * a /d);
		double y2 = circleCenter.y + (dy * a /d);
		
		double h = Math.sqrt(radius*radius - a*a);
		
		double rx = -dy * (h/d);
		double ry = dx * (h/d);
		
		double xi = x2 + rx;
		double yi = y2 + ry;
		double xiPrime = x2 - rx;
		double yiPrime = y2 -ry;
		
		ret[0] = new Coordinate(point);
		ret[1] = new Coordinate(xi,yi);
		ret[2] = new Coordinate(xiPrime, yiPrime);
		
		return ret;
	}
	
	
	/**
	 * tests whether then polar angle of vector s0s1 is bigger than the polar angle of vector t0t1
	 * @param s0
	 * @param s1
	 * @param t0
	 * @param t1
	 * @return
	 */
	public static int isAngleBigger(Coordinate s0, Coordinate s1, Coordinate t0, Coordinate t1) {
		double x0 = (s1.x - s0.x);
		double y0 =  (s1.y - s0.y);
		double x1 = (t1.x - t0.x);
		double y1 = (t1.y - t0.y);
		int q0 = getQuadrant(x0,y0);
		int q1 = getQuadrant(x1,y1);
		if (q0 != q1) {
			return q0 > q1 ? 1 : -1;
		}

		double por1 = y0/x0;
		double por2 = y1/x1;

		if (por1 > por2) {
			return 1;
		} else if (por2 > por1) {
			return -1;
		}
		return 0;

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
	 * @see see softSurfer (www.softsurfer.com) for more details on this algorithm
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
		final int numOfParts = 8;
		Coordinate[] c = new Coordinate[numOfParts + 1];
		double incr = 2*a/(numOfParts/2+1);
		double x = -a+incr;
		int pos=0;
		for (int i = 0; i < numOfParts/2; i++) {
			double ySqr = Algorithms.getYSquareOfEllipse(x,a,b);
			double y = Math.sqrt(ySqr);
			c[pos++] = new Coordinate(x,y);
			x += incr;
		}
		x = a-incr;
		for (int i = 0; i < numOfParts/2; i++) {
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

	/**
	 * Tests whether a polygon (defined by an array of Coordinate) contains a Coordinate
	 * @param coord
	 * @param p
	 * @return true if coord lays within p
	 */
	public static boolean contains(Coordinate coord, Coordinate[] p) {
		int wn = getWindingNumber(coord,p);
		return wn != 0;
	}

	//winding number algorithm
	//see softSurfer (www.softsurfer.com) for more details
	private static int getWindingNumber(Coordinate c, Coordinate[] p) {


		int wn = 0;

		for (int i=0; i<p.length-1; i++) {
			if (p[i].y <= c.y) {
				if (p[i+1].y > c.y)
					if (isLeftOfLine( c,p[i], p[i+1]) > 0)
						++wn;
			}
			else {
				if (p[i+1].y <= c.y)
					if (isLeftOfLine( c,p[i], p[i+1]) < 0)
						--wn;
			}

			//test for early return here
		}
		return wn;
	}


	public static boolean computeLineIntersection(Coordinate a0, Coordinate a1, Coordinate b0, Coordinate b1, Coordinate intersectionCoordinate) {



		double a = (b1.x - b0.x) * (a0.y - b0.y) - (b1.y - b0.y) * (a0.x - b0.x);
		double b = (a1.x - a0.x) * (a0.y - b0.y) - (a1.y - a0.y) * (a0.x - b0.x);
		double denom = (b1.y - b0.y) * (a1.x - a0.x) - (b1.x - b0.x) * (a1.y - a0.y);

		//conincident
		if (Math.abs(a) < epsilon && Math.abs(b) < epsilon && Math.abs(denom) < epsilon) {
			intersectionCoordinate.x = (a0.x+a1.x) /2;
			intersectionCoordinate.y = (a0.y+a1.y) /2;
			return true;
		}

		//parallel
		if (Math.abs(denom) < epsilon) {
			return false;
		}

		double ua = a / denom;
		double ub = b / denom;

		if (ua < 0 || ua > 1 || ub < 0 || ub > 1) {
			return false;
		}

		double x = a0.x + ua * (a1.x - a0.x);
		double y = a0.y + ua * (a1.y - a0.y);
		intersectionCoordinate.x = x;
		intersectionCoordinate.y = y;

		return true;
	}

	public static boolean testForCollision(VelocityObstacle info, Coordinate c) {
		if (info.getCollTime() <= 0) {
			return true;
		}
		Coordinate[] vo = info.getVo();
		boolean leftOfLeft = Algorithms.isLeftOfLine(c, vo[0], vo[1]) > 0;
		boolean rightOfRight = Algorithms.isLeftOfLine(c, vo[0], vo[2]) < 0;
		if (leftOfLeft && rightOfRight) {
			return true;
		}
		return false;
	}

	public static boolean testForCollision(List<? extends VelocityObstacle> infos, Coordinate c) {
		for (VelocityObstacle info : infos) {
			if (testForCollision(info,c)) {
				return true;
			}
		}
		return false;
	}
}