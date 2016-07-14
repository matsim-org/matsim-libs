/* *********************************************************************** *
 * project: org.matsim.*
 * Dijkstra.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.sergioo.bestTravelTimeRouter2012;
//package org.matsim.core.router;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.util.PreProcessDijkstra;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.vehicles.Vehicle;


/**
 * @author sergioo
 */
public class BestTravelTimePathCalculatorImpl implements IntermodalBestTravelTimePathCalculator {

	//Attributes
	/**
	 * The network on which we find routes.
	 */
	protected Network network;
	/**
	 * The travel time calculator. Provides the travel time for each link and time step.
	 */
	final TravelTime timeFunction;
	private Set<String> modeRestriction;
	private boolean uTurn = true;
	private PreProcessDijkstra preProcessDijkstra;
	
	//Constructors
	/**
	 * Default constructor.
	 *
	 * @param network
	 *            The network on which to route.
	 * @param timeFunction
	 *            Determines the travel time on links.
	 */
	public BestTravelTimePathCalculatorImpl(final Network network, final TravelTime timeFunction) {
		this.network = network;
		this.timeFunction = timeFunction;
	}
	public BestTravelTimePathCalculatorImpl(final Network network, final TravelTime timeFunction, boolean uTurn) {
		this.network = network;
		this.timeFunction = timeFunction;
		this.uTurn = uTurn;
	}
	public BestTravelTimePathCalculatorImpl(final Network network, final TravelTime timeFunction, boolean uTurn, PreProcessDijkstra preProcessDijkstra) {
		this.network = network;
		this.timeFunction = timeFunction;
		this.uTurn = uTurn;
		this.preProcessDijkstra = preProcessDijkstra;
	}
	//Methods
	@Override
	public void setModeRestriction(final Set<String> modeRestriction) {
		this.modeRestriction = modeRestriction;
	}
	/**
	 * Calculates the route with the most similar travel time
	 *
	 * @param fromNode
	 *            The Node at which the route should start.
	 * @param toNode
	 *            The Node at which the route should end.
	 * @param startTime
	 *            The time at which the route should start.
	 * @see org.matsim.core.router.util.LeastCostPathCalculator#calcLeastCostPath(org.matsim.core.network.Node,
	 *      org.matsim.core.network.Node, double)
	 */
	@Override
	public Path calcBestTravelTimePath(final Node fromNode, final Node toNode, final double travelTime, final double startTime) {
		TravelDisutility travelDisutility = new TravelDisutility() {
			@Override
			public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
				return timeFunction.getLinkTravelTime(link, time, person, vehicle);
			}			
			@Override
			public double getLinkMinimumTravelDisutility(Link link) {
				return link.getLength()/link.getFreespeed();
			}
		};
		if(preProcessDijkstra == null) {
			preProcessDijkstra = new PreProcessDijkstra();
			preProcessDijkstra.run(network);
		}
		org.matsim.core.router.util.LeastCostPathCalculator.Path path = new Dijkstra(network, travelDisutility, timeFunction, preProcessDijkstra).calcLeastCostPath(fromNode, toNode, startTime, null, null);
		Map<Link, Link> nextLinks = new HashMap<Link, Link>();
		Link[] firstLink = new Link[1];
		double bestTravelTime = calcBestTravelTime(fromNode, toNode, travelTime, startTime, new ArrayList<Link>(), 0, new ArrayList<Tuple<Double,Double>>(), travelTime, path.travelTime, nextLinks, firstLink, null);
		return constructPath(fromNode, toNode, bestTravelTime, nextLinks, firstLink[0]);
	}
	private double calcBestTravelTime(Node fromNode, Node toNode, double travelTime, double startTime, Collection<Link> visitedLinks, double timeAdvanced, List<Tuple<Double, Double>> foundDiffs, double initialTravelTime, double shortestTravelTime, Map<Link, Link> nextLinks, Link[] bestLink, Node lastNode) {
		if(fromNode==toNode)
			return 0;
		else if(timeAdvanced-initialTravelTime>Math.abs(initialTravelTime-shortestTravelTime))
			return Double.MAX_VALUE;
		else {
			for(Tuple<Double, Double> foundDiff:foundDiffs)
				if(timeAdvanced-foundDiff.getFirst()>foundDiff.getSecond())
					return Double.MAX_VALUE;
			double minDiff = Double.MAX_VALUE;
			boolean minDiffFound = false;
			Map<Link, Link> bestNextLinksI = null;
			int l=0;
			for(Link link:fromNode.getOutLinks().values())
				if(modeAllowed(link) && !visitedLinks.contains(link) && (uTurn || !(lastNode==link.getToNode()))) {
					double linkTime = timeFunction.getLinkTravelTime(link, startTime, null, null);
					visitedLinks.add(link);
					Map<Link, Link> nextLinksI = new HashMap<Link, Link>();
					Link[] nextLink = new Link[1];
					double bestOutTime = calcBestTravelTime(link.getToNode(), toNode, travelTime-linkTime, startTime+linkTime, visitedLinks, timeAdvanced+linkTime, foundDiffs, initialTravelTime, shortestTravelTime, nextLinksI, nextLink, link.getFromNode());
					visitedLinks.remove(link);
					double diff =travelTime-linkTime-bestOutTime;
					if(Math.abs(diff)<Math.abs(minDiff)) {
						if(!minDiffFound)
							minDiffFound = true;
						else
							foundDiffs.remove(foundDiffs.size()-1);
						l++;
						foundDiffs.add(new Tuple<Double, Double>(timeAdvanced, travelTime+Math.abs(diff)));
						minDiff = diff;
						bestLink[0] = link;
						nextLinksI.put(bestLink[0], nextLink[0]);
						bestNextLinksI=nextLinksI;
					}
				}
			if(minDiffFound) {
				foundDiffs.remove(foundDiffs.size()-1);
				for(Entry<Link, Link> nextLinkI:bestNextLinksI.entrySet())
					nextLinks.put(nextLinkI.getKey(), nextLinkI.getValue());
			}
			return travelTime-minDiff;
		}
	}
	private boolean modeAllowed(Link link) {
		if(modeRestriction == null)
			return true;
		for(String mode:link.getAllowedModes())
			if(modeRestriction.contains(mode))
				return true;
		return false;
	}
	/**
	 * Constructs the path after the algorithm has been run.
	 *
	 * @param fromNode
	 *            The node where the path starts.
	 * @param toNode
	 *            The node where the path ends.
	 * @param startTime
	 *            The time when the trip starts.
	 * @param preProcessData
	 *            The time when the trip ends.
	 */
	protected Path constructPath(Node fromNode, Node toNode, double travelTime, Map<Link, Link> nextLinks, Link firstLink) {
		ArrayList<Node> nodes = new ArrayList<Node>();
		ArrayList<Link> links = new ArrayList<Link>();
		for(links.add(firstLink); links.get(links.size()-1).getToNode()!=toNode;links.add(nextLinks.get(links.get(links.size()-1))))
			nodes.add(links.get(links.size()-1).getFromNode());
		nodes.add(links.get(links.size()-1).getToNode());
		Path path = new Path(nodes, links, travelTime);
		return path;
	}

}