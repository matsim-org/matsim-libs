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
package playground.wrashid.parkingSearch.ppSim.jdepSim;

import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.parking.lib.DebugLib;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;

import playground.wrashid.parkingChoice.trb2011.ParkingHerbieControler;
import playground.wrashid.parkingSearch.ppSim.jdepSim.routing.threads.RerouteTaskDuringSim;
import playground.wrashid.parkingSearch.ppSim.jdepSim.searchStrategies.manager.ParkingStrategyManager;
import playground.wrashid.parkingSearch.ppSim.jdepSim.zurich.ParkingManagerZH;
import playground.wrashid.parkingSearch.ppSim.jdepSim.zurich.ZHScenarioGlobal;

public class AgentWithParking extends AgentEventMessage {

	public static ParkingStrategyManager parkingStrategyManager;
	public static ParkingManagerZH parkingManager;
	public RerouteTaskDuringSim rerouteTask = null;
	public static int numberOfTollAreaEntryEvents;

	public static void reset() {
		numberOfTollAreaEntryEvents=0;
	}
	
	public AgentWithParking(Person person) {
		this.setPerson(person);
		this.setPlanElementIndex(0);
		ActivityImpl ai = (ActivityImpl) person.getSelectedPlan().getPlanElements().get(getPlanElementIndex());
		setMessageArrivalTime(ai.getEndTime());
	}

	public void scheduleMessage() {
		messageQueue.schedule(this);
	}

	@Override
	public void processEvent() {
		if (getPerson().getSelectedPlan().getPlanElements().get(getPlanElementIndex()) instanceof ActivityImpl) {
			Activity act = (Activity) getPerson().getSelectedPlan().getPlanElements().get(getPlanElementIndex());
			Leg nextLeg = (Leg) getPerson().getSelectedPlan().getPlanElements().get(getPlanElementIndex() + 1);

			if (act.getType().equalsIgnoreCase("parking") && nextLeg.getMode().equals(TransportMode.car)) {

				// (don't do this for first parking)
				if (getPlanElementIndex() > getIndexOfFirstCarLegOfDay()) {
					parkingStrategyManager.getParkingStrategyForCurrentLeg(getPerson(),
							duringAct_getPlanElementIndexOfPreviousCarLeg()).handleParkingDepartureActivity(this);
				}

				AgentWithParking.parkingManager.unParkAgentVehicle(getPerson().getId());
			}

			handleActivityEndEvent();
		} else {
			Leg leg = (Leg) getPerson().getSelectedPlan().getPlanElements().get(getPlanElementIndex());

			if (rerouteTask != null) {
				if (rerouteTask.getLeg() == leg) {
					rerouteTask.waitUntilDone();
				}
			}

			if (leg.getMode().equalsIgnoreCase(TransportMode.car)) {
				LinkNetworkRouteImpl route = (LinkNetworkRouteImpl) leg.getRoute();
				if (route.getLinkIds().size()>500){
					DebugLib.emptyFunctionForSettingBreakPoint();
				}
				
				
				performSiutationUpdatesForParkingMemory();

				logIfTolledAreaEntered();

				parkingStrategyManager.getParkingStrategyForCurrentLeg(getPerson(), planElementIndex).initParkingAttributes(this);
				parkingStrategyManager.getParkingStrategyForCurrentLeg(getPerson(), planElementIndex).handleAgentLeg(this);

			} else {
				handleLeg();
			}
		}
	}

	private void logIfTolledAreaEntered() {
		Coord coordinatesLindenhofZH = ParkingHerbieControler.getCoordinatesLindenhofZH();

		Link currentLink = getCurrentLink();
		Link nextLink = getNextLink();

		if (getPerson().getId().toString().equalsIgnoreCase("948") && getPlanElementIndex()==11){
			
			Activity act = (Activity) getPerson().getSelectedPlan().getPlanElements().get(getPlanElementIndex()+3);
			
			boolean a=GeneralLib.getDistance(act.getCoord(), coordinatesLindenhofZH) > ZHScenarioGlobal.loadDoubleParam("radiusTolledArea");
			boolean b=GeneralLib.getDistance(currentLink.getCoord(), coordinatesLindenhofZH) > ZHScenarioGlobal.loadDoubleParam("radiusTolledArea");
			DebugLib.emptyFunctionForSettingBreakPoint();
		}
		
		if (GeneralLib.getDistance(currentLink.getCoord(), coordinatesLindenhofZH) > ZHScenarioGlobal
				.loadDoubleParam("radiusTolledArea")
				&& GeneralLib.getDistance(nextLink.getCoord(), coordinatesLindenhofZH) < ZHScenarioGlobal
						.loadDoubleParam("radiusTolledArea")) {
			parkingStrategyManager.getParkingStrategyForCurrentLeg(getPerson(), planElementIndex).tollAreaEntered(this);
			numberOfTollAreaEntryEvents++;
		}
	}

