/* *********************************************************************** *
 * project: org.matsim.*
 * UtilFuncDiff.java
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

import gnu.trove.TIntArrayList;

import org.matsim.contrib.sna.gis.CRSUtils;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.matrix.AdjacencyMatrix;
import org.matsim.contrib.sna.graph.spatial.SpatialSparseGraph;
import org.matsim.contrib.sna.graph.spatial.SpatialSparseGraphBuilder;
import org.matsim.contrib.sna.graph.spatial.SpatialSparseVertex;
import org.matsim.contrib.sna.graph.spatial.SpatialVertex;
import org.matsim.contrib.sna.graph.spatial.io.SpatialGraphMLReader;

import playground.johannes.socialnetworks.gis.CartesianDistanceCalculator;
import playground.johannes.socialnetworks.graph.mcmc.GibbsSampler;
import playground.johannes.socialnetworks.graph.mcmc.GraphProbability;
import playground.johannes.socialnetworks.graph.spatial.io.Population2SpatialGraph;

/**
 * @author illenberger
 *
 */
public class UtilFuncDiff implements GraphProbability {

	private EdgeCostFunction costFunc;
	
	private final double beta_k;
	
	private final double beta_c;
	
	private final double budget;

	public UtilFuncDiff(EdgeCostFunction costFunc, double beta_k, double beta_c, double budget) {
		this.costFunc = costFunc;
		this.beta_k = beta_k;
		this.beta_c = beta_c;
		this.budget = budget;
	}
	
	@Override
	public <V extends Vertex> double difference(AdjacencyMatrix<V> y, int i, int j, boolean yIj) {
		double c_sum = 0;
		SpatialVertex v_i = (SpatialVertex) y.getVertex(i);
		SpatialVertex v_j = (SpatialVertex) y.getVertex(j);
		double c_ij = costFunc.edgeCost(v_i, v_j);
		
		TIntArrayList neighbors = y.getNeighbours(i);
		for(int k = 0; k < neighbors.size(); k++) {
			SpatialVertex v2 = (SpatialVertex) y.getVertex(neighbors.get(k));
			c_sum += costFunc.edgeCost(v_i, v2);
		}
		
		if(yIj)
			c_sum -= c_ij; 
		
		double c_diff_m = Math.abs(budget - c_sum);
		
		c_sum += c_ij;
		
		double c_diff_p = Math.abs(budget - c_sum);
		
		return Math.exp(- beta_k + beta_c * (c_diff_m - c_diff_p));
	}

	public static void main(String args[]) {
//		SpatialGraphMLReader reader = new SpatialGraphMLReader();
		Population2SpatialGraph reader = new Population2SpatialGraph(CRSUtils.getCRS(21781));
		SpatialSparseGraph graph = reader.read("/Users/jillenberger/Work/work/socialnets/data/schweiz/complete/plans/plans.0.003.xml");
		
		AdjacencyMatrix<SpatialSparseVertex> y = new AdjacencyMatrix<SpatialSparseVertex>(graph);
		
		GibbsSampler sampler = new GibbsSampler();
		sampler.setInterval((int)1e6);
		
		DumpHandler handler = new DumpHandler(graph, new SpatialSparseGraphBuilder(graph.getCoordinateReferenceSysten()), "/Users/jillenberger/Work/work/socialnets/mcmc/output/");
		handler.setBurnin((int)2E9);
		handler.setDumpInterval((int)5e7);
		handler.setLogInterval((int)5e6);
		
		GraphProbability p = new UtilFuncDiff(new GravityEdgeCostFunction(1.6, 1, new CartesianDistanceCalculator()), 1, -100, 60);
		
		handler.analyze(y, 0);
		sampler.sample(y, p, handler);
	}
}
