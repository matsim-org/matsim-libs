/* *********************************************************************** *
 * project: org.matsim.*
 * GraphStatistics.java
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
package playground.johannes.socialnetworks.graph;


import gnu.trove.TDoubleDoubleHashMap;
import gnu.trove.TIntIntHashMap;
import gnu.trove.TIntIntIterator;
import gnu.trove.TObjectDoubleHashMap;
import gnu.trove.TObjectDoubleIterator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.log4j.Logger;
import org.matsim.contrib.sna.graph.Edge;
import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.Vertex;

import playground.johannes.socialnetworks.graph.matrix.Centrality;
import playground.johannes.socialnetworks.graph.mcmc.AdjacencyMatrixDecorator;
import playground.johannes.socialnetworks.statistics.Correlations;
import playground.johannes.socialnetworks.statistics.Distribution;

/**
 * A collection of function to calculate statistical properties of graphs.
 * 
 * @author illenberger
 * 
 */
public class GraphStatistics {

	private static final Logger logger = Logger.getLogger(GraphStatistics.class);
	
	private static int maxNumThreads = Runtime.getRuntime().availableProcessors();

	/**
	 * Sets the number of allowed threads for calculation. Set <tt>num > 1</tt>
	 * if you have multiple cpus available.
	 * 
	 * @param num
	 *            the number of allowed threads.
	 */
	public static void setMaxAllowedThreads(int num) {
		maxNumThreads = num;
	}
	
	/**
	 * Retrieves the degree distribution of graph <tt>g</tt>. The graph is
	 * treated as undirected.
	 * 
	 * @param g
	 *            the graph to retrieve the degree distribution from.
	 * @return the degree distribution.
	 */
	public static Distribution degreeDistribution(Graph g) {
		return degreeDistribution(g.getVertices());
	}
	
	/**
	 * Retrieves the degree distribution of vertices in <tt>vertices</tt>. The
	 * graph is treated as undirected.
	 * 
	 * @param vertices
	 *            a collection of vertices to retrieve the degree distribution
	 *            from.
	 * @return the degree distribution.
	 */
	public static Distribution degreeDistribution(Collection<? extends Vertex> vertices) {
		Distribution stats = new Distribution();
		for(Vertex v : vertices)
			stats.add(v.getEdges().size());
		return stats;
	}
	
	/**
	 * Calculates the local clustering coefficient of each vertex in graph
	 * <tt>g</tt>.
	 * 
	 * @see {@link #localClusteringCoefficients(Collection)}
	 * @param g
	 *            the graph the local clustering coefficients are to be
	 *            calculated.
	 * @return an object-double map containing the vertex as key and the
	 *         clustering coefficient as value.
	 */
	public static TObjectDoubleHashMap<? extends Vertex> localClusteringCoefficients(Graph g) {
		return localClusteringCoefficients(g.getVertices());
	}
	
	/**
	 * Calculates the local clustering coefficient of each vertex in
	 * <tt>vertices</tt>. The local clustering coefficient is defined as:
	 * <ul>
	 * <li>C = 0 if k = 0 or k = 1,
	 * <li>C = 2y/k*(k-1) if k > 1, where y is the number of edges connecting
	 * neighbors of the vertex.
	 * </ul>
	 * 
	 * @param vertices
	 *            a collection of vertices the local clustering coefficients are
	 *            to be calculated.
	 * @return an object-double map containing the vertex as key and the
	 *         clustering coefficient as value.
	 */
	public static <V extends Vertex> TObjectDoubleHashMap<V> localClusteringCoefficients(Collection<V> vertices) {
		TObjectDoubleHashMap<V> cc = new TObjectDoubleHashMap<V>();
		for(V v : vertices) {
			int k = v.getEdges().size();
			if(k == 0 || k == 1) {
				cc.put(v, 0.0);
			} else {
				int edgecount = 0;
				Set<Vertex> n1s = new HashSet<Vertex>(v.getNeighbours());
				int n_Neighbours1 = v.getNeighbours().size();
				for(int i1 = 0; i1 < n_Neighbours1; i1++) {
					Vertex n1 = v.getNeighbours().get(i1);
					int n_Neighbours2 = n1.getNeighbours().size();
					for(int i2 = 0; i2 < n_Neighbours2; i2++) {
						Vertex n2 = n1.getNeighbours().get(i2);
						if (n2 != v) {
							if (n1s.contains(n2))
								edgecount++;
						}
					}
					n1s.remove(n1);
				}
				cc.put(v, 2 * edgecount / (double)(k*(k-1)));
			}
		}
		
		return cc;
	}
	
