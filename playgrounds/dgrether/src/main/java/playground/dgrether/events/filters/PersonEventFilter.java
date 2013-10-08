/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.dgrether.events.filters;

import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.AgentStuckEvent;
import org.matsim.core.api.experimental.events.Wait2LinkEvent;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;


/**
 * @author dgrether
 *
 */
public class PersonEventFilter implements EventFilter {

	private Set<Id> personIds;

	public PersonEventFilter(final Set<Id> personIDs) {
		this.personIds = personIDs;
	}
	
	
	@Override
	public boolean doProcessEvent(Event event) {
		if (event instanceof LinkEnterEvent) {
			LinkEnterEvent e = (LinkEnterEvent) event;
			Id personId = e.getPersonId();
			this.personIds.contains(personId);
		} else if (event instanceof LinkLeaveEvent) {
			LinkLeaveEvent e = (LinkLeaveEvent) event;
			Id personId = e.getPersonId();
			this.personIds.contains(personId);
		} else if (event instanceof Wait2LinkEvent) {
			Wait2LinkEvent e = (Wait2LinkEvent) event;
			Id personId = e.getPersonId();
			this.personIds.contains(personId);
		} else if (event instanceof AgentDepartureEvent) {
			AgentDepartureEvent e = (AgentDepartureEvent) event;
			Id personId = e.getPersonId();
			this.personIds.contains(personId);
		} else if (event instanceof AgentArrivalEvent) {
			AgentArrivalEvent e = (AgentArrivalEvent) event;
			Id personId = e.getPersonId();
			this.personIds.contains(personId);
		} else if (event instanceof ActivityStartEvent) {
			ActivityStartEvent e = (ActivityStartEvent) event;
			Id personId = e.getPersonId();
			this.personIds.contains(personId);
		} else if (event instanceof ActivityEndEvent) {
			ActivityEndEvent e = (ActivityEndEvent) event;
			Id personId = e.getPersonId();
			this.personIds.contains(personId);
		} else if (event instanceof AgentStuckEvent) {
			AgentStuckEvent e = (AgentStuckEvent) event;
			Id personId = e.getPersonId();
			this.personIds.contains(personId);
		} else {
			return false;
		}
		return false;
	}
	
	
}
