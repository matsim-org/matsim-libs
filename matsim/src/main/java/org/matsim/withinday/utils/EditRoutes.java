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
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.mobsim.framework.HasPerson;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.LinkWrapperFacility;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.Facility;
import org.matsim.vehicles.Vehicle;

public class EditRoutes {
	/*
	 * Design thoughts while adding access/egress walk legs to car trips:
	 * . Matsim has a computer science router (LeastCostPathCalculator, from node to node) and a behavioral router (RoutingModule, from
	 *    facility to facility).
	 * . For something like replanCurrentRoute it does not really make sense to start at a facility.
	 * . So it seems that for replanCurrentRoute, the computer science router makes more sense.
	 * . This is, however, not true for all the relocate calls, since they also need to reroute the egress leg.
	 * yyyy So, in short: all the relocate methods need to be adapted.  For "future", this is easy.  For "current", the splicing needs to be re-done.  kai, dec'15
	 * 
	 * Hm, next thought: It is not fully clear how relocateXxx has worked in the past, since also there the succeeding activity would 
	 * have needed adaptation. So in the end the method should just do what it promises (i.e. modify a single leg), and not
	 * think too much about its context.  (In the end, will presumably retrofit the mobsim in a way that it is able to process also the
	 * old car trips.)
	 */ 

	private static final Logger log = Logger.getLogger(EditRoutes.class);

	private  Network network ;
	private  LeastCostPathCalculator pathCalculator ;
	private  ModeRouteFactory routeFactories ;

	public EditRoutes(){} // for backwards compatibility

	public EditRoutes( Network network, LeastCostPathCalculator pathCalculator, ModeRouteFactory routeFactory ) {
		this.network = network ;
		this.pathCalculator = pathCalculator ;
		this.routeFactories = routeFactory ;
	}

	/**
	 * Re-locates a future route. The route is given by its leg. It just replaces the route for the given leg, without looking after
	 * overall plan consistency.
	 * 
	 * @return true when replacing the route worked, false when something went wrong
	 * 
	 */
	public boolean relocateFutureLegRoute(Leg leg, Id<Link> fromLinkId, Id<Link> toLinkId, Person person ) {

		Link fromLink = network.getLinks().get(fromLinkId);
		Link toLink = network.getLinks().get(toLinkId);

		Vehicle vehicle = null ;
		Node startNode = fromLink.getToNode() ;
		Node endNode = toLink.getFromNode() ;
		double starttime = leg.getDepartureTime() ;
		Path path = pathCalculator.calcLeastCostPath(startNode, endNode, starttime, person, vehicle) ;

		if (path == null) throw new RuntimeException("No route found from node " + startNode.getId() + " to node " + endNode.getId() + ".");
		NetworkRoute route = this.routeFactories.createRoute(NetworkRoute.class, fromLink.getId(), toLink.getId());
		route.setLinkIds(fromLink.getId(), NetworkUtils.getLinkIds(path.links), toLink.getId());
		route.setTravelTime((int) path.travelTime); // yyyy why int?  kai, dec'15
		route.setTravelCost(path.travelCost);
		route.setDistance(RouteUtils.calcDistance(route,1.,1., this.network));
		leg.setRoute(route);

		return true;
	}

