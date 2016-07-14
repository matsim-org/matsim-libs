/* *********************************************************************** *
 * project: org.matsim.*
 * LinkFactoryImpl.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.sergioo.passivePlanning2012.core.network;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.network.LinkFactory;
import org.matsim.core.router.FastDijkstra;
import org.matsim.core.router.util.FastDijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.PreProcessDijkstra;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.vehicles.Vehicle;

import playground.sergioo.passivePlanning2012.core.router.util.PathV2;
import playground.sergioo.passivePlanning2012.core.scenario.ScenarioSimplerNetwork;

public class ComposedLinkFactory implements LinkFactory {

	//Constants
	private static final Logger log = Logger.getLogger(ScenarioSimplerNetwork.class);

	//Attributes
	private final Map<Id<Node>, Id<Node>> composedNodes;
	private final FastDijkstra leastCostPathCalculator;

	//Constructors
	public ComposedLinkFactory(Network baseNetwork, Network simplerNetwork, String mode) {
		TravelDisutility travelMinCost =  new TravelDisutility() {
			@Override
			public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
				return getLinkMinimumTravelDisutility(link);
			}
			@Override
			public double getLinkMinimumTravelDisutility(Link link) {
				return link.getLength()/link.getFreespeed();
			}
		};
		PreProcessDijkstra preProcessData = new PreProcessDijkstra();
		preProcessData.run(baseNetwork);
		TravelTime timeFunction = new TravelTime() {	


			public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
				return link.getLength()/link.getFreespeed();
			}
		};
		LeastCostPathCalculatorFactory routerFactory = new FastDijkstraFactory(preProcessData);
		leastCostPathCalculator = (FastDijkstra) routerFactory.createPathCalculator(baseNetwork, travelMinCost, timeFunction);
		Set<String> modes = new HashSet<String>();
		modes.add(mode);
		leastCostPathCalculator.setModeRestriction(modes);
		composedNodes = new HashMap<Id<Node>, Id<Node>>();
		for(Node mainNode:simplerNetwork.getNodes().values()) {
			for(Node node:((ComposedNode)mainNode).getNodes()) {
				if(composedNodes.get(node.getId())!=null) {
					log.error("The network node "+node.getId()+" belongs to two or more main nodes");
					throw new RuntimeException("A network node belongs to two or more main nodes");
				}
				composedNodes.put(node.getId(), mainNode.getId());
			}
		}
	}

	//Methods
	@Override
	public Link createLink(Id<Link> id, Node nodeA, Node nodeB, Network network, double length, double freespeed, double capacity, double nOfLanes) {
		if(!nodeA.getId().equals(nodeB.getId())) {
			Path path = directShortestPath((ComposedNode)nodeA, (ComposedNode)nodeB, leastCostPathCalculator);
			if(path!=null) {
				PathV2 pathV2 = new PathV2(path);
				return new ComposedLink(id, nodeA, nodeB, network, pathV2.getLength(), pathV2.getAverageFreeSpeed(), pathV2.getAverageCapacity(), pathV2.getAverageNumLanes(), path.nodes.get(0), path.nodes.get(path.nodes.size()-1));
			}
		}
		return null;
	}
	private Path directShortestPath(ComposedNode nodeA, ComposedNode nodeB, LeastCostPathCalculator leastCostPathCalculator) {
		Path shortestPath = null;
		double shortestLength = Double.MAX_VALUE;
		for(Node nodeInA:nodeA.getNodes())
			for(Node nodeInB:nodeB.getNodes()) {
				Path path = leastCostPathCalculator.calcLeastCostPath(nodeInA, nodeInB, 0, null, null);
				if(path != null) {
					PathV2 pathV2 = new PathV2(path);
					double length = pathV2.getLength();
					if(length<shortestLength) {
						shortestPath = path;
						shortestLength = length;
					}
				}
			}
		if(shortestPath != null)
			for(Node node:shortestPath.nodes)
				if(!nodeA.getNodes().contains(node) && !nodeB.getNodes().contains(node) && composedNodes.keySet().contains(node.getId()))
					return null;
		return shortestPath;
	}

}
