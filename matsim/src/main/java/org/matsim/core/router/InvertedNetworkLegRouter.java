/* *********************************************************************** *
 * project: org.matsim.*
 * InvertedNetworkLegRouter
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
package org.matsim.core.router;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.network.NetworkFactoryImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.costcalculators.TravelCostCalculatorFactory;
import org.matsim.core.router.util.AStarLandmarksFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.NetworkInverter;
import org.matsim.core.router.util.PersonalizableLinkToLinkTravelTime;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.router.util.TravelTimesInvertedNetProxy;
import org.matsim.core.utils.misc.RouteUtils;


/**
 * This leg router takes travel times needed for turning moves into account. This is
 * done by a routing on an inverted network, i.e. the links of the street networks are
 * converted to nodes and for each turning move a link is inserted. 
 * 
 * This LegRouter can only be used if the enableLinkToLinkRouting parameter in the 
 * controler config module is set and AStarLandmarks routing is not enabled.
 * @author dgrether
 *
 */
public class InvertedNetworkLegRouter implements LegRouter{

	private LeastCostPathCalculator leastCostPathCalculator = null;

	private Network invertedNetwork = null;

	private NetworkFactoryImpl routeFactory = null;

	private Network network = null;

	public InvertedNetworkLegRouter(Network network, NetworkFactoryImpl routeFactory, 
			LeastCostPathCalculatorFactory leastCostPathCalcFactory, TravelCostCalculatorFactory travelCostCalculatorFactory, 
			PlanCalcScoreConfigGroup cnScoringGroup, TravelTime travelTimes) {
		this.network = network;
		this.routeFactory = routeFactory;

		if (!(travelTimes instanceof PersonalizableLinkToLinkTravelTime)){
			throw new IllegalStateException("If link to link travel times should be used for routing the TravelTime instance must be an instance of PersonalizableLinkToLinkTravelTime" +
					" but is an instance of " + travelTimes.getClass() + ". " + 
					"  Set the enableLinkToLinkRouting config parameter in the" +
					" controler config module and the calculateLinkToLinkTravelTimes in the travelTimeCalculator module!");
		}
		
		NetworkInverter networkInverter = new NetworkInverter(network);
		this.invertedNetwork = networkInverter.getInvertedNetwork();
		
		if (leastCostPathCalcFactory instanceof AStarLandmarksFactory){
			throw new IllegalStateException("Link to link routing is not available for AStarLandmarks routing," +
					" use the Dijkstra or AStar router instead. ");
		}

		TravelTimesInvertedNetProxy travelTimesProxy = new TravelTimesInvertedNetProxy(network, (PersonalizableLinkToLinkTravelTime) travelTimes);
		TravelCost travelCost = travelCostCalculatorFactory.createTravelCostCalculator(travelTimesProxy, cnScoringGroup);
		
		this.leastCostPathCalculator = leastCostPathCalcFactory.createPathCalculator(this.invertedNetwork, travelCost, travelTimesProxy);		
	}


	@Override
	public double routeLeg(Person person, Leg leg, Activity fromAct, Activity toAct, double departureTime) {
		double travelTime = 0.0;
		NetworkRoute route = null;
		Id fromLinkId = fromAct.getLinkId();
		Id toLinkId = toAct.getLinkId();
		if (fromLinkId == null) throw new RuntimeException("fromLink Id missing in Activity.");
		if (toLinkId == null) throw new RuntimeException("toLink Id missing in Activity.");

		if (fromLinkId.equals(toLinkId)){ //no route has to be calculated
			route = (NetworkRoute) this.routeFactory.createRoute(TransportMode.car, fromLinkId, toLinkId);
			route.setDistance(0.0);
		}
		else {
			Node fromINode = this.invertedNetwork.getNodes().get(fromLinkId);
			Node toINode = this.invertedNetwork.getNodes().get(toLinkId);
			Path path = null;
			path = this.leastCostPathCalculator.calcLeastCostPath(fromINode, toINode, departureTime);
			if (path == null) throw new RuntimeException("No route found on inverted network from link " + fromLinkId + " to link " + toLinkId + ".");
			route = this.invertPath2NetworkRoute(path, fromLinkId, toLinkId);
			travelTime = path.travelTime;
		}

		leg.setDepartureTime(departureTime);
		leg.setTravelTime(travelTime);
		((LegImpl) leg).setArrivalTime(departureTime + travelTime);
		leg.setRoute(route);
		return travelTime;
	}


	private NetworkRoute invertPath2NetworkRoute(Path path, Id fromLinkId, Id toLinkId) {
		NetworkRoute route = (NetworkRoute) this.routeFactory.createRoute(TransportMode.car, fromLinkId, toLinkId);
		List<Node> nodes = path.nodes;
		//remove first and last as their ids are, due to inversion, from and to link id of the route
		nodes.remove(0);
		nodes.remove(nodes.size() - 1);
		List<Id> linkIds = new ArrayList<Id>();
		for (Node n : nodes){
			linkIds.add(n.getId());
		}
		route.setLinkIds(fromLinkId, linkIds, toLinkId);
		route.setTravelTime((int) path.travelTime);
		route.setTravelCost(path.travelCost);
		route.setDistance(RouteUtils.calcDistance(route, this.network));
		return route;
	}
	
	
	
}
