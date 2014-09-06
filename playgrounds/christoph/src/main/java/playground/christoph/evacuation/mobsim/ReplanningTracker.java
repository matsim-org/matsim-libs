/* *********************************************************************** *
 * project: org.matsim.*
 * ReplanningTracker.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.christoph.evacuation.mobsim;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.misc.Time;
import org.matsim.withinday.events.ReplanningEvent;
import org.matsim.withinday.events.handler.ReplanningEventHandler;

import playground.christoph.evacuation.events.PersonInformationEvent;
import playground.christoph.evacuation.events.handler.PersonInformationEventHandler;

/**
 * Tracks the agents' replanning steps. Moreover, it is ensured, that each agent
 * performs an initial replanning.
 * 
 * @author cdobler
 */
public class ReplanningTracker implements ReplanningEventHandler, PersonInformationEventHandler {

	private final InformedAgentsTracker informedAgentsTracker;
	private final Set<Id<Person>> replannedAgents = new HashSet<>();
	private final Set<Id<Person>> informedButNotInitiallyReplannedAgents = new HashSet<>();
	
	public ReplanningTracker(InformedAgentsTracker informedAgentsTracker) {
		this.informedAgentsTracker = informedAgentsTracker;
	}
	
	public boolean hasAgentBeenInitiallyReplanned(Id<Person> agentId) {
		return this.replannedAgents.contains(agentId);
	}

	public boolean allAgentsInitiallyReplanned() {
		return this.replannedAgents.size() == this.informedAgentsTracker.totalAgents;
	}
	
	public Set<Id<Person>> getInformedButNotInitiallyReplannedAgents() {
		return this.informedButNotInitiallyReplannedAgents;
	}

	public Set<Id<Person>> getInformedAndInitiallyReplannedAgents() {
		return this.replannedAgents;
	}
	
	@Override
	public void handleEvent(PersonInformationEvent event) {

		this.informedButNotInitiallyReplannedAgents.add(event.getPersonId());
	}
	
	@Override
	public void handleEvent(ReplanningEvent event) {
		
		/*
		 * Skip events where time is undefined - they are created by an
		 * InitialWithinDayReplanner which is run before the simulation has started.
		 */
		if (event.getTime() == Time.UNDEFINED_TIME) return;
		
		/*
		 * Adding an element to the set returns false, if the element is already contained
		 * in the set.
		 */
		boolean initialReplanned = this.replannedAgents.add(event.getPersonId());
		
		this.informedButNotInitiallyReplannedAgents.remove(event.getPersonId());
		
		/*
		 * Consistency check.
		 */
		if (!this.informedAgentsTracker.isAgentInformed(event.getPersonId())) {
			throw new RuntimeException("Agent " + event.getPersonId().toString() + " has been "
					+ "replanned but is not informed. Aborting!");
		}
	}
		
	@Override
	public void reset(int iteration) {
		this.replannedAgents.clear();
		this.informedButNotInitiallyReplannedAgents.clear();
	}

}
