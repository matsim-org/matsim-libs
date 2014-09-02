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
package playground.wrashid.parkingSearch.ppSim.jdepSim.searchStrategies.random;

import java.util.HashMap;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;

import playground.wrashid.parkingSearch.ppSim.jdepSim.AgentWithParking;
import playground.wrashid.parkingSearch.ppSim.jdepSim.searchStrategies.RandomParkingSearch;

public class ParkAgent extends RandomParkingSearch {

	/*
	 * double startStrategyAtDistanceFromDestination = 500; double
	 * startParkingDecision = 250; int F1 = 1; int F2 = 3; double
	 * maxDistanceAcceptableForWalk=400; double maxSeachDuration=10*60;
	 */

	double startStrategyAtDistanceFromDestination;
	double startParkingDecision;
	int F1;
	int F2;
	double maxDistanceAcceptableForWalk;
	double maxSeachDur;
	double increaseAcceptableDistanceInMetersPerMinute;

	HashMap<Id, ParkAgentAttributes> attributes;

	public ParkAgent(double maxDistance, Network network, String name, double startStrategyAtDistanceFromDestination,
			double startParkingDecision, int F1, int F2, double maxDistanceAcceptableForWalk, double maxSeachDuration,double increaseAcceptableDistancePerMinute) {
		super(maxDistance, network, name);
		this.startStrategyAtDistanceFromDestination = startStrategyAtDistanceFromDestination;
		this.startParkingDecision = startParkingDecision;
		this.F1 = F1;
		this.F2 = F2;
		this.maxDistanceAcceptableForWalk = maxDistanceAcceptableForWalk;
		this.maxSeachDur = maxSeachDuration;
		increaseAcceptableDistanceInMetersPerMinute = increaseAcceptableDistancePerMinute;
		this.parkingType = "streetParking";
		super.maxSearchDuration=maxSeachDur;
	}

	@Override
	public void resetForNewIteration() {
		super.resetForNewIteration();
		attributes = new HashMap<Id, ParkAgent.ParkAgentAttributes>();
	}

	@Override
	public void handleAgentLeg(AgentWithParking aem) {
		Id personId = aem.getPerson().getId();
		if (startSearchTime.containsKey(personId)){
			double searchDuration=getSearchTime(aem);
			
			if (searchDuration>maxSeachDur){
				super.handleAgentLeg(aem);
			} else {
				parkAgentStrategy(aem, personId);
			}
		} else 	{
			parkAgentStrategy(aem, personId);
		}
	}

	public void parkAgentStrategy(AgentWithParking aem, Id personId) {
		random = RandomNumbers.getRandomNumber(personId, aem.getPlanElementIndex(), getName());
		ActivityImpl nextAct = (ActivityImpl) aem.getPerson().getSelectedPlan().getPlanElements()
				.get(aem.getPlanElementIndex() + 3);

		if (!attributes.containsKey(personId)
				&& GeneralLib.getDistance(getNextLink(aem).getCoord(), network.getLinks().get(nextAct.getLinkId()).getCoord()) < startStrategyAtDistanceFromDestination) {
			Id parkingLinkId = getParkingLinkId(aem, getParkingFilterType(personId));
			ParkAgentAttributes parkingAttr = new ParkAgentAttributes();
			if (parkingLinkId == null || !parkingLinkId.toString().contains("privateParkings")) {
				parkingAttr.takePrivateParking = false;
			} else {
				parkingAttr.takePrivateParking = true;
			}

			attributes.put(personId, parkingAttr);
			initParkingCapacitiesTillReachingDestination(aem);
			super.handleAgentLeg(aem);
		} else if (attributes.containsKey(personId)) {
			ParkAgentAttributes parkingAttr = attributes.get(personId);

			if (parkingAttr.takePrivateParking) {
				super.handleAgentLeg(aem);
			} else {
				Link nextLink = getNextLink(aem);
				Id freeParkingFacilityOnLink = AgentWithParking.parkingManager.getFreeParkingFacilityOnLink(nextLink.getId(),
						"streetParking");

				if (endOfLegReached(aem)) {
					triggerSeachTimeStart(personId, aem.getMessageArrivalTime());
					Leg leg = (LegImpl) aem.getPerson().getSelectedPlan().getPlanElements().get(aem.getPlanElementIndex());

					String filterParkingType = getParkingFilterType(personId);
					Id parkingId = getParkingLinkId(aem, filterParkingType);
					LinkNetworkRouteImpl route = (LinkNetworkRouteImpl) leg.getRoute();

					if (parkingAttr.timeWhenDestinationReached == -1) {
						parkingAttr.timeWhenDestinationReached = aem.getMessageArrivalTime();
					}

					double searchTimeDurationAfterReachingTarget = getSearchTimeDurationAfterReachingTarget(aem);
					double acceptableParkingDistance = getAcceptableParkingDistance(searchTimeDurationAfterReachingTarget);

					if (isInvalidParking(aem, parkingId)
							|| GeneralLib.getDistance(nextAct.getCoord(), nextLink.getCoord()) > acceptableParkingDistance) {

						addRandomLinkToRoute(route,aem);

						aem.processLegInDefaultWay();
					} else {
						parkVehicle(aem, parkingId);
					}
				} else {

					Id streetParkingOnLink = AgentWithParking.parkingManager.getFreeParkingFacilityOnLink(nextLink.getId(),
							"streetParking");

					if (streetParkingOnLink != null) {

						parkingAttr.totalCapacity += AgentWithParking.parkingManager.getParkingsHashMap().get(streetParkingOnLink)
								.getIntCapacity();
						parkingAttr.totalUnOccupied += AgentWithParking.parkingManager.getFreeCapacity(streetParkingOnLink);
						parkingAttr.capacitiesOfAllParkingTillDestination -= parkingAttr.totalCapacity;
					}

					if (GeneralLib.getDistance(nextAct.getCoord(), nextLink.getCoord()) < startParkingDecision
							&& parkingAttr.totalCapacity > 0) {
						double F_exp = parkingAttr.totalUnOccupied / parkingAttr.totalCapacity
								* parkingAttr.capacitiesOfAllParkingTillDestination;

						if (!isInvalidParking(aem, freeParkingFacilityOnLink) && shouldParkVehicle(F_exp)) {
							throughAwayRestOfRoute(aem);
							parkVehicle(aem, freeParkingFacilityOnLink);
						} else {
							super.handleAgentLeg(aem);
						}
					} else {
						super.handleAgentLeg(aem);
					}
				}
			}
		} else {
			super.handleAgentLeg(aem);
		}
	}

