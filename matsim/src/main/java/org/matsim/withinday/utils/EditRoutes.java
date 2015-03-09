/* *********************************************************************** *
 * project: org.matsim.*
 * EditRoutes.java
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

package org.matsim.withinday.utils;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.LinkWrapperFacility;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.Facility;

public class EditRoutes {

	private static final Logger logger = Logger.getLogger(EditRoutes.class);
	
	/**
	 * Re-locates a future route. The route is given by its leg.
	 * 
	 * @return true when replacing the route worked, false when something went wrong
	 */
	public static boolean relocateFutureLegRoute(Leg leg, Id<Link> fromLinkId, Id<Link> toLinkId, Person person, Network network, TripRouter tripRouter) {
				
		Link fromLink = network.getLinks().get(fromLinkId);
		Link toLink = network.getLinks().get(toLinkId);
		
		Facility<ActivityFacility> fromFacility = new LinkWrapperFacility(fromLink);
		Facility<ActivityFacility> toFacility = new LinkWrapperFacility(toLink);
		
		List<? extends PlanElement> planElements = tripRouter.calcRoute(leg.getMode(), fromFacility, toFacility, leg.getDepartureTime(), person);
		
		if (planElements.size() != 1) {
			throw new RuntimeException("Expected a list of PlanElements containing exactly one element, " +
					"but the returned list contained " + planElements.size() + " elements."); 
		}
		
		Leg newLeg = (Leg) planElements.get(0);
		
		leg.setTravelTime(newLeg.getTravelTime());
		leg.setRoute(newLeg.getRoute());
		
		return true;
	}
	
	/**
	 * Re-plans a future route. The route is given by its leg. It is expected that the
	 * leg's route is not null and that the start- and end link Ids are set properly.
	 * 
	 * If the start- and or end-location of the leg have changed, use relocateFutureLegRoute(...)!
	 * 
	 * @return true when replacing the route worked, false when something went wrong
	 */
	public static boolean replanFutureLegRoute(Leg leg, Person person, Network network, TripRouter tripRouter) {
		
		Route route = leg.getRoute();
		
		Link fromLink = network.getLinks().get(route.getStartLinkId());
		Link toLink = network.getLinks().get(route.getEndLinkId());
		
		Facility<ActivityFacility> fromFacility = new LinkWrapperFacility(fromLink);
		Facility<ActivityFacility> toFacility = new LinkWrapperFacility(toLink);
		
		List<? extends PlanElement> planElements = tripRouter.calcRoute(leg.getMode(), fromFacility, toFacility, leg.getDepartureTime(), person);
		
		if (planElements.size() != 1) {
			throw new RuntimeException("Expected a list of PlanElements containing exactly one element, " +
					"but the returned list contained " + planElements.size() + " elements."); 
		}
		
		Leg newLeg = (Leg) planElements.get(0);
		
		leg.setTravelTime(newLeg.getTravelTime());
		leg.setRoute(newLeg.getRoute());
		
		return true;
	}

	/**
	 * In contrast to the other replanFutureLegRoute(...) method, the leg at the given index is replaced
	 * by a new one. This is e.g. necessary when replacing a pt trip which might consists of multiple legs
	 * and pt_interaction activities.  
	 * This might become the future default approach.
	 * 
	 * @return
	 */
	public static boolean replanFutureTrip(Trip trip, Plan plan, String mainMode, double departureTime, 
			Network network, TripRouter tripRouter) {
		
		Person person = plan.getPerson();
		
		Activity fromActivity = trip.getOriginActivity();
		Activity toActivity = trip.getDestinationActivity();
		
		Link fromLink = network.getLinks().get(fromActivity.getLinkId());
		Link toLink = network.getLinks().get(toActivity.getLinkId());
		
		Facility<ActivityFacility> fromFacility = new LinkWrapperFacility(fromLink);
		Facility<ActivityFacility> toFacility = new LinkWrapperFacility(toLink);
				
		final List<? extends PlanElement> newTrip =
				tripRouter.calcRoute(mainMode, fromFacility, toFacility, departureTime, person);
				
		TripRouter.insertTrip(plan, trip.getOriginActivity(), newTrip, trip.getDestinationActivity());
		
		return true;
	}

	/**
	 * Re-locates a future route. The route is given by its leg.
	 * 
	 * @return true when replacing the route worked, false when something went wrong
	 */
	public static boolean relocateCurrentLegRoute(Leg leg, Person person, int currentLinkIndex, Id<Link> toLinkId, double time, Network network, TripRouter tripRouter) {
		
		Route route = leg.getRoute();

		// if the route type is not supported (e.g. because it is a walking agent)
		if (!(route instanceof NetworkRoute)) return false;

		NetworkRoute oldRoute = (NetworkRoute) route;

		/*
		 *  Get the Id of the current Link.
		 *  Create a List that contains all links of a route, including the Start- and EndLinks.
		 */
		List<Id<Link>> allLinkIds = getRouteLinkIds(oldRoute);
		Id<Link> currentLinkId = allLinkIds.get(currentLinkIndex);

		Link fromLink = network.getLinks().get(currentLinkId);
		Link toLink = network.getLinks().get(toLinkId);
		
		Facility<ActivityFacility> fromFacility = new LinkWrapperFacility(fromLink);
		Facility<ActivityFacility> toFacility = new LinkWrapperFacility(toLink);
		
		List<? extends PlanElement> planElements = tripRouter.calcRoute(leg.getMode(), fromFacility, toFacility, time, person);
		
		if (planElements.size() != 1) {
			throw new RuntimeException("Expected a list of PlanElements containing exactly one element, " +
					"but the returned list contained " + planElements.size() + " elements."); 
		}
				
		// The linkIds of the new Route
		List<Id<Link>> linkIds = new ArrayList<Id<Link>>();

		/*
		 * Get those Links which have already been passed.
		 * allLinkIds contains also the startLinkId, which should not
		 * be part of the List - it is set separately. Therefore we start
		 * at index 1.
		 */
		if (currentLinkIndex > 0) {
			linkIds.addAll(allLinkIds.subList(1, currentLinkIndex + 1));
		}

		Leg newLeg = (Leg) planElements.get(0);
		Route newRoute = newLeg.getRoute();

		// Merge old and new Route.
		if (newRoute instanceof NetworkRoute) {
			/*
			 * Edit cdobler 25.5.2010
			 * If the new leg ends at the current Link, we have to
			 * remove that linkId from the linkIds List - it is stored
			 * in the endLinkId field of the route.
			 */
			if (linkIds.size() > 0 && linkIds.get(linkIds.size() - 1).equals(newRoute.getEndLinkId())) {
				linkIds.remove(linkIds.size() - 1);
			}

			linkIds.addAll(((NetworkRoute) newRoute).getLinkIds());
		}
		else {
			logger.warn("The Route data could not be copied to the old Route. Old Route will be used!");
			return false;
		}

		// Overwrite old Route
		oldRoute.setLinkIds(oldRoute.getStartLinkId(), linkIds, toFacility.getLinkId());

		return true;
	}
	
	/*
	 * We create a new Plan which contains only the Leg that should be replanned and its previous and next
	 * Activities. By doing so the PlanAlgorithm will only change the Route of that Leg.
	 *
	 * Use currentNodeIndex from a DriverAgent if possible!
	 *
	 * Otherwise code it as following:
	 * startLink - Node1 - routeLink1 - Node2 - routeLink2 - Node3 - endLink
	 * The currentNodeIndex has to Point to the next Node
	 * (which is the endNode of the current Link)
	 */
	public static boolean replanCurrentLegRoute(Leg leg, Person person, int currentLinkIndex, double time, Network network, TripRouter tripRouter) {

		Route route = leg.getRoute();

		// if the route type is not supported (e.g. because it is a walking agent)
		if (!(route instanceof NetworkRoute)) return false;

		// This is just a special case of relocateCurrentLegRoute where the end link of the route is not changed.
		return relocateCurrentLegRoute(leg, person, currentLinkIndex, route.getEndLinkId(), time, network, tripRouter);
	}

	/**
	 * @param plan
	 * @param fromActivity
	 * @param tripRouter
	 * @return the Trip that starts at the given activity or null, if no trip was found
	 */
	public static Trip getTrip(Plan plan, Activity fromActivity, TripRouter tripRouter) {
		List<Trip> trips = TripStructureUtils.getTrips(plan, tripRouter.getStageActivityTypes());
		
		for (Trip trip : trips) {
			if (trip.getOriginActivity() == fromActivity) return trip;
		}
		
		// no matching trip was found
		return null;
	}
	
	/**
	 * @param trip
	 * @return the depature time of the first leg of the trip
	 */
	public static double getDepatureTime(Trip trip) {
		// does this always make sense?
		Leg leg = (Leg) trip.getTripElements().get(0);
		return leg.getDepartureTime();
	}
	
	private static List<Id<Link>> getRouteLinkIds(Route route) {
		List<Id<Link>> linkIds = new ArrayList<Id<Link>>();

		if (route instanceof NetworkRoute) {
			NetworkRoute networkRoute = (NetworkRoute) route;
			linkIds.add(networkRoute.getStartLinkId());
			linkIds.addAll(networkRoute.getLinkIds());
			linkIds.add(networkRoute.getEndLinkId());
		} else {
			throw new RuntimeException("Currently only NetworkRoutes are supported for Within-Day Replanning!");
		}

		return linkIds;
	}
}
