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

package playground.polettif.publicTransitMapping.mapping.router;

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
import playground.polettif.publicTransitMapping.mapping.pseudoRouter.LinkCandidate;
import playground.polettif.publicTransitMapping.mapping.v2.ArtificialLink;
import playground.polettif.publicTransitMapping.mapping.v2.ArtificialLinkImpl;
import playground.polettif.publicTransitMapping.tools.NetworkTools;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * A LeastCostPathCalculator using FasAStarLandmarks.
 */
public class FastAStarRouter implements Router {

	private final Network network;

	private final LeastCostPathCalculator pathCalculator;
	private final Map<Tuple<Node, Node>, LeastCostPathCalculator.Path> paths;
	private static PublicTransitMappingConfigGroup.TravelCostType travelCostType = PublicTransitMappingConfigGroup.TravelCostType.linkLength;

	public static void setTravelCostType(PublicTransitMappingConfigGroup.TravelCostType type) {
		travelCostType = type;
	}

	public FastAStarRouter(Network network) {
		this.paths = new HashMap<>();
		this.network = network;

		LeastCostPathCalculatorFactory factory = new FastAStarLandmarksFactory(network, this);
		this.pathCalculator = factory.createPathCalculator(network, this, this);
	}

	public static Router createModeSeparatedRouter(Network network, Set<String> transportModes) {
		Network filteredNetwork = NetworkTools.filterNetworkByLinkMode(network, transportModes);
		return new FastAStarRouter(filteredNetwork);
	}

	/**
	 * Synchronized since {@link org.matsim.core.router.Dijkstra} is not thread safe.
	 */
	@Override
	public synchronized LeastCostPathCalculator.Path calcLeastCostPath(Node fromNode, Node toNode) {
		if(fromNode != null && toNode != null) {
			Tuple<Node, Node> nodes = new Tuple<>(fromNode, toNode);
			if(!paths.containsKey(nodes)) {
				paths.put(nodes, pathCalculator.calcLeastCostPath(fromNode, toNode, 0.0, null, null));
			}
			return paths.get(nodes);
		} else {
			return null;
		}
	}

	@Override
	public Network getNetwork() {
		return network;
	}

	@Override
	public double getMinimalTravelCost(TransitRouteStop fromStop, TransitRouteStop toStop) {
		double travelTime = (toStop.getArrivalOffset() - fromStop.getDepartureOffset());
		double beelineDistance = CoordUtils.calcEuclideanDistance(fromStop.getStopFacility().getCoord(), fromStop.getStopFacility().getCoord());

		if(travelCostType.equals(PublicTransitMappingConfigGroup.TravelCostType.travelTime)) {
			return travelTime;
		} else {
			return beelineDistance;
		}
	}

	@Override
	public ArtificialLink createArtificialLink(LinkCandidate fromLinkCandidate, LinkCandidate toLinkCandidate) {
		double linkLength = CoordUtils.calcEuclideanDistance(fromLinkCandidate.getToNodeCoord(), toLinkCandidate.getFromNodeCoord());
		return new ArtificialLinkImpl(fromLinkCandidate, toLinkCandidate, 0.5, linkLength);
		/*
		double linkTravelCost = travelCost - 0.5*fromLinkCandidate.getLinkTravelCost() - 0.5*toLinkCandidate.getLinkTravelCost();
		if(travelCostType.equals(PublicTransitMappingConfigGroup.TravelCostType.travelTime)) {
			double linkLength = CoordUtils.calcEuclideanDistance(fromLinkCandidate.getToNodeCoord(), toLinkCandidate.getFromNodeCoord());
			return new ArtificialLinkImpl(fromLinkCandidate, toLinkCandidate, linkLength / linkTravelCost, linkLength);
		} else {
			return new ArtificialLinkImpl(fromLinkCandidate, toLinkCandidate, 0.5, travelCost);
		}
		*/
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
	}

	@Override
	public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
		return link.getLength() / link.getFreespeed();
	}
}