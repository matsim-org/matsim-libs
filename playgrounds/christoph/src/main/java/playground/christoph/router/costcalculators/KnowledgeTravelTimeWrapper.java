/* *********************************************************************** *
 * project: org.matsim.*
 * KnowledgeTravelTimeWrapper.java
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

package playground.christoph.router.costcalculators;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.core.router.util.TravelTime;

import playground.christoph.knowledge.container.NodeKnowledge;
import playground.christoph.router.util.KnowledgeTools;

/*
 * Adds a new Feature to KnowledgeTravelTime:
 * The Knowledge of a Person can be taken in account. That means
 * that it is checked if a Person knows a Link or not. If not, the
 * returned TravelTime is Double.MAX_VALUE. 
 */
public class KnowledgeTravelTimeWrapper implements PersonalizableTravelTime {
	
	protected TravelTime travelTimeCalculator;
	protected boolean checkNodeKnowledge = true;
	protected KnowledgeTools knowledgeTools;
	protected Person person;
		
	public KnowledgeTravelTimeWrapper(TravelTime travelTime) {
		this.travelTimeCalculator = travelTime;
		
		this.knowledgeTools = new KnowledgeTools();
	}
	
	public void checkNodeKnowledge(boolean value) {
		this.checkNodeKnowledge = value;
	}
	
	@Override
	public void setPerson(Person person) {
		this.person = person;
		if (travelTimeCalculator instanceof PersonalizableTravelTime) {
			((PersonalizableTravelTime)travelTimeCalculator).setPerson(person);
		}
	}
	
	public double getLinkTravelTime(final Link link, final double time) 
	{	
		NodeKnowledge nodeKnowledge = null;
		
		if (checkNodeKnowledge && person != null) {
			// try getting NodeKnowledge from the Persons Knowledge
			nodeKnowledge = knowledgeTools.getNodeKnowledge(person);
		}
		
		// if the Person doesn't know the link -> return max costs 
		if (nodeKnowledge != null && !nodeKnowledge.knowsLink(link)) {
			return Double.MAX_VALUE;
		} else {
			return travelTimeCalculator.getLinkTravelTime(link, time);
		}
	}
}
