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
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.FastAStarLandmarksFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.vehicles.Vehicle;
import playground.polettif.multiModalMap.tools.NetworkTools;

import java.util.HashMap;
import java.util.Map;

/**
 * Based on the line, mode, and link type, the traveling on links is assigned different costs.
 * The better the match, the lower the costs.
 *
 * @author boescpa
 */
public class DijkstraRouter implements Router {

	private final Network network;
    private final LeastCostPathCalculator pathCalculator;
    private final Map<Tuple<Node, Node>, LeastCostPathCalculator.Path> paths = new HashMap<>();

    public DijkstraRouter(Network network) {
        LeastCostPathCalculatorFactory factory = new DijkstraFactory();
		this.network = network;
        this.pathCalculator = factory.createPathCalculator(network, this, this);

		// Suppress "no route found" statements...
		Logger.getLogger( Dijkstra.class ).setLevel( Level.ERROR );
    }

    @Override
    public LeastCostPathCalculator.Path calcLeastCostPath(Node fromNode, Node toNode, String mode) {
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


	public LeastCostPathCalculator.Path calcLeastCostPath(Node fromNode, Node toNode) {
		return calcLeastCostPath(fromNode, toNode, "");
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
        return link.getLength()/link.getFreespeed();
    }

	/**
	 * @param link The link for which the travel time is calculated.
	 * @param time The departure time (in seconds since 00:00) at the beginning
	 * 		of the link for which the travel time is calculated.
	 * @param person TODO
	 * @param vehicle TODO
	 * @return
	 */
	@Override
    public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
        return link.getLength() / link.getFreespeed(time);
    }

}