	/**
	 * Re-locates a future route. The route is given by its leg.
	 * 
	 * @return true when replacing the route worked, false when something went wrong
	 * 
	 * @deprecated switch this to relocateFutureTrip, since with egress legs relocating the destination of a single leg leads to disconnected trips. kai, dec'15
	 */
	@Deprecated // switch this to relocateFutureTrip, since with egress legs relocating the destination of a single leg leads to disconnected trips. kai, dec'15
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
		return relocateFutureLegRoute( leg, leg.getRoute().getStartLinkId(), leg.getRoute().getEndLinkId(), person, network, tripRouter ) ;
	}

	/**
	 * Re-plans a future route. The route is given by its leg. It is expected that the
	 * leg's route is not null and that the start- and end link Ids are set properly.
	 * 
	 * If the start- and or end-location of the leg have changed, use relocateFutureLegRoute(...)!
	 * 
	 * @return true when replacing the route worked, false when something went wrong
	 */
	public boolean replanFutureLegRoute(Leg leg, Person person ) {
		return relocateFutureLegRoute( leg, leg.getRoute().getStartLinkId(), leg.getRoute().getEndLinkId(), person ) ;
	}

	/**
	 * In contrast to the other replanFutureLegRoute(...) method, the leg at the given index is replaced
	 * by a new one. This is e.g. necessary when replacing a pt trip which might consists of multiple legs
	 * and pt_interaction activities.  
	 * This might become the future default approach.
	 */
	public static boolean replanFutureTrip(Trip trip, Plan plan, String mainMode, double departureTime, Network network, TripRouter tripRouter) {

		Person person = plan.getPerson();

		Activity fromActivity = trip.getOriginActivity();
		Activity toActivity = trip.getDestinationActivity();

		Link fromLink = network.getLinks().get(fromActivity.getLinkId());
		Link toLink = network.getLinks().get(toActivity.getLinkId());

		Facility<ActivityFacility> fromFacility = new LinkWrapperFacility(fromLink);
		Facility<ActivityFacility> toFacility = new LinkWrapperFacility(toLink);

		final List<? extends PlanElement> newTrip = tripRouter.calcRoute(mainMode, fromFacility, toFacility, departureTime, person);

		TripRouter.insertTrip(plan, trip.getOriginActivity(), newTrip, trip.getDestinationActivity());

		return true;
	}

	/**
	 * @deprecated switch this to (a new) relocateCurrentTrip, since with egress legs relocating the destination of a single leg leads to disconnected trips. kai, dec'15
	 */
	@Deprecated // switch this to (a new) relocateCurrentTrip, since with egress legs relocating the destination of a single leg leads to disconnected trips. kai, dec'15
	public static boolean relocateCurrentRoute( MobsimAgent agent, Id<Link> toLinkId, double now, Network network, TripRouter tripRouter ) {
//		Leg leg = WithinDayAgentUtils.getModifiableCurrentLeg(agent) ;
//		Person person = ((HasPerson) agent).getPerson() ;
//		int currentLinkIndex = WithinDayAgentUtils.getCurrentRouteLinkIdIndex(agent) ;
//		return relocateCurrentLegRoute( leg, person, currentLinkIndex, toLinkId, now, network, tripRouter ) ;
		throw new RuntimeException("currently not implemented: existing version was not consistent with access/egress legs") ;
	}

	public static boolean replanCurrentRoute( MobsimAgent agent, double now, Network network, TripRouter tripRouter ) {
		Leg leg = WithinDayAgentUtils.getModifiableCurrentLeg(agent) ;
		Person person = ((HasPerson) agent).getPerson() ;
		int currentLinkIndex = WithinDayAgentUtils.getCurrentRouteLinkIdIndex(agent) ;
//		return replanCurrentLegRoute( leg, person, currentLinkIndex, now, network, tripRouter ) ;
		throw new RuntimeException("currently not implemented") ;
	}

	/**
	 * Re-locates a current route. The route is given by its leg.  Does not look after plans consistency!
	 * 
	 * @return true when replacing the route worked, false when something went wrong
	 * 
	 */
	public boolean relocateCurrentLegRoute(Leg leg, Person person, int currentLinkIndex, Id<Link> toLinkId, double time ) {

		Route route = leg.getRoute();

		// if the route type is not supported:
		if (!(route instanceof NetworkRoute)) {
			log.warn( "route not instance of NetworkRoute");
			return false;
		}

		NetworkRoute oldRoute = (NetworkRoute) route;

		/*
		 *  Create a List that contains all links of a route, including the Start- and EndLinks.
		 *  Get the Id of the current Link.
		 */
		List<Id<Link>> oldLinkIds = getRouteLinkIds(oldRoute);
		Id<Link> currentLinkId = oldLinkIds.get(currentLinkIndex);

		final Link startLink = network.getLinks().get(currentLinkId);
		final Link endLink = network.getLinks().get(toLinkId);

		Vehicle vehicle = null ;
		Path path = this.pathCalculator.calcLeastCostPath(startLink.getToNode(), endLink.getFromNode(), time, person, vehicle) ;

		List<Id<Link>> newLinkIds = new ArrayList<Id<Link>>();

		/*
		 * Get those Links which have already been passed.
		 * allLinkIds contains also the startLinkId, which should not
		 * be part of the List - it is set separately. Therefore we start
		 * at index 1.
		 */
		if (currentLinkIndex > 0) {
			newLinkIds.addAll(oldLinkIds.subList(1, currentLinkIndex + 1));
		}

		//		Leg newLeg = (Leg) planElements.get(0);
		//		Route newRoute = newLeg.getRoute();

		// Merge old and new Route.
		/*
		 * Edit cdobler 25.5.2010
		 * If the new leg ends at the current Link, we have to
		 * remove that linkId from the linkIds List - it is stored
		 * in the endLinkId field of the route.
		 */
		if (newLinkIds.size() > 0 && path.links.size()>0 && newLinkIds.get(newLinkIds.size() - 1).equals( path.links.get( path.links.size()-1 ) ) ) {
			newLinkIds.remove( newLinkIds.size()-1 );
		}

		newLinkIds.addAll( NetworkUtils.getLinkIds( path.links ) ) ;

		// Overwrite old Route
		oldRoute.setLinkIds(oldRoute.getStartLinkId(), newLinkIds, toLinkId );

		return true;
	}


	/**
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
//	public static boolean replanCurrentLegRoute(Leg leg, Person person, int currentLinkIndex, double time, Network network, TripRouter tripRouter) {
//
//		Route route = leg.getRoute();
//
//		// if the route type is not supported (e.g. because it is a walking agent)
//		if (!(route instanceof NetworkRoute)) return false;
//
//		// This is just a special case of relocateCurrentLegRoute where the end link of the route is not changed.
//		return relocateCurrentLegRoute(leg, person, currentLinkIndex, route.getEndLinkId(), time, network, tripRouter);
//		throw new RuntimeException("currently not implemented: not consistent with access/egress legs" ) ;
//	}

	/**
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
	public  boolean replanCurrentLegRoute(Leg leg, Person person, int currentLinkIndex, double time ) {

		Route route = leg.getRoute();

		// if the route type is not supported (e.g. because it is a walking agent)
		if (!(route instanceof NetworkRoute)) {
			log.warn("route not instance of network route");
			return false;
		}

		// This is just a special case of relocateCurrentLegRoute where the end link of the route is not changed.
		return relocateCurrentLegRoute(leg, person, currentLinkIndex, route.getEndLinkId(), time );
	}

	// #########################################################################################
	// helper methods below

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
