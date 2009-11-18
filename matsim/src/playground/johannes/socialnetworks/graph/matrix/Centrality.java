/* *********************************************************************** *
 * project: org.matsim.*
 * Centrality.java
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
package playground.johannes.socialnetworks.graph.matrix;

import gnu.trove.TIntArrayList;
import gnu.trove.TIntIntHashMap;
import gnu.trove.TIntIntIterator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.commons.math.stat.StatUtils;
import org.apache.log4j.Logger;

import playground.johannes.socialnetworks.graph.mcmc.AdjacencyMatrix;

/**
 * @author illenberger
 *
 */
public class Centrality {
	
	private double[] vertexCloseness;
	
	private int[] vertexBetweenness;
	
	private TIntIntHashMap[] edgeBetweenness;
	
	private double meanVertexCloseness;
	
	private double meanVertexBetweenness;
	
	private double meanEdgeBetweenness;
	
	private final int numThreads = Runtime.getRuntime().availableProcessors();
	
	public double[] getVertexCloseness() {
		return vertexCloseness;
	}

	public int[] getVertexBetweenness() {
		return vertexBetweenness;
	}

	public TIntIntHashMap[] getEdgeBetweenness() {
		return edgeBetweenness;
	}

	public double getMeanVertexCloseness() {
		return meanVertexCloseness;
	}

	public double getMeanVertexBetweenness() {
		return meanVertexBetweenness;
	}
	
	public double getMeanEdgeBetweenness() {
		return meanEdgeBetweenness;
	}

