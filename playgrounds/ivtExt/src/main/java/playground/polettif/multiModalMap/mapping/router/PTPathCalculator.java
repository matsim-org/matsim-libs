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


package playground.polettif.multiModalMap.mapping.router;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.util.*;
import org.matsim.vehicles.Vehicle;
import playground.polettif.multiModalMap.mapping.PTMapperUtils;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class PTPathCalculator implements TravelTime, TravelDisutility {

	protected static Logger log = Logger.getLogger(PTPathCalculator.class);


	private final LeastCostPathCalculator pathCalculator;

	public PTPathCalculator(Network network) {
		LeastCostPathCalculatorFactory factory = new FastAStarLandmarksFactory(network, this);
		this.pathCalculator = factory.createPathCalculator(network, this, this);
		// Suppress "no route found" statements...
		Logger.getLogger( Dijkstra.class ).setLevel( Level.ERROR );
	}

	/**
	 * calculates a least cost path from fromLink to toLink via viaLink
	 * @param fromLink
	 * @param viaLink
	 * @param toLink
	 * @return
	 */
	public LeastCostPathCalculator.Path calcConstrainedPath(Link fromLink, Link viaLink, Link toLink) {
		LeastCostPathCalculator.Path pathA = calcLeastCostPath(fromLink.getToNode(), viaLink.getFromNode());
		LeastCostPathCalculator.Path pathB = calcLeastCostPath(viaLink.getToNode(), toLink.getFromNode());

		List<Link> pathLinks = new ArrayList<>();
		pathLinks.addAll(pathA.links);
		pathLinks.add(viaLink);
		pathLinks.addAll(pathB.links);

		double travelTime = pathA.travelTime + pathB.travelTime + getLinkTravelTime(viaLink);

		// check if there is a loop in the path, add punishment
		if(PTMapperUtils.linkSequenceHasLoops(pathLinks)) {
			travelTime += 3600;
		}
		if(PTMapperUtils.linkSequenceHasUTurns(pathLinks)) {
			travelTime += 3600;
		}

		return new LeastCostPathCalculator.Path(null, pathLinks, travelTime, 0.0);
	}

	/**
	 * Calculates a least cost path passing multiple via links.
	 * @param fromLink
	 * @param viaLinks
	 * @param toLink
	 * @return
	 */
	public LeastCostPathCalculator.Path calcConstrainedPathMulti(Link fromLink, List<Link> viaLinks, Link toLink) {

		List<Link> linkList = new ArrayList<>();
		linkList.add(fromLink);
		linkList.addAll(viaLinks);
		linkList.add(toLink);

		double travelTime = 0;

		List<Link> pathLinks = new ArrayList<>();

		for(int i=0; i < linkList.size()-1; i++) {
			LeastCostPathCalculator.Path tmpPath = calcLeastCostPath(linkList.get(i).getToNode(), linkList.get(i+1).getFromNode());
			pathLinks.addAll(tmpPath.links);
			travelTime += tmpPath.travelTime;
			if(i < linkList.size()-2) {
				pathLinks.add(linkList.get(i+1));
				travelTime += getLinkTravelTime(linkList.get(i+1));
			}
		}

		return new LeastCostPathCalculator.Path(null, pathLinks, travelTime, 0.0);
	}


	@Override
	public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
		return this.getLinkMinimumTravelDisutility(link);
	}

	@Override
	public double getLinkMinimumTravelDisutility(Link link) {
		return link.getLength() / link.getFreespeed();
	}

	@Override
	public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
		return getLinkTravelTime(link);
	}

	public double getLinkTravelTime(Link link) {
		return link.getLength() / link.getFreespeed();
	}

	public LeastCostPathCalculator.Path calcLeastCostPath(Node fromNode, Node toNode, String mode, String routeId) {
		return this.pathCalculator.calcLeastCostPath(fromNode, toNode, 0.0, null, null);
	}

	public LeastCostPathCalculator.Path calcLeastCostPath(Node fromNode, Node toNode) {
		return this.pathCalculator.calcLeastCostPath(fromNode, toNode, 0.0, null, null);
	}

}