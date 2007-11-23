/* *********************************************************************** *
 * project: org.matsim.*
 * EventManager.java
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

package teach.multiagent07.simulation;

import java.util.ArrayList;
import java.util.List;

import teach.multiagent07.interfaces.EventHandlerI;
import teach.multiagent07.util.Event;


public class EventManager {

	private List<Event> events = new ArrayList<Event>();

	public void runHandler(EventHandlerI handler) {
		if (handler == null) return;
		for (Event event : events) {
			handler.handleEvent(event);
		}
	}

	public void addEvent(Event event) {
		events.add(event);
	}

	public void clearEvents() {
		events.clear();
	}
}
