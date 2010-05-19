/* *********************************************************************** *
 * project: org.matsim.*
 * Dijkstra.java
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

import java.util.Arrays;
import java.util.Comparator;
import java.util.PriorityQueue;

import org.matsim.contrib.sna.graph.matrix.AdjacencyMatrix;

/**
 * Implementation of the Dijkstra best path algorithm. This algorithm runs on an
 * adjacency matrix, i.e., the graph is treated as undirected and unweighted.
 * 
 * @author illenberger
 * 
 */
public class Dijkstra {

	private AdjacencyMatrix<?> y;

	private TIntArrayList[] spanningTree;

	/**
	 * Creates a new Dijkstra object with adjacency matrix <tt>y</tt>.
	 * 
	 * @param y
	 *            an adjacency matrix.
	 */
	public Dijkstra(AdjacencyMatrix<?> y) {
		this.y = y;
	}

	/**
	 * Runs the Dijkstra algorithm from <tt>source</tt> to <tt>target</tt> and
	 * returns all vertices that could be reached from <tt>source</tt>. If
	 * <tt>target</tt> is not reachable or denotes a non-existing vertex index
	 * (e.g. -1) the algorithm runs over the complete connected component. To
	 * obtain a concrete path use {@link #getPath(int, int)}.
	 * 
	 * @param source
	 *            the source vertex index.
	 * @param target
	 *            the target vertex index, or <tt>-1</tt> to build up the
	 *            complete spanning tree.
	 * @return a list of all reachable vertices.
	 */
	public TIntArrayList run(int source, int target) {
		TIntArrayList reachable = new TIntArrayList(y.getVertexCount());
		boolean settled[] = new boolean[y.getVertexCount()];
		boolean visited[] = new boolean[y.getVertexCount()];
		final int[] distances = new int[y.getVertexCount()];

		spanningTree = new TIntArrayList[y.getVertexCount()];

		Arrays.fill(distances, Integer.MAX_VALUE);
		distances[source] = 0;

		PriorityQueue<Integer> unsettled = new PriorityQueue<Integer>(y.getVertexCount(), new Comparator<Integer>() {

			public int compare(Integer o1, Integer o2) {
				int r = distances[o1] - distances[o2];
				if (r == 0) {
					return o1;
				} else
					return r;
			}

		});

		unsettled.add(source);

		Integer i_obj;
		while ((i_obj = unsettled.poll()) != null) {
			int i = i_obj.intValue();

			reachable.add(i);

			if (i == target)
				break;

			settled[i] = true;

			TIntArrayList neighbours = y.getNeighbours(i);
			for (int k = 0; k < neighbours.size(); k++) {
				int j = neighbours.get(k);
				if (!settled[j]) {
					int d = distances[i] + 1;
					if (d < distances[j]) {
						TIntArrayList predecessors = new TIntArrayList();
						spanningTree[j] = predecessors;
						predecessors.add(i);
						distances[j] = d;

						if (visited[j])
							unsettled.remove(j);
						else
							visited[j] = true;

						unsettled.add(j);
					} else if (d == distances[j]) {
						TIntArrayList predecessors = spanningTree[j];
						if (predecessors == null) {
							predecessors = new TIntArrayList();
							spanningTree[j] = predecessors;
						}

						predecessors.add(i);
					}
				}
			}
		}

		reachable.remove(reachable.indexOf(source));

		return reachable;
	}

	/**
	 * Returns a shortest path from vertex <tt>i</tt> to <tt>j</tt>, or
	 * <tt>null</tt> if there is no path from <tt>i</tt> to <tt>j</tt>. If there
	 * are multiple shortest paths there is no rule which path is returned.
	 * 
	 * @param i
	 *            the source vertex index.
	 * @param j
	 *            the target vertex index.
	 * @return a shortest path from vertex <tt>i</tt> to <tt>j</tt>, or
	 *         <tt>null</tt> if there is no path from <tt>i</tt> to <tt>j</tt>.
	 */
	public TIntArrayList getPath(int i, int j) {
		TIntArrayList path = new TIntArrayList();

		if (i == j)
			return path;

		if (spanningTree[j] == null || spanningTree[j].isEmpty())
			return null;

		while (j != i) {
			path.add(j);
			if (spanningTree[j].size() > 0)
				j = spanningTree[j].get(0);
			else
				return null;
		}
		path.reverse();

		return path;
	}

	/**
	 * Returns the spanning tree. Each element in the array contains a list with
	 * the vertex's predecessors (array indices are vertex indices). A
	 * <tt>null</tt> element or an empty list indicates that this vertex has no
	 * predecessors.
	 * 
	 * @return the spanning tree.
	 */
	public TIntArrayList[] getSpanningTree() {
		return spanningTree;
	}
}
