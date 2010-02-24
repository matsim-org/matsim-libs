/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.mrieser.scenarioFilter;

import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.ActivityEvent;
import org.matsim.core.api.experimental.events.AgentEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.LinkEvent;
import org.matsim.core.events.handler.ActivityEventHandler;
import org.matsim.core.events.handler.AgentEventHandler;
import org.matsim.core.events.handler.LinkEventHandler;

/**
 * Finds all {@link LinkEvent}s, {@link AgentEvent}s and {@link ActivityEvent}s
 * which take place on one of a set of predefined links and sends them to
 * another EventsManger for further processing. Links of different type or with
 * a non-matching link are discarded.
 *
 * @author mrieser
 */
public class EventsFilterByLink implements LinkEventHandler, AgentEventHandler, ActivityEventHandler {

	private final EventsManager em;
	private final Set<Id> linkIds;

	public EventsFilterByLink(final EventsManager em, final Set<Id> linkIds) {
		this.em = em;
		this.linkIds = linkIds;
	}

	@Override
	public void handleEvent(LinkEvent event) {
		if (this.linkIds.contains(event.getLinkId())) {
			em.processEvent(event);
		}
	}

	@Override
	public void handleEvent(AgentEvent event) {
		if (this.linkIds.contains(event.getLinkId())) {
			em.processEvent(event);
		}
	}

	@Override
	public void handleEvent(ActivityEvent event) {
		if (this.linkIds.contains(event.getLinkId())) {
			em.processEvent(event);
		}
	}

	@Override
	public void reset(int iteration) {
		// nothing to do
	}

}
