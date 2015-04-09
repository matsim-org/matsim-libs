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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.multimodal.router.util.WalkTravelTime;
import org.matsim.contrib.parking.lib.DebugLib;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.DoubleValueHashMap;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import playground.wrashid.parkingChoice.infrastructure.api.PParking;
import playground.wrashid.parkingSearch.ppSim.jdepSim.AgentWithParking;
import playground.wrashid.parkingSearch.ppSim.jdepSim.routing.EditRoute;
import playground.wrashid.parkingSearch.ppSim.jdepSim.routing.threads.RerouteTaskDuringSim;
import playground.wrashid.parkingSearch.ppSim.jdepSim.routing.threads.RerouteThreadDuringSim;
import playground.wrashid.parkingSearch.ppSim.jdepSim.searchStrategies.analysis.ParkingEventDetails;
import playground.wrashid.parkingSearch.ppSim.jdepSim.searchStrategies.random.RandomNumbers;
import playground.wrashid.parkingSearch.ppSim.jdepSim.zurich.ZHScenarioGlobal;
import playground.wrashid.parkingSearch.withinDay_v_STRC.scoring.ParkingActivityAttributes;

import java.util.*;

public class RandomParkingSearch implements ParkingSearchStrategy {

	protected DoubleValueHashMap<Id> startSearchTime;
	protected HashMap<Id, ParkingActivityAttributes> parkingActAttributes;
	protected HashMap<Id, String> useSpecifiedParkingType;
	protected HashSet<Id> tollAreaEntered;

	protected String parkingType;
	private double maxDistance;
	protected Network network;
	protected Random random;
	private final double parkingDuration = 60 * 2;
	protected double walkSpeed = 3.0 / 3.6; // [m/s]
	protected double scoreInterrupationValue = 0;
	private String name;
	public HashSet<Id> extraSearchPathNeeded = new HashSet<Id>();
	private String groupName;
	private WalkTravelTime walkTravelTime;
	private double searchBeta;
	private double randomSearchDistance;
	protected double maxSearchDuration;