	private boolean shouldParkVehicle(double f_exp) {
		if (f_exp < F1) {
			return true;
		} else if (f_exp > F2) {
			return false;
		} else {
			double probabilityThreshHold = (f_exp - F1);
			double rand = random.nextDouble() * (F2 - F1);
			if (rand > probabilityThreshHold) {
				return true;
			} else {
				return false;
			}
		}

	}

	private double getAcceptableParkingDistance(double searchTimeDurationAfterReachingTarget) {
		if (searchTimeDurationAfterReachingTarget > maxSeachDur) {
			return Double.MAX_VALUE;
		} else {
			
			if (100 + increaseAcceptableDistanceInMetersPerMinute * searchTimeDurationAfterReachingTarget / 60 < maxDistanceAcceptableForWalk) {
				return 100 + increaseAcceptableDistanceInMetersPerMinute * searchTimeDurationAfterReachingTarget / 60;
			} else {
				return maxDistanceAcceptableForWalk;
			}
		}

	}

	private double getSearchTimeDurationAfterReachingTarget(AgentWithParking aem) {
		Id personId = aem.getPerson().getId();
		ParkAgentAttributes parkingAttr = attributes.get(personId);

		if (parkingAttr.timeWhenDestinationReached == aem.getMessageArrivalTime()) {
			return 0;
		} else {
			return GeneralLib.getIntervalDuration(parkingAttr.timeWhenDestinationReached, aem.getMessageArrivalTime());
		}
	}

	private void initParkingCapacitiesTillReachingDestination(AgentWithParking aem) {
		Id personId = aem.getPerson().getId();
		ParkAgentAttributes parkingAttr = attributes.get(personId);

		Leg leg = (LegImpl) aem.getPerson().getSelectedPlan().getPlanElements().get(aem.getPlanElementIndex());

		List<Id<Link>> linkIds = ((LinkNetworkRouteImpl) leg.getRoute()).getLinkIds();
		Link nextLink = getNextLink(aem);

		boolean countCapacity = false;
		for (Id linkId : linkIds) {
			if (GeneralLib.equals(linkId, nextLink.getId())) {
				countCapacity = true;
			}

			if (countCapacity) {
				Id streetParkingOnLink = AgentWithParking.parkingManager.getFreeParkingFacilityOnLink(linkId, "streetParking");

				if (streetParkingOnLink != null) {
					parkingAttr.capacitiesOfAllParkingTillDestination += AgentWithParking.parkingManager.getParkingsHashMap()
							.get(streetParkingOnLink).getIntCapacity();
				}
			}
		}

		if (countCapacity) {
			Id streetParkingOnLink = AgentWithParking.parkingManager.getFreeParkingFacilityOnLink(
					((LinkNetworkRouteImpl) leg.getRoute()).getEndLinkId(), "streetParking");

			if (streetParkingOnLink != null) {
				parkingAttr.capacitiesOfAllParkingTillDestination += AgentWithParking.parkingManager.getParkingsHashMap()
						.get(streetParkingOnLink).getIntCapacity();
			}
		}
	}

	private static class ParkAgentAttributes {
		boolean takePrivateParking = true;
		int totalCapacity = 0;
		int totalUnOccupied = 0;
		int capacitiesOfAllParkingTillDestination = 0;
		double timeWhenDestinationReached = -1;
	}
	
	@Override
	public void handleParkingDepartureActivity(AgentWithParking aem) {
		super.handleParkingDepartureActivity(aem);
		useSpecifiedParkingType.remove(aem.getPerson().getId());
	}

}
