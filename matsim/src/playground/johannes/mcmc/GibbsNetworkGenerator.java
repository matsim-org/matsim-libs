/* *********************************************************************** *
 * project: org.matsim.*
 * GibbsNetworkGenerator.java
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

/**
 * 
 */
package playground.johannes.mcmc;

import gnu.trove.TIntIntHashMap;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import playground.johannes.graph.Graph;
import playground.johannes.graph.GraphStatistics;
import playground.johannes.graph.PlainGraph;
import playground.johannes.graph.SparseEdge;
import playground.johannes.graph.SparseVertex;
import playground.johannes.graph.Vertex;
import playground.johannes.graph.generators.ErdosRenyiGenerator;
import playground.johannes.graph.generators.PlainGraphFactory;
import playground.johannes.socialnet.Ego;
import playground.johannes.socialnet.SocialNetworkStatistics;
import playground.johannes.statistics.WeightedStatistics;

/**
 * @author illenberger
 *
 */
public class GibbsNetworkGenerator {

	private static double STEPS = 1E6;//Double.MAX_VALUE;
	
	private static double DUMP_INTERVAL = 1000;
	
	private static double LAMBDA = 2;
	
	private static double THETA;
	
	private static int N = 1000;
	
	public static Graph createNetwork() {
//		BasicPopulationImpl<BasicPerson<BasicPlan, BasicKnowledge<BasicActivity>>> pop = new BasicPopulationImpl<BasicPerson<BasicPlan,BasicKnowledge<BasicActivity>>>();
//		PopulationReader reader = new BasicPopulationReaderV5(pop, null);
//		reader.readFile("/Users/fearonni/vsp-work/socialnets/devel/MCMC/plans.100km.1.xml");
//		ErdosRenyiGenerator<SocialNetwork<BasicPerson<BasicPlan,BasicKnowledge<BasicActivity>>>, Ego<BasicPerson<BasicPlan,BasicKnowledge<BasicActivity>>>, SocialTie> generator = 
//			new ErdosRenyiGenerator<SocialNetwork<BasicPerson<BasicPlan,BasicKnowledge<BasicActivity>>>, Ego<BasicPerson<BasicPlan,BasicKnowledge<BasicActivity>>>, SocialTie>(
//					new SocialNetworkFactory<BasicPerson<BasicPlan,BasicKnowledge<BasicActivity>>>(pop));
//		return generator.generate(N, 0.01, 0);
		
		ErdosRenyiGenerator<PlainGraph, SparseVertex, SparseEdge> generator = new ErdosRenyiGenerator<PlainGraph, SparseVertex, SparseEdge>(new PlainGraphFactory());
		return generator.generate(N, 0.01, 0);
	}
	
