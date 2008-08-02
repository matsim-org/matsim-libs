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

import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.utils.io.IOUtils;

import playground.johannes.snowball.Histogram;
import edu.uci.ics.jung.graph.Graph;
import gnu.trove.TDoubleArrayList;
import gnu.trove.TIntDoubleHashMap;
import gnu.trove.TIntIntHashMap;

/**
 * @author illenberger
 * 
 */
public class Centrality {

	protected CentralityGraphDecorator graphDecorator;

	protected CentralityGraph graph;

	protected DescriptiveStatistics closenessValues;

	protected DescriptiveStatistics betweennessValues;
	
	protected TDoubleArrayList betweennessWValues;
	
	protected TDoubleArrayList betweennessWeights;
	
	private double betweennessWeighted;
	
	private double closenessWeighted;

	private int lastIteration = Integer.MIN_VALUE;

	private boolean isSampled;

	public void run(Graph g, int iteration) {
		if (iteration != lastIteration) {
			isSampled = false;
			if (g instanceof SampledGraph)
				isSampled = true;

			graphDecorator = new CentralityGraphDecorator(g);
			graph = (CentralityGraph) graphDecorator.getSparseGraph();
			Queue<CentralityVertex> vertices = new ConcurrentLinkedQueue<CentralityVertex>();
			for(SparseVertex v : graph.getVertices())
				vertices.add((CentralityVertex) v);

			int numThreads = Runtime.getRuntime().availableProcessors();
			List<DijkstraThread> threads = new ArrayList<DijkstraThread>(
					numThreads);

			DijkstraThread.count = 0;
			for (int i = 0; i < numThreads; i++) {
				threads.add(new DijkstraThread(graph, vertices));
			}

			for (DijkstraThread thread : threads) {
				thread.start();
			}

			for (DijkstraThread thread : threads) {
				try {
					thread.join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			calcCloseness();
			calcBetweenness();

			lastIteration = iteration;
		}
	}

	protected void calcCloseness() {
		double sum = 0;
		double wsum = 0;
		closenessValues = new DescriptiveStatistics();
		for (SparseVertex v : graph.getVertices()) {
			if (isSampled) {
				if (!((SampledVertex) graphDecorator.getVertex(v))
						.isAnonymous())
					closenessValues.addValue(((CentralityVertex) v)
							.getCloseness());
				double p = ((SampledVertex) graphDecorator.getVertex(v)).getSampleProbability();
				sum += ((CentralityVertex) v).getCloseness() / p;
				wsum += 1/p;
			} else {
				closenessValues.addValue(((CentralityVertex) v).getCloseness());
				sum += ((CentralityVertex) v).getCloseness();
				wsum++;
			}
		}
		
		closenessWeighted = sum/wsum;
	}

	protected void calcBetweenness() {
		betweennessWeighted = 0;
		double wsum = 0;
		betweennessValues = new DescriptiveStatistics();
		betweennessWValues = new TDoubleArrayList(graph.getVertices().size());
		betweennessWeights = new TDoubleArrayList(graph.getVertices().size());
		for (SparseVertex v : graph.getVertices()) {
			if (isSampled) {
				if (!((SampledVertex) graphDecorator.getVertex(v))
						.isAnonymous()) {
					betweennessValues.addValue(((CentralityVertex) v)
							.getBetweenness());
					double p = ((SampledVertex) graphDecorator.getVertex(v)).getSampleProbability();
					betweennessWeighted += ((CentralityVertex) v).getBetweenness() / p;
					wsum += 1/p;
					
					betweennessWValues.add(((CentralityVertex) v).getBetweenness() / p);
					betweennessWeights.add(1/p);
				}
			} else {
				betweennessValues.addValue(((CentralityVertex) v)
						.getBetweenness());
				betweennessWeighted += ((CentralityVertex) v).getBetweenness();
				wsum ++;
				
				betweennessWValues.add(((CentralityVertex) v).getBetweenness());
				betweennessWeights.add(1.0);
			}
		}
		
		betweennessWeighted = betweennessWeighted/wsum;
	}
	
	public double getBetweennessWeighted() {
		return betweennessWeighted;
	}
	
	public double getClosenessWeighted() {
		return closenessWeighted;
	}

	public double getGraphCloseness() {
		return closenessValues.getMean();
	}

	public double getGraphBetweenness() {
		return betweennessValues.getMean();
	}

	public Histogram getClosenessHistogram() {
		Histogram histogram = new Histogram(100);
		histogram.addAll(closenessValues.getValues());
		return histogram;
	}

	public Histogram getClosenessHistogram(double min, double max) {
		Histogram histogram = new Histogram(100, min, max);
		histogram.addAll(closenessValues.getValues());
		return histogram;
	}

	public Histogram getBetweennessHistogram() {
		Histogram histogram = new Histogram(0.01, 0, 100);
		histogram.addAll(betweennessValues.getValues());
		return histogram;
	}

	public Histogram getBetweennessHistogram(double min, double max) {
		Histogram histogram = new Histogram(0.01, 0, 100);
		histogram.addAll(betweennessValues.getValues());
		return histogram;
	}

	public void dumpDegreeCorrelation(String filename, String type) {
		TIntDoubleHashMap values = new TIntDoubleHashMap();
		TIntIntHashMap degrees = new TIntIntHashMap();
		
		for (SparseVertex v : graph.getVertices()) {
			int k = v.getEdges().length;
			double c = 0;
			if(type.equals("betweenness"))
				c = ((CentralityVertex) v).getBetweenness();
			else if(type.equals("closeness"))
				c = ((CentralityVertex) v).getCloseness();
			
			double val = values.get(k);
			values.put(k, val + c);
			
			int freqDegree = degrees.get(k);
			degrees.put(k, freqDegree + 1);
		}
		
		try {
			BufferedWriter writer = IOUtils.getBufferedWriter(filename);
			int[] keys = values.keys();
			Arrays.sort(keys);
			for (int k : keys) {
				double bc = values.get(k);
				int numV = degrees.get(k);

				writer.write(String.valueOf(k));
				writer.write("\t");
				writer.write(String.valueOf(bc / (double) numV));
				writer.newLine();
			}
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static class DijkstraThread extends Thread {

		private static int count = 0;

		private Queue<CentralityVertex> vertices;

		private UnweightedDijkstra dijkstra;

		private CentralityGraph g;

		public DijkstraThread(CentralityGraph g,
				Queue<CentralityVertex> vertices) {
			this.vertices = vertices;
			this.g = g;
			dijkstra = new UnweightedDijkstra(g);
		}

		public void run() {
			CentralityVertex v;

			while ((v = vertices.poll()) != null) {
				dijkstra.run(v);
				count++;
				if (count % 1000 == 0) {
					int total = g.getVertices().size();
					System.out.println(String.format(
							"Processed %1$s of %2$s vertices. (%3$s)", count,
							total, count / (float) total * 100));
					System.out.println(String.format(
							"Path ratio is %1$s", dijkstra.ratioSum/(double)count));
				}
			}

		}
	}

	public static class CentralityGraphDecorator extends SparseGraphDecorator {

		public CentralityGraphDecorator(Graph g) {
			super(g);
		}

		@Override
		protected SparseGraph newGraph(int numVertex, int numEdges) {
			return new CentralityGraph(numVertex, numEdges);
		}

	}

	public static class CentralityGraph extends SparseGraph {

		public CentralityGraph(int numVertex, int numEdge) {
			super(numVertex, numEdge);
		}

		@Override
		protected CentralityVertex newVertex() {
			return new CentralityVertex();
		}

	}

	public static class CentralityVertex extends SparseVertex {

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
