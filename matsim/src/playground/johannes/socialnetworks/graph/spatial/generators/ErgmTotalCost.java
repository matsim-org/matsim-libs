/* *********************************************************************** *
 * project: org.matsim.*
 * KMLWriter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordUtils;

import playground.johannes.socialnetworks.graph.mcmc.AdjacencyMatrix;
import playground.johannes.socialnetworks.graph.mcmc.ErgmTerm;
import playground.johannes.socialnetworks.graph.spatial.SpatialAdjacencyMatrix;

/**
 * @author illenberger
 *
 */
public class ErgmTotalCost extends ErgmTerm {

	private double cost;
	
	private final double beta;
	
	private final double totalCost;
	
	private final double sigma;
	
	public ErgmTotalCost(double beta, double totalCost) {
		this.totalCost = totalCost;
		this.sigma = totalCost/10;
		this.beta = beta;
		cost = 0;
	}
	
	@Override
	public double changeStatistic(AdjacencyMatrix y, int i, int j, boolean yIj) {
		double c_minus = cost;
		double c_plus = cost + distance2Cost(descretize(calcDistance((SpatialAdjacencyMatrix) y, i, j)));
		double r_minus = Math.log(evalGauss(c_minus, totalCost, sigma));
		double r_plus = Math.log(evalGauss(c_plus, totalCost, sigma));
		double r = (r_minus - r_plus);
		if(Double.isNaN(r))
			return Double.POSITIVE_INFINITY;
		else
			return - r;
	}

	@Override
	protected void addEdge(AdjacencyMatrix y, int i, int j) {
		cost += distance2Cost(descretize(calcDistance((SpatialAdjacencyMatrix) y, i, j)));
	}

	@Override
	protected void removeEdge(AdjacencyMatrix y, int i, int j) {
		cost -= distance2Cost(descretize(calcDistance((SpatialAdjacencyMatrix) y, i, j)));
	}

	private double calcDistance(SpatialAdjacencyMatrix y, int i, int j) {
		Coord c_i = y.getVertex(i).getCoordinate();
		Coord c_j = y.getVertex(j).getCoordinate();
		
		return CoordUtils.calcDistance(c_i, c_j);
	}

	private double evalGauss(double x, double mu, double sigma) {
		return 1/(sigma * Math.sqrt(2*Math.PI)) * Math.exp(-0.5 * Math.pow((x-mu)/sigma, 2));
	}
	
	private double distance2Cost(double d) {
		
		return beta*Math.log(d);

	}
	
	private double descretize(double d) {
		d = Math.ceil(d/1000.0);
		d = Math.max(1, d);
		return d;
	}
}
