/* *********************************************************************** *
 * project: org.matsim.*
 * ErgmEdgeCost.java
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

import gnu.trove.TObjectDoubleHashMap;

import org.matsim.contrib.sna.graph.spatial.SpatialVertex;

import playground.johannes.socialnetworks.graph.mcmc.AdjacencyMatrix;
import playground.johannes.socialnetworks.graph.mcmc.ErgmTerm;
import playground.johannes.socialnetworks.graph.spatial.SpatialAdjacencyMatrix;

/**
 * @author illenberger
 *
 */
public class ErgmEdgeCost extends ErgmTerm {

	private double[] thetas;
	
	private EdgeCostFunction costFunction;
	
	public ErgmEdgeCost(SpatialAdjacencyMatrix y, EdgeCostFunction costFunction, double budget) {
		this.costFunction = costFunction;
		
		TObjectDoubleHashMap<SpatialVertex> budgets = new TObjectDoubleHashMap<SpatialVertex>();
		for(int i = 0; i < y.getVertexCount(); i++) {
			budgets.put(y.getVertex(i), budget);
		}
		
		ThetaSolver solver = new ThetaSolver(costFunction);
		TObjectDoubleHashMap<SpatialVertex> tmpThetas = solver.solve(budgets);
		thetas = new double[y.getVertexCount()];
		for(int i = 0; i < thetas.length; i++) {
			thetas[i] = tmpThetas.get(y.getVertex(i));
		}
	}
	
	@Override
	public double changeStatistic(AdjacencyMatrix y, int i, int j, boolean yIj) {
		SpatialVertex vi = ((SpatialAdjacencyMatrix)y).getVertex(i);
		SpatialVertex vj = ((SpatialAdjacencyMatrix)y).getVertex(j);
		
		return - thetas[i] * costFunction.edgeCost(vi, vj);
	}

}
