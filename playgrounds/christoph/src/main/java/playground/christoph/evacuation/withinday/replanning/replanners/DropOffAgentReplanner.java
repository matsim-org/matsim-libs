/* *********************************************************************** *
 * project: org.matsim.*
 * DropOffAgentReplanner.java
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

package playground.christoph.evacuation.withinday.replanning.replanners;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.PassengerAgent;
import org.matsim.core.mobsim.qsim.ActivityEndRescheduler;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.mobsim.qsim.qnetsimengine.JointDeparture;
import org.matsim.core.mobsim.qsim.qnetsimengine.PassengerQNetsimEngine;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteFactory;
import org.matsim.core.router.TripRouter;
import org.matsim.facilities.ActivityFacility;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringLegReplanner;

import playground.christoph.evacuation.controler.EvacuationConstants;
import playground.christoph.evacuation.withinday.replanning.identifiers.AgentsToDropOffIdentifier;

/**
 * 
 * @author cdobler
 */
public class DropOffAgentReplanner extends WithinDayDuringLegReplanner {

	private static final Logger log = Logger.getLogger(DropOffAgentReplanner.class);
		
	private final RouteFactory routeFactory;
	private final TripRouter tripRouter;
	private final AgentsToDropOffIdentifier agentsToDropOffIdentifier;
	
	/*package*/ DropOffAgentReplanner(Id id, Scenario scenario, ActivityEndRescheduler internalInterface,
			TripRouter tripRouter, AgentsToDropOffIdentifier agentsToDropOffIdentifier) {
		super(id, scenario, internalInterface);
		this.tripRouter = tripRouter;
		this.agentsToDropOffIdentifier = agentsToDropOffIdentifier;
		this.routeFactory = new LinkNetworkRouteFactory();
	}

	@Override
	public boolean doReplanning(MobsimAgent withinDayAgent) {
	
		// If we don't have a valid WithinDayPersonAgent
		if (withinDayAgent == null) return false;
		
		if (withinDayAgent.getMode().equals(TransportMode.car)) {
			return replanDriver(withinDayAgent);
		} else if (withinDayAgent.getMode().equals(PassengerQNetsimEngine.PASSENGER_TRANSPORT_MODE)) {
			return replanPassenger(withinDayAgent);
		} else {
			log.warn("Unexpected mode was found: " + withinDayAgent.getMode());
			return false;
		}
	}
	
	private boolean replanDriver(MobsimAgent withinDayAgent) {
		
		Plan executedPlan = WithinDayAgentUtils.getModifiablePlan(withinDayAgent);
		
		// If we don't have an executed plan
		if (executedPlan == null) return false;
		
		int currentLegIndex = WithinDayAgentUtils.getCurrentPlanElementIndex(withinDayAgent);
		int currentLinkIndex = WithinDayAgentUtils.getCurrentRouteLinkIdIndex(withinDayAgent);
		Id currentLinkId = withinDayAgent.getCurrentLinkId();
		Leg currentLeg = WithinDayAgentUtils.getModifiableCurrentLeg(withinDayAgent);
		NetworkRoute currentRoute = (NetworkRoute) currentLeg.getRoute();
		Id currentVehicleId = currentRoute.getVehicleId();
		List<Id<Link>> subRoute;
		
		/*
		 * Create new drop off activity.
		 * After the linkMinTravelTime the vehicle is removed from the links buffer.
		 * At this point it is checked whether the vehicle should be parked at the link.
		 * 
		 * Set no end time but a duration of 60 seconds. Therefore, agents do not depart
		 * immediately if they arrive after their scheduled end time (e.g. because their
		 * current link is jammed and therefore it takes some time until they start their
		 * activity).
		 * 
		 * Otherwise dropping off agents might fail since the driver misses that an agent
		 * has left the vehicle and therefore waits until the simulation ends.
		 */
//		double departureTime = this.time + 60.0;
		double duration = 60.0;
		Activity dropOffActivity = scenario.getPopulation().getFactory().createActivityFromLinkId(PassengerQNetsimEngine.DROP_OFF_ACTIVITY_TYPE, currentLinkId);
		dropOffActivity.setType(PassengerQNetsimEngine.DROP_OFF_ACTIVITY_TYPE);
		dropOffActivity.setStartTime(this.time);
		dropOffActivity.setMaximumDuration(duration);
//		dropOffActivity.setEndTime(departureTime);
		String idString = currentLinkId.toString() + EvacuationConstants.PICKUP_DROP_OFF_FACILITY_SUFFIX;
		((ActivityImpl) dropOffActivity).setFacilityId(Id.create(idString, ActivityFacility.class));
		((ActivityImpl) dropOffActivity).setCoord(scenario.getNetwork().getLinks().get(currentLinkId).getCoord());
			
		/*
		 * Create new car leg from the current position to the current legs destination.
		 * Re-use existing routes vehicle.
		 */
		Leg carLeg = scenario.getPopulation().getFactory().createLeg(TransportMode.car);
//		carLeg.setDepartureTime(departureTime);
		carLeg.setDepartureTime(this.time + duration);
		subRoute = new ArrayList<Id<Link>>();
		/*
		 * If the driver is not already at the end link of its current route, copy the
		 * so far not passed parts of the route to the new route.
		 */
		if (!currentLinkId.equals(currentRoute.getEndLinkId())) {
			/*
			 * If currentLinkIndex == currentRoute.getLinkIds().size(), only
			 * the route's endLink is left of the route. As a result, the new
			 * route will start on this link and end on the next one. Therefore,
			 * there are no links in between.
			 */
			if (currentLinkIndex < currentRoute.getLinkIds().size()) {
				subRoute.addAll(currentRoute.getLinkIds().subList(currentLinkIndex, currentRoute.getLinkIds().size()));				
			}
		}
		NetworkRoute carRoute = (NetworkRoute) routeFactory.createRoute(currentLinkId, currentRoute.getEndLinkId());
		carRoute.setLinkIds(currentLinkId, subRoute, currentRoute.getEndLinkId());
		carRoute.setVehicleId(currentVehicleId);
		carLeg.setRoute(carRoute);
		
		// assign joint departure to agent's next car leg
		Id agentId = withinDayAgent.getId();
		JointDeparture jointDeparture = this.agentsToDropOffIdentifier.getJointDeparture(agentId);
		this.agentsToDropOffIdentifier.getJointDepartureOrganizer().assignAgentToJointDeparture(agentId, carLeg, jointDeparture);
		
		/*
		 * End agent's current leg at the current link.
		 * Check whether the agent is already on the routes last link.
		 */
		if (currentLinkId.equals(currentRoute.getEndLinkId())) {
			subRoute.addAll(currentRoute.getLinkIds());	
		} else {
			subRoute = new ArrayList<Id<Link>>();
			subRoute.addAll(currentRoute.getLinkIds().subList(0, currentLinkIndex));
		}
		currentRoute.setLinkIds(currentRoute.getStartLinkId(), subRoute, currentLinkId);
		currentLeg.setTravelTime(this.time - currentLeg.getDepartureTime());
		
		/*
		 * Insert drop off activity and driver leg into agent's plan.
		 */
		executedPlan.getPlanElements().add(currentLegIndex + 1, dropOffActivity);
		executedPlan.getPlanElements().add(currentLegIndex + 2, carLeg);
		
		// Finally reset the cached Values of the PersonAgent - they may have changed!
		WithinDayAgentUtils.resetCaches(withinDayAgent);
		
		return true;
	}
	
