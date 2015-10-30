/* *********************************************************************** *
 * project: org.matsim.*
 * AgentsTracker.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.christoph.evacuation.mobsim;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;

import playground.christoph.evacuation.mobsim.Tracker.Position;

public class AgentsTracker implements PersonDepartureEventHandler, PersonArrivalEventHandler,
		ActivityStartEventHandler, ActivityEndEventHandler, 
		PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler, 
		LinkEnterEventHandler, LinkLeaveEventHandler, BeforeMobsimListener {

	/*package*/ final Scenario scenario;
	/*package*/ final Map<Id, AgentPosition> agentPositions;
	
	public AgentsTracker(Scenario scenario) {
		this.scenario = scenario;
		
		this.agentPositions = new HashMap<Id, AgentPosition>();
	}
	
	public AgentPosition getAgentPosition(Id agentId) {
		return this.agentPositions.get(agentId);
	}
	
	@Override
	public void handleEvent(LinkEnterEvent event) {
		AgentPosition agentPosition = agentPositions.get(event.getDriverId());
		agentPosition.entersLink(event.getLinkId());
	}
	
	@Override
	public void handleEvent(LinkLeaveEvent event) {
		AgentPosition agentPosition = agentPositions.get(event.getDriverId());
		agentPosition.leavesLink();
	}
	
	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		AgentPosition agentPosition = agentPositions.get(event.getPersonId());
		agentPosition.entersVehicle(event.getVehicleId());
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		AgentPosition agentPosition = agentPositions.get(event.getPersonId());
		agentPosition.leavesVehicle();
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		AgentPosition agentPosition = agentPositions.get(event.getPersonId());
		agentPosition.setTransportMode(null);
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		AgentPosition agentPosition = agentPositions.get(event.getPersonId());
		agentPosition.entersLink(event.getLinkId());
		agentPosition.setTransportMode(event.getLegMode());
	}
	
	@Override
	public void handleEvent(ActivityStartEvent event) {
		AgentPosition agentPosition = agentPositions.get(event.getPersonId());
		agentPosition.entersFacility(event.getFacilityId());
	}
	
	@Override
	public void handleEvent(ActivityEndEvent event) {
		AgentPosition agentPosition = agentPositions.get(event.getPersonId());
		agentPosition.leavesFacility();
	}

	@Override
	public void reset(int iteration) {
		this.agentPositions.clear();
	}
	
	@Override
	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		initializeAgentPositions();
	}
	
	private void initializeAgentPositions() {
		this.agentPositions.clear();
		
		for (Person person : scenario.getPopulation().getPersons().values()) {
			
			Plan plan = person.getSelectedPlan();
			if (plan == null || plan.getPlanElements().size() == 0) continue;
			
			PlanElement planElement = plan.getPlanElements().get(0);
			Id facilityId = ((Activity) planElement).getFacilityId();
			AgentPosition agentPosition = new AgentPosition(person.getId(), facilityId, Position.FACILITY);
			this.agentPositions.put(person.getId(), agentPosition);
		}
	}
}