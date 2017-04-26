/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.michalm.util;

import java.util.*;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.events.handler.EventHandler;

public class MovingAgentsRegister
		implements EventHandler, PersonDepartureEventHandler, PersonStuckEventHandler, PersonArrivalEventHandler {
	private Map<Id<Person>, PersonDepartureEvent> movingAgentsMap = new HashMap<>();

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		movingAgentsMap.put(event.getPersonId(), event);
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		movingAgentsMap.remove(event.getPersonId());
	}

	@Override
	public void handleEvent(PersonStuckEvent event) {
		// throw new RuntimeException();
		System.err.println("AgentStuckEvent:");
		System.err.println(event);
	}

	public Set<Id<Person>> getMovingAgentIds() {
		return movingAgentsMap.keySet();
	}

	@Override
	public void reset(int iteration) {
		movingAgentsMap.clear();
	}

	private static final MovingAgentsRegister MOVING_AGENTS_REGISTER = new MovingAgentsRegister();

	public static AbstractModule createModule() {
		return new AbstractModule() {
			public void install() {
				addEventHandlerBinding().toInstance(MOVING_AGENTS_REGISTER);
			}
		};
	}
}
