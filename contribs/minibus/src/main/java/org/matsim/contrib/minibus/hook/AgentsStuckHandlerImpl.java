/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.contrib.minibus.hook;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.population.Person;

import java.util.Set;
import java.util.TreeSet;

/**
 * Collects all {@link PersonStuckEvent} and returns a set of the ids of the agents
 *
 * @author aneumann
 */
class AgentsStuckHandlerImpl implements PersonStuckEventHandler{

	private static final Logger log = Logger.getLogger(AgentsStuckHandlerImpl.class);

	private Set<Id<Person>> agentsStuck;

	public AgentsStuckHandlerImpl() {
		this.agentsStuck = new TreeSet<>();
		log.info("initialized");
	}

	@Override
	public void reset(int iteration) {
		this.agentsStuck = new TreeSet<>();
	}

	@Override
	public void handleEvent(PersonStuckEvent event) {
		this.agentsStuck.add(event.getPersonId());		
	}

	public Set<Id<Person>> getAgentsStuck() {
		log.info("Returning " + this.agentsStuck.size() + " agent ids");
		return this.agentsStuck;
	}
}