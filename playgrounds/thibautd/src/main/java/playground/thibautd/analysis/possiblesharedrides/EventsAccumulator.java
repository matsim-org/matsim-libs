/* *********************************************************************** *
 * project: org.matsim.*
 * EventsAccumulator.java
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
package playground.thibautd.analysis.possiblesharedrides;

import java.util.ArrayList;
import java.util.List;

import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;

/**
 * @author thibautd
 */
public class EventsAccumulator implements 
		LinkLeaveEventHandler,
		LinkEnterEventHandler,
		AgentDepartureEventHandler,
		AgentArrivalEventHandler {
	
	//private final List<AgentArrivalEvent> arrivals =
	//	new ArrayList<AgentArrivalEvent>(1000);
	//private final List<AgentDepartureEvent> departures =
	//	new ArrayList<AgentDepartureEvent>(1000);
	private final List<AgentEventWrapper> arrivals =
		new ArrayList<AgentEventWrapper>(1000);
	private final List<AgentEventWrapper> departures =
		new ArrayList<AgentEventWrapper>(1000);
	private final List<LinkEnterEvent> enterLinks =
		new ArrayList<LinkEnterEvent>(1000);
	private final List<LinkLeaveEvent> leaveLinks =
		new ArrayList<LinkLeaveEvent>(1000);

	/*
	 * =========================================================================
	 * event handler methods
	 * =========================================================================
	 */
	@Override
	public void reset(int iteration) {
		this.arrivals.clear();
		this.departures.clear();
		this.enterLinks.clear();
		this.leaveLinks.clear();
	}

	@Override
	public void handleEvent(AgentArrivalEvent event) {
		this.arrivals.add(new AgentEventWrapper(event));
	}

	@Override
	public void handleEvent(AgentDepartureEvent event) {
		this.departures.add(new AgentEventWrapper(event));
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		this.enterLinks.add(event);
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		this.leaveLinks.add(event);
	}

	/*
	 * =========================================================================
	 * getters
	 * =========================================================================
	 */

	public List<AgentEventWrapper> getArrivalEvents() {
		return this.arrivals;
	}

	public List<AgentEventWrapper> getDepartureEvents()  {
		return this.departures;
	}

	public List<LinkEnterEvent> getEnterLinkEvents() {
		return this.enterLinks;
	}

	public List<LinkLeaveEvent> getLeaveLinkEvents() {
		return this.leaveLinks;
	}

}