	// go to final link if no parking there, then try parking at other places.
	// accept only parking within 300m, choose random links, but if leave 300m
	// area, try
	// to take direction leading back to destination
	public RandomParkingSearch(double maxDistance, Network network, String name) {
		this.maxDistance = maxDistance;
		this.network = network;
		this.name = name;

		setSearchBeta(ZHScenarioGlobal.loadDoubleParam("RandomParkingSearch.searchBeta"));
		setRandomSearchDistance(ZHScenarioGlobal.loadDoubleParam("RandomParkingSearch.randomSearchDistance"));
		
		walkTravelTime = new WalkTravelTime(new PlansCalcRouteConfigGroup(), ZHScenarioGlobal.linkSlopes);
		resetForNewIteration();
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void handleAgentLeg(AgentWithParking aem) {
		maxSearchDuration=ZHScenarioGlobal.loadDoubleParam("RandomParkingSearch.maxSearchTime");
		walkSpeed = getWalkSpeed(aem);
		
		Leg leg = (LegImpl) aem.getPerson().getSelectedPlan().getPlanElements().get(aem.getPlanElementIndex());
		ActivityImpl nextAct = (ActivityImpl) aem.getPerson().getSelectedPlan().getPlanElements()
				.get(aem.getPlanElementIndex() + 1);

		if (leg.getMode().equalsIgnoreCase(TransportMode.car)) {
			Id personId = aem.getPerson().getId();

			List<Id<Link>> linkIds = ((LinkNetworkRouteImpl) leg.getRoute()).getLinkIds();

			LinkNetworkRouteImpl route = (LinkNetworkRouteImpl) leg.getRoute();
			boolean endOfLegReached = aem.getCurrentLinkIndex() == linkIds.size() - 1;

			if (endOfLegReached) {
				triggerSeachTimeStart(personId, aem.getMessageArrivalTime());
				 

				String filterParkingType = getParkingFilterType(personId);

				Id<PParking> parkingId = getParkingLinkId(aem, filterParkingType);

			//	boolean isInvalidLink = aem.isLastLinkOfRouteInvalidLinkForParking();

				// TODO: include max distance here (maxDistance variable)
				parkingId = injectBackupGarageParkingIfNeeded(aem, personId, parkingId);

				
				
				
				if (isInvalidParking(aem, parkingId)) {
					DebugLib.traceAgent(personId, 1);
					// extraSearchPathNeeded.add(personId);

					random = RandomNumbers.getRandomNumber(personId, aem.getPlanElementIndex(), getName());
					addRandomLinkToRoute(route, aem);

					// this will just continue the search of the agent
					aem.processLegInDefaultWay();

				} else {
					parkVehicle(aem, parkingId);
				}
			} else {
				aem.processLegInDefaultWay();
			}
		} else {

			setDurationOfParkingActivity(aem, nextAct);

			aem.processLegInDefaultWay();

		}

		// log search time and path! TODO:

		// TODO: add score only at end of search (store it locally during
		// search)!

		// if (aem.getPlanElementIndex() >1 && aem.getPlanElementIndex() % 2 ==
		// 0){
		// AgentWithParking.parkingStrategyManager.updateScore(person.getId(),
		// aem.getPlanElementIndex()-1, 1*rand.nextDouble());
		// }

		// only consider arrival distance at the moment for scoring (both in
		// future - but for this plans have to be pre-processed and cleaned
		// first).
	}

	public String getParkingFilterType(Id personId) {
		String filterParkingType = parkingType;

		if (useSpecifiedParkingType.containsKey(personId)) {
			filterParkingType = useSpecifiedParkingType.get(personId);
		}
		return filterParkingType;
	}

	public Id getParkingLinkId(AgentWithParking aem, String filterParkingType) {
		Id personId = aem.getPerson().getId();
		Leg leg = (LegImpl) aem.getPerson().getSelectedPlan().getPlanElements().get(aem.getPlanElementIndex());
		LinkNetworkRouteImpl route = (LinkNetworkRouteImpl) leg.getRoute();
		ActivityImpl nextNonParkingAct = (ActivityImpl) aem.getPerson().getSelectedPlan().getPlanElements()
				.get(aem.getPlanElementIndex() + 3);

		Id parkingId = AgentWithParking.parkingManager.getFreePrivateParking(nextNonParkingAct.getFacilityId(),
				nextNonParkingAct.getType());

		// actually would need to drive to private parking, but this is
		// skipped here
		// ||
		// !AgentWithParking.parkingManager.getLinkOfParking(parkingId).toString().equalsIgnoreCase(route.getEndLinkId().toString())
		
		
		
		if (isInvalidParking(aem, parkingId)) {
			parkingId = AgentWithParking.parkingManager.getFreeParkingFacilityOnLink(route.getEndLinkId(), filterParkingType);
		}
		
		return parkingId;
	}

	public Id injectBackupGarageParkingIfNeeded(AgentWithParking aem, Id<Person> personId, Id<PParking> parkingId) {
		if (getSearchTime(aem)>maxSearchDuration){
			ActivityImpl nextNonParkAct = (ActivityImpl) aem.getPerson().getSelectedPlan().getPlanElements()
					.get(aem.getPlanElementIndex() + 3);
			
			//parkingId=AgentWithParking.parkingManager.getClosestFreeGarageParking(nextNonParkAct.getCoord());
			//parkingId=new IdImpl("backupParking");
			
			if (isInvalidParking(aem, parkingId)) {
				parkingId = AgentWithParking.parkingManager.getClosestFreeGarageParkingNotOnLink(aem.getCurrentLink().getCoord(),aem.getInvalidLinkForParking());
			}
			
			if (parkingId!=null){
				// add search time
				double searchTime = Dummy_TakeClosestParking.getSearchTime(aem, parkingId);
				if (startSearchTime.containsKey(personId)){
					double d = startSearchTime.get(personId);
					d-=searchTime;
					startSearchTime.put(personId, d);
					
					if (d<0){
						DebugLib.emptyFunctionForSettingBreakPoint();
					}
				}
			} else {
				parkingId=Id.create("backupParking", PParking.class);
			}
			
			//startSearchTime.put(personId, -1.0);
		}
		return parkingId;
	}

	public static boolean isInvalidParking(AgentWithParking aem, Id parkingId) {
		Id linkOfParking = AgentWithParking.parkingManager.getLinkOfParking(parkingId);
		return parkingId == null || (aem.getInvalidLinkForParking()!=null && linkOfParking.toString().equals(aem.getInvalidLinkForParking().toString()));
	}

	public void triggerSeachTimeStart(Id personId, double time) {
		if (time<0){
			DebugLib.emptyFunctionForSettingBreakPoint();
		}
		
		
		if (!startSearchTime.containsKey(personId)) {
			startSearchTime.put(personId, time);
		}
	}

	protected void parkVehicle(AgentWithParking aem, Id parkingId) {
		ActivityImpl nextNonParkingAct = (ActivityImpl) aem.getPerson().getSelectedPlan().getPlanElements()
				.get(aem.getPlanElementIndex() + 3);
		Id personId = aem.getPerson().getId();
		Leg leg = (LegImpl) aem.getPerson().getSelectedPlan().getPlanElements().get(aem.getPlanElementIndex());
		LinkNetworkRouteImpl route = (LinkNetworkRouteImpl) leg.getRoute();
		ActivityImpl nextAct = (ActivityImpl) aem.getPerson().getSelectedPlan().getPlanElements()
				.get(aem.getPlanElementIndex() + 1);

		aem.processEndOfLegCarMode_processEvents(leg, nextAct);

		setDurationOfParkingActivity(aem, nextAct);

		Leg nextWalkLeg = (Leg) aem.getPerson().getSelectedPlan().getPlanElements().get(aem.getPlanElementIndex() + 2);
		//Link parkingLink = network.getLinks().get(route.getEndLinkId());
		Link parkingLink= network.getLinks().get( AgentWithParking.parkingManager.getLinkOfParking(parkingId));

		nextAct.setLinkId(parkingLink.getId());

		double walkDistance = GeneralLib.getDistance(parkingLink.getCoord(), nextNonParkingAct.getCoord());
		// TODO: improve this later (no straight line)
		double walkDuration = walkDistance / walkSpeed;
		nextWalkLeg.setTravelTime(walkDuration);
		getParkingAttributesForScoring(aem).setToActWalkDuration(walkDuration);
		getParkingAttributesForScoring(aem).setToParkWalkDuration(walkDuration);
		getParkingAttributesForScoring(aem).setWalkDistance(walkDistance);

		// TODO: update this later, this is an approximation
		getParkingAttributesForScoring(aem).setParkingArrivalTime(aem.getMessageArrivalTime());

		// check, if more car legs, only in that case adapt that
		// leg (TODO:)

		// if car departs again during day, adapt the departure
		// walking and routes

		int indexOfNextCarLeg = aem.duringCarLeg_getPlanElementIndexOfNextCarLeg();

		// TODO: avoid routing if parking link Id and activity
		// linkId remained the same
		if (indexOfNextCarLeg != -1) {
			ActivityImpl lastActBeforeNextCarLeg = (ActivityImpl) aem.getPerson().getSelectedPlan().getPlanElements()
					.get(indexOfNextCarLeg - 3);
			Leg nextwalkLegToParking = (Leg) aem.getPerson().getSelectedPlan().getPlanElements().get(indexOfNextCarLeg - 2);
			ActivityImpl nextParkingAct = (ActivityImpl) aem.getPerson().getSelectedPlan().getPlanElements()
					.get(indexOfNextCarLeg - 1);
			Leg nextCarLeg = (Leg) aem.getPerson().getSelectedPlan().getPlanElements().get(indexOfNextCarLeg);

			walkDistance = GeneralLib.getDistance(parkingLink.getCoord(), lastActBeforeNextCarLeg.getCoord());
			walkDuration = walkDistance / walkSpeed;
			nextwalkLegToParking.setTravelTime(walkDuration);

			nextParkingAct.setLinkId(parkingLink.getId());
			
			ActivityImpl overNextParkingAct = (ActivityImpl) aem.getPerson().getSelectedPlan().getPlanElements()
					.get(indexOfNextCarLeg + 1);
			if (nextParkingAct.getLinkId().toString().equals(overNextParkingAct.getLinkId().toString())){
				DebugLib.emptyFunctionForSettingBreakPoint();
			}
			
			
			if (ZHScenarioGlobal.loadBooleanParam("RandomParkingSearch.performReroutingDuringSimulation")) {
				if (ZHScenarioGlobal.turnParallelRoutingOnDuringSim) {
					aem.rerouteTask = new RerouteTaskDuringSim(nextNonParkingAct.getEndTime(), parkingLink.getId(), nextCarLeg);
					RerouteThreadDuringSim.addTask(aem.rerouteTask);
				} else {
					EditRoute.globalEditRoute
							.addInitialPartToRoute(nextNonParkingAct.getEndTime(), parkingLink.getId(), nextCarLeg);
				}
			}

		}

		// } else {
		// aem.processLegInDefaultWay();
		// }

		logParkingSearchDuration(aem);

		AgentWithParking.parkingManager.parkVehicle(personId, parkingId);

		if (aem.getPlanElementIndex() == aem.getPlanElementIndexOfLastCarLeg()) {
			handleLastParkingScore(aem);
		}

		aem.processEndOfLegCarMode_scheduleNextActivityEndEventIfNeeded(nextAct);
	}

	protected double getWalkSpeed(AgentWithParking aem) {
		if (ZHScenarioGlobal.paramterExists("globalWalkSpeedInMetersPerSecond")){
			return ZHScenarioGlobal.loadDoubleParam("globalWalkSpeedInMetersPerSecond");
		}
		
		Link currentLink = aem.getCurrentLink();
		double length = currentLink.getLength();
		double walkTime = walkTravelTime.getLinkTravelTime(aem.getCurrentLink(), 0, aem.getPerson(), null);
		return length / walkTime;
	}

	public void addRandomLinkToRoute(LinkNetworkRouteImpl route, AgentWithParking aem) {
		Link link = network.getLinks().get(route.getEndLinkId());

		Link nextLink = getNextLink(link, aem);

		ArrayList<Id<Link>> newRoute = new ArrayList<Id<Link>>();
		newRoute.addAll(route.getLinkIds());
		newRoute.add(link.getId());
		route.setLinkIds(route.getStartLinkId(), newRoute, nextLink.getId());
		route.setEndLinkId(nextLink.getId());
	}

	private void setDurationOfParkingActivity(AgentWithParking aem, ActivityImpl nextAct) {
		if (nextAct.getType().equalsIgnoreCase("parking")) {
			nextAct.setEndTime(aem.getMessageArrivalTime() + parkingDuration);
		}
	}

	private Link getOppositeDirectionLink(Link link){
		List<Link> links = new ArrayList<Link>(link.getToNode().getOutLinks().values());
		
		for (Link candidate:links){
			if (candidate.getToNode().equals(link.getFromNode())){
				return candidate;
			}
		}
		
		return null;
	}
	
	private Link getNextLink(Link link, AgentWithParking aem) {
		Link oppositeDirectionLink=getOppositeDirectionLink(link);
		List<Link> links = new ArrayList<Link>(link.getToNode().getOutLinks().values());
		ActivityImpl nextNonParkingAct = (ActivityImpl) aem.getPerson().getSelectedPlan().getPlanElements()
				.get(aem.getPlanElementIndex() + 3);

		if (links.size()==1){
			return links.get(0);
		} else if (GeneralLib.getDistance(link.getCoord(), nextNonParkingAct.getCoord()) < getRandomSearchDistance() || searchBeta==-1) {
			return randomNextLink(link);
		} else {
			//if (oppositeDirectionLink!=null){
			//	links.remove(oppositeDirectionLink);
			//}
			
			double probabilitySum = 0;
			double[] distances = new double[links.size()];
			double[] selectionProbabilities = new double[links.size()];
			double minDistance=Double.MAX_VALUE;
			double averageDistance=0;

			for (int i = 0; i < links.size(); i++) {
				distances[i] = GeneralLib.getDistance(links.get(i).getToNode().getCoord(), nextNonParkingAct.getCoord());

				if (minDistance>distances[i]){
					minDistance=distances[i];
				}
				
			//	if (links.size()>1 && oppositeDirectionLink!=null && oppositeDirectionLink==links.get(i)){
			//		distances[i]*=2;
			//	}
				
			}
			
			for (int i = 0; i < links.size(); i++) {
				averageDistance+=distances[i]-minDistance;
			}
			
			averageDistance/=links.size();
			averageDistance/=searchBeta;
			
			for (int i = 0; i < links.size(); i++) {
				probabilitySum += 1 / (distances[i]-minDistance+averageDistance);
			}

			for (int i = 0; i < links.size(); i++) {
				selectionProbabilities[i] = 1 / (distances[i]-minDistance+averageDistance) / probabilitySum;
			}

			double r = random.nextDouble();
			int index = 0;
			double sum = 0;

			while (sum + selectionProbabilities[index] < r) {
				sum += selectionProbabilities[index];
				index++;
			}

			//if (oppositeDirectionLink!=null && random.nextDouble()>0.8){
			//	return oppositeDirectionLink;
			//} else {
				return links.get(index);
			//}
		}
	}
	
//	private Link getNextLink(Link link, AgentWithParking aem) {
//		Link oppositeDirectionLink=getOppositeDirectionLink(link);
//		List<Link> links = new ArrayList<Link>(link.getToNode().getOutLinks().values());
//		ActivityImpl nextNonParkingAct = (ActivityImpl) aem.getPerson().getSelectedPlan().getPlanElements()
//				.get(aem.getPlanElementIndex() + 3);
//
//		if (GeneralLib.getDistance(link.getCoord(), nextNonParkingAct.getCoord()) < getRandomSearchDistance()) {
//			return randomNextLink(link);
//		} else {
//			double exponentialSum = 0;
//			double[] distances = new double[links.size()];
//			double[] selectionProbabilities = new double[links.size()];
//			double minDistance=Double.MAX_VALUE;
//
//			for (int i = 0; i < links.size(); i++) {
//				distances[i] = GeneralLib.getDistance(links.get(i).getToNode().getCoord(), nextNonParkingAct.getCoord());
//
//				if (minDistance>distances[i]){
//					minDistance=distances[i];
//				}
//				
//				if (links.size()>1 && oppositeDirectionLink!=null && oppositeDirectionLink==links.get(i)){
//					distances[i]*=10;
//				}
//			}
//
//			for (int i = 0; i < links.size(); i++) {
//				exponentialSum += Math.exp(getSearchBeta() / (distances[i]-minDistance+100));
//			}
//
//			for (int i = 0; i < links.size(); i++) {
//				selectionProbabilities[i] = Math.exp(getSearchBeta() / (distances[i]-minDistance+100)) / exponentialSum;
//			}
//
//			double r = random.nextDouble();
//			int index = 0;
//			double sum = 0;
//
//			while (sum + selectionProbabilities[index] < r) {
//				sum += selectionProbabilities[index];
//				index++;
//			}
//
//			return links.get(index);
//		}
//	}

	private Link randomNextLink(Link link) {
		List<Link> links = new ArrayList<Link>(link.getToNode().getOutLinks().values());

		int i = random.nextInt(links.size());
		return links.get(i);
	}

	private void logParkingSearchDuration(AgentWithParking aem) {
		Id personId = aem.getPerson().getId();
		double searchDuration = getSearchTime(aem);

		getParkingAttributesForScoring(aem).setParkingSearchDuration(searchDuration);
		startSearchTime.remove(personId);
	}

	/*
	 * public void handleParkingScoring(AgentWithParking aem) { Id personId =
	 * aem.getPerson().getId(); double
	 * searchDuration=GeneralLib.getIntervalDuration
	 * (startSearchTime.get(personId),aem.getMessageArrivalTime()); if
	 * (aem.getPlanElementIndex() > 1 && aem.getPlanElementIndex() % 2 == 1) {
	 * if (startSearchTime.containsKey(personId)){
	 * AgentWithParking.parkingStrategyManager.updateScore(personId,
	 * aem.getPlanElementIndex(), searchDuration); } else {
	 * AgentWithParking.parkingStrategyManager.updateScore(personId,
	 * aem.getPlanElementIndex(), searchDuration); } } /* if (this instanceof
	 * RandomGarageParkingSearch){
	 * AgentWithParking.parkingStrategyManager.updateScore(personId,
	 * aem.getPlanElementIndex(), 1000); } else {
	 * AgentWithParking.parkingStrategyManager.updateScore(personId,
	 * aem.getPlanElementIndex(), 0); }
	 */
	/*
	 * startSearchTime.remove(personId); }
	 */

	public double getSearchTime(AgentWithParking aem) {
		Id personId = aem.getPerson().getId();
		if (startSearchTime.get(personId)==-1){
			return 86400;
		}
		
		double searchDuration = GeneralLib.getIntervalDuration(startSearchTime.get(personId), aem.getMessageArrivalTime());

		if (searchDuration == 86400) {
			searchDuration = 0;
		}
		return searchDuration;
	}

	public void handleLastParkingScore(AgentWithParking aem) {
		Id personId = aem.getPerson().getId();
		ParkingActivityAttributes parkingAttributesForScoring = getParkingAttributesForScoring(aem);
		Id currentParkingId = AgentWithParking.parkingManager.getCurrentParkingId(aem.getPerson().getId());

		Activity act = (Activity) aem.getPerson().getSelectedPlan().getPlanElements().get(aem.getIndexOfFirstCarLegOfDay() - 3);

		parkingAttributesForScoring.setFacilityId(currentParkingId);

		double parkingDuration = GeneralLib.getIntervalDuration(aem.getMessageArrivalTime(), act.getEndTime());
		if (aem.getMessageArrivalTime() == act.getEndTime()) {
			parkingDuration = 0;
		}
		parkingAttributesForScoring.setParkingDuration(parkingDuration);

		// just an approximation - TODO: update later
		parkingAttributesForScoring.setActivityDuration(parkingAttributesForScoring.getParkingDuration());
		parkingAttributesForScoring.setParkingCost(ZHScenarioGlobal.parkingScoreEvaluator
				.getParkingCost(parkingAttributesForScoring));

		double parkingScore = ZHScenarioGlobal.parkingScoreEvaluator.getParkingScore(parkingAttributesForScoring);
		parkingScore = handleCostScore(personId, parkingAttributesForScoring, parkingScore);

		Activity nextActivity = (Activity) aem.getPerson().getSelectedPlan().getPlanElements().get(aem.getPlanElementIndex() + 3);
		parkingAttributesForScoring.destinationCoord=nextActivity.getCoord();
		
		
		AgentWithParking.parkingStrategyManager.updateScore(personId, aem.getPlanElementIndex(), parkingScore);
		ZHScenarioGlobal.parkingEventDetails
				.add(new ParkingEventDetails(aem.getPlanElementIndex(), parkingScore, AgentWithParking.parkingStrategyManager
						.getParkingStrategyForCurrentLeg(aem.getPerson(), aem.getPlanElementIndex()), parkingAttributesForScoring,nextActivity.getType()));
		resetVariables(personId);
	}

	public double handleCostScore(Id personId, ParkingActivityAttributes parkingAttributesForScoring, double parkingScore) {
		if (tollAreaEntered.contains(personId)
				&& !parkingAttributesForScoring.getFacilityId().toString().contains("privateParkings")) {
			scoreInterrupationValue += ZHScenarioGlobal.parkingScoreEvaluator.getParkingCostScore(personId,
					ZHScenarioGlobal.loadDoubleParam("costForEnertingTolledArea"));
		}

		if ((parkingAttributesForScoring.getFacilityId().toString().contains("illegal") || tollAreaEntered.contains(personId))
				&& !parkingAttributesForScoring.getFacilityId().toString().contains("privateParkings")) {
			parkingScore += scoreInterrupationValue;
			parkingAttributesForScoring.setParkingCost(scoreInterrupationValue);
		}
		return parkingScore;
	}

	public Link getNextLink(AgentWithParking aem) {
		return aem.getNextLink();
	}

	protected boolean endOfLegReached(AgentWithParking aem) {
		return aem.endOfLegReached();
	}

	@Override
	public void handleParkingDepartureActivity(AgentWithParking aem) {
		Id personId = aem.getPerson().getId();
		ParkingActivityAttributes parkingAttributesForScoring = getParkingAttributesForScoring(aem);
		// handleParkingScoring(aem);

		ActivityImpl currentActivity = aem.getCurrentActivity();

		// int nextLegIndex = aem.getPlanElementIndex() + 1;
		// Leg leg = (LegImpl)
		// aem.getPerson().getSelectedPlan().getPlanElements().get(nextLegIndex);

		Id currentParkingId = AgentWithParking.parkingManager.getCurrentParkingId(aem.getPerson().getId());

		parkingAttributesForScoring.setFacilityId(currentParkingId);

		double parkingDuration = GeneralLib.getIntervalDuration(parkingAttributesForScoring.getParkingArrivalTime(),
				aem.getMessageArrivalTime());
		if (parkingAttributesForScoring.getParkingArrivalTime() == aem.getMessageArrivalTime()) {
			parkingDuration = 0;
		}
		parkingAttributesForScoring.setParkingDuration(parkingDuration);

		// just an approximation - TODO: update later
		parkingAttributesForScoring.setActivityDuration(parkingAttributesForScoring.getParkingDuration());
		parkingAttributesForScoring.setParkingCost(ZHScenarioGlobal.parkingScoreEvaluator
				.getParkingCost(parkingAttributesForScoring));

		double parkingScore = ZHScenarioGlobal.parkingScoreEvaluator.getParkingScore(parkingAttributesForScoring);
		parkingScore = handleCostScore(personId, parkingAttributesForScoring, parkingScore);

		Activity nextActivity = (Activity) aem.getPerson().getSelectedPlan().getPlanElements().get(aem.getIndexOfFirstCarLegOfDay() + 3);
		
		
		int legIndex = aem.duringAct_getPlanElementIndexOfPreviousCarLeg();
		AgentWithParking.parkingStrategyManager.updateScore(personId, legIndex, parkingScore);
		ZHScenarioGlobal.parkingEventDetails.add(new ParkingEventDetails(legIndex, parkingScore,
				AgentWithParking.parkingStrategyManager.getParkingStrategyForCurrentLeg(aem.getPerson(), legIndex),
				parkingAttributesForScoring,nextActivity.getType()));
		resetVariables(personId);
	}

	public void resetVariables(Id personId) {
		parkingActAttributes.remove(personId);
		scoreInterrupationValue = 0;
		tollAreaEntered.remove(personId);
	}

	// TODO: log parkingAttributesForScoring + score + leg index

	protected ParkingActivityAttributes getParkingAttributesForScoring(AgentWithParking aem) {
		Id personId = aem.getPerson().getId();
		if (!parkingActAttributes.containsKey(personId)) {
			parkingActAttributes.put(personId, new ParkingActivityAttributes(personId));
		}
		return parkingActAttributes.get(personId);
	}

	@Override
	public void resetForNewIteration() {
		startSearchTime = new DoubleValueHashMap<Id>();
		parkingActAttributes = new HashMap<Id, ParkingActivityAttributes>();
		useSpecifiedParkingType = new HashMap<Id, String>();
		tollAreaEntered = new HashSet<Id>();
	}

	/*
	 * public void processEndOfLegCarMode(Leg leg, ActivityImpl
	 * nextAct,AgentWithParking aem) { Event event;
	 * 
	 * List<Id> linkIds = ((LinkNetworkRouteImpl)leg.getRoute()).getLinkIds();
	 * Id currentLinkId=null; if (aem.getCurrentLinkIndex()==-1){
	 * currentLinkId=((LinkNetworkRouteImpl)leg.getRoute()).getStartLinkId(); }
	 * else { currentLinkId = linkIds.get(aem.getCurrentLinkIndex()); }
	 * 
	 * event=new
	 * LinkLeaveEvent(aem.getMessageArrivalTime(),aem.getPerson().getId
	 * (),currentLinkId,aem.getPerson().getId());
	 * Message.eventsManager.processEvent(event);
	 * 
	 * Id endLinkId = leg.getRoute().getEndLinkId(); event=new
	 * LinkEnterEvent(aem
	 * .getMessageArrivalTime(),aem.getPerson().getId(),endLinkId
	 * ,aem.getPerson().getId()); Message.eventsManager.processEvent(event);
	 * 
	 * event = new
	 * PersonArrivalEvent(aem.getMessageArrivalTime(),aem.getPerson()
	 * .getId(),endLinkId , leg.getMode());
	 * Message.eventsManager.processEvent(event);
	 * 
	 * aem.setPlanElementIndex(aem.getPlanElementIndex() + 1); boolean
	 * isLastActivity =
	 * aem.getPlanElementIndex()==aem.getPerson().getSelectedPlan
	 * ().getPlanElements().size()-1;
	 * 
	 * event = new
	 * ActivityStartEvent(aem.getMessageArrivalTime(),aem.getPerson().getId(),
	 * endLinkId, nextAct.getFacilityId(), nextAct.getType());
	 * aem.eventsManager.processEvent(event);
	 * 
	 * 
	 * if (!isLastActivity){ double endTimeOfActivity =
	 * getEndTimeOfActivity(nextAct,getMessageArrivalTime());
	 * 
	 * setMessageArrivalTime(endTimeOfActivity); messageQueue.schedule(this); }
	 * }
	 */

	protected Link getCurrentLink(AgentWithParking aem) {
		return aem.getCurrentLink();
	}

	protected Id<Link> getCurrentLinkId(AgentWithParking aem) {
		return aem.getCurrentLinkId();
	}

	protected void throughAwayRestOfRoute(AgentWithParking aem) {
		Leg leg = (LegImpl) aem.getPerson().getSelectedPlan().getPlanElements().get(aem.getPlanElementIndex());
		LinkNetworkRouteImpl route = ((LinkNetworkRouteImpl) leg.getRoute());

		List<Id<Link>> linkIds = new LinkedList<Id<Link>>();
		for (int i = 0; i <= aem.getCurrentLinkIndex(); i++) {
			linkIds.add(route.getLinkIds().get(i));
		}

		leg.setRoute(new LinkNetworkRouteImpl(route.getStartLinkId(), linkIds, getNextLink(aem).getId()));
	}

	@Override
	public void tollAreaEntered(AgentWithParking aem) {
		if (!tollAreaEntered.contains(aem.getPerson().getId())) {
			tollAreaEntered.add(aem.getPerson().getId());
		}
	}

	@Override
	public String getGroupName() {
		return groupName;
	}

	@Override
	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}

	@Override
	public void initParkingAttributes(AgentWithParking aem) {
		ParkingActivityAttributes parkingAttributesForScoring = getParkingAttributesForScoring(aem);
		ActivityImpl nextAct = (ActivityImpl) aem.getPerson().getSelectedPlan().getPlanElements()
				.get(aem.getPlanElementIndex() + 3);
		parkingAttributesForScoring.destinationCoord = nextAct.getCoord();
	}

	public double getSearchBeta() {
		return searchBeta;
	}

	public void setSearchBeta(double searchBeta) {
		this.searchBeta = searchBeta;
	}

	public double getRandomSearchDistance() {
		return randomSearchDistance;
	}

	public void setRandomSearchDistance(double randomSearchDistance) {
		this.randomSearchDistance = randomSearchDistance;
	}

}
