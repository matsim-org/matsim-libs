/* *********************************************************************** *
 * project: org.matsim.*
 * Gibbs2.java
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

import gnu.trove.TIntDoubleHashMap;
import gnu.trove.TIntIntHashMap;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.commons.collections.CollectionUtils;

import playground.johannes.graph.Graph;
import playground.johannes.graph.GraphStatistics;
import playground.johannes.graph.PlainGraph;
import playground.johannes.graph.SparseEdge;
import playground.johannes.graph.SparseVertex;
import playground.johannes.graph.Vertex;
import playground.johannes.graph.generators.ErdosRenyiGenerator;
import playground.johannes.graph.generators.PlainGraphFactory;
import playground.johannes.graph.mcmc.AdjacencyMatrix;
import playground.johannes.statistics.WeightedStatistics;

/**
 * @author illenberger
 *
 */
public class Gibbs2 {

	private static final int N = 500;
	
	private static final double p = 0.05;
	
	private static final long rndSeed = 4711;
	
//	private static final double num_steps = 1E8;
//	
//	private static final int k_min = 1;
//	
//	private static final int k_max = 200;
//	
//	private static final double gamma = -2;
	
	private static final double DENSITY = 5000 / (0.5*N*(N-1));
	/**
	 * @param args
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException, IOException {
		Random random = new Random(rndSeed);
		/*
		 * Initialize graph
		 */
		ErdosRenyiGenerator<PlainGraph, SparseVertex, SparseEdge> generator = new ErdosRenyiGenerator<PlainGraph, SparseVertex, SparseEdge>(new PlainGraphFactory());
		PlainGraph graph = generator.generate(N, p, rndSeed);
		dump(graph, -1);
		ArrayList<SparseVertex> vertices = new ArrayList<SparseVertex>(graph.getVertices());
		
		double theta1_min = Math.log(0.01/(1-0.01));
		double theta1_max = 0;
		double theta2_offset = 0.351;
		double theta2_min = 0;
		double theta2_max = 0.001;
		double theta2_step = 0.0001;

		BufferedWriter writer1 = new BufferedWriter(new FileWriter(args[0] + "triads.txt"));
		BufferedWriter writer2 = new BufferedWriter(new FileWriter(args[0] + "degree.txt"));

		for(double theta2 = theta2_min; theta2 <= theta2_max; theta2 += theta2_step ) {
			writer1.write("\t" + (float)theta2);
			writer2.write("\t" + (float)theta2);
		}
		writer1.newLine();
		writer2.newLine();
		
