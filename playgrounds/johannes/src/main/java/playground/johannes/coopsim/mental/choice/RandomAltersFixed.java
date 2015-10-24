/* *********************************************************************** *
 * project: org.matsim.*
 * RadomAlters.java
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

import playground.johannes.socialnetworks.graph.social.SocialVertex;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * @author illenberger
 *
 */
public class RandomAltersFixed implements ActivityGroupGenerator {

	private final Random random;
	
	private final double count;
	
	public RandomAltersFixed(int count, Random random) {
		this.random = random;
		this.count = count;
	}
	
	@Override
	public List<SocialVertex> generate(SocialVertex ego) {
		List<SocialVertex> group = new LinkedList<SocialVertex>(ego.getNeighbours());
		
		while(group.size() > count) {
			group.remove(random.nextInt(group.size()));
		}
		
		group.add(ego);
		
		return group;
	}

}
