/* *********************************************************************** *
 * project: org.matsim.*
 * ProposalSim.java
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
package playground.johannes.socialnetworks.misc;

import gnu.trove.TObjectIntHashMap;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.contrib.sna.graph.SparseGraph;
import org.matsim.contrib.sna.graph.SparseGraphBuilder;
import org.matsim.contrib.sna.graph.SparseVertex;

/**
 * @author illenberger
 * 
 */
public class ProposalSim {
	
	private static final Logger logger = Logger.getLogger(ProposalSim.class);

	private static SparseGraphBuilder builder;

	private static SparseGraph graph;

	private static int k_mean;

	private static final Random random = new Random(815);

	private static List<SparseVertex> population;
	
	private static BufferedWriter writer;
	
	private static String output;

	public static void main(String[] args) {
		output = args[0];
		int N = Integer.parseInt(args[1]);
		k_mean = Integer.parseInt(args[2]);
		
		builder = new SparseGraphBuilder();
		
		for(int seeds = 10; seeds < 101; seeds+=10) {
			logger.info(String.format("Running sim for %1$s seeds...", seeds));
			logger.info("Creating graph...");
			
			graph = builder.createGraph();

			population = new ArrayList<SparseVertex>(N);
			for (int i = 0; i < N; i++) {
				population.add(builder.addVertex(graph));
			}
			logger.info("Expanding...");
			run(seeds);
		}
		logger.info("Done.");
	}

	private static void run(int n_seeds) {
		try {
			writer = new BufferedWriter(new FileWriter(String.format("%1$s/%2$s.stats.txt", output, n_seeds)));
			writer.write("it\tsettled\tpending\tconnections");
			writer.newLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
		Collection<SparseVertex> settled = new HashSet<SparseVertex>();
		int[][] connectionMatrix = new int[n_seeds][n_seeds];
		/*
		 * draw seeds
		 */
		Collection<SparseVertex> pending = new LinkedList<SparseVertex>();
		TObjectIntHashMap<SparseVertex> seedMapping = new TObjectIntHashMap<SparseVertex>();
		for (int i = 0; i < n_seeds; i++) {
			SparseVertex vertex = population.get(random.nextInt(population.size())); 
			pending.add(vertex);
			seedMapping.put(vertex, i);
		}
		/*
		 * iterate until no pending nodes left
		 */
		int iteration = 0;
		while (!pending.isEmpty()) {
			Collection<SparseVertex> newPending = new LinkedList<SparseVertex>();
			for (SparseVertex vertex : pending) {
				int idx1 = seedMapping.get(vertex);
				/*
				 * connect to k_mean randomly selected nodes - double edges and
				 * self-loops are ignored
				 */
				for (int i = 0; i < k_mean; i++) {
					SparseVertex neighbor = population.get(random.nextInt(population.size()));
					builder.addEdge(graph, vertex, neighbor);
					newPending.add(neighbor);
					
					if(seedMapping.containsKey(neighbor)) {
						int idx2 = seedMapping.get(neighbor);
						if(idx1 != idx2) {
							connectionMatrix[idx1][idx2]++;
							connectionMatrix[idx2][idx1]++;
						}
					} else {
						seedMapping.put(neighbor, idx1);
					}
				}
				settled.add(vertex);

				if(settled.size() % 10 == 0) {
					/*
					 * analyze graph
					 */
					boolean terminate = analyze(iteration, connectionMatrix, settled.size(), newPending.size());
					if(terminate) {
						logger.info("Terminating simulation - all seeds are connected.");
						return;
					}
				}
			}
			/*
			 * remove all settled nodes from the newPending collection.
			 * NOTE: There is no guarantee that we will connect all nodes!
			 */
			Collection<SparseVertex> remove = new ArrayList<SparseVertex>(
					newPending.size());
			for (SparseVertex vertex : newPending) {
				if (settled.contains(vertex))
					remove.add(vertex);
			}
			for (SparseVertex vertex : remove)
				newPending.remove(vertex);

			pending = newPending;
			logger.info(String.format("Iteration %1$s done - %2$s vertices processed.", iteration, settled.size()));
			iteration++;
			
			
		}
		
		try {
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static boolean analyze(int iteration, int[][] connectionMatrix, int n_settled, int n_pending) {
		try {
//		BufferedWriter matrixWriter = new BufferedWriter(new FileWriter(String.format("%1$s/%2$s.%3$s.connections.txt", output, connectionMatrix.length, iteration)));
		int connections = 0;
		for(int i = 0; i < connectionMatrix.length; i++) {
			for(int j = 0; j < connectionMatrix[i].length; j++) {
				if(connectionMatrix[i][j] > 0)
					connections++;
				
//				matrixWriter.write(String.valueOf(connectionMatrix[i][j]));
//				matrixWriter.write("\t");
			}
//			matrixWriter.newLine();
		}
//		matrixWriter.close();
		/*
		 * dump
		 */
		
			writer.write(String.valueOf(iteration));
			writer.write("\t");
			writer.write(String.valueOf(n_settled));
			writer.write("\t");
			writer.write(String.valueOf(n_pending));
			writer.write("\t");
			writer.write(String.valueOf(connections/2));
			writer.newLine();
			writer.flush();
		
			int n = connectionMatrix.length;
			if(connections*2 >= (n * (n - 1))) {
				return true;
			} else
				return false;
			
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}
}
