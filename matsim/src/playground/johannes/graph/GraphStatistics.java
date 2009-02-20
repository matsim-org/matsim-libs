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
package playground.johannes.graph;


import gnu.trove.TObjectDoubleHashMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.log4j.Logger;
import org.matsim.utils.collections.Tuple;

import playground.johannes.statistics.WeightedStatistics;

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
	 * Retrieves the degree statistics of graph <tt>g</tt>. The graph is treated
	 * as undirected.
	 * 
	 * @param g
	 *            the graph to retrieve the degree statistics from.
	 * @return A DescriptiveStatistics object containing the degree of each
	 *         vertex in the graph.
	 */
	public static DescriptiveStatistics getDegreeStatistics(Graph g) {
		DescriptiveStatistics stats = new DescriptiveStatistics();
		for(Vertex v : g.getVertices())
			stats.addValue(v.getEdges().size());
		return stats;
	}
	
	/**
	 * Retrieves the degree distribution of graph <tt>g</tt>. The graph is
	 * treated as undirected.
	 * 
	 * @param g
	 *            the graph to retrieve the degree distribution from.
	 * @return the degree distribution.
	 */
	public static WeightedStatistics getDegreeDistribution(Graph g) {
//		Frequency freq = new Frequency();
		WeightedStatistics stats = new WeightedStatistics();
		for(Vertex v : g.getVertices())
//			freq.addValue(v.getEdges().size());
			stats.add(v.getEdges().size());
//		return freq;
		return stats;
	}
	
	/**
	 * Calculates the local clustering coefficient of each vertex in graph
	 * <tt>g</tt>. The local clustering coefficient is defined as:
	 * <ul>
	 * <li>C = 0 if k = 0 or k = 1,
	 * <li>C = 2y/k*(k-1) if k > 1, where y is the number of edges connecting
	 * neighbors of the vertex.
	 * </ul>
	 * 
	 * @param g
	 *            the graph the local clustering coefficients are to be
	 *            calculated.
	 * @return an object-double map containing the vertex as key and the
	 *         clustering coefficient as value.
	 */
	public static TObjectDoubleHashMap<? extends Vertex> getClustringCoefficients(Graph g) {
		TObjectDoubleHashMap<Vertex> cc = new TObjectDoubleHashMap<Vertex>();
		for(Vertex v : g.getVertices()) {
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
	 * Retrieves the clustering statistic of graph <tt>g</tt>.
	 * 
	 * @param g
	 *            the grpah the clustering statistics are to be retrieved.
	 * @return a DescriptiveStatistics object containing the local clustering
	 *         coefficient for each vertex.
	 */
	public static DescriptiveStatistics getClusteringStatistics(Graph g) {
		DescriptiveStatistics stats = new DescriptiveStatistics();
		TObjectDoubleHashMap<? extends Vertex> cc = getClustringCoefficients(g);
		for(double d : cc.getValues())
			stats.addValue(d);
		return stats;
	}
	
	public static double getGlobalClusteringCoefficient(Graph g) {
		int n_tripples = 0;
		int n_triangles = 0;
		for(Vertex v : g.getVertices()) {
			List<? extends Vertex> n1s = v.getNeighbours();
			for(int i = 0; i < n1s.size(); i++) {
				List<? extends Vertex> n2s = n1s.get(i).getNeighbours();
				for(int k = 0; k < n2s.size(); k++) {
					n_tripples++;
					if(n2s.get(k).getNeighbours().contains(v))
						n_triangles++;
				}
			}
		}
		
		return 3 * n_triangles / (double)n_tripples;
	}
	
	public static int getNumTwoStars(Graph g) {
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
	public static double getMutuality(Graph g) {
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
	 * See: M.ÊE.ÊJ. Newman. Assortative mixing in networks. Physical Review
	 * Letters, 89(20), 2002.
	 * 
	 * @param g
	 *            the graph the degree correlation is to be calculated.
	 * @return the degree correlation.
	 */
	public static double getDegreeCorrelation(Graph g) {
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
	 * Extracts the disconnected components of graph <tt>g</tt>.
	 * 
	 * @param g
	 *            the graph the disconnected components are to be extracted.
	 * @return a sorted set containing the disconnected components descending in
	 *         size. A component is represented through a set of its vertices.
	 */
	public static SortedSet<Set<Vertex>> getComponents(Graph g) {
		UnweightedDijkstra<Vertex> dijkstra = new UnweightedDijkstra<Vertex>(g);
		Queue<Vertex> vertices = new LinkedList<Vertex>(g.getVertices());
		SortedSet<Set<Vertex>> components = new TreeSet<Set<Vertex>>(new Comparator<Collection<?>>() {
			public int compare(Collection<?> o1, Collection<?> o2) {
				int result = o2.size() - o1.size();
				if(result == 0) {
					if(o1 == o2)
						return 0;
					else
						/*
						 * Does not work for empty collections, but is
						 * ok for the purpose here.
						 */
						return o2.hashCode() - o1.hashCode();
				} else
					return result;
			}
		});
		
		Vertex v;
		while((v = vertices.poll()) != null) {
			List<? extends VertexDecorator<Vertex>> component = dijkstra.run(v);
			Set<Vertex> componentPlain = new HashSet<Vertex>();
			int cnt = component.size();
			for(int i = 0; i < cnt; i++) {
				vertices.remove(component.get(i).getDelegate());
				componentPlain.add(component.get(i).getDelegate());
			}
			componentPlain.add(v);
			components.add(new HashSet<Vertex>(componentPlain));
		}
		
		return components;
	}
	
	public static SortedSet<Graph> getSubGraphs(Graph g) {
		SortedSet<Graph> subGraphs = new TreeSet<Graph>(new Comparator<Graph>() {
			public int compare(Graph g1, Graph g2) {
				int result = g2.getVertices().size() - g1.getVertices().size();
				if(result == 0) {
					if(g1 == g2)
						return 0;
					else
						return g2.hashCode() - g1.hashCode();
				} else
					return result;
			}
		});
	
		SortedSet<Set<Vertex>> components = getComponents(g);
		for(Set<Vertex> component : components) {
			GraphProjection<Graph, Vertex, Edge> proj = new GraphProjection<Graph, Vertex, Edge>(g);
			/*
			 * Add all vertices...
			 */
			for(Vertex v : component)
				proj.addVertex(v);
			
			/*
			 * Loop through all vertices and add edges...
			 */
			for(Vertex v : component) {
				int n = v.getEdges().size();
				for(int i = 0; i < n; i++) {
					Edge e = v.getEdges().get(i);
					Tuple<? extends Vertex, ? extends Vertex> p = e.getVertices();
					proj.addEdge(proj.getVertex(p.getFirst()), proj.getVertex(p.getSecond()), e);
				}
			}
			
			subGraphs.add(proj);
		}
		
		return subGraphs;
	}
	
	/**
	 * Calculates closeness centrality, betweenness centrality, diameter and
	 * radius of graph <tt>g</tt>.
	 * 
	 * @param g
	 *            the graph the centrality statistics are to be calculated.
	 * @return a GraphDistance object storing information about closeness
	 *         centrality, betweenness centrality, diameter and radius of graph
	 *         <tt>g</tt>.
	 */
	public static GraphDistance getCentrality(Graph g) {
		logger.info("Initializing graph for centrality calculation...");
		CentralityGraph<Vertex> cGraph = new CentralityGraph<Vertex>(g);
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
		
		GraphDistance d = new GraphDistance();
		d.diatmeter = diameter;
		d.radius = radius;
		d.vertexCloseness = ccMap;
		d.vertexBetweenness = bcMap;
		d.vertexBetweennessNormalized = bcMapNorm;
		d.graphCloseness = meanCc;
		d.graphBetweenness = meanBc;
		d.graphBetweennessNormalized = meanBcNorm;
		
		return d;
	}
	
	private static int getNumPaths(UnweightedDijkstra<CentralityVertex<Vertex>>.DijkstraVertex v, List<CentralityVertex<Vertex>> vertices) {
		if (v.getPredecessors().length == 0)
			return 1;
		else {
			int numPaths = 0;
			int size = v.getPredecessors().length;
			for(int i = 0; i < size; i++) {
				vertices.add(v.getDelegate());
				numPaths += getNumPaths(v.getPredecessors()[i], vertices);
			}
			return numPaths;
		}
	}
	
	private static class CentralityGraph<V extends Vertex> extends GraphProjection<Graph, V, Edge> {

		public CentralityGraph(Graph delegate) {
			super(delegate);
			decorate();
		}

		@Override
		public VertexDecorator<V> addVertex(V delegate) {
			VertexDecorator<V> v = new CentralityVertex<V>(delegate);
//			if(insertVertex(v))
//				return v;
//			else
//				return null;
			return addVertex(v);
		}

		@SuppressWarnings("unchecked")
		@Override
		public Set<? extends CentralityVertex<V>> getVertices() {
			return (Set<? extends CentralityVertex<V>>) super.getVertices();
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
	
	private static class CentralityThread extends Thread {
	
		private static int numVProcessed;
		
		private UnweightedDijkstra<CentralityVertex<Vertex>> dijkstra;
		
		private Queue<CentralityVertex<Vertex>> vertexQueue;
		
		private int diameter;
		
		private int radius;
		
		public CentralityThread(CentralityGraph<Vertex> cGraph, Queue<CentralityVertex<Vertex>> vertexQueue) {
			this.vertexQueue = vertexQueue;
			dijkstra = new UnweightedDijkstra<CentralityVertex<Vertex>>(cGraph);
			diameter = 0;
			radius = Integer.MAX_VALUE;
		}
		
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
				int eccentricity = 0;
				List<CentralityVertex<Vertex>> passedVertices = new ArrayList<CentralityVertex<Vertex>>();
				int size2 = vertices.size();
				for(int k = 0; k < size2; k++) {
					int size;

					UnweightedDijkstra<CentralityVertex<Vertex>>.DijkstraVertex target = vertices.get(k);
					passedVertices.clear();
					numPaths = 0;
					size = target.getPredecessors().length;
					for(int i = 0; i < size; i++) {
						numPaths += getNumPaths(target.getPredecessors()[i], passedVertices);					
					}
					
					double bc = 1 / (double)numPaths;
					size = passedVertices.size();
					for(int i = 0; i < size; i++) {
						passedVertices.get(i).addBetweenness(bc);
					}
					
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
