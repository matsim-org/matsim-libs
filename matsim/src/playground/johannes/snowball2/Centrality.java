/* *********************************************************************** *
 * project: org.matsim.*
 * Centrality.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.johannes.snowball2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import playground.johannes.snowball.Histogram;

import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;

/**
 * @author illenberger
 *
 */
public class Centrality {

	protected CentralityGraphDecorator graphDecorator;
	
	protected CentralityGraph graph;
	
	protected Map<Vertex, Double> closenessValues;
	
	protected Map<Vertex, Double> betweennessValues;
	
	protected double graphCloseness;
	
	protected double graphBetweenness;
	
	private boolean didRun;
	
//	private int lastNumVertex;
	
	public void run(Graph g) {
		graphDecorator = new CentralityGraphDecorator(g);
		graph = (CentralityGraph) graphDecorator.getSparseGraph();
		Queue<CentralityVertex> vertices = new ConcurrentLinkedQueue<CentralityVertex>((Collection<? extends CentralityVertex>) graph.getVertices());
		
		int numThreads = Runtime.getRuntime().availableProcessors();
		List<DijkstraThread> threads = new ArrayList<DijkstraThread>(numThreads);
		
		DijkstraThread.count = 0;
		for(int i = 0; i < numThreads; i++) {
			threads.add(new DijkstraThread(graph, vertices));
		}
		
		for(DijkstraThread thread : threads) {
			thread.start();
		}
		
		for(DijkstraThread thread : threads) {
			try {
				thread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		calcCloseness();
		calcBetweenness();
		
		didRun = true;
	}
	
	public boolean didRun() {
		return didRun;
	}
	
	public void reset() {
		didRun = false;
	}
	
	protected void calcCloseness() {
		closenessValues = new LinkedHashMap<Vertex, Double>();
		graphCloseness = 0;
		for(SparseVertex v : graph.getVertices()) {
			closenessValues.put(graphDecorator.getVertex(v), ((CentralityVertex)v).getCloseness());
			graphCloseness+= ((CentralityVertex)v).getCloseness();
		}
		graphCloseness = graphCloseness/ (double)graph.getVertices().size();
	}
	
	protected void calcBetweenness() {
		betweennessValues = new LinkedHashMap<Vertex, Double>();
		graphBetweenness = 0;
//		double norm = (graph.getVertices().size() - 1) * (graph.getVertices().size() - 2) * 0.5; 
		for(SparseVertex v : graph.getVertices()) {
			double bc = ((CentralityVertex)v).getBetweenness();// / norm;
			betweennessValues.put(graphDecorator.getVertex(v), bc);
			graphBetweenness += bc;
		}
		
		graphBetweenness = graphBetweenness/(double)graph.getVertices().size();
	}
	
	public double getGraphCloseness() {
		return graphCloseness;
	}
	
	public double getGraphBetweenness() {
		return graphBetweenness;
	}
	
	public Histogram getClosenessHistogram() {
		Histogram histogram = new Histogram(100);
		for(Double d : closenessValues.values())
			histogram.add(d);
		return histogram;
	}
	
	public Histogram getClosenessHistogram(double min, double max) {
		Histogram histogram = new Histogram(100, min, max);
		for(Double d : closenessValues.values())
			histogram.add(d);
		return histogram;
	}
	
	public Histogram getBetweennessHistogram() {
		Histogram histogram = new Histogram(100);
		for(Double d : betweennessValues.values())
			histogram.add(d);
		return histogram;
	}
	
	public Histogram getBetweennessHistogram(double min, double max) {
		Histogram histogram = new Histogram(100, min, max);
		for(Double d : betweennessValues.values())
			histogram.add(d);
		return histogram;
	}
	
	private static class DijkstraThread extends Thread {
		
		private static int count = 0;
		
		private Queue<CentralityVertex> vertices;
		
		private UnweightedDijkstra dijkstra;
		
		private CentralityGraph g;
		
		public DijkstraThread(CentralityGraph g, Queue<CentralityVertex> vertices) {
			this.vertices = vertices;
			this.g = g;
			dijkstra = new UnweightedDijkstra(g);
		}
		
		public void run() {
			CentralityVertex v;

			while((v = vertices.poll()) != null) {

				dijkstra.run(v);

//				int vCount = 0;
//				int pCount = 0;
//				for(Collection<List<CentralityVertex>> pathsPerTarget : paths) {
//					int size = pathsPerTarget.size();
//					pCount += size;
//					double betweenness = 1 / (double)size;
//					for(List<CentralityVertex> path : pathsPerTarget) {
//						vCount += path.size();
//						for(CentralityVertex sv : path)
//							sv.addBetweenness(betweenness);
//					}
//						
//				}
//				v.setCloseness(vCount/(double)pCount);
				
//				for(SparseVertex vertex : g.getVertices())
//					((CentralityVertex)vertex).setCloseness(v.getNumPahtFrom())
					

				count++;
				if(count % 100 == 0) {
					int total = g.getVertices().size();
//					int pending = total - count;
					System.out.println(String.format("Processed %1$s of %2$s vertices. (%3$s)", count, total, count/(float)total *100));
				}
			}

		}
	}
	
	protected static class CentralityGraphDecorator extends SparseGraphDecorator {

		public CentralityGraphDecorator(Graph g) {
			super(g);
		}

		@Override
		protected SparseGraph newGraph(int numVertex, int numEdges) {
			return new CentralityGraph(numVertex, numEdges);
		}

		
	}
	
	protected static class CentralityGraph extends SparseGraph {

		public CentralityGraph(int numVertex, int numEdge) {
			super(numVertex, numEdge);
		}

		@Override
		protected CentralityVertex newVertex() {
			return new CentralityVertex();
		}
		
	}
	
	protected static class CentralityVertex extends SparseVertex {
		
		private double closeness;
		
		private double betweenness;
		
		public double getCloseness() {
			return closeness;
		}
		
		public void setCloseness(double value) {
			closeness = value;
		}
		
		public double getBetweenness() {
			return betweenness;
		}
		
		public synchronized void addBetweenness(double value) {
			betweenness += value;
		}
		
		public void setBetweenness(double value) {
			betweenness = value;
		}
		
	}
}
