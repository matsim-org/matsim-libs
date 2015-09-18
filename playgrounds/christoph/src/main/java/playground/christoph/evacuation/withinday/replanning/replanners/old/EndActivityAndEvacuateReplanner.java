/* *********************************************************************** *
 * project: org.matsim.*
 * EndActivityAndEvacuateReplanner.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.christoph.evacuation.withinday.replanning.replanners.old;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimAgent.State;
import org.matsim.core.mobsim.qsim.ActivityEndRescheduler;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.TripRouter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.facilities.ActivityFacility;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringActivityReplanner;

import playground.christoph.evacuation.controler.EvacuationConstants;
import playground.christoph.evacuation.trafficmonitoring.SwissPTTravelTimeCalculator;

public class EndActivityAndEvacuateReplanner extends WithinDayDuringActivityReplanner {
	
	private static final Logger log = Logger.getLogger(EndActivityAndEvacuateReplanner.class);
	
	private final SwissPTTravelTimeCalculator ptTravelTime;
	private final TripRouter tripRouter;
	
	/*package*/ EndActivityAndEvacuateReplanner(Id id, Scenario scenario, ActivityEndRescheduler internalInterface, 
			SwissPTTravelTimeCalculator ptTravelTime, TripRouter tripRouter) {
		super(id, scenario, internalInterface);
		this.ptTravelTime = ptTravelTime;
		this.tripRouter = tripRouter;
	}
	
	@Override
	public boolean doReplanning(MobsimAgent withinDayAgent) {		
		
		// If we don't have a valid PersonAgent
		if (withinDayAgent == null) return false;
	
		Plan executedPlan = WithinDayAgentUtils.getModifiablePlan(withinDayAgent);

		// If we don't have an executed plan
		if (executedPlan == null) return false;
		
		// check whether the agent is performing an activity
		if (!withinDayAgent.getState().equals(State.ACTIVITY)) return false;
		
		Activity currentActivity = (Activity) WithinDayAgentUtils.getCurrentPlanElement(withinDayAgent);
		
		// Set the end time of the current activity to the current time.
		currentActivity.setEndTime(this.time);
		
		// get the index of the currently performed activity in the selected plan
		int currentActivityIndex = WithinDayAgentUtils.getCurrentPlanElementIndex(withinDayAgent);

		// identify the TransportMode for the rescueLeg
		String transportMode = identifyTransportMode(currentActivityIndex, executedPlan);
		
		// Remove all legs and activities after the current activity.
		while (executedPlan.getPlanElements().size() - 1 > currentActivityIndex) {
			executedPlan.getPlanElements().remove(executedPlan.getPlanElements().size() - 1);
		}
		
		PopulationFactory factory = scenario.getPopulation().getFactory();
		
		/*
		 * Now we add a new Activity at the rescue facility.
		 * We add no endtime therefore the activity will last until the end of
		 * the simulation.
		 */
		Activity rescueActivity = factory.createActivityFromLinkId(EvacuationConstants.RESCUE_ACTIVITY, 
				Id.create(EvacuationConstants.RESCUE_LINK, Link.class));
		((ActivityImpl)rescueActivity).setFacilityId(Id.create(EvacuationConstants.RESCUE_FACILITY, ActivityFacility.class));
		
		Coord rescueCoord = ((ScenarioImpl)scenario).getActivityFacilities().getFacilities().get(Id.create(EvacuationConstants.RESCUE_FACILITY, ActivityFacility.class)).getCoord();
		((ActivityImpl)rescueActivity).setCoord(rescueCoord);
		
		// create a leg using the identified transport mode
		Leg legToRescue = factory.createLeg(transportMode);
		
		// add new activity
		int position = executedPlan.getPlanElements().indexOf(currentActivity) + 1;
		executedPlan.getPlanElements().add(position, rescueActivity);
		executedPlan.getPlanElements().add(position, legToRescue);
		
		// calculate route for the leg to the rescue facility
		this.editRoutes.relocateFutureLegRoute(legToRescue, currentActivity.getLinkId(), rescueActivity.getLinkId(), 
				executedPlan.getPerson(), scenario.getNetwork(), tripRouter);

		/*
		 * Identify the last non-rescue link and relocate rescue activity to it.
		 */
		NetworkRoute route = (NetworkRoute) legToRescue.getRoute();
		
		/*
		 * If the route is like LinkXY-RescueLinkXY-RescueLink the the activity coordinate seems to be
		 * affected but the link itself is not. Therefore end the route at the same link.
		 */
		Id endLinkId = null;
		if (route.getLinkIds().size() > 1) {
			endLinkId = route.getLinkIds().get(route.getLinkIds().size() - 2);			
		} else endLinkId = route.getStartLinkId();
		((ActivityImpl) rescueActivity).setFacilityId(Id.create(EvacuationConstants.RESCUE_FACILITY + endLinkId.toString(), ActivityFacility.class));
		((ActivityImpl) rescueActivity).setLinkId(endLinkId);
		NetworkRoute subRoute2 = route.getSubRoute(route.getStartLinkId(), endLinkId);
		legToRescue.setRoute(subRoute2);
		
		// if the person has to walk, we additionally try pt
		if (transportMode.equals(TransportMode.walk)) {
			Tuple<Double, Coord> tuple = ptTravelTime.calcSwissPtTravelTime(currentActivity, rescueActivity, this.time, executedPlan.getPerson());
			double travelTimePT = tuple.getFirst();
			double travelTimeWalk = legToRescue.getTravelTime();
			
			// If using pt is faster than walking switch to pt.
			if (travelTimePT < travelTimeWalk) {
				legToRescue.setMode(TransportMode.pt);
				
				// calculate route for the leg to the rescue facility
				this.editRoutes.relocateFutureLegRoute(legToRescue, currentActivity.getLinkId(), rescueActivity.getLinkId(), 
						executedPlan.getPerson(), scenario.getNetwork(), tripRouter);
				
				// set travel time
				legToRescue.getRoute().setTravelTime(travelTimePT);
				
				// set speed
				double routeLength = RouteUtils.calcDistance((NetworkRoute) legToRescue.getRoute(), scenario.getNetwork());
				ptTravelTime.setPersonSpeed(withinDayAgent.getId(), routeLength / travelTimePT);
			}
		}
		
		/*
		 * Reschedule the currently performed Activity in the Mobsim - there
		 * the activityEndsList has to be updated.
		 */
		// yyyy a method getMobsim in MobimAgent would be useful here. cdobler, Oct'10
		// Intuitively I would agree.  We should think about where to set this so that, under normal circumstances,
		// it can't become null.  kai, oct'10
//		WithinDayAgentUtils.calculateAndSetDepartureTime(withinDayAgent, currentActivity);
		WithinDayAgentUtils.resetCaches( withinDayAgent );
		this.internalInterface.rescheduleActivityEnd(withinDayAgent);
		return true;
	}

	/*
	 * By default we try to use a car. We can do this, if the previous or the next 
	 * Leg are performed with a car.
	 * The order is as following:
	 * car is preferred to ride is preferred to pt is preferred to bike if preferred to walk 
	 */
	private String identifyTransportMode(int currentActivityIndex, Plan selectedPlan) {
		
		boolean hasCar = false;
		boolean hasRide = false;
		boolean hasBike = false;
		
		if (currentActivityIndex > 0) {
			Leg previousLeg = (Leg) selectedPlan.getPlanElements().get(currentActivityIndex - 1);
			String transportMode = previousLeg.getMode();
			if (transportMode.equals(TransportMode.car)) hasCar = true;
			else if (transportMode.equals(TransportMode.bike)) hasBike = true;
			else if (transportMode.equals(TransportMode.ride)) hasRide = true;
		}
		
		if (currentActivityIndex + 1 < selectedPlan.getPlanElements().size()) {
			Leg nextLeg = (Leg) selectedPlan.getPlanElements().get(currentActivityIndex + 1);
			String transportMode = nextLeg.getMode();
			if (transportMode.equals(TransportMode.car)) hasCar = true;
			else if (transportMode.equals(TransportMode.bike)) hasBike = true;
			else if (transportMode.equals(TransportMode.ride)) hasRide = true;
		}
		
		if (hasCar) return TransportMode.car;
		else if (hasRide) return TransportMode.ride;
		else if (hasBike) return TransportMode.bike;
		else return TransportMode.walk;
	}	
}
