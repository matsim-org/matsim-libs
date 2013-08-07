/* *********************************************************************** *
 * project: org.matsim.*
 * PTTravelTimeKTIFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.gregor.sim2denvironment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.TreeMap;

import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;

import com.vividsolutions.jts.geom.Coordinate;

//this class provides basic implementations of geometric algorithms
public abstract class Algorithms {

	private final static double epsilon = 0.00001;

	private static boolean arrayLookup = true;
	
	private static final TreeMap<Double, Double> atans = new TreeMap<Double, Double>();
	private static final double ATANS_LOOKUP_TABLE_RES = Math.PI/100.;
	
	/*
	 * Atan is very steep between -1 and 1, therefore we use a high resolution in that area
	 * and a lower in the remaining range.
	 */
	private static final int atanLimit = 100;
	private static final int atanResoluationFine = 100000;
//	private static final double atanStepWidthFine = 1.0 / atanResoluationFine;
	private static final int atanResoluationCoarse = 1000;
	private static final double[] atansArrayFine = new double[atanResoluationFine];	// 0..1
	private static final double[] atansArrayCoarse = new double[atanLimit*atanResoluationCoarse + 1];	// 1..atanLimit

	private static final int asinResoluation = 10000000;
	private static final double[] asinArray = new double[asinResoluation + 1]; 
	
	static {
		for (int i = 0; i < asinResoluation; i++) {
			asinArray[i] = Math.asin(Double.valueOf(i) / asinResoluation);
		}
		asinArray[asinArray.length - 1] = 0.5 * Math.PI;
		
		for (int i = 0; i < atanResoluationFine; i++) {
			atansArrayFine[i] = Math.atan(((double) i)/atanResoluationFine);
		}
		
		for (int i = 0; i < atanLimit; i++) {
			for (int j = 0; j < atanResoluationCoarse; j++) {
				atansArrayCoarse[i * atanResoluationCoarse + j] = Math.atan(i + ((double) j)/atanResoluationCoarse);
			}
		}
		atansArrayCoarse[atansArrayCoarse.length - 1] = Math.atan(atanLimit);
	}
	
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
		double yiPrime = y2 - ry;
		
		ret[0] = new Coordinate(point);
		ret[1] = new Coordinate(xi,yi);
		ret[2] = new Coordinate(xiPrime, yiPrime);
		
		return ret;
	}
	
	public static Coordinate[] computeCircleIntersection(Coordinate c0, double r0, Coordinate c1, double r1) {
		Coordinate[] ret = new Coordinate[2];
		
		double dx = (c1.x - c0.x);
		double dy = (c1.y - c0.y);
		
//		double d = Math.hypot(dx, dy); // hypot avoids underflow/overflow  ... but it is to slow
		double d =  Math.sqrt(dx*dx + dy*dy); //TODO already calculated in calling method, add to arguments!!
		double a = ((r0*r0)-(r1*r1)+(d*d)) / (2.0 *d);
		
		double x2 = c0.x + (dx * a /d);
		double y2 = c0.y + (dy * a /d);
		
		double h = Math.sqrt(r0*r0 - a*a);
		
		double rx = -dy * (h/d);
		double ry = dx * (h/d);
		
		double xi = x2 + rx;
		double yi = y2 + ry;
		double xiPrime = x2 - rx;
		double yiPrime = y2 - ry;
		
		ret[0] = new Coordinate(xi,yi);
		ret[1] = new Coordinate(xiPrime, yiPrime);
		
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
			ret = lookupAtan(y/x);
		} else if (y >= 0 && x < 0) {
			ret = lookupAtan(y/x) + Math.PI;
		} else if (y < 0 && x < 0) {
			ret =  lookupAtan(y/x) - Math.PI;
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
	
	public static double lookupAtan(double d) {
		if (arrayLookup) return arrayLookupAtan(d);
		else return lazyLookupAtan(d);
	}
	
	private static double lazyLookupAtan(double d) {
		Entry<Double, Double> c = atans.ceilingEntry(d);
		double ret;
		if (c == null) {
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

		if (Math.abs(c.getValue() - f.getValue()) > ATANS_LOOKUP_TABLE_RES) {
			ret = Math.atan(d);
			atans.put(d, ret);
			return ret;
		}

		return (c.getValue() + f.getValue()) / 2;
	}
	
	private static double arrayLookupAtan(double d) {
		double sign = Math.signum(d);
		double abs = Math.abs(d);
		
		if (abs > atanLimit) return sign * atansArrayCoarse[atansArrayCoarse.length - 1];
		else if (abs < 1.0) {
			/*
			 * Still use Math.atan for very small values.
			 */
			if (abs < 0.001) return Math.atan(d);
			
			int arrayPosition = (int) (abs * atanResoluationFine);
			return sign * atansArrayFine[arrayPosition];
		} else {
			int floored = (int) abs;
			double decimal = abs - floored;	// value = 0..1
			int arrayPosition = floored * atanResoluationCoarse + (int) (decimal * atanResoluationCoarse);
			return sign * atansArrayCoarse[arrayPosition];
		}
	}

	public static double lookupAsin(double d) {
		double sign = Math.signum(d);
		double abs = Math.abs(d);
		
//		int arrayPosition = (int) (abs * asinResoluation);
		int arrayPosition = (int) Math.round(abs * asinResoluation);
		
		return sign * asinArray[arrayPosition];
	}
	
	public static void main(String[] args) {
		
		Random random = MatsimRandom.getLocalInstance();
		
		List<Double> c = new ArrayList<Double>();
		for (int i = 0; i < 10000000; i++) c.add((random.nextDouble() * 2) - 1);
		
		Gbl.startMeasurement();
		for (double d : c) Algorithms.lookupAsin(d);
		Gbl.printElapsedTime();
		
		Gbl.startMeasurement();
		for (double d : c) Math.asin(d);
		Gbl.printElapsedTime();
		
//		for (double i = -1.0; i < 1.0; i += 0.05) {
//			System.out.println(i + " " + Algorithms.lookupAsin(i));
//		}
		
		int i = 0;
		for (double d : c) {
			double a =  Algorithms.lookupAsin(d);
			double b = Math.asin(d);
					
			double err = Math.abs((b - a) / b);
			if (err > 0.1) {
				System.out.println(i++ + " " + d + " " + err);
			}
		}
		
//		List<Tuple<Double, Double>> c = new ArrayList<Tuple<Double, Double>>();
//		for (int i = 0; i < 10000000; i++) {
//			c.add(new Tuple<Double, Double>((random.nextDouble()-0.5)*2, (random.nextDouble()-0.5)*2));
//		}
//
//		Algorithms.arrayLookup = true;
//		Gbl.startMeasurement();
//		for (Tuple<Double, Double> t : c) Algorithms.getPolarAngle(t.getFirst(), t.getSecond());
//		Gbl.printElapsedTime();
//		
//		Algorithms.arrayLookup = false;
//		Gbl.startMeasurement();
//		for (Tuple<Double, Double> t : c) Algorithms.getPolarAngle(t.getFirst(), t.getSecond());
//		Gbl.printElapsedTime();
//
//		Gbl.startMeasurement();
//		for (Tuple<Double, Double> t : c) Algorithms.getPolarAngle(t.getFirst(), t.getSecond());
//		Gbl.printElapsedTime();
//		
//		Algorithms.arrayLookup = true;
//		for (int i = 0; i < 100; i++) {
//			Gbl.startMeasurement();
//			for (Tuple<Double, Double> t : c) Algorithms.getPolarAngle(t.getFirst(), t.getSecond());
//			Gbl.printElapsedTime();
//		}
		
//		for (Tuple<Double, Double> t : c) {
//			Algorithms.arrayLookup = true;
//			double a = Algorithms.getPolarAngle(t.getFirst(), t.getSecond());
//			Algorithms.arrayLookup = false;
//			double b = Algorithms.getPolarAngle(t.getFirst(), t.getSecond());
//			
//			double d = t.getSecond() / t.getFirst();
//			
//			double err = Math.abs((b - a) / b);
//			if (err > 0.01) {
//				System.out.println(d + " " + err);
//			}
//		}
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
		if (coords[0] != coords[last]) {
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
	 * tests whether coordinate x0,y0 is located left of the infinite vector that runs from x1,y1  to x2,y2
	 * @param x0 the x-coordinate to test
	 * @param y0 the y-coordinate to test
	 * @param x1 first x-coordinate of the vector
	 * @param y1 first y-coordinate of the vector
	 * @param x2 second x-coordinate of the vector
	 * @param y2 second y-coordinate of the vector
	 * @return >0 if coordinate is left of the vector
	 * 		  ==0 if coordinate is on the vector
	 * 		   <0 if coordinate is right of the vector
	 */
	public static double isLeftOfLine(double x0, double y0, double x1, double y1, double x2, double y2) {
		return (x2 - x1)*(y0 - y1) - (x0 - x1) * (y2 - y1);
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

		//coincident
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

}