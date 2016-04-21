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

package playground.polettif.multiModalMap.mapping.router;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.util.*;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.vehicles.Vehicle;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Based on the line, mode, and link type, the traveling on links is assigned different costs.
 * The better the match, the lower the costs.
 *
 * @author boescpa
 */
public class ModeDependentRouter implements Router {

	private final Set<String> routingTransportModes;
    private final LeastCostPathCalculator pathCalculator;
    private final Map<Tuple<Node, Node>, LeastCostPathCalculator.Path> paths;

	public ModeDependentRouter(Network network, Set<String> routingTransportModes) {
		this.routingTransportModes = routingTransportModes;
		paths = new HashMap<>();

		LeastCostPathCalculatorFactory factory = new FastAStarLandmarksFactory(network, this);
		this.pathCalculator = factory.createPathCalculator(network, this, this);

		// Suppress statements...
		Logger.getLogger( Dijkstra.class ).setLevel( Level.ERROR );
		Logger.getLogger( PreProcessEuclidean.class ).setLevel( Level.ERROR );
		Logger.getLogger( PreProcessLandmarks.class ).setLevel( Level.ERROR );
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
            return null;
        }
    }


	/**
	 * @param link The link for which the travel disutility is calculated.
	 * @param time The departure time (in seconds since 00:00) at the beginning of the link for which the disutility is calculated.
	 * @param person The person that wants to travel along the link. Note that this parameter can be <code>null</code>!
	 * @param vehicle The vehicle with which the person wants to travel along the link. Note that this parameter can be <code>null</code>!
	 * @return
	 */
    @Override
    public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
        return this.getLinkMinimumTravelDisutility(link);
    }

	/**
	 * @param link the link for which the minimal travel disutility over all time slots is calculated
	 * @return
	 */
    @Override
    public double getLinkMinimumTravelDisutility(Link link) {
		return (linkHasRoutingMode(link) ? 1 : 1000) * link.getLength();
	}

	/**
	 * @param link The link for which the travel time is calculated.
	 * @param time The departure time (in seconds since 00:00) at the beginning
	 * 		of the link for which the travel time is calculated.
	 */
	@Override
    public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
        return link.getLength() / link.getFreespeed(time);
    }

	public boolean linkHasRoutingMode(Link link) {
		if(routingTransportModes == null) {
			return true;
		}

		for(String routingTransportMode : routingTransportModes) {
			for(String linkTransportMode : link.getAllowedModes()) {
				if(routingTransportMode.equalsIgnoreCase(linkTransportMode)) {
					return true;
				}
			}
		}
		return false;
	}
}
