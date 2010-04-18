/* *********************************************************************** *
 * project: org.matsim.*
 * TranitRouter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.mmoyo.ptRouterAdapted;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.config.TransitConfigGroup;
import org.matsim.pt.router.MultiNodeDijkstra;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.MultiNodeDijkstra.InitialNode;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.transitSchedule.api.TransitLine;
import org.matsim.transitSchedule.api.TransitRoute;
import org.matsim.transitSchedule.api.TransitRouteStop;
import org.matsim.transitSchedule.api.TransitSchedule;
import org.matsim.transitSchedule.api.TransitStopFacility;

import playground.mmoyo.PTRouter.PTValues;

import playground.mmoyo.ptRouterAdapted.AdaptedTransitRouterNetwork.TransitRouterNetworkLink;
import playground.mmoyo.ptRouterAdapted.AdaptedTransitRouterNetwork.TransitRouterNetworkNode;

public class AdaptedTransitRouter {

	private final TransitSchedule schedule;
	private final AdaptedTransitRouterNetwork transitNetwork;
	private final Map<TransitRouterNetworkLink, Tuple<TransitLine, TransitRoute>> linkMappings;
	private final Map<TransitRouterNetworkNode, TransitStopFacility> nodeMappings;

	private final MultiNodeDijkstra dijkstra;
	private final AdaptedTransitRouterNetworkTravelTimeCost ttCalculator;
	
	private final TransitRouterConfig config ; 

	public AdaptedTransitRouter( TransitRouterConfig config, final TransitSchedule schedule) {
		this.schedule = schedule;
		this.linkMappings = new HashMap<TransitRouterNetworkLink, Tuple<TransitLine, TransitRoute>>();
		this.nodeMappings = new HashMap<TransitRouterNetworkNode, TransitStopFacility>();
		this.transitNetwork = buildNetwork();
		
		this.config = config ;

		this.ttCalculator = new AdaptedTransitRouterNetworkTravelTimeCost( config );
		this.dijkstra = new MultiNodeDijkstra(this.transitNetwork, this.ttCalculator, this.ttCalculator);
	}

	public List<Leg> calcRoute(final Coord fromCoord, final Coord toCoord, final double departureTime) {

		//progressive stop search*
		Collection <TransitRouterNetworkNode> fromNodes;
		double walkRadius = PTValues.FIRST_WALKRANGE;
		do{
			fromNodes = this.transitNetwork.getNearestNodes(fromCoord, walkRadius);  //walkRange
			walkRadius += PTValues.WALKRANGE_EXT;
		} while (fromNodes.size() < PTValues.INI_STATIONS_NUM);
		///////////////////////////////////////////////////
		
		Map<Node, InitialNode> wrappedFromNodes = new LinkedHashMap<Node, InitialNode>();
		for (TransitRouterNetworkNode node : fromNodes) {
			double distance = CoordUtils.calcDistance(fromCoord, node.stop.getStopFacility().getCoord());
			double initialTime = distance / config.beelineWalkSpeed ;
//			double initialCost = initialTime * PTValues.walkCoefficient ; // I don't know what this is.  kai, apr'10
			double initialCost =  0. ;
			wrappedFromNodes.put(node, new InitialNode(initialCost, initialTime + departureTime));
		}

		//progressive stop search*
		Collection <TransitRouterNetworkNode> toNodes;
		walkRadius = PTValues.FIRST_WALKRANGE;
		do{
			toNodes = this.transitNetwork.getNearestNodes(toCoord, walkRadius);  //walkRange
			walkRadius += PTValues.WALKRANGE_EXT;
		} while (toNodes.size() < PTValues.INI_STATIONS_NUM);
		////////////////////////////////////////////////////
		
		Map<Node, InitialNode> wrappedToNodes = new LinkedHashMap<Node, InitialNode>();
		for (TransitRouterNetworkNode node : toNodes) {
			double distance = CoordUtils.calcDistance(toCoord, node.stop.getStopFacility().getCoord());
			double initialTime = distance * PTValues.AV_WALKING_SPEED;
			double initialCost = initialTime * PTValues.walkCoefficient;
			wrappedToNodes.put(node, new InitialNode(initialCost, initialTime + departureTime));
		}

		// find routes between start and end stops
		Path p = this.dijkstra.calcLeastCostPath(wrappedFromNodes, wrappedToNodes);
		
		if (p == null) {
			return null;
		}
		
		// optional direct walk *
		if (PTValues.allowDirectWalks){
			double directWalkCost = (CoordUtils.calcDistance(fromCoord, toCoord) * PTValues.AV_WALKING_SPEED) * ( PTValues.walkCoefficient);
			double pathCost = p.travelCost + wrappedFromNodes.get(p.nodes.get(0)).initialCost + wrappedToNodes.get(p.nodes.get(p.nodes.size() - 1)).initialCost;
			if (directWalkCost < pathCost) {
				List<Leg> legs = new ArrayList<Leg>();
				Leg leg = new LegImpl(TransportMode.transit_walk);
				double walkTime = CoordUtils.calcDistance(fromCoord, toCoord) * PTValues.AV_WALKING_SPEED;
				Route walkRoute = new GenericRouteImpl(null, null);
				leg.setRoute(walkRoute);
				leg.setTravelTime(walkTime);
				legs.add(leg);
				return legs;
			}
		}

		// now convert the path back into a series of legs with correct routes
		double time = departureTime;
		List<Leg> legs = new ArrayList<Leg>();
		Leg leg = null;

		TransitLine line = null;
		TransitRoute route = null;
		TransitStopFacility accessStop = null;
		TransitRouteStop transitRouteStart = null;
		Link prevLink = null;
		int transitLegCnt = 0;
		double ptRouteDistance=0;
		for (Link link : p.links) {
			Tuple<TransitLine, TransitRoute> lineData = this.linkMappings.get(link);
			//			TransitStopFacility nodeData = this.nodeMappings.get(((TransitRouterNetworkWrapper.NodeWrapper)link.getToNode()).node);
			if (lineData == null) {
				TransitStopFacility egressStop = this.nodeMappings.get(link.getFromNode());
				// it must be one of the "transfer" links. finish the pt leg, if there was one before...
				if (route != null) {
					leg = new LegImpl(TransportMode.pt);
					ExperimentalTransitRoute ptRoute = new ExperimentalTransitRoute(accessStop, line, route, egressStop);
					ptRoute.setDistance(ptRouteDistance);
					leg.setRoute(ptRoute);
					double arrivalOffset = (((TransitRouterNetworkLink) link).getFromNode().stop.getArrivalOffset() != Time.UNDEFINED_TIME) ? ((TransitRouterNetworkLink) link).fromNode.stop.getArrivalOffset() : ((TransitRouterNetworkLink) link).fromNode.stop.getDepartureOffset();
					double arrivalTime = this.ttCalculator.getNextDepartureTime(route, transitRouteStart, time) + (arrivalOffset - transitRouteStart.getDepartureOffset());
					leg.setTravelTime(arrivalTime - time);
					time = arrivalTime;
					legs.add(leg);
					transitLegCnt++;
					accessStop = egressStop;
					ptRouteDistance=0;
				}
				line = null;
				route = null;
				transitRouteStart = null;
			} else {
				if (lineData.getSecond() != route) {
					// the line changed
					TransitStopFacility egressStop = this.nodeMappings.get(((TransitRouterNetworkLink)link).getFromNode());
					if (route == null) {
						// previously, the agent was on a transfer, add the walk leg
						transitRouteStart = ((TransitRouterNetworkLink) link).getFromNode().stop;
						if (accessStop != egressStop) {
							if (accessStop != null) {
								leg = new LegImpl(TransportMode.transit_walk);
								double walkTime = CoordUtils.calcDistance(accessStop.getCoord(), egressStop.getCoord()) * PTValues.AV_WALKING_SPEED;
								Route walkRoute = new GenericRouteImpl(accessStop.getLinkId(), egressStop.getLinkId());
								leg.setRoute(walkRoute);
								leg.setTravelTime(walkTime);
								time += walkTime;
								legs.add(leg);
							} else { // accessStop == null, so it must be the first walk-leg
								leg = new LegImpl(TransportMode.transit_walk);
								double walkTime = CoordUtils.calcDistance(fromCoord, egressStop.getCoord()) * PTValues.AV_WALKING_SPEED;
								leg.setTravelTime(walkTime);
								time += walkTime;
								legs.add(leg);
							}
						}
					}
					line = lineData.getFirst();
					route = lineData.getSecond();
					accessStop = egressStop;
				}
				ptRouteDistance +=link.getLength();
			}
			prevLink = link;
		}
		if (route != null) {
			// the last part of the path was with a transit route, so add the pt-leg and final walk-leg
			leg = new LegImpl(TransportMode.pt);
			TransitStopFacility egressStop = this.nodeMappings.get(((TransitRouterNetworkLink) prevLink).getToNode());
			ExperimentalTransitRoute ptRoute = new ExperimentalTransitRoute(accessStop, line, route, egressStop);
			ptRoute.setDistance(ptRouteDistance);
			leg.setRoute(ptRoute);
			double arrivalOffset = (((TransitRouterNetworkLink) prevLink).toNode.stop.getArrivalOffset() != Time.UNDEFINED_TIME) ? ((TransitRouterNetworkLink) prevLink).toNode.stop.getArrivalOffset() : ((TransitRouterNetworkLink) prevLink).toNode.stop.getDepartureOffset();
			double arrivalTime = this.ttCalculator.getNextDepartureTime(route, transitRouteStart, time) + (arrivalOffset - transitRouteStart.getDepartureOffset());
			leg.setTravelTime(arrivalTime - time);
			legs.add(leg);
			transitLegCnt++;
			accessStop = egressStop;
			ptRouteDistance=0;
		}
		if (prevLink != null) {
			leg = new LegImpl(TransportMode.transit_walk);
			double walkTime;
			if (accessStop == null) {
				walkTime = CoordUtils.calcDistance(fromCoord, toCoord) * PTValues.AV_WALKING_SPEED;
			} else {
				walkTime = CoordUtils.calcDistance(accessStop.getCoord(), toCoord) * PTValues.AV_WALKING_SPEED;
			}
			leg.setTravelTime(walkTime);
			legs.add(leg);
		}
		if (transitLegCnt == 0) {
			// it seems, the agent only walked
			legs.clear();
			leg = new LegImpl(TransportMode.transit_walk);
			double walkTime = CoordUtils.calcDistance(fromCoord, toCoord) * PTValues.AV_WALKING_SPEED;
			leg.setTravelTime(walkTime);
			legs.add(leg);
		}
		return legs;
	}

	private AdaptedTransitRouterNetwork buildNetwork() {
		final AdaptedTransitRouterNetwork network = new AdaptedTransitRouterNetwork();

		// build nodes and links connecting the nodes according to the transit routes
		for (TransitLine line : this.schedule.getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				TransitRouterNetworkNode prevNode = null;
				for (TransitRouteStop stop : route.getStops()) {
					TransitRouterNetworkNode node = network.createNode(stop, route, line);
					this.nodeMappings.put(node, stop.getStopFacility());
					if (prevNode != null) {
						TransitRouterNetworkLink link = network.createLink(prevNode, node, route, line);
						this.linkMappings.put(link, new Tuple<TransitLine, TransitRoute>(line, route));
					}
					prevNode = node;
				}
			}
		}
		network.finishInit(); // not nice to call "finishInit" here before we added all links...
		List<Tuple<TransitRouterNetworkNode, TransitRouterNetworkNode>> toBeAdded = new LinkedList<Tuple<TransitRouterNetworkNode, TransitRouterNetworkNode>>();
		
		
		int transfers= 0;
				
		//reduced creation of transferlinks*
		for (TransitRouterNetworkNode centerNode : network.getNodes().values()) {
			for (TransitRouterNetworkNode nearNode : network.getNearestNodes(centerNode.getCoord(), PTValues.DETTRANSFER_RANGE)){
				if (centerNode!= nearNode && centerNode.line != nearNode.line) {   // || centerNode.stop.getStopFacility() != nearNode.stop.getStopFacility()  this condition creates more transfer links
					if (centerNode.route.getStops().get(0) != centerNode.stop   && nearNode.route.getStops().get(nearNode.route.getStops().size()-1) != nearNode.stop) {
						toBeAdded.add(new Tuple<TransitRouterNetworkNode, TransitRouterNetworkNode>(centerNode, nearNode));
						transfers++;
					}
				}
			}
		}
		//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		
		for (Tuple<TransitRouterNetworkNode, TransitRouterNetworkNode> tuple : toBeAdded) {
			network.createLink(tuple.getFirst(), tuple.getSecond(), null, null);
		}
		
		System.out.println("\n\n\n\ntransfers:" + transfers);
		System.out.println("transit router network statistics:");
		System.out.println(" # nodes: " + network.getNodes().size());
		System.out.println(" # links: " + network.getLinks().size());

		return network;
	}

	public AdaptedTransitRouterNetwork getTransitRouterNetwork() {
		return this.transitNetwork;
	}

	// the procedures with * were adapted
}
