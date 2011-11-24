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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.handler.ActivityEndEventHandler;
import org.matsim.core.api.experimental.events.handler.ActivityStartEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.events.SimulationInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.SimulationInitializedListener;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.ptproject.qsim.agents.ExperimentalBasicWithindayAgent;

public class ParkingAgentsTracker implements LinkEnterEventHandler, AgentArrivalEventHandler, AgentDepartureEventHandler,
ActivityStartEventHandler, ActivityEndEventHandler, SimulationInitializedListener {

	private final Set<Id> carLegAgents;
	private final Map<Id, Coord> nextActivityCoordMap;
	private final Map<Id, ExperimentalBasicWithindayAgent> agents;
	
	public ParkingAgentsTracker() {
		this.carLegAgents = new HashSet<Id>();
		this.nextActivityCoordMap = new HashMap<Id, Coord>();
		this.agents = new HashMap<Id, ExperimentalBasicWithindayAgent>();
	}
	
	@Override
	public void notifySimulationInitialized(SimulationInitializedEvent e) {
		for (MobsimAgent agent : ((QSim) e.getQueueSimulation()).getAgents()) {
			this.agents.put(agent.getId(), (ExperimentalBasicWithindayAgent) agent);
		}
	}
	
	@Override
	public void handleEvent(ActivityEndEvent event) {
		PlanElement pe = this.agents.get(event.getPersonId()).getCurrentPlanElement();
			
	}


	@Override
	public void handleEvent(AgentDepartureEvent event) {
		if (event.getLegMode().equals(TransportMode.car)) {
			this.carLegAgents.add(event.getPersonId());
			
			ExperimentalBasicWithindayAgent agent = this.agents.get(event.getPersonId());
			Plan executedPlan = agent.getSelectedPlan();
			int planElementIndex = agent.getCurrentPlanElementIndex();
			
			/*
			 * Get the coordinate of the next non-parking activity.
			 * The currentPlanElement is a car leg, which is followed
			 * by a parking activity and a walking leg to the next
			 * non-parking activity.
			 */
			Activity nextNonParkingActivity = (Activity) executedPlan.getPlanElements().get(planElementIndex+3);
			nextActivityCoordMap.put(event.getPersonId(), nextNonParkingActivity.getCoord());
			
//			System.out.println(executedPlan.getPlanElements().get(planElementIndex).toString());
//			System.out.println(executedPlan.getPlanElements().get(planElementIndex+1).toString());
//			System.out.println(executedPlan.getPlanElements().get(planElementIndex+2).toString());
//			System.out.println(executedPlan.getPlanElements().get(planElementIndex+3).toString());
		}
	}

	@Override
	public void handleEvent(AgentArrivalEvent event) {
		this.carLegAgents.remove(event.getPersonId());
	}
	
	@Override
	public void handleEvent(ActivityStartEvent event) {
		// TODO
		
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		// TODO
		
	}

	@Override
	public void reset(int iteration) {
		carLegAgents.clear();
		nextActivityCoordMap.clear();
	}

}
