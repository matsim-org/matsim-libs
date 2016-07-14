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

package playground.polettif.boescpa.converters.osm.ptMapping;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.util.FastAStarLandmarksFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.vehicles.Vehicle;

import java.util.HashMap;
import java.util.Map;

/**
 * Based on the line, mode, and link type, the traveling on links is assigned different costs.
 * The better the match, the lower the costs.
 *
 * @author boescpa
 */
public class PTLRFastAStarLandmarksSimpleRouting implements PTLRouter {

    private final LeastCostPathCalculator pathCalculator;
    private final Map<Tuple<Node, Node>, LeastCostPathCalculator.Path> paths = new HashMap<>();

    public PTLRFastAStarLandmarksSimpleRouting(Network network) {
        LeastCostPathCalculatorFactory factory = new FastAStarLandmarksFactory(network, this);
        this.pathCalculator = factory.createPathCalculator(network, this, this);
		// Suppress "no route found" statements...
		Logger.getLogger( Dijkstra.class ).setLevel( Level.ERROR );
    }

    @Override
    public LeastCostPathCalculator.Path calcLeastCostPath(Node fromNode, Node toNode, String mode, String routeId) {
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
    public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
        return this.getLinkMinimumTravelDisutility(link);
    }

    @Override
    public double getLinkMinimumTravelDisutility(Link link) {
        return link.getLength()/link.getFreespeed();
    }

    @Override
    public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
        return link.getLength() / link.getFreespeed(time);
    }
}
