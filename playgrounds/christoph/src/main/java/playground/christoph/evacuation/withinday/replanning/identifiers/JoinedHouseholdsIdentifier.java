/* *********************************************************************** *
 * project: org.matsim.*
 * JoinedHouseholdsIdentifier.java
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

package playground.christoph.evacuation.withinday.replanning.identifiers;

import java.io.Serializable;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.PriorityBlockingQueue;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.internal.MatsimComparator;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.events.SimulationInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.SimulationInitializedListener;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.ptproject.qsim.agents.PlanBasedWithinDayAgent;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringActivityIdentifier;

import playground.christoph.evacuation.events.HouseholdJoinedEvent;
import playground.christoph.evacuation.events.HouseholdSeparatedEvent;
import playground.christoph.evacuation.events.handler.HouseholdJoinedEventHandler;
import playground.christoph.evacuation.events.handler.HouseholdSeparatedEventHandler;
import playground.christoph.evacuation.withinday.replanning.utils.HouseholdInfo;
import playground.christoph.evacuation.withinday.replanning.utils.HouseholdsUtils;
import playground.christoph.evacuation.withinday.replanning.utils.SelectHouseholdMeetingPoint;

/**
 *  Define which households will relocate to another (secure!) location
 *  at which time.
 *   
 *  @author cdobler
 */
public class JoinedHouseholdsIdentifier extends DuringActivityIdentifier implements 
	HouseholdJoinedEventHandler, HouseholdSeparatedEventHandler, SimulationInitializedListener {

	private static final Logger log = Logger.getLogger(JoinedHouseholdsIdentifier.class);
	
	private final HouseholdsUtils householdUtils;
	private final SelectHouseholdMeetingPoint selectHouseholdMeetingPoint;
	private final Map<Id, PlanBasedWithinDayAgent> agentMapping;
	private final Set<Id> joinedHouseholds;
	private final Queue<HouseholdDeparture> householdDepartures;
	
	public JoinedHouseholdsIdentifier(HouseholdsUtils householdUtils, SelectHouseholdMeetingPoint selectHouseholdMeetingPoint) {
		this.householdUtils = householdUtils;
		this.selectHouseholdMeetingPoint = selectHouseholdMeetingPoint;
		this.agentMapping = new HashMap<Id, PlanBasedWithinDayAgent>();
		
		this.householdDepartures = new PriorityBlockingQueue<HouseholdDeparture>(500, new DepartureTimeComparator());
		this.joinedHouseholds = new HashSet<Id>();
	}

	@Override
	public Set<PlanBasedWithinDayAgent> getAgentsToReplan(double time) {
		Set<PlanBasedWithinDayAgent> set = new HashSet<PlanBasedWithinDayAgent>();
	
		while (this.householdDepartures.peek() != null) {
			HouseholdDeparture householdDeparture = this.householdDepartures.peek();
			if (householdDeparture.getDepartureTime() <= time) {
				this.householdDepartures.poll();
				Id householdId = householdDeparture.getHouseholdId();
				HouseholdInfo householdInfo = householdUtils.getHouseholdInfoMap().get(householdId);
				selectHouseholdMeetingPoint.selectRescueMeetingPoint(householdInfo);
				
				for (Id agentId : householdInfo.getHousehold().getMemberIds()) {
					set.add(agentMapping.get(agentId));
				}
			} else {
				break;
			}
		}
		
		return set;
	}

	@Override
	public void reset(int iteration) {
		agentMapping.clear();
		joinedHouseholds.clear();
		householdDepartures.clear();
	}

	@Override
	public void handleEvent(HouseholdSeparatedEvent event) {
		joinedHouseholds.remove(event.getHouseholdId());
		
		/*
		 * The household has been separated, therefore remove scheduled departure.
		 */
		if (householdDepartures.remove(new HouseholdDeparture(event.getHouseholdId(), 0.0))) {
			log.warn("Household has been separated before scheduled departure. Id " + event.getHouseholdId());
		}
	}

	@Override
	public void handleEvent(HouseholdJoinedEvent event) {
		joinedHouseholds.add(event.getHouseholdId());
		
		// TODO: use a function to estimate the departure time
		HouseholdDeparture householdDeparture = new HouseholdDeparture(event.getHouseholdId(), event.getTime() + 600);
		
		// if there is an old entry: remove it first
		householdDepartures.remove(householdDeparture);
		householdDepartures.add(householdDeparture);
	}
	
	/*
	 * Create a mapping between personIds and the agents in the mobsim.
	 */
	@Override
	public void notifySimulationInitialized(SimulationInitializedEvent e) {
		
		QSim sim = (QSim) e.getQueueSimulation();

		for (MobsimAgent mobsimAgent : sim.getAgents()) {
			PlanBasedWithinDayAgent withinDayAgent = (PlanBasedWithinDayAgent) mobsimAgent;
			agentMapping.put(withinDayAgent.getId(), withinDayAgent);				
		}
	}
	
	/*
	 * A datastructure to store households and their planned departures.
	 */
	private static class HouseholdDeparture {
		
		private final Id householdId;
		private final double departureTime;
		
		public HouseholdDeparture(Id householdId, double departureTime) {
			this.householdId = householdId;
			this.departureTime = departureTime;
		}
		
		public Id getHouseholdId() {
			return this.householdId;
		}
		
		public double getDepartureTime() {
			return this.departureTime;
		}
		
		@Override
		public boolean equals(Object o) {
			if (o instanceof HouseholdDeparture) {
				return ((HouseholdDeparture) o).equals(householdId);
			}
			return false;
		}
	}
	
	private static class DepartureTimeComparator implements Comparator<HouseholdDeparture>, Serializable, MatsimComparator {

		private static final long serialVersionUID = 1L;

		@Override
		public int compare(HouseholdDeparture h1, HouseholdDeparture h2) {
			int cmp = Double.compare(h1.getDepartureTime(), h2.getDepartureTime());
			if (cmp == 0) {
				// Both depart at the same time -> let the one with the larger id be first (=smaller)
				return h2.getHouseholdId().compareTo(h2.getHouseholdId());
			}
			return cmp;
		}
	}

}