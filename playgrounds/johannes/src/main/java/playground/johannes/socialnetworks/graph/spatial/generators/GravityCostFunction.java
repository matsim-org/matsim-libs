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

import playground.johannes.socialnetworks.statistics.Discretizer;
import playground.johannes.socialnetworks.statistics.LinearDiscretizer;

/**
 * @author illenberger
 *
 */
public class GravityCostFunction implements EdgeCostFunction {

	private final double gamma;
	
	private final double constant;
	
	private Discretizer discretizer;
	
	public GravityCostFunction(double gamma, double constant) {
		this.gamma = gamma;
		this.constant = constant;
		discretizer = new LinearDiscretizer(1000.0);
	}
	
	@Override
	public double edgeCost(SpatialVertex vi, SpatialVertex vj) {
		double dx = vj.getPoint().getCoordinate().x - vi.getPoint().getCoordinate().x;
		double dy = vj.getPoint().getCoordinate().y - vi.getPoint().getCoordinate().y;
		double d = Math.max(1.0, discretizer.discretize(Math.sqrt(dx*dx + dy*dy)));
		
		return gamma * Math.log(d) + constant;
	}

}
