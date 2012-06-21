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
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.mobsim.qsim.agents.ExperimentalBasicWithindayAgent;

import playground.wrashid.lib.GeneralLib;

//Done.
public class CaptureWalkDurationOfFirstAndLastOfDay implements AgentDepartureEventHandler, AgentArrivalEventHandler {

	private final Map<Id, Double> firstParkWalkOfDayTmp;
	private final Map<Id, Double> lastParkWalkOfDayTmp;
	private final Map<Id, ExperimentalBasicWithindayAgent> agents;
	private final Map<Id, Integer> firstParkingActivityPlanElemIndex;
	private final Map<Id, Integer> lastParkingActivityPlanElemIndex;

	public CaptureWalkDurationOfFirstAndLastOfDay(Map<Id, ExperimentalBasicWithindayAgent> agents,
			Map<Id, Integer> firstParkingActivityPlanElemIndex, Map<Id, Integer> lastParkingActivityPlanElemIndex) {
		this.agents = agents;
		this.firstParkingActivityPlanElemIndex = firstParkingActivityPlanElemIndex;
		this.lastParkingActivityPlanElemIndex = lastParkingActivityPlanElemIndex;
		firstParkWalkOfDayTmp = new HashMap<Id, Double>();
		lastParkWalkOfDayTmp = new HashMap<Id, Double>();
	}

	public Double getDuration(Id personId) {
		return GeneralLib.getIntervalDuration(firstParkWalkOfDayTmp.get(personId), lastParkWalkOfDayTmp.get(personId));
	}

	@Override
	public void reset(int iteration) {
		firstParkWalkOfDayTmp.clear();
		lastParkWalkOfDayTmp.clear();
	}

	@Override
	public void handleEvent(AgentArrivalEvent event) {
		Id personId = event.getPersonId();
		ExperimentalBasicWithindayAgent agent = this.agents.get(personId);
		int planElementIndex = agent.getCurrentPlanElementIndex();

		if (agentHasNoCarLegDuringDay(personId)){
			return;
		}
		
		double durationFirstWalk = GeneralLib.getIntervalDuration(firstParkWalkOfDayTmp.get(personId), event.getTime());
		double durationLastWalk = GeneralLib.getIntervalDuration(lastParkWalkOfDayTmp.get(personId), event.getTime());

		updateWalkTmpVariables(personId, planElementIndex, durationFirstWalk, durationLastWalk);
	}
	
	private boolean agentHasNoCarLegDuringDay(Id personId) {
		
		return firstParkWalkOfDayTmp.get(personId)==null || lastParkWalkOfDayTmp.get(personId)==null;
	}
	

	private void updateWalkTmpVariables(Id personId, int planElementIndex, double valueA, double valueB) {
		if (firstParkingActivityPlanElemIndex.get(personId) == planElementIndex + 1) {
			firstParkWalkOfDayTmp.put(personId, valueA);
		} else if (lastParkingActivityPlanElemIndex.get(personId) == planElementIndex - 1) {
			lastParkWalkOfDayTmp.put(personId, valueB);
		}
	}

	@Override
	public void handleEvent(AgentDepartureEvent event) {
		Id personId = event.getPersonId();
		ExperimentalBasicWithindayAgent agent = this.agents.get(event.getPersonId());
		int planElementIndex = agent.getCurrentPlanElementIndex();
		double startTimeWalkLeg = event.getTime();

		updateWalkTmpVariables(personId, planElementIndex, startTimeWalkLeg, startTimeWalkLeg);
	}

}
