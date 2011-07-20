/* *********************************************************************** *
 * project: org.matsim.*
 * RandomGraphGenerator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.johannes.socialnetworks.graph.generators;

import gnu.trove.TObjectIntHashMap;
import gnu.trove.TObjectIntIterator;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.log4j.Logger;
import org.matsim.contrib.sna.graph.Edge;
import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.GraphBuilder;
import org.matsim.contrib.sna.graph.GraphUtils;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.util.ProgressLogger;

/**
 * @author illenberger
 *
 */
public class RandomGraphGenerator<G extends Graph, V extends Vertex, E extends Edge> {
	
	private final static Logger logger = Logger.getLogger(RandomGraphGenerator.class);

	private Random random;
	
	private UnivariateRealFunction degreeDistribution;
	
	private GraphBuilder<G, V, E> builder;
	
	private int invalidSequences;
	
	private int invalidGraphs;
	
	public RandomGraphGenerator(UnivariateRealFunction function, GraphBuilder<G, V, E> builder, long rndSeed) {
		this.degreeDistribution = function;
		this.builder = builder;
		this.random = new Random(rndSeed);
	}
	
	public int getInvalidSequences() {
		return invalidSequences;
	}
	
	public int getInvalidGraphs() {
		return invalidGraphs;
	}
	
	public G generate(int N, int maxDegree) {
		/*
		 * create empty graph
		 */
		G graph = builder.createGraph();
		for(int i = 0; i < N; i++) {
			builder.addVertex(graph);
		}
		/*
		 * generate and validate degree sequence
		 */
		logger.info("Generating degree sequence...");
		
		TObjectIntHashMap<Vertex> kMap = null;
		boolean valid = false;
		int M = 0;
		while(!valid) {
			kMap = generateDegreeSequence(graph, maxDegree);
			
			int sum = 0;
			TObjectIntIterator<Vertex> it = kMap.iterator();
			for(int i = 0; i < kMap.size(); i++) {
				it.advance();
				sum += it.value();
			}
			
			if(sum % 2 == 0) {
				valid = true;
				M = sum/2;
			} else {
				invalidSequences++;
			}
		}
		/*
		 * connect graph
		 */
		logger.info("Connecting vertices...");
		ProgressLogger.init(M, 1, 5);
		
		@SuppressWarnings("unchecked")
		List<V> pending = new LinkedList<V>((Collection<? extends V>) graph.getVertices());
		
		while(pending.size() > 1) {
			int idx_i = random.nextInt(pending.size());
			int idx_j = random.nextInt(pending.size());
			
			if(idx_i != idx_j) {
				V v_i = pending.get(idx_i);
				V v_j = pending.get(idx_j);
				
				int k_i_target = kMap.get(v_i);
				int k_j_target = kMap.get(v_j);
				int k_i = v_i.getNeighbours().size();
				int k_j = v_j.getNeighbours().size();
				
				if(k_i < k_i_target && k_j < k_j_target) {
					Edge edge = builder.addEdge(graph, v_i, v_j);
					
					if(edge != null) {
						ProgressLogger.step();
						
						if(k_i_target == k_i + 1) {
							pending.remove(idx_i);
							if(idx_j > idx_i)
								idx_j--;
							
						}
						
						if(k_j_target == k_j + 1) {
							pending.remove(idx_j);
						}
					} else {
						/*
						 * check if all pending vertices are already connected
						 */
						boolean connected = true;
						for(int i = 0; i < pending.size(); i++) {
							for(int j = i + 1; j < pending.size(); j++) {
								Vertex v1 = pending.get(i);
								Vertex v2 = pending.get(j);
								if(GraphUtils.findEdge(v1, v2) == null) {
									connected = false;
									break;
								}
							}
							
							if(!connected) {
								break;
							}
						}
						
						if(connected) {
							ProgressLogger.termiante();
							logger.warn(String.format("Degree distribution not exact. %1$s vertices still pending.", pending.size()));
							invalidGraphs++;
							return graph;
						}
					}
				}
			}
		}
		
		if(!pending.isEmpty()) {
			ProgressLogger.termiante();
			logger.warn(String.format("Degree distribution not exact. %1$s vertices still pending.", pending.size()));
			invalidGraphs++;
		}
		
		logger.info("Done.");
		return graph;
	}
	
	private TObjectIntHashMap<Vertex> generateDegreeSequence(Graph graph, int maxDegree) {
		TObjectIntHashMap<Vertex> kMap = new TObjectIntHashMap<Vertex>();
		for(Vertex vertex : graph.getVertices()) {
			boolean accept = false;
			
			while(!accept) {
				try {
					int k = random.nextInt(maxDegree + 1);
					double p = degreeDistribution.value(k);
					
					if(p >= random.nextDouble()) {
						kMap.put(vertex, k);
						accept = true;
					}
				} catch(FunctionEvaluationException e) {
					e.printStackTrace();
				}
			}
		}
		
		return kMap;
	}
}
