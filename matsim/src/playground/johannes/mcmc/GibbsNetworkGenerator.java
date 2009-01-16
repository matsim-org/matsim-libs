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

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import playground.johannes.graph.GraphStatistics;
import playground.johannes.graph.PlainGraph;
import playground.johannes.graph.SparseEdge;
import playground.johannes.graph.SparseVertex;
import playground.johannes.graph.generators.ErdosRenyiGenerator;
import playground.johannes.graph.generators.PlainGraphFactory;
import playground.johannes.statistics.WeightedStatistics;

/**
 * @author illenberger
 *
 */
public class GibbsNetworkGenerator {

	private static double STEPS = 100000000;
	
	private static double DUMP_INTERVAL = 1000;
	
	private static double LAMBDA = 2;
	
	private static int N = 100;
	
	private static PlainGraph g; 
	
	public static void main(String args[]) throws FileNotFoundException, IOException {
		STEPS = Integer.parseInt(args[0]);
		N = Integer.parseInt(args[1]);
		LAMBDA = Double.parseDouble(args[2]);
		DUMP_INTERVAL = Integer.parseInt(args[3]);
		String outputStats = args[4];
		String outputHist = args[5];
		
		BufferedWriter meanDegreeWriter = new BufferedWriter(new FileWriter(outputStats));
		meanDegreeWriter.write("it\tz\tP_Y\tn_p_1\tn_p_0\tn_edges");
		meanDegreeWriter.newLine();
		/*
		 * Initialize with a random graph.
		 */
		ErdosRenyiGenerator<PlainGraph, SparseVertex, SparseEdge> generator = new ErdosRenyiGenerator<PlainGraph, SparseVertex, SparseEdge>(new PlainGraphFactory());
		g = generator.generate(N, 0.000001, 0);
		System.out.println("Initial mean degree is " + GraphStatistics.getDegreeStatistics(g).getMean());
		/*
		 * Create a list of vertices.
		 */
		Random random = new Random(2);
		ArrayList<SparseVertex> vertices = new ArrayList<SparseVertex>(g.getVertices());
		int size = vertices.size();

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
				
				double P_Y_0;
				double P_Y_1;
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
				P_Y_0 = calcP_Y(v1.getEdges().size(), v2.getEdges().size()); 
				/*
				 * Insert an edge between v1 and v2.
				 */
				e = g.addEdge(v1, v2);
				if(e == null)
					throw new RuntimeException();
				/*
				 * Edge is switched on.
				 */
				P_Y_1 = calcP_Y(v1.getEdges().size(), v2.getEdges().size());
				/*
				 * Calc P_Y
				 */
				P_Y = P_Y_0 / (P_Y_0 + P_Y_1);
					
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

				sum_P_Y_0 += P_Y_0;
				sum_P_Y_1 += P_Y_1;
				sum_P_Y += P_Y;
				
				if(i % DUMP_INTERVAL == 0) {
					double z = GraphStatistics.getDegreeStatistics(g).getMean();
					
					meanDegreeWriter.write(String.format("%1$s\t%2$s\t%3$s\t%4$s\t%5$s\t%6$s", i, z, sum_P_Y / (double)DUMP_INTERVAL, sum_p1, sum_p0, g.getEdges().size()));
					meanDegreeWriter.newLine();
					
					System.out.println(String.format("[%1$s] z = %2$s, P_Y_0 = %3$s, P_Y_1 = %4$s, P_Y = %5$s, p_1 = %6$s, p_0 = %7$s, edges = %8$s",
							i, z, sum_P_Y_0 / (float)DUMP_INTERVAL, sum_P_Y_1 / (float)DUMP_INTERVAL,
							sum_P_Y / (float)DUMP_INTERVAL, sum_p1, sum_p0, g.getEdges().size()));
					
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
		WeightedStatistics stats = GraphStatistics.getDegreeDistribution(g);
		WeightedStatistics.writeHistogram(stats.absoluteDistribution(), outputHist);
	}
	
	private static double calcP_Y(int k1, int k2) {
//		return Math.exp(-LAMBDA * (k1 + k2) / N);
		return Math.pow(k1, - LAMBDA) * Math.pow(k2, - LAMBDA); 
	}
}
