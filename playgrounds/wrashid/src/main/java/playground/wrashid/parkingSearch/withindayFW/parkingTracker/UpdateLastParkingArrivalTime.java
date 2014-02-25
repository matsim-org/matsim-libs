/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.wrashid.parkingSearch.withindayFW.parkingTracker;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.mobsim.qsim.agents.PersonDriverAgentImpl;
import org.matsim.core.mobsim.qsim.agents.PersonDriverAgentImpl;

import playground.wrashid.parkingSearch.withindayFW.util.ParallelSafePlanElementAccessLib;

public class UpdateLastParkingArrivalTime implements PersonArrivalEventHandler {

	private final Map<Id, PersonDriverAgentImpl> agents;
	private final Map<Id, Double> lastParkingArrivalTime;
	private Map<Id, Double> currentPlanElementIndexCarDeparture;

	public UpdateLastParkingArrivalTime(Map<Id, PersonDriverAgentImpl> agents) {
		this.agents = agents;
		this.lastParkingArrivalTime = new HashMap<Id, Double>();
		this.currentPlanElementIndexCarDeparture=new HashMap<Id, Double>();
	}

	public Double getTime(Id personId) {
		return lastParkingArrivalTime.get(personId);
	}

	@Override
	public void reset(int iteration) {
		lastParkingArrivalTime.clear();
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		Id personId = event.getPersonId();
		PersonDriverAgentImpl agent = this.agents.get(personId);
		Plan executedPlan = agent.getCurrentPlan();
		int planElementIndex = ParallelSafePlanElementAccessLib.getCurrentExpectedLegIndex(agent);

		
		if (planElementIndex+2==executedPlan.getPlanElements().size()){
			return;
		}
		
		Leg nextLeg = (Leg) executedPlan.getPlanElements().get(planElementIndex + 2);

		if (event.getLegMode().equals(TransportMode.car) && nextLeg.getMode().equals(TransportMode.walk)) {
			lastParkingArrivalTime.put(event.getPersonId(), event.getTime());
		}
	}
	
	

}
