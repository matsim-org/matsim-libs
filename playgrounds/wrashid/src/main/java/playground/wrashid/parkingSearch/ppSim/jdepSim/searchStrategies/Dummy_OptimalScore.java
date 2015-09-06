/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.wrashid.parkingSearch.ppSim.jdepSim.searchStrategies;

import java.util.Collection;
import java.util.PriorityQueue;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.contrib.parking.lib.DebugLib;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.SortableMapObject;
import org.matsim.core.population.ActivityImpl;

import playground.wrashid.parkingChoice.infrastructure.api.PParking;
import playground.wrashid.parkingSearch.ppSim.jdepSim.AgentWithParking;
import playground.wrashid.parkingSearch.ppSim.jdepSim.zurich.ZHScenarioGlobal;
import playground.wrashid.parkingSearch.withinDay_v_STRC.scoring.ParkingActivityAttributes;

public class Dummy_OptimalScore extends Dummy_TakeClosestParking {

	protected double scoreFactor=0;
	
	public Dummy_OptimalScore(double maxDistance, Network network, String name) {
		super(maxDistance, network, name);
		scoreFactor=-1;
	}

	@Override
	public void handleAgentLeg(AgentWithParking aem) {
		Id personId = aem.getPerson().getId();
		walkSpeed = getWalkSpeed(aem);

		boolean endOfLegReached = aem.endOfLegReached();

		
		
		if (endOfLegReached) {
			if (ZHScenarioGlobal.iteration==5){
				DebugLib.traceAgent(personId);
			}
			
			//DebugLib.traceAgent(personId);
			if (!parkingFound.contains(personId)) {
				parkingFound.add(personId);

				ActivityImpl nextAct = (ActivityImpl) aem.getPerson().getSelectedPlan().getPlanElements()
						.get(aem.getPlanElementIndex() + 3);

				Id parkingId = AgentWithParking.parkingManager.getFreePrivateParking(nextAct.getFacilityId(), nextAct.getType());

				if (isInvalidParking(aem, parkingId)) {
					double distance = 300;
					Collection<PParking> parkings = AgentWithParking.parkingManager.getParkingWithinDistance(nextAct.getCoord(),
							1000);
					Dummy_RandomSelection.removeInvalidParkings(aem, parkings);
					while (parkings.size() == 0) {
						distance *= 2;
						parkings = AgentWithParking.parkingManager.getParkingWithinDistance(nextAct.getCoord(), distance);
					
						Dummy_RandomSelection.removeInvalidParkings(aem, parkings);
					}

					PriorityQueue<SortableMapObject<PParking>> priorityQueue = new PriorityQueue<SortableMapObject<PParking>>();

					for (PParking parking : parkings) {
						ParkingActivityAttributes parkingAttributes = new ParkingActivityAttributes(aem.getPerson().getId());

						double activityDuration = getActivityDuration(aem);
						parkingAttributes.setActivityDuration(activityDuration);
						parkingAttributes.setParkingDuration(activityDuration);
						parkingAttributes.setFacilityId(parking.getId());
						parkingAttributes.setParkingSearchDuration(0);
						double walkDuration = getWalkDuration(aem, parking.getCoord());
						parkingAttributes.setToActWalkDuration(walkDuration);
						parkingAttributes.setToParkWalkDuration(walkDuration);

						double parkingScore = ZHScenarioGlobal.parkingScoreEvaluator.getParkingScore(parkingAttributes);
						priorityQueue.add(new SortableMapObject<PParking>(parking, scoreFactor*parkingScore));
					}

					parkingId = priorityQueue.poll().getKey().getId();
				}

				parkVehicleAndLogSearchTime(aem, personId, parkingId);
			}
		} else {
			super.handleAgentLeg(aem);
		}

	}

	private double getWalkDuration(AgentWithParking aem, Coord coord) {
		ActivityImpl nextAct = (ActivityImpl) aem.getPerson().getSelectedPlan().getPlanElements()
				.get(aem.getPlanElementIndex() + 3);

		double walkDistance = GeneralLib.getDistance(coord, nextAct.getCoord());
		return walkDistance / walkSpeed;
	}

	private double getActivityDuration(AgentWithParking aem) {
		double activityDuration = 0;
		boolean isLastLegOfDay = aem.getPlanElementIndex() == aem.getPlanElementIndexOfLastCarLeg();
		if (isLastLegOfDay) {
			Activity act = (Activity) aem.getPerson().getSelectedPlan().getPlanElements().get(aem.getIndexOfFirstCarLegOfDay() - 3);

			double parkingDuration = GeneralLib.getIntervalDuration(aem.getMessageArrivalTime(), act.getEndTime());
			if (aem.getMessageArrivalTime() == act.getEndTime()) {
				parkingDuration = 0;
			}
			activityDuration = parkingDuration;
		} else {
			int indexOfNextCarLeg = aem.duringCarLeg_getPlanElementIndexOfNextCarLeg();

			ActivityImpl lastActBeforeNextCarLeg = (ActivityImpl) aem.getPerson().getSelectedPlan().getPlanElements()
					.get(indexOfNextCarLeg - 3);

			double parkingDuration = GeneralLib.getIntervalDuration(aem.getMessageArrivalTime(),
					lastActBeforeNextCarLeg.getEndTime());
			if (aem.getMessageArrivalTime() == lastActBeforeNextCarLeg.getEndTime()) {
				parkingDuration = 0;
			}
			activityDuration = parkingDuration;
		}

		return activityDuration;
	}
}
