/* *********************************************************************** *
 * project: org.matsim.*
 * BeanObjective.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.jhackney.optimization;

import java.util.Collection;
import java.util.Iterator;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.gbl.Gbl;

/**
 * Objective Function based on a Bean Curve. The Bean Curve is defined as
 * <ul><li>cartesian:       x^4+(x^2)*(y^2)+y^4=x*(x^2+y^2)</li>
 *     <li>parametrically:  X = a*cos(t), Y = b*sin(t)</li>
 *     <li>area:            A = 1.058049*a*b</li></ul>
 * In space, a Bean curve is described by the following 5 parameters:
 * <ul><li>x, y: the Coordinates of the center of the bean</li>
 *     <li>a, b: the length of the two orthogonal axes</li>
 *     <li>theta: the rotation of the main axis a</li></ul>
 * In our case, we use a 4-dimensional parameter space with the following
 * 4 axes and their ranges:
 * <ul><li>x : ]-INF .. +INF[</li>
 *     <li>y : ]-INF .. +INF[</li>
 *     <li>theta : ]-pi/2 .. pi/2]</li>
 *     <li>ratio : ]0 .. +INF[</li></ul>
 * The effective size of the bean curve is given implicitely by a minimum
 * cover-percentage. The bean curve will be enlarged until it contains at
 * least the specified percentage of all points.
 */
public class BeanObjective implements Objective {

	//////////////////////////////////////////////////////////////////////
	// member constants
	//////////////////////////////////////////////////////////////////////

	public static final String OBJECTIVE_NAME = "bean";

	public static final double EPSILON = 100.0;		// TODO, test what value is best for this param

	public static final int DIMENSION = 3;

	public static final int X_idx = 0;
	public static final int Y_idx = 1;
	public static final int RATIO_idx = 2;

	public static final String X_name = "x";
	public static final String Y_name = "y";

	public static final String THETA_name = "theta";
	public static final String A_name = "a";
	public static final String B_name = "b";
	public static final String COVER_name = "cover";

	private final static Logger log = Logger.getLogger(BeanObjective.class);
	
	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final Collection<Coord> coords;	// list of points which the calculated bean curve region should cover
	private final double minCover;

	private double theta;

	private ParamPoint[] initPPoints;

	//////////////////////////////////////////////////////////////////////
	// constructor
	//////////////////////////////////////////////////////////////////////

	public BeanObjective(Collection<Coord> coords, double minCover, double theta) {
		this.coords = coords;
		this.minCover = minCover;
		this.theta = theta;
		this.initPPoints = new ParamPoint[DIMENSION+1];
	}

	public ParamPoint getNewParamPoint() {
		ParamPoint p = new ParamPoint(DIMENSION);
		p.setValue(RATIO_idx, 0.1);
		return p;
	}

	public double getResponse(ParamPoint p) {
		if (this.coords.size() == 0) {
			return Double.MAX_VALUE;
		}
		double a = getBestA(p);
		return (1.058049*a*a*p.getValue(RATIO_idx)); // area = 1.058049*a*b
	}

	/**
	 * returns the value for the bean curve-parameter a that best fulfills the cover-percentage
	 * @param p a parameter point for which a is calculated
	 * @return smallest bean-axis parameter a so that the bean covers at least a predefined percentage of points
	 */
	private final double getBestA(ParamPoint p) {

		double cover = 0;
		double addFactor = 1;
		double a = 1;	// the ''real'' size of a

		double bestA = Double.MAX_VALUE; // if we include everything, we sure ...
		double bestCover = 1.0; // ... get all points

		while ((bestA - a) > EPSILON) {
			double testA = a*(1+addFactor);
			cover = getCover(p, testA);
			if (cover < this.minCover) {
				// we're still to small...
				a = testA;
			}
			if (cover >= this.minCover) {
				// we're too big now (or equal)
				if (cover < bestCover) {
					// but we're still better than the current best-known size
					bestCover = cover;
					bestA = testA;
				}
				if (cover == bestCover && testA < bestA) {
					bestA = testA;
				}
				// let's make smaller steps when we are too big now
				addFactor = addFactor / 2;
			}
		}
		return bestA;
	}

