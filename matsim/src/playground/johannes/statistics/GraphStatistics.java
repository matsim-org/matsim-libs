/* *********************************************************************** *
 * project: org.matsim.*
 * GraphStatistivcsd.java
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
package playground.johannes.statistics;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;

import playground.johannes.snowball2.SparseVertex;
import playground.johannes.snowball2.UnweightedDijkstra;
import playground.johannes.snowball2.Centrality.CentralityGraph;
import playground.johannes.snowball2.Centrality.CentralityGraphDecorator;
import playground.johannes.snowball2.Centrality.CentralityVertex;
import playground.johannes.socialnets.UserDataKeys;
import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.impl.UndirectedSparseEdge;
import edu.uci.ics.jung.graph.impl.UndirectedSparseGraph;
import edu.uci.ics.jung.graph.impl.UndirectedSparseVertex;

/**
 * @author illenberger
 *
 */
public class GraphStatistics {

	protected static class SizeComparator implements Comparator<Collection<?>> {

		public int compare(Collection<?> o1, Collection<?> o2) {
			int result = o2.size() - o1.size();
			if(result == 0) {
				if(o1 == o2)
					return 0;
				else
					return o2.hashCode() - o1.hashCode();
			} else
				return result;
		}
		
	}
	
	public static DescriptiveStatistics getDegreeStatistics(Graph g) {
		DescriptiveStatistics stats = new DescriptiveStatistics();
		Set<Vertex> vertices = g.getVertices();
		for(Vertex v : vertices) {
			stats.addValue(v.degree());
		}
		return stats;
	}
	
	public static DescriptiveStatistics getClusteringStatistics(Graph g) {
		DescriptiveStatistics stats = new DescriptiveStatistics();
		Map<Vertex, Double> values = edu.uci.ics.jung.statistics.GraphStatistics.clusteringCoefficients(g);
		for (Vertex v : values.keySet()) {
			if (v.degree() == 1)
				stats.addValue(0.0);
			else
				stats.addValue(values.get(v));
		}

		return stats;
	}
	
	public static SortedSet<Collection<Vertex>> getDisconnectedComponents(Graph g) {
		TreeSet<Collection<Vertex>> clusters = new TreeSet<Collection<Vertex>>(new SizeComparator());
		CentralityGraphDecorator graphDecorator = new CentralityGraphDecorator(g);
		UnweightedDijkstra dijkstra = new UnweightedDijkstra((CentralityGraph) graphDecorator.getSparseGraph());
		Queue<CentralityVertex> vertices = new LinkedList<CentralityVertex>();
		for(SparseVertex v : graphDecorator.getSparseGraph().getVertices())
			vertices.add((CentralityVertex) v);
		
		CentralityVertex source;
		while((source = vertices.poll()) != null) {
			List<CentralityVertex> reached = dijkstra.run(source);
			reached.add(source);
			List<Vertex> reached2 = new LinkedList<Vertex>();
			for(CentralityVertex cv : reached)
				reached2.add(graphDecorator.getVertex(cv));
			clusters.add(reached2);
			vertices.removeAll(reached);
		}
		
		return clusters;
	}
	
	public static Graph extractGraphFromCluster(Collection<Vertex> cluster) {
		UndirectedSparseGraph g = new UndirectedSparseGraph();
		Map<Vertex, UndirectedSparseVertex> mapping = new HashMap<Vertex, UndirectedSparseVertex>();
		for(Vertex v : cluster) {
			UndirectedSparseVertex vCopy = new UndirectedSparseVertex();
			for(Iterator<String> it = v.getUserDatumKeyIterator(); it.hasNext();) {
				String key = it.next();
				vCopy.addUserDatum(key, v.getUserDatum(key), UserDataKeys.COPY_ACT);
			}
			g.addVertex(vCopy);
			mapping.put(v, vCopy);
		}
		for(Vertex v : cluster) {
			Set<Edge> edges = v.getIncidentEdges();
			for(Edge e : edges) {
				UndirectedSparseVertex v1 = mapping.get(v);
				UndirectedSparseVertex v2 = mapping.get(e.getOpposite(v));
				if (v1 != null && v2 != null) {
					UndirectedSparseEdge eCopy = new UndirectedSparseEdge(v1, v2);
					try {
						g.addEdge(eCopy);
					} catch (IllegalArgumentException ex) {
						// do nothing
					}
				}
			}
		}
		
		return g;
	}
}
