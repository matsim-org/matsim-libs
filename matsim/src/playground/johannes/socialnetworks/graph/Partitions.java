/* *********************************************************************** *
 * project: org.matsim.*
 * Partitions.java
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
package playground.johannes.socialnetworks.graph;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.matsim.contrib.sna.graph.Edge;
import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.core.utils.collections.Tuple;

import gnu.trove.TDoubleObjectHashMap;
import gnu.trove.TObjectDoubleHashMap;
import gnu.trove.TObjectDoubleIterator;

/**
 * @author illenberger
 *
 * Test
 */
public class Partitions {

	/**
	 * Creates vertex partitions based on a vertex attribute.
	 * 
	 * @param <V>
	 * @param vertexValues
	 *            a map with the vertices as key and the vertex attribute value as value.
	 * @param binsize
	 *            a disrectization constant. Pass <tt>0</tt> if the vertex
	 *            attribute values should not be discretized.
	 * @return a map with the partitions as values and the vertex value as key.
	 *         Partitions are represented through a set of vertices. If values
	 *         are discretized the key is the lower bound of the bin.
	 */
	public static <V extends Vertex> TDoubleObjectHashMap<Set<V>> createPartitions(TObjectDoubleHashMap<V> vertexValues, double binsize) {
		TDoubleObjectHashMap<Set<V>> partitions = new TDoubleObjectHashMap<Set<V>>();
		TObjectDoubleIterator<V> it = vertexValues.iterator();
		for(int i = vertexValues.size(); i > 0; i--) {
			it.advance();
			double bin = it.value();
			if(binsize > 0)
				bin = Math.floor(it.value()/binsize) * binsize;
			Set<V> partition = partitions.get(bin);
			if(partition == null) {
				partition = new HashSet<V>();
				partitions.put(bin, partition);
			}
			partition.add(it.key());
		}
		return partitions;
	}
	
	/**
	 * Creates vertex partitions based on the degree.
	 * 
	 * @param <V>
	 * @param vertices
	 *            the set of vertices that should be partitioned.
	 * @return a map with the partitions as value and the degree as key
	 * @see {@link #createPartitions(TObjectDoubleHashMap, double)}
	 */
	public static <V extends Vertex> TDoubleObjectHashMap<Set<V>> createDegreePartitions(Set<V> vertices) {
		TObjectDoubleHashMap<V> degrees = new TObjectDoubleHashMap<V>();
		for(V v : vertices)
			degrees.put(v, v.getNeighbours().size());
		
		return createPartitions(degrees, 1.0);
	}

	/**
	 * Extracts the disconnected components of graph <tt>g</tt>.
	 * 
	 * @param g
	 *            the graph the disconnected components are to be extracted.
	 * @return a sorted set containing the disconnected components descending in
	 *         size. A component is represented through a set of its vertices.
	 */
	public static <V extends Vertex> SortedSet<Set<V>> disconnectedComponents(Graph g) {
		UnweightedDijkstra<V> dijkstra = new UnweightedDijkstra<V>(g);
		Queue<V> vertices = new LinkedList<V>((Collection<? extends V>) g.getVertices());
		SortedSet<Set<V>> components = new TreeSet<Set<V>>(new Comparator<Collection<?>>() {
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
		
		V v;
		while((v = vertices.poll()) != null) {
			List<? extends VertexDecorator<V>> component = dijkstra.run(v);
			Set<V> componentPlain = new HashSet<V>();
			int cnt = component.size();
			for(int i = 0; i < cnt; i++) {
				vertices.remove(component.get(i).getDelegate());
				componentPlain.add(component.get(i).getDelegate());
			}
			componentPlain.add(v);
			components.add(new HashSet<V>(componentPlain));
		}
		
		return components;
	}

	/**
	 * Extracts the disconnected components of graph <tt>g</tt> and returns each
	 * component as a graph projection.
	 * 
	 * @param g
	 *            the graph the disconnected components are to be extracted.
	 * @return a sorted set containing the disconnected components as graph
	 *         projections descending in size.
	 */
	public static SortedSet<Graph> subGraphs(Graph g) {
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
	
		SortedSet<Set<Vertex>> components = disconnectedComponents(g);
		SparseGraphProjectionBuilder<Graph, Vertex, Edge> builder = new SparseGraphProjectionBuilder<Graph, Vertex, Edge>();
		for(Set<Vertex> component : components) {
			GraphProjection<Graph, Vertex, Edge> proj = new GraphProjection<Graph, Vertex, Edge>(g);
			/*
			 * Add all vertices...
			 */
			for(Vertex v : component)
//				proj.addVertex(v);
				builder.addVertex(proj, v);
			
			/*
			 * Loop through all vertices and add edges...
			 */
			for(Vertex v : component) {
				int n = v.getEdges().size();
				for(int i = 0; i < n; i++) {
					Edge e = v.getEdges().get(i);
					Tuple<? extends Vertex, ? extends Vertex> p = e.getVertices();
//					proj.addEdge(proj.getVertex(p.getFirst()), proj.getVertex(p.getSecond()), e);
					builder.addEdge(proj, proj.getVertex(p.getFirst()), proj.getVertex(p.getSecond()), e);
				}
			}
			
			subGraphs.add(proj);
		}
		
		return subGraphs;
	}
	
	
}
