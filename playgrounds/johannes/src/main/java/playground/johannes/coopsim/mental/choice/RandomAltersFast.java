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

import org.matsim.contrib.socnetgen.sna.graph.social.SocialVertex;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author illenberger
 *
 */
public class RandomAltersFast implements ActivityGroupGenerator {

	private final Random random;
	
	private final double proba;
	
	private final double min;
	
	public RandomAltersFast(double proba, Random random) {
		this(proba, 0, random);
	}
	
	public RandomAltersFast(double proba, int min, Random random) {
		this.proba = proba;
		this.random = random;
		this.min = min;
	}
	
	@Override
	public List<SocialVertex> generate(SocialVertex ego) {
		List<SocialVertex> group = new ArrayList<SocialVertex>(ego.getNeighbours().size() + 1);
		group.add(ego);
		
		if(ego.getNeighbours().isEmpty())
			return group;
		
		int n = (int) Math.round(ego.getNeighbours().size() * proba);
		n = (int) Math.max(n, min);
		
		while(group.size() < n + 1) {
			SocialVertex alter = ego.getNeighbours().get(random.nextInt(ego.getNeighbours().size()));
			if(!group.contains(alter))
				group.add(alter);
		}
		
		return group;
	}

}
