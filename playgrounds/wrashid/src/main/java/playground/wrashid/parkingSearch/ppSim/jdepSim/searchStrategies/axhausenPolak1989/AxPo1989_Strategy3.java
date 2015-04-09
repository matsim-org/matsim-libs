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
package playground.wrashid.parkingSearch.ppSim.jdepSim.searchStrategies.axhausenPolak1989;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.DoubleValueHashMap;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import playground.wrashid.parkingChoice.infrastructure.api.PParking;
import playground.wrashid.parkingSearch.ppSim.jdepSim.AgentWithParking;
import playground.wrashid.parkingSearch.ppSim.jdepSim.Message;
import playground.wrashid.parkingSearch.ppSim.jdepSim.routing.EditRoute;
import playground.wrashid.parkingSearch.ppSim.jdepSim.searchStrategies.RandomParkingSearch;
import playground.wrashid.parkingSearch.ppSim.jdepSim.zurich.ZHScenarioGlobal;
import playground.wrashid.parkingSearch.withinDay_v_STRC.scoring.ParkingActivityAttributes;

import java.util.List;

// TODO: start considering search time, when agent passes destination link for first time!

// triggerSeachTimeStart(personId,aem.getMessageArrivalTime());

public class AxPo1989_Strategy3 extends RandomParkingSearch {

	DoubleValueHashMap<Id> garageParkingScore;

	@Override
	public void resetForNewIteration() {
		super.resetForNewIteration();

		garageParkingScore = new DoubleValueHashMap<Id>();
	}

	public AxPo1989_Strategy3(double maxDistance, Network network, String name) {
		super(maxDistance, network, name);
		this.parkingType = "streetParking";
		resetForNewIteration();
	}

	@Override
	public void handleAgentLeg(AgentWithParking aem) {
		triggerSearchTimerIfNeeded(aem);
		
		if (!endOfLegReached(aem)) {
			if (!hasStreetParkingWithBetterScoreThanGPThanTakeIt(aem)) {
				continueDriving(aem);
			}
		} else {
			if (!hasFreeGPParkingThenTakeIt(aem)) {
				findClosestFreeGarageAndDriveThere(aem);
			}
		}
	}

	private void triggerSearchTimerIfNeeded(AgentWithParking aem) {
		Id personId = aem.getPerson().getId();
	
		if (!startSearchTime.containsKey(personId)){
			ActivityImpl nextNonParkingAct = (ActivityImpl) aem.getPerson().getSelectedPlan().getPlanElements()
					.get(aem.getPlanElementIndex() + 3);
			if (GeneralLib.equals(getCurrentLinkId(aem),nextNonParkingAct.getLinkId())){
				startSearchTime.put(personId, aem.getMessageArrivalTime());
			}
		}
	}

	private boolean hasStreetParkingWithBetterScoreThanGPThanTakeIt(AgentWithParking aem) {
		Id personId = aem.getPerson().getId();
		double scoreGP = getScoreOfClosestGPAtCurrentEndOfRoute(aem);

		
		double searchTime=0;
		if (startSearchTime.containsKey(personId)){
			searchTime=getSearchTime(aem);
		}
		
		Link nextLinkId = getNextLink(aem);
		
		Id freeParkingFacilityOnLink = AgentWithParking.parkingManager.getFreeParkingFacilityOnLink(nextLinkId.getId(), "streetParking");
		boolean isInvalidLink = aem.isLastLinkOfRouteInvalidLinkForParking();
		
		if (!isInvalidLink && freeParkingFacilityOnLink!=null){
			ParkingActivityAttributes paa = getParkingAttributesForParking(aem, nextLinkId, freeParkingFacilityOnLink, searchTime);
			double parkingScore = ZHScenarioGlobal.parkingScoreEvaluator.getParkingScore(paa);
			
			if (parkingScore>scoreGP){
				throughAwayRestOfRoute(aem);
				parkVehicle(aem, freeParkingFacilityOnLink);
			}
		}
		
		return false;
	}
	
	private double getScoreOfClosestGPAtCurrentEndOfRoute(AgentWithParking aem) {
		Id personId = aem.getPerson().getId();
		if (!garageParkingScore.containsKey(personId)) {

			Leg leg = (LegImpl) aem.getPerson().getSelectedPlan().getPlanElements().get(aem.getPlanElementIndex());
			Id endLinkId = leg.getRoute().getEndLinkId();
			
			Link parkingLink = network.getLinks().get(endLinkId);
			Id closestFreeGarageParkingId = AgentWithParking.parkingManager.getClosestFreeGarageParkingNotOnLink(parkingLink.getCoord(),aem.getInvalidLinkForParking());

			

			ParkingActivityAttributes paa = getParkingAttributesForParking(aem, parkingLink, closestFreeGarageParkingId,getTravelTimeFromActivityToGP(aem));

			double parkingScore = ZHScenarioGlobal.parkingScoreEvaluator.getParkingScore(paa);
			garageParkingScore.put(personId, parkingScore);
		}

		return garageParkingScore.get(personId);
	}
	
	