	/**
	 * Retrieves the distribution of local clustering coefficients.
	 * 
	 * @param g
	 *            the graph of which the local clustering distribution is
	 *            obtained.
	 * @return the distribution of local clustering coefficients.
	 */
	public static Distribution localClusteringDistribution(Graph g) {
		return localClusteringDistribution(g.getVertices());
	}
	
	/**
	 * Retrieves the distribution of local clustering coefficients.
	 * 
	 * @param vertices
	 *            a collection of vertices of which the local clustering distribution is
	 *            obtained.
	 * @return the distribution of local clustering coefficients.
	 */
	public static Distribution localClusteringDistribution(Collection<? extends Vertex> vertices) {
		Distribution stats = new Distribution();
		stats.addAll(localClusteringCoefficients(vertices).getValues());
		return stats;
	}
	
	/**
	 * Calculates the global clustering coefficient, which is defined as the
	 * three times the number of triangles over the number of connected
	 * tripples.
	 * 
	 * @param g
	 *            the graph the global clustering coefficient is calculated for.
	 * @return the global clustering coefficient.
	 */
	public static double globalClusteringCoefficient(Graph g) {
		int n_tripples = 0;
		int n_triangles = 0;
		for(Vertex v : g.getVertices()) {
			List<? extends Vertex> n1s = v.getNeighbours();
			for(int i = 0; i < n1s.size(); i++) {
				List<? extends Vertex> n2s = n1s.get(i).getNeighbours();
				for(int k = 0; k < n2s.size(); k++) {
					if(!n2s.get(k).equals(v)) {
					n_tripples++;
					if(n2s.get(k).getNeighbours().contains(v))
						n_triangles++;
					}
				}
			}
		}
		
		return n_triangles / (double)n_tripples;
	}
	
	/**
	 * Counts the number of 2-stars.
	 * 
	 * @param g
	 *            the graph the number of 2-stars is counted for.
	 * @return the number of 2-stars.
	 */
	public static int twoStars(Graph g) {
		int n_tripples = 0;
		for(Vertex v : g.getVertices()) {
			List<? extends Vertex> n1s = v.getNeighbours();
			for(int i = 0; i < n1s.size(); i++) {
				n_tripples += n1s.get(i).getNeighbours().size();
			}
		}
		
		return n_tripples/2;
	}
	
	/**
	 * Calculates the mutuality of graph <tt>g</tt>, where mutuality is defined
	 * as<br>
	 * M = number of neighbors two steps away / number of path of length two to
	 * those vertices.
	 * 
	 * @param g
	 *            the graph the mutuality is to be calculated.
	 * @return the mutuality of graph <tt>g</tt>.
	 */
	public static double mutuality(Graph g) {
		int len2Paths = 0;
		int nVertex2Steps = 0;

		for (Vertex v : g.getVertices()) {
			List<? extends Vertex> n1List = v.getNeighbours();
			Set<Vertex> n2Set = new HashSet<Vertex>();
			int n_Neighbours1 = n1List.size();
			for(int i1 = 0; i1 < n_Neighbours1; i1++) {
				Vertex n1 = n1List.get(i1);
				int n_Neighbours2 = n1.getNeighbours().size();
				for(int i2 = 0; i2 < n_Neighbours2; i2++) {
					Vertex n2 = n1.getNeighbours().get(i2);
					if (n2 != v && !n1List.contains(n2)) {
						n2Set.add(n2);
						len2Paths++;
					}
				}
			}
			nVertex2Steps += n2Set.size();
		}

		return nVertex2Steps / (double) len2Paths;
	}
	
	/**
	 * Calculates the degree correlation of graph <tt>g</tt>.<br>
	 * See: M.�E.�J. Newman. Assortative mixing in networks. Physical Review
	 * Letters, 89(20), 2002.
	 * 
	 * @param g
	 *            the graph the degree correlation is to be calculated.
	 * @return the degree correlation.
	 */
	public static double degreeDegreeCorrelation(Graph g) {
		double product = 0;
		double sum = 0;
		double squareSum = 0;

		for (Edge e : g.getEdges()) {
			Vertex v1 = e.getVertices().getFirst();
			Vertex v2 = e.getVertices().getSecond();
			int d_v1 = v1.getEdges().size();
			int d_v2 = v2.getEdges().size();

			sum += 0.5 * (d_v1 + d_v2);
			squareSum += 0.5 * (Math.pow(d_v1, 2) + Math.pow(d_v2, 2));
			product += d_v1 * d_v2;			
		}
		
		double norm = 1 / (double)g.getEdges().size();
		return ((norm * product) - Math.pow(norm * sum, 2)) / ((norm * squareSum) - Math.pow(norm * sum, 2));
	}
	
