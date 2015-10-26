/* *********************************************************************** *
 * project: org.matsim.*
 * Components.java
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
package org.matsim.contrib.socnetgen.sna.graph.analysis;

import gnu.trove.TIntArrayList;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.contrib.socnetgen.sna.graph.Graph;
import org.matsim.contrib.socnetgen.sna.graph.Vertex;
import org.matsim.contrib.socnetgen.sna.graph.matrix.AdjacencyMatrix;
import org.matsim.contrib.socnetgen.sna.graph.matrix.Dijkstra;

import java.util.*;

/**
 * A class that provides functionality to analyze connectivity of graph.
 * 
 * @author illenberger
 * 
 */
public class Components {

	private static Components instance;
	
	public static Components getInstance() {
		if(instance == null)
			instance = new Components();
		return instance;
	}
	
	/**
	 * Counts the number of disconnected components in the graph.
	 * 
	 * @param graph
	 *            a graph.
	 * @return the number of disconnected components.
	 */
	public int countComponents(Graph graph) {
		return extractComponents(new AdjacencyMatrix<Vertex>(graph)).size();
	}

	/**
	 * Extracts the disconnected components and returns them as a list of vertex
	 * sets. The components in the list are descending in size.
	 * 
	 * @param <V>
	 *            the vertex type.
	 * @param graph
	 *            a graph.
	 * @return a list of vertex sets.
	 */
	public <V extends Vertex> List<Set<V>> components(Graph graph) {
		AdjacencyMatrix<V> y = new AdjacencyMatrix<V>(graph);

		List<TIntArrayList> comps = extractComponents(y);

		List<Set<V>> components = new ArrayList<Set<V>>(comps.size());
		for (TIntArrayList comp : comps) {
			Set<V> component = new HashSet<V>();
			for (int i = 0; i < comp.size(); i++) {
				component.add(y.getVertex(comp.get(i)));
			}
			components.add(component);
		}

		return components;
	}

	/**
	 * Returns the distribution of the sizes of disconnected components.
	 * 
	 * @param graph
	 *            a graph.
	 * @return the distribution of the sizes of disconnected components.
	 */
	public DescriptiveStatistics distribution(Graph graph) {
		AdjacencyMatrix<Vertex> y = new AdjacencyMatrix<Vertex>(graph);
		List<TIntArrayList> components = extractComponents(y);
		DescriptiveStatistics stats = new DescriptiveStatistics();
		for (TIntArrayList component : components) {
			stats.addValue(component.size());
		}
		return stats;
	}

	private List<TIntArrayList> extractComponents(AdjacencyMatrix<?> y) {
		boolean[] pending = new boolean[y.getVertexCount()];
		Arrays.fill(pending, true);

		List<TIntArrayList> components = new ArrayList<TIntArrayList>();
		Dijkstra router = new Dijkstra(y);
		for (int i = 0; i < pending.length; i++) {
			if (pending[i] == true) {
				TIntArrayList reachable = router.run(i, -1);
				reachable.add(i);
				components.add(reachable);
				for (int k = 0; k < reachable.size(); k++) {
					pending[reachable.get(k)] = false;
				}
			}
		}

		Collections.sort(components, new Comparator<TIntArrayList>() {

			@Override
			public int compare(TIntArrayList o1, TIntArrayList o2) {
				int result = o1.size() - o2.size();
				if (result == 0) {
					if (o1 == o2)
						return 0;
					else
						return o2.hashCode() - o1.hashCode();
				} else
					return result;
			}
		});

		Collections.reverse(components);

		return components;
	}
}
