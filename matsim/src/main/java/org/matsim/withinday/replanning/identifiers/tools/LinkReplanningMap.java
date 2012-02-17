/* *********************************************************************** *
 * project: org.matsim.*
 * LinkReplanningMap.java
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

package org.matsim.withinday.replanning.identifiers.tools;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.AgentStuckEvent;
import org.matsim.core.api.experimental.events.AgentWait2LinkEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentStuckEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentWait2LinkEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.events.SimulationInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.SimulationInitializedListener;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.ptproject.qsim.agents.PlanBasedWithinDayAgent;
import org.matsim.ptproject.qsim.comparators.PersonAgentComparator;
import org.matsim.ptproject.qsim.interfaces.Mobsim;

/**
 * This Module is used by a CurrentLegReplanner. It calculates the time
 * when an agent should do CurrentLegReplanning.
 * <p/>
 * The time is estimated as following:
 * When a LinkEnterEvent is thrown the Replanning Time is set to
 * the current time + the FreeSpeed Travel Time. This guarantees that
 * the replanning will be done while the agent is on the Link.
  * <p/>
 * Additionally a Replanning Interval can be set. This allows an Agent
 * to do multiple Replanning on a single Link. This may be useful if the
 * Traffic System is congested and the Link Travel Times are much longer
 * than the Freespeed Travel Times.
 */
