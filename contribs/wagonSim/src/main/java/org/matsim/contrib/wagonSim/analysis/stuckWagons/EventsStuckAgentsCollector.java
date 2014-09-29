/* *********************************************************************** *
 * project: org.matsim.*                                                   *
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

/**
 * 
 */
package org.matsim.contrib.wagonSim.analysis.stuckWagons;

import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.wagonSim.WagonSimConstants;

/**
 * @author balmermi
 *
 */
public class EventsStuckAgentsCollector implements ActivityEndEventHandler, ActivityStartEventHandler {

	//////////////////////////////////////////////////////////////////////
	// variables
	//////////////////////////////////////////////////////////////////////

	private final Set<Id<Person>> stuckAgents;
	
	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////
	
	public EventsStuckAgentsCollector(Set<Id<Person>> stuckAgents) {
		this.stuckAgents = stuckAgents;
		if (this.stuckAgents == null) { throw new RuntimeException("stuckAgents Set is null. Bailing out."); }
	}

	//////////////////////////////////////////////////////////////////////
	// interface implementation
	//////////////////////////////////////////////////////////////////////
	
	@Override
	public void reset(int iteration) {
		this.stuckAgents.clear();
	}

	//////////////////////////////////////////////////////////////////////

	@Override
	public void handleEvent(ActivityStartEvent event) {
		if (event.getActType().equals(WagonSimConstants.DESTINATION)) {
			if (!stuckAgents.remove(event.getPersonId())) {
				throw new RuntimeException("Agent Id="+event.getPersonId()+" starts "+WagonSimConstants.DESTINATION+" activity without ending "+WagonSimConstants.ORIGIN+" activity. (At event: "+event.getAttributes().toString()+")");
			}
		}
	}

	//////////////////////////////////////////////////////////////////////

	@Override
	public void handleEvent(ActivityEndEvent event) {
		if (event.getActType().equals(WagonSimConstants.ORIGIN)) {
			stuckAgents.add(event.getPersonId());
		}
	}
}