	/**
	 * Calculates closeness centrality, betweenness centrality, diameter and
	 * radius of graph <tt>g</tt>.
	 * 
	 * @param g
	 *            the graph the centrality statistics are to be calculated.
	 * @return a GraphDistance object storing information about closeness
	 *         centrality, betweenness centrality, diameter and radius of graph
	 *         <tt>g</tt>. If a graph has disconnected components the diameter
	 *         will be the greatest diameter found in all components and radius
	 *         will equal zero.
	 */
	public static GraphDistance centrality(Graph g) {
		logger.info("Initializing graph for centrality calculation...");
		CentralityGraphBuilder<Vertex, Edge> builder = new CentralityGraphBuilder<Vertex, Edge>();
		CentralityGraph<Vertex, Edge> cGraph = builder.decorateGraph(g);
//		CentralityGraph<Vertex, Edge> cGraph = new CentralityGraph<Vertex, Edge>(g);
		Queue<CentralityVertex<Vertex>> vertexQueue = new ConcurrentLinkedQueue<CentralityVertex<Vertex>>();
		vertexQueue.addAll(cGraph.getVertices());
		
		List<CentralityThread> threads = new ArrayList<CentralityThread>(maxNumThreads);
		CentralityThread.numVProcessed = 0;
		
		for (int i = 0; i < maxNumThreads; i++) {
			logger.info(String.format("Initializing thread %1$s for centrality calculation...", i));
			threads.add(new CentralityThread(cGraph, vertexQueue));
		}

		logger.info("Starting centrality calculation...");
		for (CentralityThread thread : threads) {
			thread.start();
		}

		int diameter = 0;
		int radius = Integer.MAX_VALUE;
		for (CentralityThread thread : threads) {
			try {
				thread.join();
				diameter = Math.max(diameter, thread.diameter);
				radius = Math.min(radius, thread.radius);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		
		double meanBc = 0;
		double meanBcNorm = 0;
		int size = cGraph.getVertices().size();
		double norm = (size - 1) * (size - 2);
		TObjectDoubleHashMap<Vertex> bcMap = new TObjectDoubleHashMap<Vertex>();
		TObjectDoubleHashMap<Vertex> bcMapNorm = new TObjectDoubleHashMap<Vertex>();
		for(CentralityVertex<Vertex> v : cGraph.getVertices()) {
			bcMap.put(v.getDelegate(), v.getBetweenness());
			bcMapNorm.put(v.getDelegate(), v.getBetweenness()/norm);
			meanBc += v.getBetweenness();
			meanBcNorm += v.getBetweenness()/norm;
		}
		meanBc /= cGraph.getVertices().size();
		meanBcNorm /= cGraph.getVertices().size();
		
		double meanCc = 0;
		TObjectDoubleHashMap<Vertex> ccMap = new TObjectDoubleHashMap<Vertex>();
		for(CentralityVertex<Vertex> v : cGraph.getVertices()) {
			ccMap.put(v.getDelegate(), v.getCloseness());
			meanCc += v.getCloseness();
		}
		meanCc /= cGraph.getVertices().size();
		
		TObjectDoubleHashMap<Edge> bcEdgeMap = new TObjectDoubleHashMap<Edge>();
		for(CentralityEdge<Vertex, Edge> e : cGraph.getEdges()) {
			bcEdgeMap.put(e.getDelegate(), e.getBetweenness());
		}
		
		GraphDistance d = new GraphDistance();
		d.diatmeter = diameter;
		d.radius = radius;
		d.vertexCloseness = ccMap;
		d.vertexBetweenness = bcMap;
		d.vertexBetweennessNormalized = bcMapNorm;
		d.graphCloseness = meanCc;
		d.graphBetweenness = meanBc;
		d.graphBetweennessNormalized = meanBcNorm;
		
		d.edgeBetweeness = bcEdgeMap;
		//************************************
//		d.edgeBetweeness = new TObjectDoubleHashMap<Edge>();
//		for(CentralityThread thread : threads) {
//			TObjectDoubleIterator<EdgeDecorator<?>> it = thread.edgeCentrality.iterator();
//			for(int i = 0; i < thread.edgeCentrality.size(); i++) {
//				it.advance();
//				d.edgeBetweeness.adjustOrPutValue(it.key().getDelegate(), it.value(), it.value());
//			}
//		}
		//************************************
		
		return d;
	}
	
	public static TDoubleDoubleHashMap clusteringDegreeCorrelation(Graph g) {
		return degreeCorrelation(localClusteringCoefficients(g));
	}
	
	public static TDoubleDoubleHashMap degreeCorrelation(TObjectDoubleHashMap<? extends Vertex> values) {
		double[] values1 = new double[values.size()];
		double[] values2 = new double[values.size()];
		
		TObjectDoubleIterator<? extends Vertex> it = values.iterator();
		for(int i = 0; i < values1.length; i++) {
			it.advance();
			values1[i] = it.key().getNeighbours().size();
			values2[i] = it.value();
		}
		
		return Correlations.correlationMean(values1, values2);
	}
	
	private static class CentralityGraph<V extends Vertex, E extends Edge> extends GraphProjection<Graph, V, E> {

		public CentralityGraph(Graph delegate) {
			super(delegate);
//			decorate();
		}

//		@Override
//		public VertexDecorator<V> addVertex(V delegate) {
//			VertexDecorator<V> v = new CentralityVertex<V>(delegate);
//			return addVertex(v);
//		}

		@SuppressWarnings("unchecked")
		@Override
		public Set<? extends CentralityVertex<V>> getVertices() {
			return (Set<? extends CentralityVertex<V>>) super.getVertices();
		}

//		@Override
//		public EdgeDecorator<E> addEdge(VertexDecorator<V> v1,
//				VertexDecorator<V> v2, E delegate) {
//			EdgeDecorator<E> e = new CentralityEdge<V, E>((CentralityVertex<V>)v1, (CentralityVertex<V>)v2, delegate);
//			return addEdge(v1, v2, e);
//		}

		@Override
		public Set<? extends CentralityEdge<V, E>> getEdges() {
			return (Set<? extends CentralityEdge<V, E>>) super.getEdges();
		}
		
		
	}
	
	private static class CentralityVertex<V extends Vertex> extends VertexDecorator<V> {

		private double closeness;

		private double betweenness;
		
		protected CentralityVertex(V delegate) {
			super(delegate);
		}

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
	
	private static class CentralityEdge<V extends Vertex, E extends Edge> extends EdgeDecorator<E> {
		
		private double betweenness;
		
		/**
		 * @deprecated
		 * 
		 */
		@Deprecated
		protected CentralityEdge(CentralityVertex<V> v1, CentralityVertex<V> v2, E delegate) {
			super(v1, v2,delegate);
		}
		
		protected CentralityEdge(E delegate) {
			super(delegate);
		}
		
		public double getBetweenness() {
			return betweenness;
		}

		public synchronized void addBetweenness(double value) {
			betweenness += value;
		}

	}
	
	private static class CentralityThread extends Thread {
	
		private static int numVProcessed;
		
		private UnweightedDijkstra<CentralityVertex<Vertex>> dijkstra;
		
		private Queue<CentralityVertex<Vertex>> vertexQueue;
		
		private int diameter;
		
		private int radius;
		
//		private TObjectDoubleHashMap<EdgeDecorator<?>> edgeCentrality;
		
		public CentralityThread(CentralityGraph<Vertex, Edge> cGraph, Queue<CentralityVertex<Vertex>> vertexQueue) {
			this.vertexQueue = vertexQueue;
			dijkstra = new UnweightedDijkstra<CentralityVertex<Vertex>>(cGraph);
			diameter = 0;
			radius = Integer.MAX_VALUE;
			
//			edgeCentrality = new TObjectDoubleHashMap<EdgeDecorator<?>>(cGraph.getEdges().size());
		}
		
		@Override
		public void run() {
			long dkTime = 0;
			long cTime = 0;
			CentralityVertex<Vertex> v;
			while((v = vertexQueue.poll()) != null) {
				long time = System.currentTimeMillis();
				List<UnweightedDijkstra<CentralityVertex<Vertex>>.DijkstraVertex> vertices = dijkstra.run(v);
				dkTime += System.currentTimeMillis() - time;
				
				
				time = System.currentTimeMillis();
				int aplsum = 0;
				int numPaths;
				int eccentricity = 0;//Integer.MAX_VALUE;
				List<CentralityVertex<Vertex>> passedVertices = new ArrayList<CentralityVertex<Vertex>>();
//				List<CentralityEdge<?,?>> passedEdges = new ArrayList<CentralityEdge<?,?>>();
				int size2 = vertices.size();
				for(int k = 0; k < size2; k++) {
					int size;

					UnweightedDijkstra<CentralityVertex<Vertex>>.DijkstraVertex target = vertices.get(k);
					passedVertices.clear();
//					passedEdges.clear();
					numPaths = 0;
					size = target.getPrecedingVertices().length;
					for(int i = 0; i < size; i++) {
						numPaths += getNumPaths(target.getPrecedingVertices()[i], passedVertices);					
					}
					
					double bc = 1 / (double)numPaths;
					size = passedVertices.size();
					for(int i = 0; i < size; i++) {
						passedVertices.get(i).addBetweenness(bc);
					}
//					size = passedEdges.size();
//					for(int i = 0; i < size; i++) {
//						passedEdges.get(i).addBetweenness(bc);
//					}
					//**************************************************************
//					size = passedEdges.size();
//					for(int i = 0; i < size; i++) {
//						edgeCentrality.adjustOrPutValue(passedEdges.get(i), bc, bc);
//					}
					//**************************************************************
					int distance = (passedVertices.size() + numPaths)/numPaths; 
					aplsum += distance;
					eccentricity = Math.max(eccentricity, distance);
				}
				diameter = Math.max(diameter, eccentricity);
				radius = Math.min(radius, eccentricity);
				cTime += System.currentTimeMillis() - time;

				if(size2 == 0)
					v.setCloseness(0.0);
				else
					v.setCloseness(aplsum/(double)size2);
				
				numVProcessed++;
				int div = 1000;
				if(numVProcessed < 1000)
					div = 100;
				if(numVProcessed % div == 0) {
					logger.info(String.format("Processed %1$s vertices (dijkstra %2$s msec/vertex, centrality %3$s msec/vertex).", numVProcessed, dkTime/div, cTime/div));
					dkTime = 0;
					cTime = 0;
				}
			}
		}

		private int getNumPaths(UnweightedDijkstra<CentralityVertex<Vertex>>.DijkstraVertex v, List<CentralityVertex<Vertex>> vertices) {
			if (v.getPrecedingVertices().length == 0)
				return 1;
			else {
				int numPaths = 0;
				int size = v.getPrecedingVertices().length;
				for(int i = 0; i < size; i++) {
					vertices.add(v.getDelegate());
//					edges.add((CentralityEdge<?, ?>) v.getPrecedingEdges()[i].getDelegate());
					//****************************************************************
//					List<? extends Edge> adjacentEdges = v.getDelegate().getDelegate().getEdges();
//					Vertex toNode = v.getPrecedingVertices()[i].getDelegate().getDelegate();
//					for(int k = 0; k < adjacentEdges.size(); k++) {
//						if(adjacentEdges.get(k).getOpposite(v.getDelegate().getDelegate()).equals(toNode)) {
//							edges.add(adjacentEdges.get(k));
//						}
//					}
					//****************************************************************
					numPaths += getNumPaths(v.getPrecedingVertices()[i], vertices);
				}
				return numPaths;
			}
		}
	}
	
	/**
	 * Container class storing centrality attributes of a graph.
	 * 
	 * @author illenberger
	 * 
	 */
	public static class GraphDistance {

		private TObjectDoubleHashMap<Vertex> vertexCloseness;

		private double graphCloseness;

		private TObjectDoubleHashMap<Vertex> vertexBetweenness;

		private TObjectDoubleHashMap<Vertex> vertexBetweennessNormalized;
		
		private TObjectDoubleHashMap<Edge> edgeBetweeness;

		private double graphBetweenness;

		private double graphBetweennessNormalized;

		private int diatmeter;

		private int radius;

		/**
		 * Returns the closeness values for vertices.
		 * 
		 * @return a object-double map containing the vertex as key and the
		 *         closeness as value.
		 */
		public TObjectDoubleHashMap<Vertex> getVertexCloseness() {
			return vertexCloseness;
		}

		/**
		 * Returns the closeness centrality of the graph.
		 * 
		 * @return the closeness centrality of the graph.
		 */
		public double getGraphCloseness() {
			return graphCloseness;
		}

		/**
		 * Returns the betweenness values for vertices.
		 * 
		 * @return a object-double map containing the vertex as key and the
		 *         betweenness as value.
		 */

		public TObjectDoubleHashMap<Vertex> getVertexBetweennees() {
			return vertexBetweenness;
		}

		public TObjectDoubleHashMap<Edge> getEdgeBetweeness() {
			return edgeBetweeness;
		}
		
		/**
		 * Returns the betweenness centrality of the graph.
		 * 
		 * @return the betweenness centrality of the graph.
		 */
		public double getGraphBetweenness() {
			return graphBetweenness;
		}

		/**
		 * Returns the betweenness values for vertices normalized with
		 * (n-1)*(n-2).
		 * 
		 * @return a object-double map containing the vertex as key and the
		 *         normalized betweenness as value.
		 */
		public TObjectDoubleHashMap<Vertex> getVertexBetweenneesNormalized() {
			return vertexBetweennessNormalized;
		}

		/**
		 * Returns the betweenness centrality of the graph normalized with
		 * (n-1)*(n-2).
		 * 
		 * @return the normalized betweenness centrality of the graph.
		 */
		public double getGraphBetweennessNormalized() {
			return graphBetweennessNormalized;
		}

		/**
		 * Returns the diameter of the graph.
		 * 
		 * @return the diameter of the graph.
		 */
		public int getDiameter() {
			return diatmeter;
		}

		/**
		 * Returns the radius of the graph.
		 * 
		 * @return the radius of the graph.
		 */
		public int getRadius() {
			return radius;
		}
	}
	
	private static class CentralityGraphFactory<V extends Vertex, E extends Edge> implements GraphProjectionFactory<Graph, V, E, CentralityGraph<V, E>, CentralityVertex<V>, CentralityEdge<V, E>> {

		public CentralityEdge<V, E> createEdge(E delegate) {
			return new CentralityEdge<V, E>(delegate);
		}

		public CentralityGraph<V, E> createGraph(Graph delegate) {
			return new CentralityGraph<V, E>(delegate);
		}

		public CentralityVertex<V> createVertex(V delegate) {
			return new CentralityVertex<V>(delegate);
		}
		
	}
	
	private static class CentralityGraphBuilder<V extends Vertex, E extends Edge> extends GraphProjectionBuilder<Graph, V, E, CentralityGraph<V, E>, CentralityVertex<V>, CentralityEdge<V, E>> {
		
		public CentralityGraphBuilder() {
			super(new CentralityGraphFactory<V, E>());
		}
	}
//	public static void main(String args[]) {
//		ErdosRenyiGenerator generator = new ErdosRenyiGenerator(200, 0.3);
//		edu.uci.ics.jung.graph.ArchetypeGraph g = generator.generateGraph();
//		Map<ArchetypeVertex, Double> apl = edu.uci.ics.jung.statistics.GraphStatistics.averageDistances(g);
//		double sum = 0;
//		for(Double d : apl.values())
//			sum += d;
//		System.out.println("APL with JUNG: " + sum/(double)apl.size());
//		sum = 0;
//		for(Object v : g.getVertices())
//			sum += ((ArchetypeVertex)v).degree();
//		System.out.println("Degree with JUNG: " + sum/(double)g.getVertices().size());
//		
//		SparseGraph g2 = new SparseGraph();
//		Map<ArchetypeVertex, SparseVertex> vMapping = new HashMap<ArchetypeVertex, SparseVertex>();
//		for(Object v : g.getVertices()) {
//			vMapping.put((ArchetypeVertex) v, g2.addVertex());
//		}
//		for(Object e : g.getEdges()) {
//			Iterator it = ((ArchetypeEdge)e).getIncidentVertices().iterator();
//			SparseVertex v1 = vMapping.get(it.next());
//			SparseVertex v2 = vMapping.get(it.next());
//			g2.addEdge(v1, v2);
//		}
//		System.out.println("APL : " + getCentralityStatistics(g2).getSecond().getMean());
//		System.out.println("Degree: " + getDegreeStatistics(g2).getMean());
//	}
}
