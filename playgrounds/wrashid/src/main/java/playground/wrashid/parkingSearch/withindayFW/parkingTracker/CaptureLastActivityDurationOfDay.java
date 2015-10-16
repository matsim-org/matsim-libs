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
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.parking.lib.DebugLib;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.core.mobsim.qsim.agents.PersonDriverAgentImpl;
import org.matsim.core.mobsim.qsim.agents.PersonDriverAgentImpl;

import playground.wrashid.parkingSearch.withindayFW.util.ParallelSafePlanElementAccessLib;

public class CaptureLastActivityDurationOfDay implements ActivityStartEventHandler, ActivityEndEventHandler {
	private final Map<Id, PersonDriverAgentImpl> agents;
	private final Map<Id, Integer> firstParkingActivityPlanElemIndex;
	private Map<Id, Integer> lastParkingActivityPlanElemIndex;
	private Map<Id, Double> activityDurationTmpValue;

	public CaptureLastActivityDurationOfDay(Map<Id, PersonDriverAgentImpl> agents,
			Map<Id, Integer> firstParkingActivityPlanElemIndex, Map<Id, Integer> lastParkingActivityPlanElemIndex) {
		this.agents = agents;
		this.firstParkingActivityPlanElemIndex = firstParkingActivityPlanElemIndex;
		this.lastParkingActivityPlanElemIndex = lastParkingActivityPlanElemIndex;
		this.activityDurationTmpValue = new HashMap<Id, Double>();
	}

	// Precondition: only invoke at the end of the iteration (during scoring).
	public Double getDuration(Id personId) {
		return activityDurationTmpValue.get(personId);
	}

	@Override
	public void reset(int iteration) {
		activityDurationTmpValue.clear();
	}

	// this is invoked in the morning
	@Override
	public void handleEvent(ActivityEndEvent event) {
		//DebugLib.traceAgent(event.getDriverId());
		
		Id personId = event.getPersonId();
		PersonDriverAgentImpl agent = this.agents.get(event.getPersonId());
		Plan executedPlan = agent.getCurrentPlan();
		int planElementIndex = ParallelSafePlanElementAccessLib.getCurrentExpectedActIndex(agent);

		if (agentDoesNotDriveCarDuringWholeDay(personId)){
			return;
		}
		
		if (!isPlanElementDuringDay(personId, planElementIndex) && executedPlan.getPlanElements().size()>planElementIndex+2) {
			
			Activity nextAct = (Activity) executedPlan.getPlanElements().get(planElementIndex+2);

			if (nextAct.getType().equals("parking")) {
				double endActivityTime = event.getTime();
				activityDurationTmpValue.put(personId, endActivityTime);
			}
		}
	}
	
	

	// this is invoked in the evening
	@Override
	public void handleEvent(ActivityStartEvent event) {
		//DebugLib.traceAgent(event.getDriverId());
		
		Id personId = event.getPersonId();

		PersonDriverAgentImpl agent = this.agents.get(personId);
		Plan executedPlan = agent.getCurrentPlan();
		int planElementIndex = ParallelSafePlanElementAccessLib.getCurrentExpectedActIndex(agent);

		if (agentDoesNotDriveCarDuringWholeDay(personId)){
			return;
		}
		
		if (!isPlanElementDuringDay(personId, planElementIndex)) {
			Activity previousAct = (Activity) executedPlan.getPlanElements().get(planElementIndex - 2);

			if (previousAct.getType().equals("parking")) {
				
				if (activityDurationTmpValue.get(personId)==null){
					List<PlanElement> planElements = agents.get(personId).getCurrentPlan().getPlanElements();
					
					DebugLib.emptyFunctionForSettingBreakPoint();
				}
				
				activityDurationTmpValue.put(personId,
						GeneralLib.getIntervalDuration(activityDurationTmpValue.get(personId), event.getTime()));

			}

		}

	}
	
	private boolean agentDoesNotDriveCarDuringWholeDay(Id personId) {
		return firstParkingActivityPlanElemIndex.get(personId)==null;
	}

	private boolean isPlanElementDuringDay(Id personId, int planElementIndex) {
		return planElementIndex > firstParkingActivityPlanElemIndex.get(personId)
				&& planElementIndex < lastParkingActivityPlanElemIndex.get(personId);
	}
}