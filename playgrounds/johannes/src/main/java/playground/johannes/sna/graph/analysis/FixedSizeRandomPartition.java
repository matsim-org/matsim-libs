/* *********************************************************************** *
 * project: org.matsim.*
 * RandomSeedGenerator.java
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
package playground.johannes.sna.graph.analysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import playground.johannes.sna.graph.Vertex;

/**
 * A vertex filter which creates a random subset of vertices with a fixed size.
 * 
 * @author illenberger
 * 
 */
public class FixedSizeRandomPartition<V extends Vertex> implements VertexFilter<V> {

	private Random random;

	private int n;

	/**
	 * Creates a new vertex filter.
	 * 
	 * @param n
	 *            the size of the subset
	 */
	public FixedSizeRandomPartition(int n) {
		random = new Random();
		this.n = n;
	}

	/**
	 * Creates a new vertex filter.
	 * 
	 * @param n
	 *            the size of the subset
	 * @param randomSeed
	 *            a random seed number
	 */
	public FixedSizeRandomPartition(int numSeeds, long randomSeed) {
		random = new Random(randomSeed);
		this.n = numSeeds;
	}

	/**
	 * Returns a random subset of vertices from <tt>vertices</tt> with a given
	 * size.
	 * 
	 * @return a random subset.
	 */
	@Override
	public Set<V> apply(Set<V> vertices) {
		List<V> list = new ArrayList<V>(vertices);
		Collections.shuffle(list, random);
		Set<V> seeds = new LinkedHashSet<V>();
		for (int i = 0; i < n; i++)
			seeds.add(list.get(i));

		return seeds;
	}

}