public class LinkReplanningMap implements LinkEnterEventHandler, LinkLeaveEventHandler, 
		AgentArrivalEventHandler, AgentDepartureEventHandler, AgentWait2LinkEventHandler,
		AgentStuckEventHandler, SimulationInitializedListener {

	private static final Logger log = Logger.getLogger(LinkReplanningMap.class);

	// Repeated replanning if a person gets stuck in a Link
	private boolean repeatedReplanning = true;
	private double replanningInterval = 300.0;

	private Network network;

	/*
	 * Mapping between the PersonDriverAgents and the PersonIds.
	 * The events only contain a PersonId.
	 */
	private final Map<Id, PlanBasedWithinDayAgent> personAgentMapping;	// PersonId, PersonDriverAgent

	private final Map<Id, Tuple<Id, Double>> replanningMap;	// PersonId, Tuple<LinkId, ReplanningTime>
	
	private final Set<String> observedModes;
	
	private final Set<Id> enrouteAgents;
	
	private final Map<Id, String> agentTransportModeMap;
	
	public LinkReplanningMap() {
		log.info("Note that the LinkReplanningMap has to be registered as an EventHandler and a SimulationListener!");

		this.enrouteAgents = new HashSet<Id>();
		this.replanningMap = new HashMap<Id, Tuple<Id, Double>>();
		this.personAgentMapping = new HashMap<Id, PlanBasedWithinDayAgent>();
		this.agentTransportModeMap = new HashMap<Id, String>();
		this.observedModes = new HashSet<String>();
		this.addObservedMode(TransportMode.car);
	}
	
	public void addObservedMode(String mode) {
		this.observedModes.add(mode);
	}
	
	public void setObservedModes(Set<String> modes) {
		this.observedModes.clear();
		this.observedModes.addAll(modes);
	}

	public void doRepeatedReplanning(boolean value) {
		this.repeatedReplanning = value;
	}
	
	public boolean isRepeatedReplanning() {
		return this.repeatedReplanning;
	}
	
	public void setRepeatedReplanningInterval(double interval) {
		this.replanningInterval = interval;
	}
	
	public double getReplanningInterval() {
		return this.replanningInterval;
	}
	
	@Override
	public void notifySimulationInitialized(SimulationInitializedEvent e) {

		Mobsim sim = (Mobsim) e.getQueueSimulation();

		// Update Reference to network
		this.network = sim.getScenario().getNetwork();

		personAgentMapping.clear();

		if (sim instanceof QSim) {
			for (MobsimAgent mobsimAgent : ((QSim)sim).getAgents()) {
				if (mobsimAgent instanceof PlanBasedWithinDayAgent) {
					PlanBasedWithinDayAgent withinDayAgent = (PlanBasedWithinDayAgent) mobsimAgent;
					personAgentMapping.put(withinDayAgent.getId(), withinDayAgent);
				}
			}
		}
	}

	// set the earliest possible leave link time as replanning time
	@Override
	public void handleEvent(LinkEnterEvent event) {
		String mode = agentTransportModeMap.get(event.getPersonId());
		if (observedModes.contains(mode)) {
			double now = event.getTime();
			Link link = network.getLinks().get(event.getLinkId());
			double departureTime = Math.floor((now + ((LinkImpl) link).getFreespeedTravelTime(now)));
			
			replanningMap.put(event.getPersonId(), new Tuple<Id, Double>(event.getLinkId(), departureTime));
		}
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		replanningMap.remove(event.getPersonId());
	}

	@Override
	public void handleEvent(AgentArrivalEvent event) {
		replanningMap.remove(event.getPersonId());
		agentTransportModeMap.remove(event.getPersonId());
		enrouteAgents.remove(event.getPersonId());
	}

	/*
	 * The agent has ended an activity and returns to the network.
	 * We do a replanning so the agent can choose his next link.
	 * 
	 * We don't do this anymore since the agent is limited in its 
	 * replanning capabilities on the link he is departing to. 
 	 * It is e.g. not possible, to schedule a new activity there. 
	 */
	@Override
	public void handleEvent(AgentDepartureEvent event) {
		agentTransportModeMap.put(event.getPersonId(), event.getLegMode());
		if (observedModes.contains(event.getLegMode())) {
			this.enrouteAgents.add(event.getPersonId());
		}		
	}

	/*
	 * Person is added directly to the Buffer Queue so we don't need a
	 * time offset here.
	 *
	 * At the moment we use the DepartureEvent to add an Agent
	 * to the replanningMap. Otherwise situations could occur where
	 * an Agent is not performing an Activity but is also not
	 * performing a Leg.
	 */
	@Override
	public void handleEvent(AgentWait2LinkEvent event) {
//		replanningMap.put(event.getPersonId(), new Tuple<Id, Double>(event.getLinkId(), event.getTime()));
	}

	@Override
	public void handleEvent(AgentStuckEvent event) {
		replanningMap.remove(event.getPersonId());
		agentTransportModeMap.remove(event.getPersonId());
		enrouteAgents.remove(event.getPersonId());
	}

	/*
	 * returns a List of Agents who might need a replanning
	 */
	public synchronized Set<PlanBasedWithinDayAgent> getReplanningAgents(double time) {
		Set<PlanBasedWithinDayAgent> agentsToReplanLeaveLink = new TreeSet<PlanBasedWithinDayAgent>(new PersonAgentComparator());
		
		Iterator<Entry<Id, Tuple<Id, Double>>> entries = replanningMap.entrySet().iterator();
		while (entries.hasNext()) {
			Entry<Id, Tuple<Id, Double>> entry = entries.next();
			Id personId = entry.getKey();
			Id linkId = entry.getValue().getFirst();

			double replanningTime = entry.getValue().getSecond();

			if (time >= replanningTime) {
				PlanBasedWithinDayAgent withinDayAgent = this.personAgentMapping.get(personId);

				// Repeated Replanning per Link possible?
				if (repeatedReplanning) entry.setValue(new Tuple<Id,Double>(linkId, time + this.replanningInterval));
				else entries.remove();

				agentsToReplanLeaveLink.add(withinDayAgent);
			}
		}

		return agentsToReplanLeaveLink;
	}

	/*
	 * Returns a List of all Agents, that are currently performing a Leg.
	 */
	public synchronized Set<PlanBasedWithinDayAgent> getLegPerformingAgents() {
		Set<PlanBasedWithinDayAgent> legPerformingAgents = new TreeSet<PlanBasedWithinDayAgent>(new PersonAgentComparator());

		for (Id id : this.enrouteAgents) {
			PlanBasedWithinDayAgent agent = this.personAgentMapping.get(id);
			legPerformingAgents.add(agent);			
		}

		return legPerformingAgents;
	}

	@Override
	public void reset(int iteration) {
		this.replanningMap.clear();
		this.enrouteAgents.clear();
	}

}
