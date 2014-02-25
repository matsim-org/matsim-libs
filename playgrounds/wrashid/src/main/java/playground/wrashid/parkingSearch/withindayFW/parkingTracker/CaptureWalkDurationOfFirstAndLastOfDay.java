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
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.core.mobsim.qsim.agents.PersonDriverAgentImpl;
import org.matsim.core.mobsim.qsim.agents.PersonDriverAgentImpl;

import playground.wrashid.parkingSearch.withindayFW.util.ParallelSafePlanElementAccessLib;

//Done.
public class CaptureWalkDurationOfFirstAndLastOfDay implements PersonDepartureEventHandler, PersonArrivalEventHandler {

	private final Map<Id, Double> firstParkWalkOfDayTmp;
	private final Map<Id, Double> lastParkWalkOfDayTmp;
	private final Map<Id, PersonDriverAgentImpl> agents;
	private final Map<Id, Integer> firstParkingActivityPlanElemIndex;
	private final Map<Id, Integer> lastParkingActivityPlanElemIndex;

	public CaptureWalkDurationOfFirstAndLastOfDay(Map<Id, PersonDriverAgentImpl> agents,
			Map<Id, Integer> firstParkingActivityPlanElemIndex, Map<Id, Integer> lastParkingActivityPlanElemIndex) {
		this.agents = agents;
		this.firstParkingActivityPlanElemIndex = firstParkingActivityPlanElemIndex;
		this.lastParkingActivityPlanElemIndex = lastParkingActivityPlanElemIndex;
		firstParkWalkOfDayTmp = new HashMap<Id, Double>();
		lastParkWalkOfDayTmp = new HashMap<Id, Double>();
	}

	public Double getSumBothParkingWalkDurationsInSecond(Id personId) {
		return firstParkWalkOfDayTmp.get(personId) + lastParkWalkOfDayTmp.get(personId);
	}

	@Override
	public void reset(int iteration) {
		firstParkWalkOfDayTmp.clear();
		lastParkWalkOfDayTmp.clear();
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		Id personId = event.getPersonId();
		PersonDriverAgentImpl agent = this.agents.get(personId);
		int planElementIndex = ParallelSafePlanElementAccessLib.getCurrentExpectedLegIndex(agent);

		if (agentDoesNotDriveCarDuringWholeDay(personId)){
			return;
		}
		
		
		double durationFirstWalk=0.0;
		if (firstParkWalkOfDayTmp.get(personId)!=null && firstParkWalkOfDayTmp.get(personId) != event.getTime()){
			durationFirstWalk = GeneralLib.getIntervalDuration(firstParkWalkOfDayTmp.get(personId), event.getTime());
		}
		
		double durationLastWalk=0.0;
		if (lastParkWalkOfDayTmp.get(personId)!=null && lastParkWalkOfDayTmp.get(personId) != event.getTime()){
			durationLastWalk = GeneralLib.getIntervalDuration(lastParkWalkOfDayTmp.get(personId), event.getTime());
		}
		
		updateWalkTmpVariables(personId, planElementIndex, durationFirstWalk, durationLastWalk);
	}
	
	private boolean agentDoesNotDriveCarDuringWholeDay(Id personId) {
		return firstParkingActivityPlanElemIndex.get(personId)==null;
	}
	

	private void updateWalkTmpVariables(Id personId, int planElementIndex, double valueA, double valueB) {
		if (firstParkingActivityPlanElemIndex.get(personId) == planElementIndex + 1) {
			firstParkWalkOfDayTmp.put(personId, valueA);
		} else if (lastParkingActivityPlanElemIndex.get(personId) == planElementIndex - 1) {
			lastParkWalkOfDayTmp.put(personId, valueB);
		}
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		Id personId = event.getPersonId();
		PersonDriverAgentImpl agent = this.agents.get(event.getPersonId());
		int planElementIndex = ParallelSafePlanElementAccessLib.getCurrentExpectedLegIndex(agent);
		double startTimeWalkLeg = event.getTime();

		if (agentDoesNotDriveCarDuringWholeDay(personId)){
			return;
		}
		
		updateWalkTmpVariables(personId, planElementIndex, startTimeWalkLeg, startTimeWalkLeg);
	}

}