	private boolean replanPassenger(MobsimAgent withinDayAgent) {
		
		Plan executedPlan = WithinDayAgentUtils.getModifiablePlan(withinDayAgent);

		// If we don't have an executed plan
		if (executedPlan == null) return false;
		
		int currentLegIndex = WithinDayAgentUtils.getCurrentPlanElementIndex(withinDayAgent);
		Leg currentLeg = WithinDayAgentUtils.getModifiableCurrentLeg(withinDayAgent);
		
		/*
		 * Get agent's current link from the vehicle since the agent's
		 * current link is not updated. 
		 */
		PassengerAgent passenger = (PassengerAgent) withinDayAgent;
		Id<Link> currentLinkId = passenger.getVehicle().getCurrentLink().getId();
		
		/*
		 * Create new drop off activity.
		 */
		double departureTime = this.time + 60.0;
		Activity dropOffActivity = scenario.getPopulation().getFactory().createActivityFromLinkId(PassengerQNetsimEngine.DROP_OFF_ACTIVITY_TYPE, currentLinkId);
		dropOffActivity.setType(PassengerQNetsimEngine.DROP_OFF_ACTIVITY_TYPE);
		dropOffActivity.setStartTime(this.time);
		dropOffActivity.setEndTime(departureTime);
		String idString = currentLinkId.toString() + EvacuationConstants.PICKUP_DROP_OFF_FACILITY_SUFFIX;
		((ActivityImpl) dropOffActivity).setFacilityId(Id.create(idString, ActivityFacility.class));
		((ActivityImpl) dropOffActivity).setCoord(scenario.getNetwork().getLinks().get(currentLinkId).getCoord());
				
		/*
		 * End agent's current leg at the current link.
		 */
		Id oldDestinationLinkId = currentLeg.getRoute().getEndLinkId();
		currentLeg.getRoute().setEndLinkId(currentLinkId);
		currentLeg.setTravelTime(this.time - currentLeg.getDepartureTime());
		
		/*
		 * Create new walk leg to the agents destination.
		 */
		Leg walkLeg = scenario.getPopulation().getFactory().createLeg(TransportMode.walk);
		walkLeg.setDepartureTime(departureTime);
				
		/*
		 * Insert drop off activity and walk leg into agent's plan.
		 */
		executedPlan.getPlanElements().add(currentLegIndex + 1, dropOffActivity);
		executedPlan.getPlanElements().add(currentLegIndex + 2, walkLeg);
		
		/*
		 * Create a new route for the walk leg.
		 */
		this.editRoutes.relocateFutureLegRoute(walkLeg, currentLinkId, oldDestinationLinkId, executedPlan.getPerson(), 
				scenario.getNetwork(), tripRouter); 
				
		// Finally reset the cached Values of the PersonAgent - they may have changed!
		WithinDayAgentUtils.resetCaches(withinDayAgent);
		
		return true;
	}
}