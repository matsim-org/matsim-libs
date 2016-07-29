/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.polettif.publicTransitMapping.mapping.networkRouter;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.FastAStarLandmarksFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.vehicles.Vehicle;
import playground.polettif.publicTransitMapping.config.PublicTransitMappingConfigGroup;
import playground.polettif.publicTransitMapping.mapping.linkCandidateCreation.LinkCandidate;
import playground.polettif.publicTransitMapping.tools.NetworkTools;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * A LeastCostPathCalculator using FastAStarLandmarks.
 *
 * @author polettif
 */
public class FastAStarRouter implements Router {

	private final Network network;

	private final LeastCostPathCalculator pathCalculator;
	private final Map<Tuple<LinkCandidate, LinkCandidate>, LeastCostPathCalculator.Path> paths;
	private static PublicTransitMappingConfigGroup.TravelCostType travelCostType = PublicTransitMappingConfigGroup.TravelCostType.linkLength;

	private Id<Node> uTurnFromNodeId = null;
	private Id<Node> uTurnToNodeId = null;

	public static void setTravelCostType(PublicTransitMappingConfigGroup.TravelCostType type) {
		travelCostType = type;
	}

	public FastAStarRouter(Network network) {
		this.paths = new HashMap<>();
		this.network = network;

		LeastCostPathCalculatorFactory factory = new FastAStarLandmarksFactory(network, this);
		this.pathCalculator = factory.createPathCalculator(network, this, this);
	}

	/**
	 * Filters the network with the given transport modes and creates a router with it
	 */
	public static Router createModeSeparatedRouter(Network network, Set<String> transportModes) {
		Network filteredNetwork = NetworkTools.filterNetworkByLinkMode(network, transportModes);
		return new FastAStarRouter(filteredNetwork);
	}

	/**
	 * Synchronized since {@link org.matsim.core.router.Dijkstra} is not thread safe.
	 */
	@Override
	public synchronized LeastCostPathCalculator.Path calcLeastCostPath(LinkCandidate fromLinkCandidate, LinkCandidate toLinkCandidate) {
		if(fromLinkCandidate != null && toLinkCandidate != null) {
			Tuple<LinkCandidate, LinkCandidate> nodes = new Tuple<>(fromLinkCandidate, toLinkCandidate);
			if(!paths.containsKey(nodes)) {
				Node nodeA = network.getNodes().get(fromLinkCandidate.getToNodeId());
				Node nodeB = network.getNodes().get(toLinkCandidate.getFromNodeId());

				setUTurnLink(fromLinkCandidate.getFromNodeId(), fromLinkCandidate.getToNodeId());
				paths.put(nodes, pathCalculator.calcLeastCostPath(nodeA, nodeB, 0.0, null, null));
				resetUTurnLink();
			}
			return paths.get(nodes);
		} else {
			return null;
		}
	}

	private void resetUTurnLink() {
		uTurnFromNodeId = null;
		uTurnToNodeId = null;
	}

	private void setUTurnLink(Id<Node> fromNodeId, Id<Node> toNodeId) {
		uTurnFromNodeId = fromNodeId;
		uTurnToNodeId = toNodeId;
	}

	@Override
	public Network getNetwork() {
		return network;
	}

	@Override
	public double getMinimalTravelCost(TransitRouteStop fromStop, TransitRouteStop toStop) {
		double travelTime = (toStop.getArrivalOffset() - fromStop.getDepartureOffset());
		double beelineDistance = CoordUtils.calcEuclideanDistance(fromStop.getStopFacility().getCoord(), toStop.getStopFacility().getCoord());

		if(travelCostType.equals(PublicTransitMappingConfigGroup.TravelCostType.travelTime)) {
			return travelTime;
		} else {
			return beelineDistance;
		}
	}

	@Override
	public double getArtificialLinkFreeSpeed(double maxAllowedTravelCost, LinkCandidate fromLinkCandidate, LinkCandidate toLinkCandidate) {
		return 1;
		/* Varying freespeeds do not work with maxAllowedTravelcost == 0.
		if(travelCostType.equals(PublicTransitMappingConfigGroup.TravelCostType.travelTime)) {
			double linkLength = CoordUtils.calcEuclideanDistance(fromLinkCandidate.getToNodeCoord(), toLinkCandidate.getFromNodeCoord());
			return linkLength / maxAllowedTravelCost;
		} else {
			return 1;
		}
		*/
	}

	@Override
	public double getArtificialLinkLength(double maxAllowedTravelCost, LinkCandidate fromLinkCandidate, LinkCandidate toLinkCandidate) {
		if(travelCostType.equals(PublicTransitMappingConfigGroup.TravelCostType.travelTime)) {
			return CoordUtils.calcEuclideanDistance(fromLinkCandidate.getToNodeCoord(), toLinkCandidate.getFromNodeCoord());
		} else {
			return maxAllowedTravelCost;
		}
	}

	@Override
	public LeastCostPathCalculator.Path calcLeastCostPath(Node fromNode, Node toNode) {
		return pathCalculator.calcLeastCostPath(fromNode, toNode, 0.0, null, null);
	}

	@Override
	public double getLinkTravelCost(Link link) {
		return getLinkMinimumTravelDisutility(link);
	}


	// LeastCostPathCalculator methods
	@Override
	public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
		return this.getLinkMinimumTravelDisutility(link);
	}

	@Override
	public double getLinkMinimumTravelDisutility(Link link) {
		return (travelCostType.equals(PublicTransitMappingConfigGroup.TravelCostType.travelTime) ? link.getLength() / link.getFreespeed() : link.getLength());
		/* u-turn punishment, experimental
		double travelCost = (travelCostType.equals(PublicTransitMappingConfigGroup.TravelCostType.travelTime) ? link.getLength() / link.getFreespeed() : link.getLength());
		if(link.getToNode().getId().equals(uTurnFromNodeId) && link.getFromNode().getId().equals(uTurnToNodeId)) {
			return 30 + travelCost;
		} else {
			return travelCost;
		}
		*/
	}

	@Override
	public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
		return link.getLength() / link.getFreespeed();
	}
}