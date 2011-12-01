/* *********************************************************************** *
 * project: org.matsim.*
 * ParkingAgentsTracker.java
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

package playground.wrashid.parkingSearch.withinday;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.events.SimulationInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.SimulationInitializedListener;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.ptproject.qsim.agents.ExperimentalBasicWithindayAgent;

public class ParkingAgentsTracker implements LinkEnterEventHandler, AgentArrivalEventHandler, AgentDepartureEventHandler,
		SimulationInitializedListener {

	private final Scenario scenario;
	private final double distance;
	
	private final Set<Id> carLegAgents;
	private final Set<Id> searchingAgents;
	private final Map<Id, ActivityFacility> nextActivityFacilityMap;
	private final Map<Id, ExperimentalBasicWithindayAgent> agents;
	
	/**
	 * Tracks agents' car legs and check whether they have to start
	 * their parking search.
	 * 
	 * @param scenario
	 * @param distance defines in which distance to the destination of a car trip an agent starts its parking search 
	 */
	public ParkingAgentsTracker(Scenario scenario, double distance) {
		this.scenario = scenario;
		this.distance = distance;
		
		this.carLegAgents = new HashSet<Id>();
		this.searchingAgents = new HashSet<Id>();
		this.nextActivityFacilityMap = new HashMap<Id, ActivityFacility>();
		this.agents = new HashMap<Id, ExperimentalBasicWithindayAgent>();
	}
	
	public Set<Id> getSearchingAgents() {
		return Collections.unmodifiableSet(this.searchingAgents);
	}
	
	@Override
	public void notifySimulationInitialized(SimulationInitializedEvent e) {
		for (MobsimAgent agent : ((QSim) e.getQueueSimulation()).getAgents()) {
			this.agents.put(agent.getId(), (ExperimentalBasicWithindayAgent) agent);
		}
	}
	
	@Override
	public void handleEvent(AgentDepartureEvent event) {
		if (event.getLegMode().equals(TransportMode.car)) {
			this.carLegAgents.add(event.getPersonId());
			
			ExperimentalBasicWithindayAgent agent = this.agents.get(event.getPersonId());
			Plan executedPlan = agent.getSelectedPlan();
			int planElementIndex = agent.getCurrentPlanElementIndex();
			
			/*
			 * Get the coordinate of the next non-parking activity's facility.
			 * The currentPlanElement is a car leg, which is followed by a 
			 * parking activity and a walking leg to the next non-parking activity.
			 */
			Activity nextNonParkingActivity = (Activity) executedPlan.getPlanElements().get(planElementIndex+3);
			ActivityFacility facility = ((ScenarioImpl) scenario).getActivityFacilities().getFacilities().get(nextNonParkingActivity.getFacilityId());
			nextActivityFacilityMap.put(event.getPersonId(), facility);
			
			Coord coord = scenario.getNetwork().getLinks().get(event.getLinkId()).getCoord();
			double d = CoordUtils.calcDistance(facility.getCoord(), coord);
			
			if (d <= distance) {
				searchingAgents.add(event.getPersonId());
//				System.out.println("searching...");
			}
			/*
			 * If the agent enters the link where its next non-parking activity
			 * is performed.
			 */
			else if (facility.getLinkId().equals(event.getLinkId())) {
				searchingAgents.add(event.getPersonId());
			}
			
//			System.out.println(executedPlan.getPlanElements().get(planElementIndex).toString());
//			System.out.println(executedPlan.getPlanElements().get(planElementIndex+1).toString());
//			System.out.println(executedPlan.getPlanElements().get(planElementIndex+2).toString());
//			System.out.println(executedPlan.getPlanElements().get(planElementIndex+3).toString());
		}
	}

	@Override
	public void handleEvent(AgentArrivalEvent event) {
		this.carLegAgents.remove(event.getPersonId());
		this.searchingAgents.remove(event.getPersonId());
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		if (!searchingAgents.contains(event.getPersonId())) {
			Coord coord = scenario.getNetwork().getLinks().get(event.getLinkId()).getCoord();
			ActivityFacility facility = nextActivityFacilityMap.get(event.getPersonId());
			double d = CoordUtils.calcDistance(facility.getCoord(), coord);
			
			/*
			 * If the agent is within the parking radius
			 */
			if (d <= distance) {
				searchingAgents.add(event.getPersonId());
//				System.out.println("searching...");
			} 
			/*
			 * If the agent enters the link where its next non-parking activity
			 * is performed.
			 */
			else if (facility.getLinkId().equals(event.getLinkId())) {
				searchingAgents.add(event.getPersonId());
			}
		}
	}
	
	@Override
	public void reset(int iteration) {
		agents.clear();
		carLegAgents.clear();
		searchingAgents.clear();
		nextActivityFacilityMap.clear();
	}

}
