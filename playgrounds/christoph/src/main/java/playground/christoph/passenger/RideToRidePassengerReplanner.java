/* *********************************************************************** *
 * project: org.matsim.*
 * RideToRidePassengerReplanner.java
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

package playground.christoph.passenger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.api.experimental.facilities.Facility;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.mobsim.qsim.qnetsimengine.JointDepartureOrganizer;
import org.matsim.core.mobsim.qsim.qnetsimengine.PassengerQNetsimEngine;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.routes.GenericRouteFactory;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteFactory;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.TripRouter;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayInitialReplanner;
import org.matsim.withinday.utils.EditRoutes;

import playground.christoph.passenger.RideToRidePassengerContextProvider.RideToRidePassengerContext;

public class RideToRidePassengerReplanner extends WithinDayInitialReplanner {

	private final TripRouter tripRouter;
	private final JointDepartureOrganizer jointDepartureOrganizer;
	private final RideToRidePassengerContextProvider rideToRidePassengerContextProvider;
	
	private final RouteFactory carRouteFactory = new LinkNetworkRouteFactory();
	private final RouteFactory rideRouteFactory = new GenericRouteFactory();
	
	/*package*/ RideToRidePassengerReplanner(Id id, Scenario scenario, InternalInterface internalInterface,
			TripRouter tripRouter, RideToRidePassengerContextProvider rideToRidePassengerContextProvider,
			JointDepartureOrganizer jointDepartureOrganizer) {
		super(id, scenario, internalInterface);
		this.tripRouter = tripRouter;
		this.rideToRidePassengerContextProvider = rideToRidePassengerContextProvider;
		this.jointDepartureOrganizer = jointDepartureOrganizer;
	}

	@Override
	public boolean doReplanning(MobsimAgent withinDayAgent) {
		
		Collection<RideToRidePassengerContext> collection = this.rideToRidePassengerContextProvider.removeContextCollection(withinDayAgent.getId());

		for (RideToRidePassengerContext context : collection) {

			if (context.carLegAgent != null) {
				rideToCar(withinDayAgent, context);
			} else {
				rideToNonCar(withinDayAgent, context);
			}
		}

		return true;
	}
	
	private void rideToCar(MobsimAgent agent, RideToRidePassengerContext context) {
		
		adaptPassenger(agent, context);
		
		/*
		 * A car agent's plan could be modified by multiply thread concurrently.
		 * Synchronizing to avoid this.
		 */
		synchronized (context.carLegAgent) {
			adaptDriver(context);
		}
	}
	

	private void adaptPassenger(MobsimAgent agent, RideToRidePassengerContext context) {

		Plan plan = WithinDayAgentUtils.getModifiablePlan(agent);
		Leg rideLeg = context.rideLeg;
		Route route = rideLeg.getRoute();
		
		// Create walk leg to pickup point.
		Leg toPickupLeg = scenario.getPopulation().getFactory().createLeg(TransportMode.walk);
		toPickupLeg.setDepartureTime(rideLeg.getDepartureTime());
		Route toPickupRoute = rideRouteFactory.createRoute(route.getStartLinkId(), context.pickupLink.getId());
		toPickupLeg.setRoute(toPickupRoute);
		
		// Create pickup activity.
		Activity pickupActivity = scenario.getPopulation().getFactory().createActivityFromLinkId(PassengerQNetsimEngine.PICKUP_ACTIVITY_TYPE, 
				context.pickupLink.getId());
		pickupActivity.setStartTime(rideLeg.getDepartureTime());
		pickupActivity.setEndTime(rideLeg.getDepartureTime());
//		String idString = currentLinkId.toString() + EvacuationConstants.PICKUP_DROP_OFF_FACILITY_SUFFIX;
//		((ActivityImpl) waitForPickupActivity).setFacilityId(scenario.createId(idString));
		((ActivityImpl) pickupActivity).setCoord(context.pickupLink.getCoord());

		// Create new ride leg.
		Leg ridePassengerLeg = scenario.getPopulation().getFactory().createLeg(TransportMode.ride);
		ridePassengerLeg.setDepartureTime(rideLeg.getDepartureTime());
		Route ridePassengerRoute = rideRouteFactory.createRoute(context.pickupLink.getId(), context.dropOffLink.getId());
		ridePassengerLeg.setRoute(ridePassengerRoute);
		double estimatedNetworkDistance = CoordUtils.calcDistance(context.pickupLink.getCoord(), context.dropOffLink.getCoord()) *
				scenario.getConfig().plansCalcRoute().getModeRoutingParams().get( TransportMode.ride ).getBeelineDistanceFactor() ;
//				scenario.getConfig().plansCalcRoute().getBeelineDistanceFactor();
		ridePassengerRoute.setDistance(estimatedNetworkDistance);
		
		// Create drop off activity.
		Activity dropOffActivity = scenario.getPopulation().getFactory().createActivityFromLinkId(PassengerQNetsimEngine.DROP_OFF_ACTIVITY_TYPE, 
				context.dropOffLink.getId());
		dropOffActivity.setStartTime(rideLeg.getDepartureTime() + rideLeg.getTravelTime());
		dropOffActivity.setEndTime(rideLeg.getDepartureTime() + rideLeg.getTravelTime());
//		String idString = currentLinkId.toString() + EvacuationConstants.PICKUP_DROP_OFF_FACILITY_SUFFIX;
//		((ActivityImpl) waitForPickupActivity).setFacilityId(scenario.createId(idString));
		((ActivityImpl) dropOffActivity).setCoord(context.dropOffLink.getCoord());
		
		// Create walk leg to pickup point.
		Leg fromDropOffLeg = scenario.getPopulation().getFactory().createLeg(TransportMode.walk);
		fromDropOffLeg.setDepartureTime(rideLeg.getDepartureTime() + rideLeg.getTravelTime());
		Route fromDropOffRoute = rideRouteFactory.createRoute(context.dropOffLink.getId(), route.getEndLinkId());
		fromDropOffLeg.setRoute(fromDropOffRoute);
		
		// replace ride leg in agent's plan
		int index = plan.getPlanElements().indexOf(rideLeg);
		plan.getPlanElements().remove(index);
		plan.getPlanElements().add(index, fromDropOffLeg);
		plan.getPlanElements().add(index, dropOffActivity);
		plan.getPlanElements().add(index, ridePassengerLeg);
		plan.getPlanElements().add(index, pickupActivity);
		plan.getPlanElements().add(index, toPickupLeg);
		
		// update routes of the created walk legs
		EditRoutes.replanFutureLegRoute(toPickupLeg, plan.getPerson(), scenario.getNetwork(), this.tripRouter);
		EditRoutes.replanFutureLegRoute(fromDropOffLeg, plan.getPerson(), scenario.getNetwork(), this.tripRouter);
		
		// assign joint departure to agent's ride leg
		this.jointDepartureOrganizer.assignAgentToJointDeparture(agent.getId(), ridePassengerLeg, context.pickupDeparture);
	}
	
	private void adaptDriver(RideToRidePassengerContext context) {
		
		Plan plan = WithinDayAgentUtils.getModifiablePlan(context.carLegAgent);
		Leg rideLeg = context.rideLeg;
		Leg carLeg = context.carLeg;
		NetworkRoute route = (NetworkRoute) carLeg.getRoute();
		List<Id<Link>> linkIds = new ArrayList<Id<Link>>();
		linkIds.add(route.getStartLinkId());
		linkIds.addAll(route.getLinkIds());
		if (linkIds.size() > 1 || !route.getStartLinkId().equals(route.getEndLinkId())) linkIds.add(route.getEndLinkId());
		
		// Create walk leg to pickup point.
		Leg toPickupLeg = scenario.getPopulation().getFactory().createLeg(TransportMode.car);
		toPickupLeg.setDepartureTime(carLeg.getDepartureTime());
		Route toPickupRoute = createRouteAndSetLinks(linkIds, route.getStartLinkId(), context.pickupLink.getId());
		toPickupLeg.setRoute(toPickupRoute);
		
		// Create pickup activity.
		Activity pickupActivity = scenario.getPopulation().getFactory().createActivityFromLinkId(PassengerQNetsimEngine.PICKUP_ACTIVITY_TYPE, 
				context.pickupLink.getId());
		pickupActivity.setStartTime(rideLeg.getDepartureTime());
		pickupActivity.setEndTime(rideLeg.getDepartureTime());
//		String idString = currentLinkId.toString() + EvacuationConstants.PICKUP_DROP_OFF_FACILITY_SUFFIX;
//		((ActivityImpl) waitForPickupActivity).setFacilityId(scenario.createId(idString));
		((ActivityImpl) pickupActivity).setCoord(context.pickupLink.getCoord());

		// Create new car leg.
		Leg pickUpToDropOffLeg = scenario.getPopulation().getFactory().createLeg(TransportMode.car);
		pickUpToDropOffLeg.setDepartureTime(rideLeg.getDepartureTime());
		Route pickupToDropOffRoute = createRouteAndSetLinks(linkIds, context.pickupLink.getId(), context.dropOffLink.getId());
		pickUpToDropOffLeg.setRoute(pickupToDropOffRoute);

		// Create drop off activity.
		Activity dropOffActivity = scenario.getPopulation().getFactory().createActivityFromLinkId(PassengerQNetsimEngine.DROP_OFF_ACTIVITY_TYPE, 
				context.dropOffLink.getId());
		dropOffActivity.setStartTime(rideLeg.getDepartureTime() + rideLeg.getTravelTime());
		dropOffActivity.setEndTime(rideLeg.getDepartureTime() + rideLeg.getTravelTime());
//		String idString = currentLinkId.toString() + EvacuationConstants.PICKUP_DROP_OFF_FACILITY_SUFFIX;
//		((ActivityImpl) waitForPickupActivity).setFacilityId(scenario.createId(idString));
		((ActivityImpl) dropOffActivity).setCoord(context.dropOffLink.getCoord());
		
		// Create walk leg to pickup point.
		Leg fromDropOffLeg = scenario.getPopulation().getFactory().createLeg(TransportMode.car);
		fromDropOffLeg.setDepartureTime(rideLeg.getDepartureTime() + rideLeg.getTravelTime());
		Route fromDropOffRoute = createRouteAndSetLinks(linkIds, context.dropOffLink.getId(), route.getEndLinkId());
		fromDropOffLeg.setRoute(fromDropOffRoute);
		
		// replace car leg in agent's plan
		int index = plan.getPlanElements().indexOf(carLeg);
		plan.getPlanElements().remove(index);
		plan.getPlanElements().add(index, fromDropOffLeg);
		plan.getPlanElements().add(index, dropOffActivity);
		plan.getPlanElements().add(index, pickUpToDropOffLeg);
		plan.getPlanElements().add(index, pickupActivity);
		plan.getPlanElements().add(index, toPickupLeg);
		
		// assign joint departure to agent's ride leg
		this.jointDepartureOrganizer.assignAgentToJointDeparture(context.carLegAgent.getId(), pickUpToDropOffLeg, context.pickupDeparture);

	}
	
	private Route createRouteAndSetLinks(List<Id<Link>> linkIds, Id fromLinkId, Id toLinkId) {
		
		NetworkRoute networkRoute = (NetworkRoute) carRouteFactory.createRoute(fromLinkId, toLinkId);
		if (!fromLinkId.equals(toLinkId)) {
			List<Id<Link>> subLinkIds = new ArrayList<Id<Link>>();
			int fromIndex = linkIds.indexOf(fromLinkId);
			int toIndex = linkIds.indexOf(toLinkId);
			
			if (toIndex == -1) {
				for (Id<Link> linkId : linkIds) System.out.print(linkId.toString() + " ");
				System.out.println("");
				System.out.println(fromLinkId.toString());
				System.out.println(toLinkId.toString());
			}
			
			subLinkIds.addAll(linkIds.subList(fromIndex + 1, toIndex));
			networkRoute.setLinkIds(fromLinkId, subLinkIds, toLinkId);
		}
		networkRoute.setDistance(RouteUtils.calcDistance(networkRoute, scenario.getNetwork()));

		return networkRoute;
	}
	
	private void rideToNonCar(MobsimAgent agent, RideToRidePassengerContext context) {
		
		Plan plan = WithinDayAgentUtils.getModifiablePlan(agent);
		Leg rideLeg = context.rideLeg;
		Route route = rideLeg.getRoute();
		
		Facility fromFacility = new LinkWrapperFacility(scenario.getNetwork().getLinks().get(route.getStartLinkId()));
		Facility toFacility = new LinkWrapperFacility(scenario.getNetwork().getLinks().get(route.getEndLinkId()));
		
		List<? extends PlanElement> pt = this.tripRouter.calcRoute(TransportMode.pt, fromFacility, toFacility, rideLeg.getDepartureTime(), plan.getPerson());
		
		List<? extends PlanElement> walk = this.tripRouter.calcRoute(TransportMode.walk, fromFacility, toFacility, rideLeg.getDepartureTime(), plan.getPerson());
		
		double ptTravelTime = calcTravelTime(pt);
		double walkTravelTime = calcTravelTime(walk);
		
		List<? extends PlanElement> replacement = null;
		if (walkTravelTime <= ptTravelTime) replacement = walk;
		else replacement = pt;

		int index = plan.getPlanElements().indexOf(rideLeg);
		plan.getPlanElements().remove(index);
		plan.getPlanElements().addAll(index, replacement);
	}

	private double calcTravelTime(List<? extends PlanElement> planElements) {
		
		double travelTime = 0.0;
		for (PlanElement planElement : planElements) {
			if (planElement instanceof Leg) {
				Leg leg = (Leg) planElement;
				travelTime += leg.getTravelTime();
			}
		}
		return travelTime;
	}

	/*
	 * Wraps a Link into a Facility.
	 */
	private static class LinkWrapperFacility implements Facility {
		
		private final Link wrapped;

		public LinkWrapperFacility(final Link toWrap) {
			wrapped = toWrap;
		}

		@Override
		public Coord getCoord() {
			return wrapped.getCoord();
		}

		@Override
		public Id getId() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Map<String, Object> getCustomAttributes() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Id getLinkId() {
			return wrapped.getId();
		}

		@Override
		public String toString() {
			return "[LinkWrapperFacility: wrapped="+wrapped+"]";
		}
	}
}