/* *********************************************************************** *
 * project: org.matsim.*
 * SuperEllipseObjective.java
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

package playground.andreas.optimization;

import java.util.Collection;
import java.util.Iterator;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.gbl.Gbl;

/**
 * Objective Function based on a SuperEllipse. The superellipse is defined as
 * <ul><li>cartesian:       |x/a|^r + |y/b|^r = 1</li>
 *     <li>parametrically:  X = a*[(cos(t))^(2/r)], Y = b*[(sin(t))^(2/r)]</li>
 *     <li>area:            A = {4^(1-1/r)*a*b*(??)*?(1+1/r)}/{(1/2+1/r)}
 * In space, a superellipse is described by the following 6 parameters:
 * <ul><li>x, y: the Coordinates of the center of the ellipse</li>
 *     <li>a, b: the length of the two orthogonal axes</li>
 *     <li>r:exponent</li>
 *     <li>theta: the rotation of the main axis a</li></ul>
 * In our case, we use a 5-dimensional parameter space with the following
 * 5 axes and their ranges:
 * <ul><li>x : ]-INF .. +INF[</li>
 *     <li>y : ]-INF .. +INF[</li>
 *     <li>theta : ]-pi/2 .. +pi/2[</li>
 *     <li>r:]1..+INF[</li>
 *     <li>ratio : ]0 .. +INF[</li></ul>
 * The effective size of the superellipse is given implicitely by a minimum
 * cover-percentage. The superellipse will be enlarged until it contains at
 * least the specified percentage of all points.
 */
public class SuperEllipseObjective implements Objective {

	//////////////////////////////////////////////////////////////////////
	// member constants
	//////////////////////////////////////////////////////////////////////

	public static final String OBJECTIVE_NAME = "superellipse";

	public static final double EPSILON = 100.0;		// TODO, test what value is best for this param

	public static final int DIMENSION = 4;

	public static final int X_idx = 0;
	public static final int Y_idx = 1;
	public static final int RATIO_idx = 2;
    public static final int R_idx = 3;

	public static final String X_name = "x";
	public static final String Y_name = "y";

	public static final String THETA_name = "theta";
	public static final String R_name = "r";
	public static final String A_name = "a";
	public static final String B_name = "b";
	public static final String COVER_name = "cover";

	private final static Logger log = Logger.getLogger(SuperEllipseObjective.class);
	
	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final Collection<Coord> coords;	// list of points which the calculated superelliptic region should cover
	private final double minCover;

	private double theta;

	private ParamPoint[] initPPoints;

	//////////////////////////////////////////////////////////////////////
	// constructor
	//////////////////////////////////////////////////////////////////////

	public SuperEllipseObjective(Collection<Coord> coords, double minCover, double theta) {
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

		return a * a * p.getValue(RATIO_idx)*Math.sqrt(Math.PI)*(Math.pow(4.0,(1.0-(1/(p.getValue(R_idx)))))*dgamma(1.0+(1/(p.getValue(R_idx))))/dgamma((1.0/2.0)+(1/(p.getValue(R_idx)))));

	}

	/**
	 * returns the value for the superellipse-parameter a that best fulfills the cover-percentage
	 * @param p a parameter point for which a is calculated
	 * @return smallest superellipse-axis parameter a so that the ellipse covers at least a predefined percentage of points
	 */
	private final double getBestA(ParamPoint p) {

		double cover = 0;
		double addFactor = 1;
		double a = 1;	// the ``real'' size of a

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


//	This routine calculates the value of Gamma function which is a prerequesite for area of superellipse.
	double dgamma(double x)
	{
	    int k, n;
	    double w, y;
	    n = x < 1.5 ? -((int) (2.5 - x)) : (int) (x - 1.5);
	    w = x - (n + 2);
	    y = (((((((((((((-1.99542863674e-7 * w + 1.337767384067e-6) * w -
	        2.591225267689e-6) * w - 1.7545539395205e-5) * w +
	        1.45596568617526e-4) * w - 3.60837876648255e-4) * w -
	        8.04329819255744e-4) * w + 0.008023273027855346) * w -
	        0.017645244547851414) * w - 0.024552490005641278) * w +
	        0.19109110138763841) * w - 0.233093736421782878) * w -
	        0.422784335098466784) * w + 0.99999999999999999);
	    if (n > 0) {
	        w = x - 1;
	        for (k = 2; k <= n; k++) {
	            w *= x - k;
	        }
	    } else {
	        w = 1;
	        for (k = 0; k > n; k--) {
	            y *= x - k;
	        }
	    }
//	    System.out.println(w/y);
	    return w / y;
	}

	/**
	 * calculates the percentage of covered points with the ellipse defined by p and a.
	 *
	 * @return percentage of the points covered by the ellipse
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
		double r = p.getValue(R_idx);
		double b = p.getValue(RATIO_idx) * a;

		double x = coord.getX();
		double y = coord.getY();

		double term1 = (Math.cos(this.theta)*(x-x0) + Math.sin(this.theta)*(y-y0))/a;
		double term2 = (-Math.sin(this.theta)*(x-x0) + Math.cos(this.theta)*(y-y0))/b;

		double dist = Math.sqrt(Math.pow(Math.abs(term1),r) +Math.pow(Math.abs( term2),r));
		return (dist<= 1);
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
			log.warn("Initial paramPoint " + index + " was requested, but we only have 4 Dimensions. Returning initial paramPoint 0.");
			return this.initPPoints[0];
	  }
		return this.initPPoints[index];
	}

	public final boolean isValidParamPoint(ParamPoint p) {
		// do not validate x and y, as they have no real limitation
//		double theta = p.getValue(THETA_idx);
		double ratio = p.getValue(RATIO_idx);
		double r = p.getValue(R_idx);
		// when the ratio can be anything larger than 0, a and b can switch their meaning.
		// so the ellipse can be transformed over to itself by switching the ratio and rotating it by 90 degrees = pi/2
		// thus check that ratio > 0 and theta in a range of pi/2
//		return ( (theta > -(Math.PI/4)) && (theta <= +(Math.PI/4))
//				&& (ratio > 0) );
		return ((ratio > 0) && (r >= 0.1) && (r <= 0.9));
	}

	public final TreeMap<String, Double> getParamMap(ParamPoint p) {
		TreeMap<String, Double> map = new TreeMap<String, Double>();
		map.put(X_name, p.getValue(X_idx));
		map.put(Y_name, p.getValue(Y_idx));
		map.put(THETA_name, this.theta);
		map.put(R_name,p.getValue(R_idx));
		double a = getBestA(p);
		map.put(A_name, a);
		map.put(B_name, p.getValue(RATIO_idx) * a);
		map.put(COVER_name, getCover(p, a));
		return map;
	}
}


