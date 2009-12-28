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

import gnu.trove.TIntArrayList;
import gnu.trove.TIntIterator;

import java.util.Collection;
import java.util.LinkedList;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordUtils;

import playground.johannes.socialnetworks.graph.mcmc.AdjacencyMatrix;
import playground.johannes.socialnetworks.graph.mcmc.ConditionalDistribution;
import playground.johannes.socialnetworks.graph.mcmc.GibbsSampler;
import playground.johannes.socialnetworks.graph.spatial.SpatialAdjacencyMatrix;
import playground.johannes.socialnetworks.graph.spatial.SpatialSparseGraph;
import playground.johannes.socialnetworks.graph.spatial.SpatialSparseVertex;
import playground.johannes.socialnetworks.graph.spatial.io.Population2SpatialGraph;

/**
 * @author illenberger
 *
 */
public class FixedCostGenerator {
	
	private static double z;
	
	private static double totalCosts = 10;

	private static final Logger logger = Logger.getLogger(FixedCostGenerator.class);
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Population2SpatialGraph reader = new Population2SpatialGraph();
		SpatialSparseGraph graph = reader.read("/Users/fearonni/vsp-work/work/socialnets/data/schweiz/complete/plans/plans.0.003.xml");
		SpatialAdjacencyMatrix y = new SpatialAdjacencyMatrix(graph);
		
//		z = getNormConstant(graph, -2, 5);
		z = 1;
		GibbsSampler sampled = new GibbsSampler();
		
		DumpHandler handler = new DumpHandler("/Users/fearonni/vsp-work/work/socialnets/mcmc/output/", null, null);
		handler.setBurnin((int)1E8);
		handler.setDumpInterval((int)1E8);
		handler.setLogInterval((int)1e6);
		
		sampled.sample(y, new CondProba(), handler);
		
	}
	
	private static double getNormConstant(SpatialSparseGraph graph, double gamma, double k_mean) {
		LinkedList<SpatialSparseVertex> pending = new LinkedList<SpatialSparseVertex>();
		pending.addAll((Collection<? extends SpatialSparseVertex>) graph.getVertices());
	
		int count = 0;
		int total = graph.getVertices().size();
		
		double sum = 0;
		SpatialSparseVertex v1;
		while ((v1 = pending.poll()) != null) {
			for (SpatialSparseVertex v2 : pending) {
				sum += calcProba(v1, v2, gamma);
			}
			count++;
			if(count % 1000 == 0)
				logger.info(String.format("Processed %1$s of %2$s vertices (%3$s)", count, total, count/(float)total));
		}
		
		return k_mean * graph.getVertices().size()/sum * 0.5;
	}
	
	private static double calcProba(SpatialSparseVertex v_i, SpatialSparseVertex v_j, double gamma) {
		double d = CoordUtils.calcDistance(v_i.getCoordinate(), v_j.getCoordinate());
		d = Math.ceil(d/1000.0);
		d = Math.max(d, 1.0);
		return Math.pow(d, gamma);
	}
	
	public static class CondProba implements ConditionalDistribution {

		private final double gamma = 2;
		
		private double descretization = 1000.0;
		
		public double changeStatistic(AdjacencyMatrix y, int i, int j,
				boolean yIj) {
			Coord c_i = ((SpatialAdjacencyMatrix)y).getVertex(i).getCoordinate();
			Coord c_j = ((SpatialAdjacencyMatrix)y).getVertex(j).getCoordinate();
			
			int d = descretize(CoordUtils.calcDistance(c_i, c_j));
			double p_dist = Math.pow(d, gamma)/z;
			
			TIntArrayList neighbours = y.getNeighbours(i);
			double sum = 0;
			for(int k = 0; k < neighbours.size(); k++) {
				c_j = ((SpatialAdjacencyMatrix)y).getVertex(neighbours.get(k)).getCoordinate();
				sum += descretize(CoordUtils.calcDistance(c_i, c_j));
			}
			
			double p_cost = 1000000*sum/totalCosts;
			
			return p_dist * p_cost;
		}
		
		private int descretize(double d) {
			if(d <= 0)
				return 1;
			else
				return (int)Math.ceil(d/descretization);
		}

		/* (non-Javadoc)
		 * @see playground.johannes.socialnetworks.graph.mcmc.ConditionalDistribution#addEdge(playground.johannes.socialnetworks.graph.mcmc.AdjacencyMatrix, int, int)
		 */
		public void addEdge(AdjacencyMatrix y, int i, int j) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see playground.johannes.socialnetworks.graph.mcmc.ConditionalDistribution#removeEdge(playground.johannes.socialnetworks.graph.mcmc.AdjacencyMatrix, int, int)
		 */
		public void removeEdge(AdjacencyMatrix y, int i, int j) {
			// TODO Auto-generated method stub
			
		}
		
	}

}
