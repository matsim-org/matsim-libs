/* *********************************************************************** *
 * project: org.matsim.*
 * PickupAgentReplanner.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.christoph.evacuation.withinday.replanning.replanners;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.mobsim.qsim.agents.PlanBasedWithinDayAgent;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringLegReplanner;
import org.matsim.withinday.utils.EditRoutes;

import playground.christoph.evacuation.mobsim.PassengerDepartureHandler;

/**
 * 
 * @author cdobler
 */
public class PickupAgentReplanner extends WithinDayDuringLegReplanner {

	private static final String activityType = "pickup";
	
	/*package*/ PickupAgentReplanner(Id id, Scenario scenario) {
		super(id, scenario);
	}

	@Override
	public boolean doReplanning(PlanBasedWithinDayAgent withinDayAgent) {
		
		// If we don't have a valid Replanner.
		if (this.routeAlgo == null) return false;

		// If we don't have a valid WithinDayPersonAgent
		if (withinDayAgent == null) return false;
		
		PlanImpl executedPlan = (PlanImpl) withinDayAgent.getSelectedPlan();

		// If we don't have an executed plan
		if (executedPlan == null) return false;
		
		int currentLegIndex = withinDayAgent.getCurrentPlanElementIndex();
		int currentLinkIndex = withinDayAgent.getCurrentRouteLinkIdIndex();
		Id currentLinkId = withinDayAgent.getCurrentLinkId();
		Leg currentLeg = withinDayAgent.getCurrentLeg();
		
		/*
		 * Create new pickup activity.
		 */
		Activity waitForPickupActivity = scenario.getPopulation().getFactory().createActivityFromLinkId(activityType, currentLinkId);
		waitForPickupActivity.setType("pickup");
		waitForPickupActivity.setStartTime(this.time);
		waitForPickupActivity.setEndTime(this.time);
		String idString = currentLinkId.toString() + "_pickup";
		((ActivityImpl) waitForPickupActivity).setFacilityId(scenario.createId(idString));
		((ActivityImpl) waitForPickupActivity).setCoord(scenario.getNetwork().getLinks().get(currentLinkId).getCoord());
				
		/*
		 * Create new ride_passenger leg to the rescue facility.
		 * Set mode to ride, then create route for the leg, then
		 * set the mode to the correct value (ride_passenger).
		 */
		Leg ridePassengerLeg = scenario.getPopulation().getFactory().createLeg(TransportMode.ride);
		ridePassengerLeg.setDepartureTime(this.time);
		
		/*
		 * Insert pickup activity and ride_passenger leg into agent's plan.
		 */
		executedPlan.getPlanElements().add(currentLegIndex + 1, waitForPickupActivity);
		executedPlan.getPlanElements().add(currentLegIndex + 2, ridePassengerLeg);
		
		/*
		 * End agent's current leg at the current link.
		 */
		NetworkRoute route = (NetworkRoute) currentLeg.getRoute();
		List<Id> subRoute = new ArrayList<Id>(route.getLinkIds().subList(0, currentLinkIndex));
		route.setLinkIds(route.getStartLinkId(), subRoute, currentLinkId);
		currentLeg.setTravelTime(this.time - currentLeg.getDepartureTime());

		/*
		 * Create a new route for the ride_passenger leg
		 * and set correct mode afterwards.
		 */
		new EditRoutes().replanFutureLegRoute(executedPlan, currentLegIndex + 2, this.routeAlgo);
		ridePassengerLeg.setMode(PassengerDepartureHandler.passengerTransportMode);		
		
		// Finally reset the cached Values of the PersonAgent - they may have changed!
		withinDayAgent.resetCaches();
		
		return true;
	}
}