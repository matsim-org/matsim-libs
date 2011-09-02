/* *********************************************************************** *
 * project: org.matsim.*
 * RandomAlter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.johannes.coopsim.mental.choice;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import playground.johannes.socialnetworks.graph.social.SocialVertex;

/**
 * @author illenberger
 *
 */
public class RandomAlter implements ActivityGroupGenerator {

	private final Random random;
	
	public RandomAlter(Random random) {
		this.random = random;
	}
	
	@Override
	public Set<SocialVertex> generate(SocialVertex ego) {
		Set<SocialVertex> group = new HashSet<SocialVertex>(2);
		group.add(ego);
		group.add(ego.getNeighbours().get(random.nextInt(ego.getNeighbours().size())));
		
		return group;
	}

}