		for(double theta1 = theta1_min; theta1 <= theta1_max; theta1 += 0.5) {
			writer1.write(String.valueOf((float)theta1));
			writer2.write(String.valueOf((float)theta1));
			
			for(double theta2 = theta2_min; theta2 <= theta2_max; theta2 += theta2_step ) {
				System.out.println("Simulation graph with theta1="+theta1+", theta2="+theta2);
				long time = System.currentTimeMillis();
				AdjacencyMatrix m = sample(graph, vertices, random, theta1, theta2_offset + (theta2/1000000.0), (int)1E6, 0);
				System.exit(0);
				graph = m.getGraph(new PlainGraphFactory());
				System.out.println("Sampling took " + (System.currentTimeMillis() - time));
				writer1.write("\t" + (float)GraphStatistics.getClusteringStatistics(graph).getMean());
				writer2.write("\t" + (float)GraphStatistics.getDegreeStatistics(graph).getMean());
				writer1.flush();
				writer2.flush();
			}
			writer1.newLine();
			writer2.newLine();
		}
		
//		dump(graph, -1);
//		System.out.println("Expected degree was " + mean_k);
//		System.out.println("Clustering is " + GraphStatistics.getClusteringStatistics(graph).getMean());
//		System.out.println("Degree correlation is " + GraphStatistics.getDegreeCorrelation(graph));
//		WeightedStatistics stats = GraphStatistics.getDegreeDistribution(graph);
//		WeightedStatistics.writeHistogram(stats.absoluteDistribution(), "/Users/fearonni/vsp-work/socialnets/devel/MCMC/hist.txt");
	}
	
	private static AdjacencyMatrix sample(PlainGraph graph,
			ArrayList<SparseVertex> vertices, Random random, double theta1, double theta2, 
			int burnin, int samplesize) throws IOException {
//		double[] changeStats = new double[samplesize];
//		double max_edges = 0.5 * N * (N-1);
		/*
		 * Start sampling
		 */
		BufferedWriter writer = new BufferedWriter(new FileWriter("/Users/fearonni/vsp-work/socialnets/devel/MCMC/mcmc.txt"));
		writer.write("iter\tadd\tremove\ttotal\tedges");
		writer.newLine();
		int add = 0;
		int remove = 0;
		int edges = graph.getEdges().size();
		AdjacencyMatrix m = new AdjacencyMatrix(graph);
		for (int iter = 0; iter < (burnin + samplesize); iter++) {
			/*
			 * Draw two random vertices
			 */
//			SparseVertex v1 = vertices.get(random.nextInt(N));
//			SparseVertex v2 = vertices.get(random.nextInt(N));
			
			int i = random.nextInt(N);
			int j = random.nextInt(N);
//			if (v1 != v2) {
			if(i != j) {
//				SparseEdge e = graph.getEdge(v1, v2);
			

			boolean y_ij = m.getEdge(i, j);
			/*
			 * 
			 */
//			int commonNeighbours = countCommonNeighbours(v1, v2);
			int commonNeighbours = m.countCommonNeighbours(i, j);
//			double P_Y_1 = 1 / (1 + Math.exp(-(theta1 + theta2 * commonNeighbours)));
			double pi_1 = Math.exp(theta1);// + theta2 * commonNeighbours);
			double pi_0 = Math.exp(-(theta1));// + theta2 * commonNeighbours));
			
			/*
			 * 
			 */
			if(y_ij) {
				if(random.nextDouble() <= pi_0) {
					m.removeEdge(i, j);
					remove++;
					edges--;
				}
			} else {
				if(random.nextDouble() <= pi_1) {
					m.addEdge(i, j);
					add++;
					edges++;
				}
			}
			if(i%1000 == 0) {
				writer.write(iter + "\t"+ add/1000.0 + "\t" + remove/1000.0 + "\t" + (add+remove)/1000.0+"\t"+edges);
				writer.newLine();
				add=0;
				remove=0;
			}
//				if (random.nextDouble() <= P_Y_1) {
//					/*
//					 * y_ij = 1
//					 */
//					if(y_ij == false) {
//						if(!m.addEdge(i, j))
//							throw new RuntimeException();
//					}
//				} else {
//					/*
//					 * y_ij = 0
//					 */
//					if(y_ij == true) {
//						if(!m.removeEdge(i, j)) {
//							throw new RuntimeException();
//						}
//					}
//				}
			}
		}
		writer.close();
		return m;
		}
		
//		return changeStats;
	
	private static double mle(double theta_0, double[] changeStats) {
		double theta_min = -1000;
		double theta_max = -2;
		double theta_step = 0.01;
		double theta_head = theta_min;
		double max_loglike = Double.NEGATIVE_INFINITY;
		
		for(double theta = theta_min; theta < theta_max; theta += theta_step) {
			double theta_diff = theta - theta_0;
			double m = changeStats.length;
			double sum = 0;
			int i = 0;
			for(double changeStat : changeStats) {
				double val = Math.exp(theta_diff * changeStat);
				if(val == 0 || Double.isInfinite(val) || Double.isNaN(val))
					throw new RuntimeException();
				sum += (val - sum)/(double)(i+1);
				i++;
			}
//			double loglike = - Math.log(sum/m);
			double loglike = -Math.log(sum);
			if(Double.isInfinite(loglike) || Double.isNaN(loglike))
				throw new RuntimeException();
			if(loglike > max_loglike) {
				theta_head = theta;
				max_loglike = loglike;
			}
		}
		
		return theta_head;
	}
	
	private static TIntIntHashMap countDegrees(Graph g) {
		TIntIntHashMap degrees = new TIntIntHashMap();
		for(Vertex v : g.getVertices()) {
			degrees.adjustOrPutValue(v.getEdges().size(), 1, 1);
		}
		return degrees;
	}
	
	private static void dump(Graph graph, int i) {
		double k_mean = GraphStatistics.getDegreeStatistics(graph).getMean();
		System.out.println(String.format("[%1$s] <k> = %2$s, n_edges = %3$s", i, k_mean, graph.getEdges().size()));
	}
	
	private static int countCommonNeighbours(Vertex v1, Vertex v2) {
		List<? extends Vertex> n1set = v1.getNeighbours();
		List<? extends Vertex> n2set = v2.getNeighbours();
		
		return CollectionUtils.intersection(n1set, n2set).size();
	}
}
