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
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.handler.ActivityStartEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.mobsim.qsim.agents.ExperimentalBasicWithindayAgent;

public class UpdateLastParkingArrivalTime implements AgentArrivalEventHandler {

	private final Map<Id, ExperimentalBasicWithindayAgent> agents;
	private final Map<Id, Double> lastParkingArrivalTime;

	public UpdateLastParkingArrivalTime(Map<Id, ExperimentalBasicWithindayAgent> agents) {
		this.agents = agents;
		this.lastParkingArrivalTime = new HashMap<Id, Double>();
	}

	public Double getTime(Id personId) {
		return lastParkingArrivalTime.get(personId);
	}

	@Override
	public void reset(int iteration) {
		lastParkingArrivalTime.clear();
	}

	@Override
	public void handleEvent(AgentArrivalEvent event) {
		Id personId = event.getPersonId();
		ExperimentalBasicWithindayAgent agent = this.agents.get(personId);
		Plan executedPlan = agent.getSelectedPlan();
		int planElementIndex = agent.getCurrentPlanElementIndex();

		if (planElementIndex+2==executedPlan.getPlanElements().size()){
			return;
		}
		
		Leg currentLeg = (Leg) executedPlan.getPlanElements().get(planElementIndex);
		Leg nextLeg = (Leg) executedPlan.getPlanElements().get(planElementIndex + 2);

		if (currentLeg.getMode().equals(TransportMode.car) && nextLeg.getMode().equals(TransportMode.walk)) {
			lastParkingArrivalTime.put(event.getPersonId(), event.getTime());
		}
	}

}
