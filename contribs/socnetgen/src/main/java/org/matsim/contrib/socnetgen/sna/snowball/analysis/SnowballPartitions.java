/* *********************************************************************** *
 * project: org.matsim.*
 * SnowballPartititions.java
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
package org.matsim.contrib.socnetgen.sna.snowball.analysis;

import gnu.trove.TIntObjectHashMap;
import org.matsim.contrib.socnetgen.sna.snowball.SampledVertex;

import java.util.*;

/**
 * Utility class for creating subsets of snowball sampled vertices.
 * 
 * @author illenberger
 * 
 */
public class SnowballPartitions {

	/**
	 * Creates a subset of sampled vertices.
	 * 
	 * @param <V>
	 *            the vertex type.
	 * @param vertices
	 *            a set of vertices.
	 * @return a subset of sampled vertices.
	 */
	public static <V extends SampledVertex> Set<V> createSampledPartition(Collection<V> vertices) {
		Set<V> partition = new HashSet<V>();
		for (V vertex : vertices) {
			if (vertex.isSampled())
				partition.add(vertex);
		}
		return partition;
	}

	/**
	 * Creates a subset of vertices sampled in a specific iteration.
	 * 
	 * @param <V>
	 *            the vertex type.
	 * @param vertices
	 *            a set of vertices.
	 * @param iteration
	 *            the snowball iteration.
	 * @return a subset of vertices sampled in iteration <tt>iteration</tt>.
	 */
	public static <V extends SampledVertex> Set<V> createSampledPartition(Collection<V> vertices, int iteration) {
		Set<V> partition = new HashSet<V>();
		for (V vertex : vertices) {
			if (vertex.isSampled()) {
				if (vertex.getIterationSampled() == iteration)
					partition.add(vertex);
			}
		}
		return partition;
	}

	/**
	 * Creates a subset of vertices detected in a specific iteration.
	 * 
	 * @param <V>
	 *            the vertex type.
	 * @param vertices
	 *            a set of vertices.
	 * @param iteration
	 *            the snowball iteration.
	 * @return a subset of vertices detected in a specific iteration.
	 */
	public static <V extends SampledVertex> Set<V> createDetectedPartition(Collection<V> vertices, int iteration) {
		Set<V> partition = new HashSet<V>();
		for (V vertex : vertices) {
			if (vertex.isDetected()) {
				if (vertex.getIterationDetected() == iteration)
					vertices.add(vertex);
			}
		}
		return partition;
	}

	/**
	 * Creates a list of sets of sampled vertices where the list index
	 * corresponds to the iteration the vertices have been sampled.
	 * 
	 * @param <V>
	 *            the vertex type.
	 * @param vertices
	 *            a set of vertices.
	 * @return a list of sets of sampled vertices where the list index
	 *         corresponds to the iteration the vertices have been sampled.
	 */
	public static <V extends SampledVertex> List<Set<V>> createSampledPartitions(Collection<V> vertices) {
		TIntObjectHashMap<Set<V>> partitions = new TIntObjectHashMap<Set<V>>();
		for (V vertex : vertices) {
			if (vertex.isSampled()) {
				int it = vertex.getIterationSampled();
				Set<V> partition = partitions.get(it);
				if (partition == null) {
					partition = new HashSet<V>();
					partitions.put(it, partition);
				}

				partition.add(vertex);
			}
		}

		List<Set<V>> list = new ArrayList<Set<V>>(partitions.size());
		for (int i = 0; i < partitions.size() - 1; i++) {
			list.add(partitions.get(i));
		}

		return list;
	}
}
