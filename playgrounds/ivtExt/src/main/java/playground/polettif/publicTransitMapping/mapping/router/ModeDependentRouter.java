/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package playground.polettif.publicTransitMapping.mapping.router;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.AStarLandmarksFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.vehicles.Vehicle;
import playground.polettif.publicTransitMapping.config.PublicTransitMappingConfigGroup;
import playground.polettif.publicTransitMapping.tools.MiscUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A LeastCostPathCalculator that allowes mode dependent routing on
 * a network. The cost of using links without the allowed transport
 * modes are increased. Only uses distance as travel disutility.
 *
 * @author polettif
 */
@Deprecated
public class ModeDependentRouter implements Router {

	private final Network network;

	private final Set<String> routingTransportModes;
	private final LeastCostPathCalculator pathCalculator;

    private final Map<Tuple<Node, Node>, LeastCostPathCalculator.Path> paths;
	private static PublicTransitMappingConfigGroup.TravelCostType travelCostType = PublicTransitMappingConfigGroup.TravelCostType.linkLength;

	public ModeDependentRouter(Network network, Set<String> routingTransportModes) {
		this.network = network;
		this.routingTransportModes = new HashSet<>();
		this.routingTransportModes.add(PublicTransitMappingConfigGroup.ARTIFICIAL_LINK_MODE);
		if(routingTransportModes != null) {
			this.routingTransportModes.addAll(routingTransportModes);
		}

		paths = new HashMap<>();

		LeastCostPathCalculatorFactory factory = new AStarLandmarksFactory(network, this);
		this.pathCalculator = factory.createPathCalculator(network, this, this);
	}

    @Override
    public LeastCostPathCalculator.Path calcLeastCostPath(Node fromNode, Node toNode) {
        if (fromNode != null && toNode != null) {
            Tuple<Node, Node> nodes = new Tuple<>(fromNode, toNode);
            if (!paths.containsKey(nodes)) {
                paths.put(nodes, pathCalculator.calcLeastCostPath(fromNode, toNode, 0.0, null, null));
            }
            return paths.get(nodes);
        } else {
			throw new RuntimeException("Both nodes are null!");
        }
    }

	@Override
	public Network getNetwork() {
		return network;
	}


	/**
	 * @param link The link for which the travel disutility is calculated.
	 * @param time The departure time (in seconds since 00:00) at the beginning of the link for which the disutility is calculated.
	 * @param person The person that wants to travel along the link. Note that this parameter can be <code>null</code>!
	 * @param vehicle The vehicle with which the person wants to travel along the link. Note that this parameter can be <code>null</code>!
	 */
    @Override
    public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
        return this.getLinkMinimumTravelDisutility(link);
    }

	/**
	 * @param link the link for which the minimal travel disutility over all time slots is calculated
	 */
    @Override
    public double getLinkMinimumTravelDisutility(Link link) {
		return (linkHasRoutingMode(link) ? 1 : 100000) * link.getLength() / link.getFreespeed();
	}

	/**
	 * Link travel time is ignored for pseudoRouting!
	 */
	@Override
    public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
        return link.getLength() / link.getFreespeed(time);
    }

	public boolean linkHasRoutingMode(Link link) {
		return MiscUtils.setsShareMinOneStringEntry(link.getAllowedModes(), routingTransportModes);
	}


}
