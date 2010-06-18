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
package playground.johannes.socialnetworks.graph.spatial.generators;

import org.matsim.contrib.sna.graph.spatial.SpatialVertex;

import playground.johannes.socialnetworks.gis.DistanceCalculator;
import playground.johannes.socialnetworks.gis.GravityCostFunction;
import playground.johannes.socialnetworks.gis.OrthodromicDistanceCalculator;

/**
 * @author illenberger
 *
 */
public class GravityEdgeCostFunction implements EdgeCostFunction {

//	private final double gamma;
//	
//	private final double constant;
//	
//	private Discretizer discretizer;
//	
//	private DistanceCalculator calculator;
	
	private GravityCostFunction delegate;
	
	public GravityEdgeCostFunction(double gamma, double constant) {
		this(gamma, constant, new OrthodromicDistanceCalculator());
	}
	
	public GravityEdgeCostFunction(double gamma, double constant, DistanceCalculator calculator) {
		delegate = new GravityCostFunction(gamma, constant, calculator);
//		this.gamma = gamma;
//		this.constant = constant;
//		discretizer = new LinearDiscretizer(1000.0);
//		this.calculator = calculator;
	}
	
	@Override
	public double edgeCost(SpatialVertex vi, SpatialVertex vj) {
//		double d = Math.max(1.0, discretizer.discretize(calculator.distance(vi.getPoint(), vj.getPoint())));
//		
//		return gamma * Math.log(d) + constant;
		return delegate.costs(vi.getPoint(), vj.getPoint());
	}

}
