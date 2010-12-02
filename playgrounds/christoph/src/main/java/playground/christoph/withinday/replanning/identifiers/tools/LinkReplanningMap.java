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

package playground.christoph.withinday.replanning.identifiers.tools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.AgentStuckEvent;
import org.matsim.core.api.experimental.events.AgentWait2LinkEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentStuckEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentWait2LinkEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.PersonAgent;
import org.matsim.core.mobsim.framework.events.SimulationInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.SimulationInitializedListener;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.ptproject.qsim.interfaces.NetsimLink;
import org.matsim.ptproject.qsim.interfaces.Mobsim;
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
	private boolean repeatedReplanning = true;
	private double replanningInterval = 300.0;

	private NetsimNetwork netsimNetwork;

	/*
	 * Mapping between the PersonDriverAgents and the PersonIds.
	 * The events only contain a PersonId.
	 */
	private Map<Id, PersonAgent> personAgentMapping;	// PersonId, PersonDriverAgent

	private Map<Id, Tuple<Id, Double>> replanningMap;	// PersonId, Tuple<LinkId, ReplanningTime>

	// simulationListeners... the List used in the Controller!
//	public LinkReplanningMap(EventsManager eventsManager, List<SimulationListener> simulationListeners) {
//		eventsManager.addHandler(this);
//		simulationListeners.add(this);
//		init();
//	}
	
	public LinkReplanningMap(EventsManager eventsManager) {
		log.warn("LinkReplanningMap is initialized without a MobSim. " +
			"Please ensure that it is added as a Listener to an ObserableSimulation!");
		eventsManager.addHandler(this);
		init();
	}
	
	public LinkReplanningMap(EventsManager eventsManager, Mobsim sim) {
		eventsManager.addHandler(this);
		sim.addQueueSimulationListeners(this);
		init();
	}
	
	private void init() {
		this.replanningMap = new HashMap<Id, Tuple<Id, Double>>();
	}

	@Override
	public void notifySimulationInitialized(SimulationInitializedEvent e) {

		Mobsim sim = (Mobsim) e.getQueueSimulation();

		// Update Reference to QNetwork
		this.netsimNetwork = sim.getNetsimNetwork();

		personAgentMapping = new HashMap<Id, PersonAgent>();

		if (sim instanceof QSim) {
			for (MobsimAgent mobsimAgent : ((QSim)sim).getAgents()) {
				if (mobsimAgent instanceof PersonAgent) {
					PersonAgent personAgent = (PersonAgent) mobsimAgent;
					personAgentMapping.put(personAgent.getId(), personAgent);
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

	public synchronized List<PersonAgent> getReplanningAgents(double time) {
		// using the ArrayList is just a Workaround...
		ArrayList<PersonAgent> agentsToReplanLeaveLink = new ArrayList<PersonAgent>();

		Iterator<Entry<Id, Tuple<Id, Double>>> entries = replanningMap.entrySet().iterator();
		while (entries.hasNext()) {
			Entry<Id, Tuple<Id, Double>> entry = entries.next();
			Id personId = entry.getKey();
			Id linkId = entry.getValue().getFirst();

			double replanningTime = entry.getValue().getSecond();

			if (time >= replanningTime) {
				PersonAgent personAgent = this.personAgentMapping.get(personId);

				// Repeated Replanning per Link possible?
				if (repeatedReplanning) entry.setValue(new Tuple<Id,Double>(linkId, time + this.replanningInterval));
				else entries.remove();

				agentsToReplanLeaveLink.add(personAgent);
			}
		}

//		log.info(time + ": replanning " + vehiclesToReplanLeaveLink.size() + " vehicles");

		return agentsToReplanLeaveLink;
	}

	/*
	 * Returns a List of all Agents, that are currently performing an Activity.
	 */
	public synchronized List<PersonAgent> getLegPerformingAgents() {
		ArrayList<PersonAgent> legPerformingAgents = new ArrayList<PersonAgent>();

		for (Entry<Id, Tuple<Id, Double>> entry : replanningMap.entrySet()) {
			Id personId = entry.getKey();
			PersonAgent agent = this.personAgentMapping.get(personId);
			legPerformingAgents.add(agent);
		}

		return legPerformingAgents;
	}

	@Override
	public void reset(int iteration) {
		replanningMap = new HashMap<Id, Tuple<Id, Double>>();
	}

}
