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
package playground.johannes.socialnetworks.snowball2.sim;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.matsim.contrib.sna.graph.Vertex;

/**
 * @author illenberger
 *
 */
public class RandomSeedGenerator implements VertexPartition {

	private Random random;
	
	private int numSeeds;
	
	public RandomSeedGenerator(int numSeeds, long randomSeed) {
		random = new Random(randomSeed);
		this.numSeeds = numSeeds;
	}
	
	@Override
	public <V extends Vertex> Set<V> partition(Set<V> vertices) {
		List<V> list = new ArrayList<V>(vertices);
		Collections.shuffle(list, random);
		Set<V> seeds = new HashSet<V>();
		for(int i = 0; i < numSeeds; i++)
			seeds.add(list.get(i));
		
		return seeds;
	}

}
