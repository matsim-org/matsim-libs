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
package playground.johannes.socialnetworks.snowball2.sim;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.matsim.contrib.sna.graph.Vertex;

/**
 * @author illenberger
 *
 */
public class RandomResponse implements VertexPartition {

	private final double responseRate;
	
	private final Random random;
	
	public RandomResponse(double responseRate, long randomSeed) {
		this.responseRate = responseRate;
		random = new Random(randomSeed);
	}
	
	@Override
	public <V extends Vertex> Set<V> getPartition(Set<V> vertices) {
		Set<V> responding = new HashSet<V>();
		for(V vertex : vertices) {
			if(random.nextDouble() < responseRate) {
				responding.add(vertex);
			}
		}
		return responding;
	}

}
