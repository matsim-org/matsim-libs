/* *********************************************************************** *
 * project: org.matsim.*
 * RandomResponse.java
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

import org.matsim.contrib.socnetgen.sna.graph.Vertex;

import java.util.LinkedHashSet;
import java.util.Random;
import java.util.Set;

/**
 * A vertex filter which creates a random subset of vertices form a given
 * set of vertices.
 * 
 * @author illenberger
 * 
 */
public class RandomPartition<V extends Vertex> implements VertexFilter<V> {

	private final double proba;

	private final Random random;

	/**
	 * Creates a new vertex filter.
	 * 
	 * @param proba
	 *            the inclusion probability of a vertex in the subset
	 */
	public RandomPartition(double proba) {
		this.proba = proba;
		random = new Random();
	}

	/**
	 * Creates a new partition generator.
	 * 
	 * @param proba
	 *            the inclusion probability of a vertex in the subset
	 * @param randomSeed
	 *            a random seed number
	 */
	public RandomPartition(double proba, long randomSeed) {
		this.proba = proba;
		random = new Random(randomSeed);
	}

	/**
	 * Returns a random subset of vertices from <tt>vertices</tt> where each
	 * vertex is included in the subset with a given constant probability.
	 * 
	 * @return a random subset of vertices.
	 */
	@Override
	public Set<V> apply(Set<V> vertices) {
		Set<V> partition = new LinkedHashSet<V>();
		for (V vertex : vertices) {
			if (random.nextDouble() < proba) {
				partition.add(vertex);
			}
		}
		return partition;
	}

}
