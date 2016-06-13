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
import org.matsim.vehicles.Vehicle;
import playground.polettif.publicTransitMapping.config.PublicTransitMappingConfigGroup;

import java.util.HashMap;
import java.util.Map;

/**
 * A LeastCostPathCalculator using FasAStarLandmarks.
 */
public class FastAStarRouter implements Router {
	
	private final LeastCostPathCalculator pathCalculator;
	private final Map<Tuple<Node, Node>, LeastCostPathCalculator.Path> paths;
	private static PublicTransitMappingConfigGroup.PseudoRouteWeightType pseudoRouteWeightType = PublicTransitMappingConfigGroup.PseudoRouteWeightType.linkLength;

	public static void setPseudoRouteWeightType(PublicTransitMappingConfigGroup.PseudoRouteWeightType type) {
		pseudoRouteWeightType = type;
	}

	public  FastAStarRouter(Network network) {
		this.paths = new HashMap<>();

		LeastCostPathCalculatorFactory factory = new FastAStarLandmarksFactory(network, this);
		this.pathCalculator = factory.createPathCalculator(network, this, this);
	}

	@Override
	public synchronized LeastCostPathCalculator.Path calcLeastCostPath(Node fromNode, Node toNode) {
		if (fromNode != null && toNode != null) {
			Tuple<Node, Node> nodes = new Tuple<>(fromNode, toNode);
			if (!paths.containsKey(nodes)) {
				paths.put(nodes, pathCalculator.calcLeastCostPath(fromNode, toNode, 0.0, null, null));
			}
			return paths.get(nodes);
		} else {
			return null;
		}
	}

	@Override
	public LeastCostPathCalculator.Path calcLeastCostPath(Link fromLink, Link toLink) {
		return calcLeastCostPath(fromLink.getToNode(), toLink.getFromNode());
	}

	@Override
	public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
		return this.getLinkMinimumTravelDisutility(link);
	}

	@Override
	public double getLinkMinimumTravelDisutility(Link link) {
		return (pseudoRouteWeightType.equals(PublicTransitMappingConfigGroup.PseudoRouteWeightType.travelTime) ? link.getLength() / link.getFreespeed() : link.getLength());
	}

	@Override
	public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
		return link.getLength()/link.getFreespeed();
	}
}