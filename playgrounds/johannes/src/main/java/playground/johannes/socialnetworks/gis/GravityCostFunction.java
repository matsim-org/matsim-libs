/* *********************************************************************** *
 * project: org.matsim.*
 * GravityCostFunction.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.johannes.socialnetworks.gis;

import playground.johannes.socialnetworks.statistics.Discretizer;
import playground.johannes.socialnetworks.statistics.LinearDiscretizer;

import com.vividsolutions.jts.geom.Point;

/**
 * @author illenberger
 *
 */
public class GravityCostFunction implements SpatialCostFunction {

	private final double gamma;
	
	private final double constant;
	
	private Discretizer discretizer;
	
	private DistanceCalculator calculator;
	
	public GravityCostFunction(double gamma, double constant) {
		this(gamma, constant, new OrthodromicDistanceCalculator());
	}
	
	public GravityCostFunction(double gamma, double constant, DistanceCalculator calculator) {
		this.gamma = gamma;
		this.constant = constant;
		discretizer = new LinearDiscretizer(1000.0);
		this.calculator = calculator;
	}
	
	public void setDiscretizer(Discretizer discretizer) {
		this.discretizer = discretizer;
	}
	
	@Override
	public double costs(Point p1, Point p2) {
		double d = Math.max(1.0, discretizer.discretize(calculator.distance(p1, p2)));
		
		return gamma * Math.log(d) + constant;
	}

}
