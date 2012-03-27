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
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
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
import org.matsim.ptproject.qsim.QSim;
import org.matsim.ptproject.qsim.agents.PlanBasedWithinDayAgent;
import org.matsim.ptproject.qsim.comparators.PersonAgentComparator;
import org.matsim.ptproject.qsim.interfaces.Mobsim;
import org.matsim.ptproject.qsim.multimodalsimengine.router.util.MultiModalTravelTime;

/**
 * This Module is used by a CurrentLegReplanner. It calculates the time
 * when an agent should do CurrentLegReplanning.
 * <p/>
 * The time is estimated as following:
 * When a LinkEnterEvent is thrown the Replanning Time is set to
 * the current time + the FreeSpeed Travel Time. This guarantees that
 * the replanning will be done while the agent is on the Link. After that
 * time, the agent might be already in the outgoing queue of a QLink
 * where not all replanning operations are possible anymore (the agent
 * can e.g. not insert an Activity on its current link anymore).
 * <p/>
 * <p>
 * The replanning interval (multiple replannings on the same link when
 * an agent is stuck on a link due to a traffic jam) has been removed
 * since it cannot be guaranteed that all replanning operations are
 * valid anymore.
 * </p>
 * 
 * @author cdobler
 */
public class LinkReplanningMap implements LinkEnterEventHandler, LinkLeaveEventHandler, 
		AgentArrivalEventHandler, AgentDepartureEventHandler, AgentWait2LinkEventHandler,
		AgentStuckEventHandler, SimulationInitializedListener {

	private static final Logger log = Logger.getLogger(LinkReplanningMap.class);

	private final Network network;
	private final MultiModalTravelTime multiModalTravelTime;

	/*
	 * EXACT... replanning is scheduled for the current time step (time == replanning time)
	 * RESTRICTED ... available replanning operations are restricted (time > replanning time)
	 * UNRESTRICTED ... replanning operations are not restricted (time <= replanning time)
	 */
	private enum TimeFilterMode {
		EXACT, RESTRICTED, UNRESTRICTED
	}
	
	/*
	 * Mapping between the PersonDriverAgents and the PersonIds.
	 * The events only contain a PersonId.
	 */
	private final Map<Id, PlanBasedWithinDayAgent> personAgentMapping;	// PersonId, PersonDriverAgent

	private final Map<Id, Double> replanningMap;	// PersonId, ReplanningTime
	
	private final Set<Id> enrouteAgents;
	
	private final Map<Id, String> agentTransportModeMap;
	
	public LinkReplanningMap(Network network) {
		this(network, null);
		log.info("Note: no MultiModalTravelTime object given. Therefore use free speed car travel time as " +
				"minimal link travel time for all modes.");
	}
	
	public LinkReplanningMap(Network network, MultiModalTravelTime multiModalTravelTime) {
		log.info("Note that the LinkReplanningMap has to be registered as an EventHandler and a SimulationListener!");

		this.network = network;
		this.multiModalTravelTime = multiModalTravelTime;
		
		this.enrouteAgents = new HashSet<Id>();
		this.replanningMap = new HashMap<Id, Double>();
		this.personAgentMapping = new HashMap<Id, PlanBasedWithinDayAgent>();
		this.agentTransportModeMap = new HashMap<Id, String>();
	}
	
	@Override
	public void notifySimulationInitialized(SimulationInitializedEvent e) {
		Mobsim sim = (Mobsim) e.getQueueSimulation();
		
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

		double now = event.getTime();
		Link link = network.getLinks().get(event.getLinkId());
		
		double departureTime;
		if (this.multiModalTravelTime != null) {
			Person person = this.personAgentMapping.get(event.getPersonId()).getSelectedPlan().getPerson();
			multiModalTravelTime.setPerson(person);
			double travelTime = multiModalTravelTime.getModalLinkTravelTime(link, now, mode);
			departureTime = Math.floor(now + travelTime);				
		} else {
			departureTime = Math.floor((now + ((LinkImpl) link).getFreespeedTravelTime(now)));
		}
		
		replanningMap.put(event.getPersonId(), departureTime);
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
 	 * Instead we create an entry in the replanning map that
 	 * says that the agents should perform a replanning in the
 	 * current time step. Doing so allows to identify the agent
 	 * as performing a leg and being allowed to perform a restricted
 	 * set of replanning operations.
	 */
	@Override
	public void handleEvent(AgentDepartureEvent event) {
		this.agentTransportModeMap.put(event.getPersonId(), event.getLegMode());
		this.replanningMap.put(event.getPersonId(), event.getTime());
		this.enrouteAgents.add(event.getPersonId());	
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
		enrouteAgents.remove(event.getPersonId());
		agentTransportModeMap.remove(event.getPersonId());
	}
	
	@Override
	public void reset(int iteration) {
		this.replanningMap.clear();
		this.enrouteAgents.clear();
		this.agentTransportModeMap.clear();
	}
	
	/**
	 * @param time
	 * @return a list of agents who might need a replanning
	 */
	public Set<PlanBasedWithinDayAgent> getReplanningAgents(final double time) {
		Set<String> transportModes = null;
		return this.getReplanningAgents(time, transportModes);
	}

	/**
	 * @param time
	 * @param transportMode
	 * @return a list of agents who might need a replanning and use the given transport mode
	 */
	public Set<PlanBasedWithinDayAgent> getReplanningAgents(final double time, final String transportMode) {
		Set<String> transportModes = new HashSet<String>();
		transportModes.add(transportMode);
		return this.getReplanningAgents(time, transportModes);
	}
	
	/**
	 * @param time
	 * @param transportModes
	 * @return a list of agents who might need a replanning and use one of the given transport modes
	 */
	public Set<PlanBasedWithinDayAgent> getReplanningAgents(final double time, final Set<String> transportModes) {
		return this.filterAgents(time, transportModes, TimeFilterMode.EXACT);
	}

	/**
	 * @param time
	 * @return a list of agents who might need an unrestricted replanning and use the given transport mode
	 */
	public Set<PlanBasedWithinDayAgent> getUnrestrictedReplanningAgents(final double time) {
		Set<String> transportModes = null;
		return this.getUnrestrictedReplanningAgents(time, transportModes);
	}
	
	/**
	 * @param time
	 * @param transportMode
	 * @return a list of agents who might need an unrestricted replanning and use the given transport mode
	 */
	public Set<PlanBasedWithinDayAgent> getUnrestrictedReplanningAgents(final double time, final String transportMode) {
		Set<String> transportModes = new HashSet<String>();
		transportModes.add(transportMode);
		return this.getUnrestrictedReplanningAgents(time, transportModes);
	}
	
	/**
	 * @param time
	 * @param transportModes
	 * @return a list of agents who might need an unrestricted replanning and use one of the given transport modes
	 */
	public Set<PlanBasedWithinDayAgent> getUnrestrictedReplanningAgents(final double time, final Set<String> transportModes) {
		return this.filterAgents(time, transportModes, TimeFilterMode.UNRESTRICTED);
	}
	
	/**
	 * @param time
	 * @return a list of agents who might need a restricted replanning and use the given transport mode
	 */
	public Set<PlanBasedWithinDayAgent> getRestrictedReplanningAgents(final double time) {
		Set<String> transportModes = null;
		return this.getRestrictedReplanningAgents(time, transportModes);
	}
	
	/**
	 * @param time
	 * @param transportMode
	 * @return a list of agents who might need a restricted replanning and use the given transport mode
	 */
	public Set<PlanBasedWithinDayAgent> getRestrictedReplanningAgents(final double time, final String transportMode) {
		Set<String> transportModes = new HashSet<String>();
		transportModes.add(transportMode);
		return this.getRestrictedReplanningAgents(time, transportModes);
	}
	
	/**
	 * @param time
	 * @param transportModes
	 * @return a list of agents who might need a restricted replanning and use one of the given transport modes
	 */
	public Set<PlanBasedWithinDayAgent> getRestrictedReplanningAgents(final double time, final Set<String> transportModes) {
		return this.filterAgents(time, transportModes, TimeFilterMode.RESTRICTED);
	}
	
	private Set<PlanBasedWithinDayAgent> filterAgents(final double time, final Set<String> transportModes, final TimeFilterMode timeMode) {
		Set<PlanBasedWithinDayAgent> set = new TreeSet<PlanBasedWithinDayAgent>(new PersonAgentComparator());
		
		Iterator<Entry<Id, Double>> entries = replanningMap.entrySet().iterator();
		while (entries.hasNext()) {
			Entry<Id, Double> entry = entries.next();
			Id personId = entry.getKey();

			double replanningTime = entry.getValue();

			// check time
			if (timeMode == TimeFilterMode.EXACT) {
				if (time != replanningTime) continue;				
			} else if (timeMode == TimeFilterMode.RESTRICTED) {
				if (time <= replanningTime) continue;
			} else if (timeMode == TimeFilterMode.UNRESTRICTED) {
				if (time > replanningTime) continue;
			}

			// check transport mode
			if (transportModes != null) {
				String mode = this.agentTransportModeMap.get(personId);
				if (!transportModes.contains(mode)) continue;
			}
			
			// non of the checks fails therefore add agent to the replanning set
			PlanBasedWithinDayAgent withinDayAgent = this.personAgentMapping.get(personId);
			set.add(withinDayAgent);	
		}

		return set;
	}
	
	/**
	 * @return A list of all agents that are currently performing a leg. Note that
	 * some of them might be limited in the available replanning operations! 
	 */
	public Set<PlanBasedWithinDayAgent> getLegPerformingAgents() {
		Set<String> transportModes = null;
		return this.getLegPerformingAgents(transportModes);
	}
	
	/**
	 * @param transportMode
	 * @return A list of all agents that are currently performing a leg with the
	 * given transport mode. Note that some of them might be limited in the available 
	 * replanning operations! 
	 */
	public Set<PlanBasedWithinDayAgent> getLegPerformingAgents(final String transportMode) {
		Set<String> transportModes = new HashSet<String>();
		transportModes.add(transportMode);
		return this.getLegPerformingAgents(transportModes);
	}

	/**
	 * @param transportModes
	 * @return A list of all agents that are currently performing a leg with the
	 * given transport mode. Note that some of them might be limited in the available 
	 * replanning operations! 
	 */
	public Set<PlanBasedWithinDayAgent> getLegPerformingAgents(final Set<String> transportModes) {
		Set<PlanBasedWithinDayAgent> legPerformingAgents = new TreeSet<PlanBasedWithinDayAgent>(new PersonAgentComparator());
		
		for (Id id : this.enrouteAgents) {
			
			// check transport mode
			if (transportModes != null) {
				String mode = this.agentTransportModeMap.get(id);
				if (!transportModes.contains(mode)) continue;
			}
			
			// the check did not fail therefore add agent to the replanning set
			PlanBasedWithinDayAgent agent = this.personAgentMapping.get(id);
			legPerformingAgents.add(agent);
		}

		return legPerformingAgents;
	}

}
