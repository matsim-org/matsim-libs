/* *********************************************************************** *
 * project: org.matsim.*
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
package playground.ikaddoura.analysis.welfare;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.population.Person;

/**
 * @author ikaddoura
 *
 */
public class StuckEventHandler implements PersonStuckEventHandler{
	
	private List<Id<Person>> stuckingAgentIds = new ArrayList<Id<Person>>();
	private int agentStuckEvents = 0;
	
	@Override
	public void reset(int iteration) {
		this.agentStuckEvents = 0;
		this.stuckingAgentIds.clear();
	}

	@Override
	public void handleEvent(PersonStuckEvent event) {
		agentStuckEvents++;
		
		if (!this.stuckingAgentIds.contains(event.getPersonId())){
			this.stuckingAgentIds.add(event.getPersonId());
		}
	}
	
	public int getAgentStuckEvents() {
		return agentStuckEvents;
	}

	public List<Id<Person>> getStuckingAgentIds() {
		return stuckingAgentIds;
	}
	
}
