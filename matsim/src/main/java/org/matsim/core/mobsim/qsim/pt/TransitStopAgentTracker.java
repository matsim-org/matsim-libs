/* *********************************************************************** *
 * project: org.matsim.*
 * TransitStop.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package org.matsim.core.mobsim.qsim.pt;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.AgentWaitingForPtEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.qsim.AgentTracker;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author mrieser
 */
public class TransitStopAgentTracker implements AgentTracker {

	private final static Logger log = LogManager.getLogger(TransitStopAgentTracker.class);
	
	private final EventsManager events;
	private final Map<Id<TransitStopFacility>, List<PTPassengerAgent>> agentsAtStops = new ConcurrentHashMap<>();

	public TransitStopAgentTracker(final EventsManager events) {
		this.events = events;
	}
	
	public void addAgentToStop(final double now, final PTPassengerAgent agent, final Id<TransitStopFacility> stopId) {
		if (stopId == null) {
			throw new NullPointerException("stop must not be null.");
		}
		List<PTPassengerAgent> agents = this.agentsAtStops.get(stopId);
		if (agents == null) {
			agents = new CopyOnWriteArrayList<>();// TODO check again. this might turn out to be slow, but we likely need something thread safe here. marcel/oct2014 
			this.agentsAtStops.put(stopId, agents);
		}
		if ( !agents.add(agent) ) {
			log.error("did NOT add agent " + agent.getId() + " since it was already there.");
		}
		Id<TransitStopFacility> destinationStopId = agent.getDesiredDestinationStopId();
		events.processEvent(new AgentWaitingForPtEvent(now, agent.getId(), stopId, destinationStopId));
	}

	public void removeAgentFromStop(final PTPassengerAgent agent, final Id<TransitStopFacility> stopId) {
		if (stopId == null) {
			throw new NullPointerException("stopId must not be null.");
		}
		List<PTPassengerAgent> agents = this.agentsAtStops.get(stopId);
		if (agents != null) {
			if (!agents.remove(agent)) {
				log.error("Agent " + agent.getId() + " could not be removed from waiting at stop " + stopId);
			}
		} else {
			log.error("Agent " + agent.getId() + " could not be removed from waiting at stop " + stopId + " since agents list was null.");
		}
	}

	@Override
	public List<PTPassengerAgent> getAgentsAtFacility(final Id<TransitStopFacility> stopId) {
		List<PTPassengerAgent> agents = this.agentsAtStops.get(stopId);
		if (agents == null) {
			return Collections.emptyList();
		}
		return Collections.unmodifiableList(agents);
	}

	public Map<Id<TransitStopFacility>, List<PTPassengerAgent>> getAgentsAtStop() {
		return this.agentsAtStops;
	}
}
