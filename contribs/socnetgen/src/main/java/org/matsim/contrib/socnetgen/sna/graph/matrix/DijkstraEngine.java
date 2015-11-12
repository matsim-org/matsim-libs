/* *********************************************************************** *
 * project: org.matsim.*
 * DijkstraEngine.java
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
package org.matsim.contrib.socnetgen.sna.graph.matrix;

import gnu.trove.list.array.TIntArrayList;

import java.util.Arrays;
import java.util.Comparator;
import java.util.PriorityQueue;

/**
 * @author jillenberger
 * 
 */
public abstract class DijkstraEngine {

	protected AdjacencyMatrix<?> y;

	/**
	 * Creates a new Dijkstra object with adjacency matrix <tt>y</tt>.
	 * 
	 * @param y
	 *            an adjacency matrix.
	 */
	protected DijkstraEngine(AdjacencyMatrix<?> y) {
		this.y = y;
	}

	/**
	 * Runs the Dijkstra algorithm from <tt>source</tt> to <tt>target</tt> and
	 * returns all vertices that could be reached from <tt>source</tt>. If
	 * <tt>target</tt> is not reachable or denotes a non-existing vertex index
	 * (e.g. -1) the algorithm runs over the complete connected component.
	 * 
	 * @param source
	 *            the source vertex index.
	 * @param target
	 *            the target vertex index, or <tt>-1</tt> to build up the
	 *            complete spanning tree.
	 * @return a list of all reachable vertices.
	 */
	protected TIntArrayList run(int source, int target) {
		TIntArrayList reachable = new TIntArrayList(y.getVertexCount());
		boolean settled[] = new boolean[y.getVertexCount()];
		boolean visited[] = new boolean[y.getVertexCount()];
		final double[] costs = new double[y.getVertexCount()];

		Arrays.fill(costs, Integer.MAX_VALUE);
		costs[source] = 0;

		PriorityQueue<Integer> unsettled = new PriorityQueue<Integer>(y.getVertexCount(), new Comparator<Integer>() {

			public int compare(Integer o1, Integer o2) {
				int r = Double.compare(costs[o1], costs[o2]);
				if (r == 0) {
					return o1-o2;
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
					double d = costs[i] + getCost(i, j);
					if (d < costs[j]) {
						foundCheaper(i, j, d);
						costs[j] = d;

						if (visited[j])
							unsettled.remove(j);
						else
							visited[j] = true;

						unsettled.add(j);
					} else if (d == costs[j]) {
						foundEqual(i, j, d);
					}
				}
			}
		}

		reachable.remove(reachable.indexOf(source));

		return reachable;
	}

	/**
	 * Returns the costs for traversing the edge from <tt>i</tt> to <tt>j</tt>,
	 * per default 1.0.
	 * 
	 * @param i
	 *            a vertex index
	 * @param j
	 *            a vertex index
	 * @return the costs for traversing the edge from <tt>i</tt> to <tt>j</tt>,
	 *         per default 1.0.
	 */
	protected double getCost(int i, int j) {
		return 1.0;
	}

	protected abstract void foundCheaper(int i, int j, double cost);

	protected void foundEqual(int i, int j, double cost) {

	}
}