	public ParkingActivityAttributes getParkingAttributesForParking(AgentWithParking aem, Link parkingLink, Id closestFreeGarageParkingId, double searchDuration) {
		ParkingActivityAttributes paa = new ParkingActivityAttributes(aem.getPerson().getId());
		paa.setFacilityId(closestFreeGarageParkingId);
		
		ActivityImpl nextNonParkingAct = (ActivityImpl) aem.getPerson().getSelectedPlan().getPlanElements()
				.get(aem.getPlanElementIndex() + 3);
		

		double walkDistance = GeneralLib.getDistance(parkingLink.getCoord(), nextNonParkingAct.getCoord());
		double walkDuration = walkDistance / walkSpeed;
		paa.setToActWalkDuration(walkDuration);
		paa.setToParkWalkDuration(walkDuration);
		paa.setWalkDistance(walkDistance);

		paa.setParkingArrivalTime(aem.getMessageArrivalTime());

		double activityDuration = getActivityDuration(aem);
		paa.setActivityDuration(activityDuration);
		paa.setParkingDuration(activityDuration);
		paa.setParkingSearchDuration(searchDuration);
		return paa;
	}

	public double getActivityDuration(AgentWithParking aem) {
		double activityDuration;
		ActivityImpl act;

		int indexOfNextCarLeg = aem.duringCarLeg_getPlanElementIndexOfNextCarLeg();
		if (indexOfNextCarLeg != -1) {
			act = (ActivityImpl) aem.getPerson().getSelectedPlan().getPlanElements().get(indexOfNextCarLeg - 3);
		} else {
			act = (ActivityImpl) aem.getPerson().getSelectedPlan().getPlanElements().get(0);
		}
		activityDuration = GeneralLib.getIntervalDuration(aem.getMessageArrivalTime(), act.getEndTime());

		if (activityDuration >= 24 * 60 * 60) {
			activityDuration = 0;
		}

		return activityDuration;
	}

	private double getTravelTimeFromActivityToGP(AgentWithParking aem) {
		Id nextActLinkId = ((ActivityImpl) aem.getPerson().getSelectedPlan().getPlanElements().get(aem.getPlanElementIndex() + 3))
				.getLinkId();
		Leg leg = (LegImpl) aem.getPerson().getSelectedPlan().getPlanElements().get(aem.getPlanElementIndex());

		List<Id<Link>> linkIds = ((LinkNetworkRouteImpl) leg.getRoute()).getLinkIds();

		double travelTime = 0;
		boolean routeStarted = false;
		for (Id linkId : linkIds) {
			if (linkId.toString().equalsIgnoreCase(nextActLinkId.toString())) {
				routeStarted = true;
			}

			if (routeStarted) {
				travelTime += Message.ttMatrix.getTravelTime(aem.getMessageArrivalTime(), linkId);
			}
		}

		return travelTime;
	}

	private boolean hasFreeGPParkingThenTakeIt(AgentWithParking aem) {
		boolean isInvalidLink = aem.isLastLinkOfRouteInvalidLinkForParking();
		if (!isInvalidLink) {
			//System.out.println(AgentWithParking.parkingManager);
			//System.out.println(getCurrentLinkId(aem));
			//System.out.println(AgentWithParking.parkingManager.getParkingsOnLink(getCurrentLinkId(aem)));
            Id<Link> currentLinkId = getCurrentLinkId(aem);
            for (Id parkingId : AgentWithParking.parkingManager.getParkingsOnLink(currentLinkId)) {
				PParking parking = AgentWithParking.parkingManager.getParkingsHashMap().get(parkingId);
				int freeCapacity = AgentWithParking.parkingManager.getFreeCapacity(parkingId);

				if (freeCapacity > 0 && parking.getId().toString().contains("gp")) {
					parkVehicle(aem, currentLinkId);
					return true;
				}
			}
		}
		return false;
	}
	
	
	
	

	private void continueDriving(AgentWithParking aem) {
		super.handleAgentLeg(aem);
	}

	// TODO: push method to AgentWithParking class
	private Coord getCurrentLinkCoordinate(AgentWithParking aem) {
		Id currentLinkId = getCurrentLinkId(aem);
		return network.getLinks().get(currentLinkId).getCoord();
	}

	private void findClosestFreeGarageAndDriveThere(AgentWithParking aem) {
		Leg leg = (LegImpl) aem.getPerson().getSelectedPlan().getPlanElements().get(aem.getPlanElementIndex());
		Id closestFreeGarageParking = AgentWithParking.parkingManager.getClosestFreeGarageParkingNotOnLink(getCurrentLinkCoordinate(aem),aem.getInvalidLinkForParking());
		Id linkOfParking = AgentWithParking.parkingManager.getLinkOfParking(closestFreeGarageParking);
		EditRoute.globalEditRoute.addLastPartToRoute(aem.getMessageArrivalTime(), leg, linkOfParking);
		super.handleAgentLeg(aem);
	}

	
	@Override
	public void handleParkingDepartureActivity(AgentWithParking aem) {
		Id personId = aem.getPerson().getId();
		super.handleParkingDepartureActivity(aem);
		garageParkingScore.remove(personId);
	}
}
