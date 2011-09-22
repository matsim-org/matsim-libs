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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import playground.johannes.socialnetworks.graph.social.SocialVertex;

/**
 * @author illenberger
 *
 */
public class RandomAlters implements ActivityGroupGenerator {

	private final Random random;
	
	private final double proba;
	
	public RandomAlters(double proba, Random random) {
		this.proba = proba;
		this.random = random;
	}
	
	@Override
	public List<SocialVertex> generate(SocialVertex ego) {
		List<SocialVertex> group = new ArrayList<SocialVertex>(ego.getNeighbours().size() + 1);
		group.add(ego);
		
		for(int i = 0; i < ego.getNeighbours().size(); i++) {
			if(random.nextDouble() < proba) {
				group.add(ego.getNeighbours().get(i));
			}
		}
		
		return group;
	}

}
