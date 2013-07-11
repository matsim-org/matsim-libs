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
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.api.experimental.facilities.Facility;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.TripRouter;

public class EditRoutes {

	private static final Logger logger = Logger.getLogger(EditRoutes.class);

	private Network network;
	
	/**
	 * Re-plans a future route between two activities.  The route is given by its leg, which is given by the planElementIndex.
	 * <p/>
	 * The leg needs to be preceded and followed by activities in order for this method to work.  This is not as strong a
	 * requirement as one may think, since pt plans also need to be stripped down to the "real" activities before routing starts.
	 * <p/>
	 * @param plan the plan containing the leg/route to be re-planned
	 * @param legPlanElementIndex the index for the leg containing the route to be re-planned
	 * @param planAlgorithm an algorithm that fulfills the PlanAlgorithm interface, but needs to be a router for this method to make sense
	 * @return true when replacing the route worked, false when something went wrong
	 */
	// TODO: refactor this to (network, leg, triprouter)
	public boolean replanFutureLegRoute(Plan plan, int legPlanElementIndex, TripRouter tripRouter) {
		
		Leg leg;
		PlanElement planElement = plan.getPlanElements().get(legPlanElementIndex);
		if (planElement instanceof Leg) {
			leg = (Leg) planElement;
		} else return false;

		Route route = leg.getRoute();
		
		Link fromLink = network.getLinks().get(route.getStartLinkId());
		Link toLink = network.getLinks().get(route.getEndLinkId());
		
		Facility fromFacility = new LinkWrapperFacility(fromLink);
		Facility toFacility = new LinkWrapperFacility(toLink);
		
		List<? extends PlanElement> planElements = tripRouter.calcRoute(leg.getMode(), fromFacility, toFacility, leg.getDepartureTime(), plan.getPerson());
		
		if (planElements.size() != 1) {
			throw new RuntimeException("Expected a list of PlanElements containing exactly one element, " +
					"but the returned list contained " + planElements.size() + " elements."); 
		}
		
		Leg newLeg = (Leg) planElements.get(0);
		
		leg.setTravelTime(newLeg.getTravelTime());
		leg.setRoute(newLeg.getRoute());
		
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
	// TODO: refactor this to (leg, linkindex, triprouter, time) 
	public boolean replanCurrentLegRoute(Plan plan, int legPlanElementIndex, int currentLinkIndex, TripRouter tripRouter, double time) {

		Leg leg;
		PlanElement planElement = plan.getPlanElements().get(legPlanElementIndex);
		if (planElement instanceof Leg) {
			leg = (Leg) planElement;
		} else return false;

		Route route = leg.getRoute();

		// if the route type is not supported (e.g. because it is a walking agent)
		if (!(route instanceof NetworkRoute)) return false;

		NetworkRoute oldRoute = (NetworkRoute) route;

		/*
		 *  Get the Id of the current Link.
		 *  Create a List that contains all links of a route, including the Start- and EndLinks.
		 */
		List<Id> allLinkIds = getRouteLinkIds(oldRoute);
		Id currentLinkId = allLinkIds.get(currentLinkIndex);

		Link fromLink = network.getLinks().get(currentLinkId);
		Link toLink = network.getLinks().get(route.getEndLinkId());
		
		Facility fromFacility = new LinkWrapperFacility(fromLink);
		Facility toFacility = new LinkWrapperFacility(toLink);
		
		List<? extends PlanElement> planElements = tripRouter.calcRoute(leg.getMode(), fromFacility, toFacility, time, plan.getPerson());
		
		if (planElements.size() != 1) {
			throw new RuntimeException("Expected a list of PlanElements containing exactly one element, " +
					"but the returned list contained " + planElements.size() + " elements."); 
		}
				
		// The linkIds of the new Route
		List<Id> linkIds = new ArrayList<Id>();

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

	private List<Id> getRouteLinkIds(Route route) {
		List<Id> linkIds = new ArrayList<Id>();

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
	
	/*
	 * Wrapps a Link into a Facility.
	 */
	public class LinkWrapperFacility implements Facility {
		
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