	/**
	 * calculates the percentage of covered points with the bean defined by p and a.
	 *
	 * @return percentage of the points covered by the bean
	 */
	private final double getCover(ParamPoint p, double a) {
		int cntTotal = 0;
		int cntInside = 0;
		Iterator<Coord> iter = this.coords.iterator();
		while (iter.hasNext()) {
			Coord coord = iter.next();
			cntTotal++;
			if (isPointWithin(coord, p, a)) {
				cntInside++;
			}
		}
		if (cntTotal == 0) return 0;
		return (1.0 * cntInside / (1.0 * cntTotal));
	}


	private final boolean isPointWithin(Coord coord, ParamPoint p, double a) {

		double x0 = p.getValue(X_idx);
		double y0 = p.getValue(Y_idx);
		double b = p.getValue(RATIO_idx) * a;

		double x = coord.getX();
		double y = coord.getY();
//		double x1;
//		double y1;
		double x_;
		double y_;
		double x1_;
		double y1_;
//		x1 = (Math.cos( theta)*(x)- Math.sin( theta)*(y))/a;
//		y1 = (Math.sin( theta)*(x)+ Math.cos( theta)*(y))/b;
		x1_ = ( Math.cos(this.theta) * (x0) + Math.sin(this.theta) * (y0) ) - a/2.0;
		y1_ = ( -Math.sin(this.theta) * (x0) + Math.cos(this.theta) * (y0) );
//		x1 = ( Math.cos(theta) * (x1_) - Math.sin(theta) * (y1_) );
//		y1 = ( Math.sin(theta) * (x1_) + Math.cos(theta) * (y1_) );
		x_ = ( Math.cos(this.theta) * (x) + Math.sin(this.theta) * (y) ) - x1_;
		y_ = ( - Math.sin(this.theta) * (x) + Math.cos(this.theta) * (y) ) - y1_;

//		System.out.println(a + " " + b + " " + x0 + " " + y0 + " " + x + " " + y + " " + p.getValue(RATIO_idx) + " " + x1_ + " " + y1_ + " " + x1 + " " + y1 + " " + x_ + " " + y_);

//		double dist =  b*( Math.sqrt( (x_/a) * ( (1-(x_/a)) + Math.sqrt(1+(2-3*(x_/a))*(x_/a)) ) ) )/(Math.sqrt(2.00));
//      double dist = Math.pow(x1,4)+ Math.pow(y1,4)+ Math.pow(x1,2)* Math.pow(y1,2)- x * (Math.pow(x1,2)+ Math.pow(y1,2));
		double dist = Math.pow(y_,4)-Math.pow(y_,2)*b*b*(x_/a)*(1-(x_/a))+Math.pow((x_/a),4)*Math.pow(b,4)-Math.pow(b,4)*Math.pow((x_/a),3);
//		System.out.println(">>> dist: " + dist + "<<<");
//		System.out.println(">>> theta: " + theta + "<<<");

		return (dist<= 0.0);
	}

	public final void setInitParamPoint(ParamPoint p, int i) {
		if ((0 > i) || (i > DIMENSION)) {
			Gbl.errorMsg("index " + i + " not allowed!");
		}
		this.initPPoints[i] = p;
	}

	public final void setTheta(double theta) {
		this.theta = theta;
	}

	public final ParamPoint getInitialParamPoint(final int index) {
		if (index > DIMENSION) {
			log.warn("Initial paramPoint " + index + " was requested, but we only have 4 Dimensions. Returning Initial paramPoint 0.");
			return this.initPPoints[0];
	  }
		return this.initPPoints[index];
	}

	public final boolean isValidParamPoint(ParamPoint p) {
		// do not validate x and y, as they have no real limitation
//		double theta = p.getValue(THETA_idx);
		double ratio = p.getValue(RATIO_idx);
		// when the ratio can be anything larger than 0, a and b can switch their meaning.
		// so the bean can be transformed over to itself by switching the ratio and rotating it by 90 degrees = pi/2
		// thus check that ratio > 0 and theta in a range of pi/2
//		return ( (theta > -(Math.PI)) && (theta <= +(Math.PI))
//				&& (ratio > 0) );
		return ratio > 0;
	}

	public final TreeMap<String, Double> getParamMap(ParamPoint p) {
		TreeMap<String, Double> map = new TreeMap<String, Double>();
		map.put(X_name, p.getValue(X_idx));
		map.put(Y_name, p.getValue(Y_idx));
		map.put(THETA_name, this.theta); // p.getValue(THETA_idx));
		double a = getBestA(p);
		map.put(A_name, a);
		map.put(B_name, p.getValue(RATIO_idx) * a);
		map.put(COVER_name, getCover(p, a));
		return map;
	}
}
