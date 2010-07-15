/* *********************************************************************** *
 * project: org.matsim.*
 * MultiModalPlansCalcRoute.java
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

package playground.christoph.multimodal.router;

import java.util.Set;
import java.util.TreeSet;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteFactory;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.IntermodalLeastCostPathCalculator;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.utils.misc.NetworkUtils;
import org.matsim.core.utils.misc.RouteUtils;

import playground.christoph.multimodal.router.costcalculator.BufferedTravelTime;
import playground.christoph.multimodal.router.costcalculator.MultiModalTravelTimeCost;
import playground.christoph.multimodal.router.costcalculator.TravelTimeCalculatorWithBuffer;

public class MultiModalPlansCalcRoute extends PlansCalcRoute {

	private RouteFactory walkRouteFactory = new LinkNetworkRouteFactory();
	private RouteFactory bikeRouteFactory = new LinkNetworkRouteFactory();
	private RouteFactory ptRouteFactory = new LinkNetworkRouteFactory();
	
	private IntermodalLeastCostPathCalculator walkRouteAlgo;
	private IntermodalLeastCostPathCalculator bikeRouteAlgo;
	private IntermodalLeastCostPathCalculator ptRouteAlgo;
	
	public MultiModalPlansCalcRoute(final PlansCalcRouteConfigGroup group, final Network network,
			final PersonalizableTravelCost costCalculator,
			final TravelTime timeCalculator, LeastCostPathCalculatorFactory factory){
		super(group, network, costCalculator, timeCalculator, factory);
		
		initRouteAlgos(network, timeCalculator);
	}

	public MultiModalPlansCalcRoute(final PlansCalcRouteConfigGroup group, final Network network, final PersonalizableTravelCost costCalculator, final TravelTime timeCalculator) {
		this(group, network, costCalculator, timeCalculator, new DijkstraFactory());
	}

	private void initRouteAlgos(Network network, TravelTime travelTime)
	{	
		/*
		 * Walk
		 */
		MultiModalTravelTimeCost walkTravelTimeCost = new MultiModalTravelTimeCost(this.configGroup, TransportMode.walk);
		walkRouteAlgo = new Dijkstra(network, walkTravelTimeCost, walkTravelTimeCost);
		
		Set<String> walkModeRestrictions = new TreeSet<String>();
		walkModeRestrictions.add(TransportMode.walk);
		walkRouteAlgo.setModeRestriction(walkModeRestrictions);
		
		
		/*
		 * Bike
		 */
		MultiModalTravelTimeCost bikeTravelTimeCost = new MultiModalTravelTimeCost(this.configGroup, TransportMode.bike);
		bikeRouteAlgo = new Dijkstra(network, bikeTravelTimeCost, bikeTravelTimeCost);
		
		Set<String> bikeModeRestrictions = new TreeSet<String>();
		bikeModeRestrictions.add(TransportMode.bike);
		bikeRouteAlgo.setModeRestriction(bikeModeRestrictions);
		
		
		/*
		 * PT
		 * If possible, we use "real" TravelTimes from previous iterations - if not,
		 * freeSpeedTravelTimes are used.
		 * We set the ScaleFactor to 1.25 which means that the returned TravelTimes
		 * will be higher (e.g. to respect stops that a bus performs to pick up agents).
		 */
		MultiModalTravelTimeCost ptTravelTimeCost = new MultiModalTravelTimeCost(this.configGroup, TransportMode.pt);
		if (travelTime instanceof TravelTimeCalculatorWithBuffer) {
			BufferedTravelTime bufferedTravelTime = new BufferedTravelTime((TravelTimeCalculatorWithBuffer) travelTime);
			bufferedTravelTime.setScaleFactor(1.25);
			ptTravelTimeCost.setTravelTime(bufferedTravelTime);
		}
		ptRouteAlgo = new Dijkstra(network, ptTravelTimeCost, ptTravelTimeCost);
		
		Set<String> ptModeRestrictions = new TreeSet<String>();
		ptModeRestrictions.add(TransportMode.pt);
		ptRouteAlgo.setModeRestriction(ptModeRestrictions);
	}
	
	@Override
	protected double handleWalkLeg(final Leg leg, final Activity fromAct, final Activity toAct, final double depTime) {
		
		return handleMultiModalLeg(leg, fromAct, toAct, depTime, walkRouteAlgo, walkRouteFactory);
		
//		double travTime = 0;
//		Link fromLink = this.network.getLinks().get(fromAct.getLinkId());
//		Link toLink = this.network.getLinks().get(toAct.getLinkId());
//		if (fromLink == null) throw new RuntimeException("fromLink missing.");
//		if (toLink == null) throw new RuntimeException("toLink missing.");
//
//		Node startNode = fromLink.getToNode();	// start at the end of the "current" link
//		Node endNode = toLink.getFromNode(); // the target is the start of the link
//
//		Path path = null;
//		if (toLink != fromLink) {
//			// do not drive/walk around, if we stay on the same link
//			path = this.walkRouteAlgo.calcLeastCostPath(startNode, endNode, depTime);
//			if (path == null) throw new RuntimeException("No route found from node " + startNode.getId() + " to node " + endNode.getId() + ".");
//			NetworkRoute route = (NetworkRoute) walkRouteFactory.createRoute(fromLink.getId(), toLink.getId());
//			route.setLinkIds(fromLink.getId(), NetworkUtils.getLinkIds(path.links), toLink.getId());
//			route.setTravelTime((int) path.travelTime);
//			route.setTravelCost(path.travelCost);
//			route.setDistance(RouteUtils.calcDistance(route, this.network));
//			leg.setRoute(route);
//			travTime = (int) path.travelTime;
//		} else {
//			// create an empty route == staying on place if toLink == endLink
//			NetworkRoute route = (NetworkRoute) walkRouteFactory.createRoute(fromLink.getId(), toLink.getId());
//			route.setTravelTime(0);
//			route.setDistance(0.0);
//			leg.setRoute(route);
//			travTime = 0;
//		}
//
//		leg.setDepartureTime(depTime);
//		leg.setTravelTime(travTime);
//		((LegImpl) leg).setArrivalTime(depTime + travTime); // yy something needs to be done once there are alternative implementations of the interface.  kai, apr'10
//		return travTime;
	}
	
	@Override
	protected double handleBikeLeg(final Leg leg, final Activity fromAct, final Activity toAct, final double depTime) {
		return handleMultiModalLeg(leg, fromAct, toAct, depTime, bikeRouteAlgo, bikeRouteFactory);
	}
	
	@Override
	protected double handlePtLeg(final Leg leg, final Activity fromAct, final Activity toAct, final double depTime) {
		return handleMultiModalLeg(leg, fromAct, toAct, depTime, ptRouteAlgo, ptRouteFactory);
	}
	
	/*
	 * Adapted from handleCarLeg - exchanged only the routeAlgo and the routeFactory.
	 */
	private double handleMultiModalLeg(Leg leg, Activity fromAct, Activity toAct, double depTime, 
			IntermodalLeastCostPathCalculator routeAlgo, RouteFactory routeFactory)
	{
		double travTime = 0;
		Link fromLink = this.network.getLinks().get(fromAct.getLinkId());
		Link toLink = this.network.getLinks().get(toAct.getLinkId());
		if (fromLink == null) throw new RuntimeException("fromLink missing.");
		if (toLink == null) throw new RuntimeException("toLink missing.");

		Node startNode = fromLink.getToNode();	// start at the end of the "current" link
		Node endNode = toLink.getFromNode(); // the target is the start of the link

		Path path = null;
		if (toLink != fromLink) {
			// do not drive/walk around, if we stay on the same link
			path = routeAlgo.calcLeastCostPath(startNode, endNode, depTime);
			if (path == null) throw new RuntimeException("No route found from node " + startNode.getId() + " to node " + endNode.getId() + ".");
			NetworkRoute route = (NetworkRoute) routeFactory.createRoute(fromLink.getId(), toLink.getId());
			route.setLinkIds(fromLink.getId(), NetworkUtils.getLinkIds(path.links), toLink.getId());
			route.setTravelTime((int) path.travelTime);
			route.setTravelCost(path.travelCost);
			route.setDistance(RouteUtils.calcDistance(route, this.network));
			leg.setRoute(route);
			travTime = (int) path.travelTime;
		} else {
			// create an empty route == staying on place if toLink == endLink
			NetworkRoute route = (NetworkRoute) routeFactory.createRoute(fromLink.getId(), toLink.getId());
			route.setTravelTime(0);
			route.setDistance(0.0);
			leg.setRoute(route);
			travTime = 0;
		}

		leg.setDepartureTime(depTime);
		leg.setTravelTime(travTime);
		((LegImpl) leg).setArrivalTime(depTime + travTime); // yy something needs to be done once there are alternative implementations of the interface.  kai, apr'10
		return travTime;
	}
}
