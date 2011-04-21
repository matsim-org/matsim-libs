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
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
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
import org.matsim.ptproject.qsim.agents.WithinDayAgent;
import org.matsim.ptproject.qsim.comparators.PersonAgentComparator;
import org.matsim.ptproject.qsim.interfaces.NetsimLink;
import org.matsim.ptproject.qsim.interfaces.Netsim;
import org.matsim.ptproject.qsim.interfaces.NetsimNetwork;

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
public class LinkReplanningMap implements LinkEnterEventHandler,
		LinkLeaveEventHandler, AgentArrivalEventHandler,
		AgentDepartureEventHandler, AgentWait2LinkEventHandler,
		AgentStuckEventHandler, SimulationInitializedListener {

	private static final Logger log = Logger.getLogger(LinkReplanningMap.class);

	// Repeated replanning if a person gets stuck in a Link
	private boolean repeatedReplanning = false;
	private double replanningInterval = 300.0;

	private NetsimNetwork netsimNetwork;

	/*
	 * Mapping between the PersonDriverAgents and the PersonIds.
	 * The events only contain a PersonId.
	 */
	private Map<Id, WithinDayAgent> personAgentMapping;	// PersonId, PersonDriverAgent

	private Map<Id, Tuple<Id, Double>> replanningMap;	// PersonId, Tuple<LinkId, ReplanningTime>
	
	public LinkReplanningMap() {
		log.info("Note that the LinkReplanningMap has to be registered as an EventHandler and a SimulationListener!");
		init();
	}
		
	private void init() {
		this.replanningMap = new HashMap<Id, Tuple<Id, Double>>();
	}

	@Override
	public void notifySimulationInitialized(SimulationInitializedEvent e) {

		Netsim sim = (Netsim) e.getQueueSimulation();

		// Update Reference to QNetwork
		this.netsimNetwork = sim.getNetsimNetwork();

		personAgentMapping = new HashMap<Id, WithinDayAgent>();

		if (sim instanceof QSim) {
			for (MobsimAgent mobsimAgent : ((QSim)sim).getAgents()) {
				if (mobsimAgent instanceof WithinDayAgent) {
					WithinDayAgent withinDayAgent = (WithinDayAgent) mobsimAgent;
					personAgentMapping.put(withinDayAgent.getId(), withinDayAgent);
				}
			}
		}
	}

	// set the earliest possible leave link time as replanning time
	@Override
	public void handleEvent(LinkEnterEvent event) {
		double now = event.getTime();
		NetsimLink qLink = netsimNetwork.getNetsimLink(event.getLinkId());
		double departureTime = (now + ((LinkImpl)qLink.getLink()).getFreespeedTravelTime(now));

		replanningMap.put(event.getPersonId(), new Tuple<Id, Double>(event.getLinkId(), departureTime));
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		replanningMap.remove(event.getPersonId());
	}

	@Override
	public void handleEvent(AgentArrivalEvent event) {
		replanningMap.remove(event.getPersonId());
	}

	/*
	 * The agent has ended an activity and returns to the network.
	 * We do a replanning so the agent can choose his next link.
	 */
	@Override
	public void handleEvent(AgentDepartureEvent event) {
		replanningMap.put(event.getPersonId(), new Tuple<Id, Double>(event.getLinkId(), event.getTime()));
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
	}

	/*
	 * returns a List of Agents who might need a replanning
	 */
	public synchronized Set<WithinDayAgent> getReplanningAgents(double time) {
		Set<WithinDayAgent> agentsToReplanLeaveLink = new TreeSet<WithinDayAgent>(new PersonAgentComparator());
		
		Iterator<Entry<Id, Tuple<Id, Double>>> entries = replanningMap.entrySet().iterator();
		while (entries.hasNext()) {
			Entry<Id, Tuple<Id, Double>> entry = entries.next();
			Id personId = entry.getKey();
			Id linkId = entry.getValue().getFirst();

			double replanningTime = entry.getValue().getSecond();

			if (time >= replanningTime) {
				WithinDayAgent withinDayAgent = this.personAgentMapping.get(personId);

				// Repeated Replanning per Link possible?
				if (repeatedReplanning) entry.setValue(new Tuple<Id,Double>(linkId, time + this.replanningInterval));
				else entries.remove();

				agentsToReplanLeaveLink.add(withinDayAgent);
			}
		}

//		log.info(time + ": replanning " + vehiclesToReplanLeaveLink.size() + " vehicles");

		return agentsToReplanLeaveLink;
	}

	/*
	 * Returns a List of all Agents, that are currently performing a Leg.
	 */
	public synchronized Set<WithinDayAgent> getLegPerformingAgents() {
		Set<WithinDayAgent> legPerformingAgents = new TreeSet<WithinDayAgent>(new PersonAgentComparator());

		for (Entry<Id, Tuple<Id, Double>> entry : replanningMap.entrySet()) {
			Id personId = entry.getKey();
			WithinDayAgent agent = this.personAgentMapping.get(personId);
			legPerformingAgents.add(agent);
		}

		return legPerformingAgents;
	}

	@Override
	public void reset(int iteration) {
		replanningMap = new HashMap<Id, Tuple<Id, Double>>();
	}

}