	public static void main(String args[]) throws FileNotFoundException, IOException {
//		STEPS = Integer.parseInt(args[0]);
//		N = Integer.parseInt(args[1]);
//		LAMBDA = Double.parseDouble(args[2]);
//		DUMP_INTERVAL = Integer.parseInt(args[3]);
		String outputStats = args[4];
		String outputHist = args[5];
	
		BufferedWriter meanDegreeWriter = new BufferedWriter(new FileWriter(outputStats));
		meanDegreeWriter.write("it\tz\tP_Y\tn_p_1\tn_p_0\tn_edges");
		meanDegreeWriter.newLine();
		/*
		 * Initialize with a random graph.
		 */
		PlainGraph g = (PlainGraph) createNetwork();
		System.out.println("Initial mean degree is " + GraphStatistics.getDegreeStatistics(g).getMean()); 
		WeightedStatistics stats = GraphStatistics.getDegreeDistribution(g);
		WeightedStatistics.writeHistogram(stats.absoluteDistribution(), outputHist +".initial");
		/*
		 * Create a list of vertices.
		 */
		Random random = new Random(2);
		ArrayList<SparseVertex> vertices = new ArrayList<SparseVertex>(g.getVertices());
		int size = vertices.size();
		
		THETA = Math.log(0.02/( 1 - 0.02));

		int sum_p1 = 0;
		int sum_p0 = 0;
		double sum_P_Y_0 = 0;
		double sum_P_Y_1 = 0;
		double sum_P_Y = 0;
		
		for(int i = 0; i < STEPS; i++) {
			/*
			 * Draw two random vertices.
			 */
			SparseVertex v1 = vertices.get((int)Math.round(random.nextDouble() * (size-1)));
			random.nextDouble();
			SparseVertex v2 = vertices.get((int)Math.round(random.nextDouble() * (size-1)));
			
			if(v1 != v2) {
				double P_Y;
				/*
				 * If v1 and v2 are connected, remove the edge.
				 */
				SparseEdge e = g.getEdge(v1, v2);
				if(e != null) {
					if(g.removeEdge(e) == false)
						throw new RuntimeException();
				}
				/*
				 * Edge is switched off.
				 */
				TIntIntHashMap deltaN_k = new TIntIntHashMap();
				TIntIntHashMap n_k_minus = new TIntIntHashMap();
				
				int k_i_minus = v1.getEdges().size();
				int k_j_minus = v2.getEdges().size();
				int k_i_plus = k_i_minus + 1;
				int k_j_plus = k_j_minus + 1;
				
				if(k_i_minus == k_j_minus) {
					n_k_minus.put(k_i_minus, countVertices(g, k_i_minus));
					n_k_minus.put(k_i_plus, countVertices(g, k_i_plus));
					
					deltaN_k.put(k_i_minus, - 2);
					deltaN_k.put(k_i_plus, + 2);
				} else {
					n_k_minus.put(k_i_minus, countVertices(g, k_i_minus));
					n_k_minus.put(k_j_minus, countVertices(g, k_j_minus));
					n_k_minus.put(k_i_plus, countVertices(g, k_i_plus));
					n_k_minus.put(k_j_plus, countVertices(g, k_j_plus));
					
					deltaN_k.put(k_i_minus, - 1);
					deltaN_k.put(k_j_minus, - 1);
					deltaN_k.put(k_i_plus, + 1);
					deltaN_k.put(k_j_plus, + 1);
				}
				/*
				 * Insert an edge between v1 and v2.
				 */
				e = g.addEdge(v1, v2);
				if(e == null)
					throw new RuntimeException();
				/*
				 * Edge is switched on.
				 */
				
				double prod = 1;
				for(int k : deltaN_k.keys()) {
					int deltaN = deltaN_k.get(k);
					if(deltaN != 0) {
						int n_minus = n_k_minus.get(k);
						double numerator =1;
						double denumerator = 1;
						
						if(n_minus < 3) {
							numerator = 1;
							
							for(int h = 1; h <= (n_minus + deltaN); h++)
								denumerator *= h;
						} else {
							numerator = (n_minus - 2) * (n_minus - 1) * n_minus;
							
							for (int h = -2; h <= deltaN; h++) {
								denumerator *= n_minus + h;
							}
						}
						prod *= numerator/denumerator * Math.pow(k+1, - LAMBDA * deltaN);
						if(Double.isNaN(prod)) {
							System.err.println("nan!");
						} else if(Double.isInfinite(prod))
							System.err.println("infinity");
					}
				}
				
				P_Y = 1 / (1 + (prod * Math.exp(THETA)));
				
				if(Double.isInfinite(P_Y)) {
					P_Y = 1;
					System.err.println("infinity!");
				}
				
				
				if(Double.isNaN(P_Y)) {
					P_Y = 1;
					System.err.println("not a number");
				} else if (P_Y == 0) {
					System.err.println("zero");
				}
				
				if(random.nextDouble() <= P_Y) {
					/*
					 * Remove the edge.
					 */
					if(g.removeEdge(e) == false)
						throw new RuntimeException();
					
					sum_p0++;
				} else {
					/*
					 * Leave the edge switched on.
					 */
					sum_p1++;
				}

				sum_P_Y += P_Y;
				if(i % DUMP_INTERVAL == 0) {
					double z = GraphStatistics.getDegreeStatistics(g).getMean();
					
					meanDegreeWriter.write(String.format("%1$s\t%2$s\t%3$s\t%4$s\t%5$s\t%6$s", i, z, sum_P_Y / (double)DUMP_INTERVAL, sum_p1, sum_p0, g.getEdges().size()));
					meanDegreeWriter.newLine();
					
					System.out.println(String.format("[%1$s] z = %2$s, P_Y = %3$s, p_1 = %4$s, p_0 = %5$s, edges = %6$s",
							i, z, sum_P_Y / (float)DUMP_INTERVAL, sum_p1, sum_p0, g.getEdges().size()));
					
					sum_P_Y_0 = 0;
					sum_P_Y_1 = 0;
					sum_P_Y = 0;
					sum_p1 = 0;
					sum_p0 = 0;
				}
			
			} else {
//				System.err.println("Selected same vertices...");
			}
		}
		meanDegreeWriter.close();
		
		System.out.println("Mean degree is " + GraphStatistics.getDegreeStatistics(g).getMean());
		System.out.println("Clustering is " + GraphStatistics.getClusteringStatistics(g).getMean());
		System.out.println("Degree correlation is " + GraphStatistics.getDegreeCorrelation(g));
		stats = GraphStatistics.getDegreeDistribution(g);
		WeightedStatistics.writeHistogram(stats.absoluteDistribution(), outputHist);	
//		WeightedStatistics.writeHistogram(SocialNetworkStatistics.getEdgeLengthDistribution(g, true, 1000).absoluteDistribution(1000), "/Users/fearonni/vsp-work/socialnets/devel/MCMC/edgelength.txt");
	}
	
	private static int countVertices(Graph g, int k) {
		int count = 0;
		for(Vertex v : g.getVertices())
			if(v.getEdges().size() == k)
				count++;
		
		return count;
	}
}