	public void run(AdjacencyMatrix y) {
		int n = y.getVertexCount();
		vertexCloseness = new double[n];
		Arrays.fill(vertexCloseness, Double.POSITIVE_INFINITY);
		vertexBetweenness = new int[n];
		edgeBetweenness = new TIntIntHashMap[n];
		/*
		 * put all vertex indices in a queue
		 */
//		Queue<Integer> vertices = new ConcurrentLinkedQueue<Integer>();
//		for(int i = 0; i < y.getVertexCount(); i++)
//			vertices.add(i);
		/*
		 * create threads
		 */
		List<CentralityThread> threads = new ArrayList<CentralityThread>();
		int size = (int) Math.floor(n/(double)numThreads);
		int i_start = 0;
		int i_stop = size;
		for(int i = 0; i < numThreads-1; i++) {
//			threads.add(new CentralityThread(y, vertices));
			threads.add(new CentralityThread(y, i_start, i_stop));
			i_start = i_stop;
			i_stop += size;
		}
		threads.add(new CentralityThread(y, i_start, n));
		/*
		 * start threads
		 */
		for (CentralityThread thread : threads) {
			thread.start();
		}
		/*
		 * wait for threads
		 */
		for (CentralityThread thread : threads) {
			try {
				thread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		/*
		 * merge results of threads
		 */
		for(CentralityThread thread : threads) {
			for(int i = 0; i < n; i++) {
//				if(!Double.isInfinite(thread.vertexCloseness[i]))
//					vertexCloseness[i] = thread.vertexCloseness[i];
				/*
				 * if this thread did not calculate the closeness of i it returns infinity
				 */
				vertexCloseness[i] = Math.min(vertexCloseness[i], thread.vertexCloseness[i]);
				/*
				 * merge vertex betweenness values
				 */
				vertexBetweenness[i] += thread.vertexBetweenness[i];
				/*
				 * merge edge betweenness values
				 */
				if (thread.edgeBetweenness[i] != null) {
					TIntIntIterator it = thread.edgeBetweenness[i].iterator();
					for (int j = 0; j < thread.edgeBetweenness[i].size(); j++) {
						it.advance();
						/*
						 * since we have an undirected graph, merge betweenness for both edges
						 */
						if (edgeBetweenness[i] == null)
							edgeBetweenness[i] = new TIntIntHashMap();
						edgeBetweenness[i].adjustOrPutValue(it.key(), it.value(), it.value());

						if (edgeBetweenness[it.key()] == null)
							edgeBetweenness[it.key()] = new TIntIntHashMap();
						edgeBetweenness[it.key()].adjustOrPutValue(i, it.value(), it.value());
					}
				}
			}
		}
		/*
		 * calculate mean values
		 */
		meanVertexCloseness = StatUtils.mean(vertexCloseness);
		int sum = 0;
		for(int i = 0; i< y.getVertexCount(); i++)
			sum += vertexBetweenness[i];
		meanVertexBetweenness = sum/(double)y.getVertexCount();
		
		sum = 0;
		int count = 0; 
		for(int i = 0; i < n; i++) {
			if(edgeBetweenness[i] != null) {
				TIntIntIterator it = edgeBetweenness[i].iterator();
				for(int k = 0; k < edgeBetweenness[i].size(); k++) {
					it.advance();
					sum += it.value();
					count++;
				}
			}
		}
		meanEdgeBetweenness = sum/(double)count;
	}

	private static class CentralityThread extends Thread {
		
		private Dijkstra dijkstra;
		
//		private Queue<Integer> vertices;
		private int i_start;
		
		private int i_stop;
		
		private TIntIntHashMap[] edgeBetweenness;
		
		private int vertexBetweenness[];
		
		private double vertexCloseness[];
		
		private int n;
		
		private static int counter = 0;
		
		private final Logger logger = Logger.getLogger(CentralityThread.class);
		
		public CentralityThread(AdjacencyMatrix y, int i_start, int i_stop) {
			dijkstra = new Dijkstra(y);
			this.i_start = i_start;
			this.i_stop = i_stop;
//			this.vertices = vertices;
			n = y.getVertexCount();
			counter = 0;
		}
		
		@Override
		public void run() {
			PathAnalyzer pathAnalyzer = new PathAnalyzer();
			PathExtractor pathExtractor = new PathExtractor();
			/*
			 * initialize the closeness array with infinity
			 */
			vertexCloseness = new double[n];
			Arrays.fill(vertexCloseness, Double.POSITIVE_INFINITY);
			/*
			 * initialize the betweenness arrays with zero
			 */
			vertexBetweenness = new int[n];
			edgeBetweenness = new TIntIntHashMap[n];
			/*
			 * time measuring
			 */
			long dkTime = 0;
			long cTime = 0;
			
//			Integer i_obj;
//			while((i_obj = vertices.poll()) != null) {
			for(int i = i_start; i < i_stop; i++) {
				
//				int i = i_obj.intValue();
				/*
				 * run the Dijkstra to all nodes
				 */
				long time = System.currentTimeMillis();
				TIntArrayList reachable = dijkstra.run(i, -1);

				dkTime += System.currentTimeMillis() - time;
				/*
				 * extract the paths
				 */
				time = System.currentTimeMillis();
				int pathLengthSum = 0;
				/*
				 * iterate over all reachable nodes
				 */
				for(int k = 0; k < reachable.size(); k++) {
					int j = reachable.get(k);
					/*
					 * determine the length and number of paths
					 */
					pathAnalyzer.run(dijkstra.getSpanningTree(), j);
					int pathLength = pathAnalyzer.pathLength;
					int pathCount = pathAnalyzer.pathCount;
					pathLengthSum += pathLength;
					/*
					 * extract all paths
					 */
					int[][] matrix = new int[pathLength][pathCount];
					pathExtractor.run(dijkstra.getSpanningTree(), matrix, j);
					/*
					 * increase betweenness values for each passed vertex and edge
					 */
					for(int col = 0; col < pathCount; col++) {
						int prevVertex = i;
						/*
						 * reverse the order in case we have some day directed graphs...
						 */
						for(int row = pathLength - 1; row > -1; row--) {
							int vertex = matrix[row][col];
							vertexBetweenness[vertex]++;
							
							if(edgeBetweenness[prevVertex] == null) {
								edgeBetweenness[prevVertex] = new TIntIntHashMap();
							}
							edgeBetweenness[prevVertex].adjustOrPutValue(vertex, 1, 1);
							
							prevVertex = vertex;
						}
						/*
						 * decrease betweenness of target node
						 */
						vertexBetweenness[j]--;
					}
				}
				
//				if(reachable.size() == 0)
//					vertexCloseness[i] = 0;
//				else
//					vertexCloseness[i] = pathLengthSum/(double)reachable.size();
				if(reachable.size() > 0)
					vertexCloseness[i] = pathLengthSum/(double)reachable.size();
				
				cTime += System.currentTimeMillis() - time;
				counter++;
				if(counter % 1000 == 0) {
					logger.info(String.format("Procesed %1$s vertices, dijkstra took %2$s ms, misc took %3$s ms.", counter, dkTime, cTime));
					dkTime = 0;
					cTime = 0;
				}
			}
		}
	}
	
	private static class PathAnalyzer {
		
		private TIntArrayList[] spanningTree;
		
		private int pathLength;
		
		private int pathCount;
		
		public void run(TIntArrayList[] spanningTree, int i) {
			this.spanningTree = spanningTree;
			pathLength = 0;
			pathCount = 1;
			step(i, 1);
		}
		
		private void step(int i, int depth) {
			if(spanningTree[i] != null && spanningTree[i].size() > 0) {
				pathLength = Math.max(depth, pathLength);
				
				if(spanningTree[i].size() > 1)
					pathCount += spanningTree[i].size() - 1;
				
				for(int k = 0; k < spanningTree[i].size(); k++) {
					step(spanningTree[i].get(k), depth + 1);
				}
			}
		}
	}
	
	private static class PathExtractor {
		
		private TIntArrayList[] spanningTree;
		
		private int[][] matrix;
		
		private int column;
		
		public void run(TIntArrayList[] spanningTree, int[][] matrix, int i) {
			this.spanningTree = spanningTree;
			this.matrix = matrix;
			column = 0;
			step(i, 0);
		}
		
		private void step(int i, int row) {
			if(spanningTree[i] != null && spanningTree[i].size() > 0) {
				matrix[row][column] = i;
				
				step(spanningTree[i].get(0), row + 1);
				
				if(spanningTree[i].size() > 1) {
					for(int k = 1; k < spanningTree[i].size(); k++) {
						/*
						 * copy the upper vertex indices from left to right
						 */
						for(int l = 0; l < row+1; l++) {
							matrix[l][column + 1] = matrix[l][column]; 
						}
						column++;
						step(spanningTree[i].get(k), row+1);
					}
				}
			}
		}
	}
}