	private void performSiutationUpdatesForParkingMemory() {
		/*
		 * ParkingMemory parkingMemory =
		 * ParkingMemory.getParkingMemory(getPerson().getId(),
		 * getPlanElementIndex()); if
		 * (parkingMemory.closestFreeGarageParkingAtTimeOfArrival == null) {
		 * Activity nextActivity = (Activity)
		 * getPerson().getSelectedPlan().getPlanElements
		 * ().get(getPlanElementIndex() + 3);
		 * 
		 * Id closestFreeGarageParking =
		 * parkingManager.getClosestFreeGarageParking(nextActivity.getCoord());
		 * 
		 * parkingMemory.closestFreeGarageParkingAtTimeOfArrival =
		 * closestFreeGarageParking; }
		 */
	}

	public void processLegInDefaultWay() {
		handleLeg();
	}

	// avoid temporary problem with car leave and next planned parking on same
	// link
	// TODO: resolve in future implementation
	public boolean isLastLinkOfRouteInvalidLinkForParking() {
		Leg leg = (LegImpl) getPerson().getSelectedPlan().getPlanElements().get(getPlanElementIndex());
		LinkNetworkRouteImpl route = (LinkNetworkRouteImpl) leg.getRoute();

		boolean isInvalidLink = false;
		int nextCarLegIndex = duringCarLeg_getPlanElementIndexOfNextCarLeg();
		if (nextCarLegIndex != -1) {
			isInvalidLink = route.getEndLinkId().toString().equalsIgnoreCase(getInvalidLinkForParking().toString());
		}
		return isInvalidLink;
	}

	public Id getInvalidLinkForParking() {
		int nextCarLegIndex = duringCarLeg_getPlanElementIndexOfNextCarLeg();
		if (nextCarLegIndex != -1) {
			ActivityImpl nextActAfterNextCarLeg = (ActivityImpl) getPerson().getSelectedPlan().getPlanElements()
					.get(nextCarLegIndex + 1);
			return nextActAfterNextCarLeg.getLinkId();
		} else {
			return null;
		}
	}

	public boolean isInvalidLinkForParking(Id linkId) {
		boolean isInvalidLink = false;
		int nextCarLegIndex = duringCarLeg_getPlanElementIndexOfNextCarLeg();
		if (nextCarLegIndex != -1) {
			ActivityImpl nextActAfterNextCarLeg = (ActivityImpl) getPerson().getSelectedPlan().getPlanElements()
					.get(nextCarLegIndex + 3);
			isInvalidLink = GeneralLib.equals(linkId, nextActAfterNextCarLeg.getLinkId());
		}
		return isInvalidLink;
	}

	public ActivityImpl getCurrentActivity() {
		return (ActivityImpl) getPerson().getSelectedPlan().getPlanElements().get(getPlanElementIndex());
	}

	public Link getCurrentLink() {
		Id currentLinkId = getCurrentLinkId();
		return ZHScenarioGlobal.scenario.getNetwork().getLinks().get(currentLinkId);
	}

	public Id getCurrentLinkId() {
		Leg leg = (LegImpl) getPerson().getSelectedPlan().getPlanElements().get(getPlanElementIndex());
		List<Id<Link>> linkIds = ((LinkNetworkRouteImpl) leg.getRoute()).getLinkIds();

		if (getCurrentLinkIndex() == -1) {
			return ((LinkNetworkRouteImpl) leg.getRoute()).getStartLinkId();
		} else {
			return linkIds.get(getCurrentLinkIndex());
		}
	}

	public Link getNextLink() {
		Leg leg = (LegImpl) getPerson().getSelectedPlan().getPlanElements().get(getPlanElementIndex());
		List<Id<Link>> linkIds = ((LinkNetworkRouteImpl) leg.getRoute()).getLinkIds();
		Id nextLinkId;
		if (!endOfLegReached()) {
			nextLinkId = linkIds.get(getCurrentLinkIndex() + 1);
		} else {
			nextLinkId = ((LinkNetworkRouteImpl) leg.getRoute()).getEndLinkId();
		}
		return ZHScenarioGlobal.scenario.getNetwork().getLinks().get(nextLinkId);
	}

	public boolean endOfLegReached() {
		Leg leg = (LegImpl) getPerson().getSelectedPlan().getPlanElements().get(getPlanElementIndex());
		List<Id<Link>> linkIds = ((LinkNetworkRouteImpl) leg.getRoute()).getLinkIds();
		return getCurrentLinkIndex() == linkIds.size() - 1;
	}
}
