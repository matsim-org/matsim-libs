/* *********************************************************************** *
 * project: org.matsim.*
 * RandomSelector.java
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

/**
 * 
 */
package playground.johannes.socialnetworks.interaction;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * @author illenberger
 *
 */
public class RandomSelector implements InteractionSelector {

	private int maxInteractions;
	
	private Random random;

	public RandomSelector(int maxInteractions, long randomSeed) {
		this.maxInteractions = maxInteractions;
		random = new Random(randomSeed);
	}
	
	public Collection<Visitor> select(Visitor v, Collection<Visitor> choiceSet) {
		List<Visitor> targets = new LinkedList<Visitor>();
		
		if(choiceSet.isEmpty())
			return targets;
		
		if(maxInteractions == 1)
			targets.add(selectSingleTarget(v, choiceSet));
		else if(maxInteractions > 1)
			throw new UnsupportedOperationException("Not implemented yet!");
		
		return targets;
	}
	
	private Visitor selectSingleTarget(Visitor v, Collection<Visitor> choiceSet) {
		List<Visitor> visitors = new LinkedList<Visitor>(choiceSet);
		return visitors.get(random.nextInt(choiceSet.size()));
	}

}
