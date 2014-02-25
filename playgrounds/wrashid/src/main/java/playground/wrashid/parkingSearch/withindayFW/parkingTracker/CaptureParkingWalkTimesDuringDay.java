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
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.parking.lib.DebugLib;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.core.mobsim.qsim.agents.PersonDriverAgentImpl;
import org.matsim.core.mobsim.qsim.agents.PersonDriverAgentImpl;

import playground.wrashid.parkingSearch.withindayFW.util.ParallelSafePlanElementAccessLib;

/**
 * 
 * 
 * @author wrashid
 * 
 */
// Done.
public class CaptureParkingWalkTimesDuringDay implements PersonDepartureEventHandler, PersonArrivalEventHandler {

	private final Map<Id, PersonDriverAgentImpl> agents;

	private Map<Id, Double> firstParkingWalkTmp = new HashMap<Id, Double>();
	private Map<Id, Double> secondParkingWalkTmp = new HashMap<Id, Double>();

	private Map<Id, Integer> firstParkingActivityPlanElemIndex;
	private Map<Id, Integer> lastParkingActivityPlanElemIndex;

	public CaptureParkingWalkTimesDuringDay(Map<Id, PersonDriverAgentImpl> agents,
			Map<Id, Integer> firstParkingActivityPlanElemIndex, Map<Id, Integer> lastParkingActivityPlanElemIndex) {
		this.firstParkingActivityPlanElemIndex = firstParkingActivityPlanElemIndex;
		this.lastParkingActivityPlanElemIndex = lastParkingActivityPlanElemIndex;
		this.agents = agents;

		for (PersonDriverAgentImpl agent : agents.values()) {
			Id personId = agent.getCurrentPlan().getPerson().getId();

			for (PlanElement pe : agent.getCurrentPlan().getPlanElements()) {
				if (pe instanceof Leg) {
					Leg leg = (Leg) pe;

					if (leg.getMode().equals(TransportMode.car)) {
						firstParkingWalkTmp.put(personId, 0.0);
						secondParkingWalkTmp.put(personId, 0.0);
						break;
					}

				}
			}
		}
	}

	public double getSumBothParkingWalkDurationsInSecond(Id personId) {
		return firstParkingWalkTmp.get(personId) + secondParkingWalkTmp.get(personId);
	}

	@Override
	public void reset(int iteration) {
		firstParkingWalkTmp.clear();
		secondParkingWalkTmp.clear();
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		Id personId = event.getPersonId();
		
		PersonDriverAgentImpl agent = this.agents.get(personId);
		Plan executedPlan = agent.getCurrentPlan();
		int planElementIndex = ParallelSafePlanElementAccessLib.getCurrentExpectedLegIndex(agent);

		if (agentDoesNotDriveCarDuringWholeDay(personId)) {
			return;
		}

		if (!isPlanElementDuringDay(personId, planElementIndex)) {
			return;
		}

		if (firstParkingWalkTmp.get(personId) == null) {
			DebugLib.emptyFunctionForSettingBreakPoint();
		}

		double durationFirstWalk = 0;
		double durationSecondWalk = 0;

		
		// same start and end time causes wrong interval calculation (3600*24 seconds, instead of zero).
		if (firstParkingWalkTmp.get(personId) != null && firstParkingWalkTmp.get(personId) != event.getTime()) {
			durationFirstWalk = GeneralLib.getIntervalDuration(firstParkingWalkTmp.get(personId), event.getTime());
		}

		if (secondParkingWalkTmp.get(personId) != null && secondParkingWalkTmp.get(personId) != event.getTime()) {
			durationSecondWalk = GeneralLib.getIntervalDuration(secondParkingWalkTmp.get(personId), event.getTime());
		}

		
		
		updateWalkTimeTmpVariables(event.getLegMode(), personId, executedPlan, planElementIndex, durationFirstWalk,
				durationSecondWalk);
	}

	private boolean agentDoesNotDriveCarDuringWholeDay(Id personId) {
		return firstParkingActivityPlanElemIndex.get(personId) == null;
	}

	private boolean isPlanElementDuringDay(Id personId, int planElementIndex) {
		return planElementIndex > firstParkingActivityPlanElemIndex.get(personId)
				&& planElementIndex < lastParkingActivityPlanElemIndex.get(personId);
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {

		Id personId = event.getPersonId();
	
		PersonDriverAgentImpl agent = this.agents.get(personId);
		Plan executedPlan = agent.getCurrentPlan();
		int planElementIndex = ParallelSafePlanElementAccessLib.getCurrentExpectedLegIndex(agent);
		double startTimeWalkLeg = event.getTime();

		if (agentDoesNotDriveCarDuringWholeDay(personId)) {
			return;
		}

		updateWalkTimeTmpVariables(event.getLegMode(), personId, executedPlan, planElementIndex, startTimeWalkLeg,
				startTimeWalkLeg);
	}

	private void updateWalkTimeTmpVariables(String legMod, Id personId, Plan executedPlan, int planElementIndex, double valueA,
			double valueB) {
		
		
		if (isPlanElementDuringDay(personId, planElementIndex)) {
			if (legMod.equals(TransportMode.walk)) {
				
				Activity previousAct = (Activity) executedPlan.getPlanElements().get(planElementIndex - 1);
				Leg previousLeg = (Leg) executedPlan.getPlanElements().get(planElementIndex - 2);

				if (previousAct.getType().equalsIgnoreCase("parking") && previousLeg.getMode().equals(TransportMode.car)) {
					firstParkingWalkTmp.put(personId, valueA);
				}

				Activity nextAct = (Activity) executedPlan.getPlanElements().get(planElementIndex + 1);
				Leg nextLeg = (Leg) executedPlan.getPlanElements().get(planElementIndex + 2);

				if (nextAct.getType().equalsIgnoreCase("parking") && nextLeg.getMode().equals(TransportMode.car)) {
					secondParkingWalkTmp.put(personId, valueB);
				}
			}
		}
	}

}
