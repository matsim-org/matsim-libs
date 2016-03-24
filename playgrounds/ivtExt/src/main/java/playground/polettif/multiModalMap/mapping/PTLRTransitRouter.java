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


package playground.polettif.multiModalMap.mapping;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
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
import playground.polettif.boescpa.converters.osm.ptMapping.PTLRouter;

import java.util.Map;


public class PTLRTransitRouter implements PTLRouter {

	Logger log = Logger.getLogger(PTLRTransitRouter.class);

	private final Dijkstra pathCalculator;
	private final Map<Id<Link>, Double> linkWeights;

	private static final double MAX_DISUTILITY = 2000; // TODO get a sensible value for MAX_DISUTILITY

	public PTLRTransitRouter(Network network, Map<Id<Link>, Double> linkWeights) {
		LeastCostPathCalculatorFactory factory = new DijkstraFactory();
		this.pathCalculator = (Dijkstra) factory.createPathCalculator(network, this, this);

		// Suppress "no route found" statements...
//		Logger.getLogger( org.matsim.core.router.Dijkstra.class ).setLevel( Level.ERROR );

		this.linkWeights = linkWeights;
	}

	@Override
	public LeastCostPathCalculator.Path calcLeastCostPath(Node fromNode, Node toNode, String mode, String routeId) {
		return pathCalculator.calcLeastCostPath(fromNode, toNode, 0.0, null, null);
	}


	@Override
	public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
		return this.getLinkMinimumTravelDisutility(link);
	}

	@Override
	public double getLinkMinimumTravelDisutility(Link link) {
		if(linkWeights.containsKey(link.getId()))
			return MAX_DISUTILITY - linkWeights.get(link.getId());
		else
			return MAX_DISUTILITY*2;
	}

	@Override
	public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
		return 0; //link.getLength()/link.getFreespeed();
	}


}
