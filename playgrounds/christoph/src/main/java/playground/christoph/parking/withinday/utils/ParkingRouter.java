/* *********************************************************************** *
 * project: org.matsim.*
 * ParkingRouter.java
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

package playground.christoph.parking.withinday.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.population.routes.GenericRoute;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.ActivityWrapperFacility;
import org.matsim.core.router.InitialNode;
import org.matsim.core.router.MultiNodeDijkstra;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.FastMultiNodeDijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.facilities.Facility;
import org.matsim.vehicles.Vehicle;

public class ParkingRouter {

	private static final Logger log = Logger.getLogger(ParkingRouter.class);
	
	private final Scenario scenario;
	private final TravelTime carTravelTime;
	private final TravelTime walkTravelTime;
	private final TripRouter tripRouter;
	private final int nodesToCheck;
	
	private final TravelDisutility carTravelDisutility;
	private final TravelDisutility walkTravelDisutility;
	private final MultiNodeDijkstra carDijkstra;
	private final MultiNodeDijkstra walkDijkstra;
		
	public ParkingRouter(Scenario scenario, TravelTime carTravelTime, TravelTime walkTravelTime, TravelDisutilityFactory travelDisutilityFactory, 
			TripRouter tripRouter, int nodesToCheck) {
		this.scenario = scenario;
		this.carTravelTime = carTravelTime;
		this.walkTravelTime = walkTravelTime;
		this.tripRouter = tripRouter;
		
		if (nodesToCheck > 0) this.nodesToCheck = nodesToCheck;
		else this.nodesToCheck = 1;
		
		this.carTravelDisutility = travelDisutilityFactory.createTravelDisutility(this.carTravelTime, scenario.getConfig().planCalcScore());
		this.walkTravelDisutility = travelDisutilityFactory.createTravelDisutility(this.walkTravelTime, scenario.getConfig().planCalcScore());
		
		this.carDijkstra = (MultiNodeDijkstra) new FastMultiNodeDijkstraFactory().createPathCalculator(scenario.getNetwork(), 
				this.carTravelDisutility, this.carTravelTime);
		this.walkDijkstra = (MultiNodeDijkstra) new FastMultiNodeDijkstraFactory().createPathCalculator(scenario.getNetwork(), 
				this.walkTravelDisutility, this.walkTravelTime);
	}
	
	/**
	 * Adapts the start of a car route and reuses parts of the existing route.
	 * 
	 * @param route
	 * @param startLinkId
	 * @param time
	 * @param person
	 * @param vehicle
	 */
	public void adaptStartOfCarRoute(NetworkRoute route, Id startLinkId, double time, Person person, Vehicle vehicle) {
		adaptStartOfRoute(route, startLinkId, time, person, vehicle, this.carDijkstra, this.carTravelDisutility);
	}
	
	/**
	 * Adapts the start of a walk route. If it is a NetworkRoute, parts of the existing route
	 * are reused to speed up the routing process. If it is a GenericRoute, a new route is
	 * created.
	 * 
	 * In both cases, the route inside the given walkLeg is updated.
	 * 
	 * @param fromActivity
	 * @param walkLeg
	 * @param toActivity
	 * @param time
	 * @param person
	 * @param vehicle
	 */
	public void adaptStartOfWalkRoute(Activity fromActivity, Leg walkLeg, Activity toActivity, double time, Person person, Vehicle vehicle) {
		Route route = walkLeg.getRoute();
		if (route instanceof NetworkRoute) {
			Id startLinkId = fromActivity.getLinkId();
			adaptStartOfRoute((NetworkRoute) route, startLinkId, time, person, vehicle, this.walkDijkstra, this.walkTravelDisutility);			
		} else if (route instanceof GenericRoute) {
			updateWalkRoute(fromActivity, walkLeg, toActivity, person);
		} else throw new RuntimeException("Found unexpected route type: " + route.getClass().toString() + ". Aborting!");
	}
	
	private void adaptStartOfRoute(NetworkRoute route, Id startLinkId, double time, Person person, Vehicle vehicle, 
			MultiNodeDijkstra dijkstra, TravelDisutility travelDisutility) {
		
		// check whether the start link has really changed
		if (route.getStartLinkId().equals(startLinkId)) return;
		
		Link startLink = this.scenario.getNetwork().getLinks().get(startLinkId);
		Node startNode = startLink.getToNode();
		
		List<InitialNode> initialToNodes = new ArrayList<InitialNode>();
		Map<Node, Integer> nodeIndices = new HashMap<Node, Integer>();
		List<Id<Link>> routeLinkIds = new ArrayList<Id<Link>>();
		
		// if the route does not start and end on the same link and is not a round-trip
		if (!route.getStartLinkId().equals(route.getEndLinkId()) || route.getLinkIds().size() != 0) {
			
			routeLinkIds.add(route.getStartLinkId());
			routeLinkIds.addAll(route.getLinkIds());
			routeLinkIds.add(route.getEndLinkId());
				
			/*
			 * Define how many nodes of the route should be checked. By default,
			 * this is limited by nodesToCheck. For short routes, it is limited
			 * by the route's length.
			 */
			int n = Math.min(nodesToCheck, routeLinkIds.size());
			
			double postCosts = 0.0;
			for (int i = n-1; i >= 0; i--) {
				Id linkId = routeLinkIds.get(i);
				Link link = this.scenario.getNetwork().getLinks().get(linkId);
				Node fromNode = link.getFromNode();
				InitialNode initialNode = new InitialNode(fromNode, postCosts, time);
				initialToNodes.add(initialNode);
				nodeIndices.put(fromNode, i);
				
				// add costs of current link to previous node
				postCosts += travelDisutility.getLinkTravelDisutility(link, time, person, vehicle);
			}
		}
		// route starts on the same link as it ends and is not a round trip
		else {
			Id linkId = route.getStartLinkId();
			Link link = this.scenario.getNetwork().getLinks().get(linkId);
			Node toNode = link.getFromNode();
			double postCosts = 0.0;
			InitialNode initialNode = new InitialNode(toNode, postCosts, time);
			initialToNodes.add(initialNode);
			nodeIndices.put(toNode, 0);
			routeLinkIds.add(startLinkId);
		}
				
		Node toNode = dijkstra.createImaginaryNode(initialToNodes);
		
		Path path = dijkstra.calcLeastCostPath(startNode, toNode, time, person, vehicle);
		
		/*
		 * Merge old and new route.
		 */
		List<Id<Link>> linkIds = new ArrayList<Id<Link>>();
		
		// new links
		for (Link link : path.links) linkIds.add(link.getId());
		
		// existing links
		Node lastNode = path.nodes.get(path.nodes.size() - 1);
		int mergeIndex = nodeIndices.get(lastNode);
		linkIds.addAll(routeLinkIds.subList(mergeIndex, routeLinkIds.size() - 1));
		
		route.setLinkIds(startLink.getId(), linkIds, route.getEndLinkId());
	}
	
	/**
	 * Adapts the end of a car route. If it is a NetworkRoute, parts of the existing route
	 * are reused to speed up the routing process. 
	 * 
	 * @param route
	 * @param endLinkId
	 * @param time
	 * @param person
	 * @param vehicle
	 */
	public void adaptEndOfCarRoute(NetworkRoute route, Id endLinkId, double time, Person person, Vehicle vehicle) {
		adaptEndOfRoute(route, endLinkId, time, person, vehicle, this.carDijkstra, this.carTravelDisutility);
	}
	
	/**
	 * Adapts the end of a walk route. If it is a NetworkRoute, parts of the existing route
	 * are reused to speed up the routing process. If it is a GenericRoute, a new route is
	 * created.
	 * 
	 * In both cases, the route inside the given walkLeg is updated.
	 * 
	 * @param fromActivity
	 * @param walkLeg
	 * @param toActivity
	 * @param time
	 * @param person
	 * @param vehicle
	 */
	public void adaptEndOfWalkRoute(Activity fromActivity, Leg walkLeg, Activity toActivity, double time, Person person, Vehicle vehicle) {
		Route route = walkLeg.getRoute();
		if (route instanceof NetworkRoute) {
			adaptEndOfRoute((NetworkRoute) route, toActivity.getLinkId(), time, person, vehicle, this.walkDijkstra, this.walkTravelDisutility);			
		} else if (route instanceof GenericRoute) {
			updateWalkRoute(fromActivity, walkLeg, toActivity, person);
		} else throw new RuntimeException("Found unexpected route type: " + route.getClass().toString() + ". Aborting!");
	}
	
	private void adaptEndOfRoute(NetworkRoute route, Id endLinkId, double time, Person person, Vehicle vehicle, 
			MultiNodeDijkstra dijkstra, TravelDisutility travelDisutility) {
		
		// check whether the end link has really changed
		if (route.getEndLinkId().equals(endLinkId)) return;
		
		Link endLink = this.scenario.getNetwork().getLinks().get(endLinkId);
		Node endNode = endLink.getFromNode();
		
		List<InitialNode> initialFromNodes = new ArrayList<InitialNode>();
		Map<Node, Integer> nodeIndices = new HashMap<Node, Integer>();
		List<Id<Link>> routeLinkIds = new ArrayList<Id<Link>>();

		// if the route does not start and end on the same link and is not a round-trip
		if (!route.getStartLinkId().equals(route.getEndLinkId()) || route.getLinkIds().size() != 0) {
			
			routeLinkIds.add(route.getStartLinkId());
			routeLinkIds.addAll(route.getLinkIds());
			routeLinkIds.add(route.getEndLinkId());

			/*
			 * Define how many nodes of the route should be checked. By default,
			 * this is limited by nodesToCheck. For short routes, it is limited
			 * by the route's length.
			 */
			int n = Math.min(nodesToCheck, routeLinkIds.size());
			
			double preCosts = 0.0;
			int j = 1;
			for (int i = routeLinkIds.size() - n; i < routeLinkIds.size(); i++) {
				
				Id linkId = routeLinkIds.get(i);
				Link link = this.scenario.getNetwork().getLinks().get(linkId);
				Node fromNode = link.getToNode();
				
				// ignore link costs of first link
				if (i > initialFromNodes.size() - n) {
					// add costs of current link to previous node
					preCosts += travelDisutility.getLinkTravelDisutility(link, time, person, vehicle);				
				}
				
				InitialNode initialNode = new InitialNode(fromNode, preCosts, time);
				initialFromNodes.add(initialNode);
				nodeIndices.put(fromNode, (n - j));
				
				j++;
			}
		}
		// route starts on the same link as it ends and is not a round trip
		else {
			Id linkId = route.getEndLinkId();
			Link link = this.scenario.getNetwork().getLinks().get(linkId);
			Node fromNode = link.getToNode();
			double preCosts = 0.0;
			InitialNode initialNode = new InitialNode(fromNode, preCosts, time);
			initialFromNodes.add(initialNode);
			nodeIndices.put(fromNode, 0);
		}
		
		Node fromNode = dijkstra.createImaginaryNode(initialFromNodes);
		
		Path path = dijkstra.calcLeastCostPath(fromNode, endNode, time, person, vehicle);

		/*
		 * Merge old and new route.
		 */
		List<Id<Link>> linkIds = new ArrayList<Id<Link>>();
		
		// existing links
		Node firstNode = path.nodes.get(0);
		int mergeIndex = nodeIndices.get(firstNode);
		if (routeLinkIds.size() > 1) {
			linkIds.addAll(routeLinkIds.subList(1, routeLinkIds.size() - mergeIndex));			
		}
		
		// new links
		for (Link link : path.links) linkIds.add(link.getId());
		
		route.setLinkIds(route.getStartLinkId(), linkIds, endLinkId);
	}
	
	/**
	 * 
	 * @param route ... the initial route that has to be adapted
	 * @param startLinkId ... the new start link (which does not equals the routes start link)
	 * @param endLinkId ... the new end link (which does not equals the routes end link)
	 * @param time
	 * @param person
	 * @param vehicle
	 * @param mode
	 */
	public void adaptStartAndEndOfRoute(NetworkRoute route, Id startLinkId, Id endLinkId, double time, Person person, Vehicle vehicle, String mode) {
		if (TransportMode.car.equals(mode)) {
			adaptStartAndEndOfRoute(route, startLinkId, endLinkId, time, person, vehicle, this.carDijkstra, this.carTravelDisutility);
		} else if (TransportMode.walk.equals(mode)) {
			adaptStartAndEndOfRoute(route, startLinkId, endLinkId, time, person, vehicle, this.walkDijkstra, this.walkTravelDisutility);
		}
	}
	
	private void adaptStartAndEndOfRoute(NetworkRoute route, Id startLinkId, Id endLinkId, double time, Person person, Vehicle vehicle, 
			MultiNodeDijkstra dijkstra, TravelDisutility travelDisutility) {
		
		// check whether the start and / or link have really changed
		if (route.getStartLinkId().equals(startLinkId)) {
			if (route.getEndLinkId().equals(endLinkId)) return;
			else {
				adaptEndOfRoute(route, endLinkId, time, person, vehicle, dijkstra, travelDisutility);
				return;
			}
		} else {
			if (route.getEndLinkId().equals(endLinkId)) {
				adaptStartOfRoute(route, startLinkId, time, person, vehicle, dijkstra, travelDisutility);
				return;
			}
		}
		
		Link startLink = this.scenario.getNetwork().getLinks().get(startLinkId);
		Node startNode = startLink.getToNode();
		
		Link endLink = this.scenario.getNetwork().getLinks().get(endLinkId);
		Node endNode = endLink.getFromNode();
		
		List<Id> routeLinkIds = new ArrayList<Id>();
		routeLinkIds.add(route.getStartLinkId());
		routeLinkIds.addAll(route.getLinkIds());
		routeLinkIds.add(route.getEndLinkId());
		
		/*
		 * Check whether possible start and end nodes overlap. If yes, we create a new route from scratch.
		 * Otherwise, we can adapt the start and end of the route separated.
		 */
		if (routeLinkIds.size() + 1 <= 2 * nodesToCheck) {
			Path path = dijkstra.calcLeastCostPath(startNode, endNode, time, person, vehicle);
			
			List<Id<Link>> linkIds = new ArrayList<Id<Link>>();
			for (Link link : path.links) linkIds.add(link.getId());
			
			route.setLinkIds(startLinkId, linkIds, endLinkId);
		} else {
			adaptStartOfRoute(route, startLinkId, time, person, vehicle, dijkstra, travelDisutility);
			adaptEndOfRoute(route, endLinkId, time, person, vehicle, dijkstra, travelDisutility);
		}
	}
	
	/**
	 * 
	 * Adds additional links at the end of a route to reach the new end link. 
	 * 
	 * @param route ... the initial route that has to be extended
	 * @param endLinkId ... the new end link (which does not equals the routes end link)
	 * @param time
	 * @param person
	 * @param vehicle
	 * @param mode
	 */
	@Deprecated
	public void extendRoute(NetworkRoute route, Id endLinkId, double time, Person person, Vehicle vehicle, String mode) {
		if (TransportMode.car.equals(mode)) {
			extendRoute(route, endLinkId, time, person, vehicle, this.carDijkstra);
		} else if (TransportMode.walk.equals(mode)) {
			extendRoute(route, endLinkId, time, person, vehicle, this.walkDijkstra);
		}
	}
	
	public void extendCarRoute(NetworkRoute carRoute, Id endLinkId, double time, Person person, Vehicle vehicle) {
		extendRoute(carRoute, endLinkId, time, person, vehicle, this.carDijkstra);
	}
	
	private void extendRoute(NetworkRoute route, Id endLinkId, double time, Person person, Vehicle vehicle, MultiNodeDijkstra dijkstra) {
		
		Link startLink = this.scenario.getNetwork().getLinks().get(route.getEndLinkId());
		Node startNode = startLink.getToNode();
		
		Link endLink = this.scenario.getNetwork().getLinks().get(endLinkId);
		Node endNode = endLink.getFromNode();
		
		Path path = dijkstra.calcLeastCostPath(startNode, endNode, time, person, vehicle);
		
		List<Id<Link>> mergedLinks = new ArrayList<Id<Link>>();
		mergedLinks.addAll(route.getLinkIds());
		/*
		 * If the route starts and ends on the same link and is not a round trip, do not add the end link
		 * since it otherwise the link would be duplicated!
		 */
		if (mergedLinks.size() > 0 || !route.getEndLinkId().equals(route.getStartLinkId())) {
			mergedLinks.add(route.getEndLinkId());
		}
		
		for (Link link : path.links) mergedLinks.add(link.getId());
		
		route.setLinkIds(route.getStartLinkId(), mergedLinks, endLinkId);
	}
	
	/**
	 * Creates a new route from the fromActivity to the toActivity and inserts it
	 * into the given walkLeg.
	 * 
	 * @param fromActivity
	 * @param walkLeg
	 * @param toActivity
	 * @param person
	 */
	public void updateWalkRoute(Activity fromActivity, Leg walkLeg, Activity toActivity, Person person) {
		
		Facility fromFacility;
		Facility toFacility;
		
		if (fromActivity.getFacilityId() != null) {
			fromFacility = scenario.getActivityFacilities().getFacilities().get(fromActivity.getFacilityId());
		} else fromFacility = new ActivityWrapperFacility(fromActivity);
		
		if (toActivity.getFacilityId() != null) {
			toFacility = scenario.getActivityFacilities().getFacilities().get(toActivity.getFacilityId());
		} else toFacility = new ActivityWrapperFacility(toActivity);
		
		RoutingModule routingModule = tripRouter.getRoutingModule(TransportMode.walk);
		List<? extends PlanElement> planElements = routingModule.calcRoute(fromFacility, toFacility, walkLeg.getDepartureTime(), person);
		
		if (planElements.size() != 1) {
			throw new RuntimeException("Expected a list of PlanElements containing exactly one element, " +
					"but the returned list contained " + planElements.size() + " elements."); 
		}
		
		Leg leg = (Leg) planElements.get(0);
		
		if (leg.getRoute() instanceof NetworkRoute && walkLeg.getRoute() instanceof NetworkRoute) {
			NetworkRoute oldRoute = (NetworkRoute) walkLeg.getRoute();
			NetworkRoute newRoute = (NetworkRoute) leg.getRoute();
			
			oldRoute.setLinkIds(newRoute.getStartLinkId(), newRoute.getLinkIds(), newRoute.getEndLinkId());
		} else {
			walkLeg.getRoute().setStartLinkId(leg.getRoute().getStartLinkId());
			walkLeg.getRoute().setEndLinkId(leg.getRoute().getEndLinkId());
		}
		walkLeg.getRoute().setDistance(leg.getRoute().getDistance());
	}
}
