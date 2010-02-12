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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;

import org.apache.log4j.Logger;
import org.matsim.contrib.sna.graph.SparseGraph;
import org.matsim.contrib.sna.graph.SparseGraphBuilder;
import org.matsim.contrib.sna.graph.SparseVertex;

import playground.johannes.socialnetworks.graph.Partitions;

/**
 * @author illenberger
 * 
 */
public class ProposalSim {
	
	private static final Logger logger = Logger.getLogger(ProposalSim.class);

	private static SparseGraphBuilder builder;

	private static SparseGraph graph;

	private static final int k_mean = 4;

	private static final Random random = new Random(815);

	private static List<SparseVertex> population;
	
	private static BufferedWriter writer;
	
	private static String output;

	public static void main(String[] args) {
		output = args[0];
		int N = Integer.parseInt(args[1]);

		builder = new SparseGraphBuilder();
		
		for(int seeds = 10; seeds < 101; seeds+=10) {
			logger.info(String.format("Running sim for %1$s seeds...", seeds));
			logger.info("Creating graph...");
			
			graph = builder.createGraph();

			population = new ArrayList<SparseVertex>(N);
			for (int i = 0; i < N; i++) {
				population.add(builder.addVertex(graph));
			}
			run(seeds);
		}
		logger.info("Done.");
	}

	private static void run(int n_seeds) {
		try {
			writer = new BufferedWriter(new FileWriter(String.format("%1$s/%2$s.stats.txt", output, n_seeds)));
			writer.write("it\tsettled\tpending\tcomponents");
			writer.newLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
		Collection<SparseVertex> settled = new HashSet<SparseVertex>();
		/*
		 * draw seeds
		 */
		Collection<SparseVertex> pending = new LinkedList<SparseVertex>();
		for (int i = 0; i < n_seeds; i++)
			pending.add(population.get(random.nextInt(population.size())));
		/*
		 * iterate until no pending nodes left
		 */
		int iteration = 0;
		while (!pending.isEmpty()) {
			Collection<SparseVertex> newPending = new LinkedList<SparseVertex>();
			for (SparseVertex vertex : pending) {
				/*
				 * connect to k_mean randomly selected nodes - double edges and
				 * self-loops are ignored
				 */
				for (int i = 0; i < k_mean; i++) {
					SparseVertex neighbor = population.get(random
							.nextInt(population.size()));
					builder.addEdge(graph, vertex, neighbor);
					newPending.add(neighbor);
				}
				settled.add(vertex);

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
			/*
			 * analyze graph
			 */
			analyze(iteration, settled.size(), newPending.size());

			pending = newPending;
			logger.info(String.format("Iteration %1$s done.", iteration));
			iteration++;
		}
		
		try {
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void analyze(int iteration, int n_settled, int n_pending) {
		SortedSet<Set<SparseVertex>> components = Partitions.disconnectedComponents(graph);
		/*
		 * count all components with size > 1 - exclude the isolated nodes that
		 * have not been connected by the algo yet.
		 */
		int count = 0;
		for(Set<SparseVertex> component : components) {
			if(component.size() > 1)
				count++;
			else
				/*
				 * we can break here since it is a sorted set
				 */
				break;
		}
		/*
		 * dump
		 */
		try {
			writer.write(String.valueOf(iteration));
			writer.write("\t");
			writer.write(String.valueOf(n_settled));
			writer.write("\t");
			writer.write(String.valueOf(n_pending));
			writer.write("\t");
			writer.write(String.valueOf(count));
			writer.newLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
