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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.matrix.AdjacencyMatrix;
import org.matsim.contrib.sna.graph.spatial.SpatialVertex;
import org.matsim.contrib.sna.math.Distribution;

import playground.johannes.socialnetworks.graph.mcmc.GraphProbability;

/**
 * @author illenberger
 *
 */
public class ErgmEdgeCost implements GraphProbability {

	private double[] thetas;
	
	private EdgeCostFunction costFunction;
	
	public ErgmEdgeCost(AdjacencyMatrix<? extends SpatialVertex> y, EdgeCostFunction costFunction, double budget, String thetaFile, double theta_edge) {
		this.costFunction = costFunction;
		
//		TObjectDoubleHashMap<SpatialVertex> budgets = new TObjectDoubleHashMap<SpatialVertex>();
//		for(int i = 0; i < y.getVertexCount(); i++) {
//			budgets.put(y.getVertex(i), budget);
//		}
		
//		ThetaSolver solver = new ThetaSolver(costFunction);
//		TObjectDoubleHashMap<SpatialVertex> tmpThetas = solver.solve(budgets);
		
		ThetaApproximator approximator = new ThetaApproximator();
		Set<SpatialVertex> vertices = new HashSet<SpatialVertex>();
		
		for(int i = 0; i < y.getVertexCount(); i++) {
			vertices.add(y.getVertex(i));
		}
		
		TObjectDoubleHashMap<SpatialVertex> tmpThetas = approximator.approximate(vertices, budget, costFunction, theta_edge);
		
		thetas = new double[y.getVertexCount()];
		Distribution distr = new Distribution();
		for(int i = 0; i < thetas.length; i++) {
			thetas[i] = tmpThetas.get(y.getVertex(i));
			distr.add(thetas[i]);
		}
		
		if(thetaFile != null) {
			double binsize = (distr.max() - distr.min())/100.0;
			try {
				Distribution.writeHistogram(distr.absoluteDistribution(binsize), thetaFile);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <V extends Vertex> double difference(AdjacencyMatrix<V> y, int i, int j, boolean yIj) {
		SpatialVertex vi = ((AdjacencyMatrix<SpatialVertex>)y).getVertex(i);
		SpatialVertex vj = ((AdjacencyMatrix<SpatialVertex>)y).getVertex(j);
		
		return Math.exp(thetas[i] * costFunction.edgeCost(vi, vj));
	}

}
