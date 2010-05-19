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
package playground.johannes.socialnetworks.graph.analysis;

import gnu.trove.TIntArrayList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.matrix.AdjacencyMatrix;

import playground.johannes.socialnetworks.graph.matrix.Dijkstra;

/**
 * @author illenberger
 * 
 */
public class Components {

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

	public <V extends Vertex> Set<Set<V>> components(Graph graph) {
		AdjacencyMatrix<V> y = new AdjacencyMatrix<V>(graph);
		Set<Set<V>> components = new HashSet<Set<V>>();
		List<TIntArrayList> comps = extractComponents(y);
		for(TIntArrayList comp : comps) {
			Set<V> component = new HashSet<V>();
			for(int i = 0; i < comp.size(); i++) {
				component.add(y.getVertex(comp.get(i)));
			}
			components.add(component);
		}
		return components;
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
				int result = o2.size() - o1.size();
				if (result == 0) {
					if (o1 == o2)
						return 0;
					else
						return o2.hashCode() - o1.hashCode();
				} else
					return result;
			}
		});

		return components;
	}
}
