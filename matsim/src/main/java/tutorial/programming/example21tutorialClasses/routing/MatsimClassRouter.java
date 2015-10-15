/* *********************************************************************** *
 * project: org.matsim.*
 * RunEmissionToolOffline.java
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
package tutorial.programming.example21tutorialClasses.routing;

import java.util.ArrayList;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

/**
 * @author jbischoff
 *this is a very simple router developed exclusively for the one-agent example discussed in class. 
* 	
//
 */
public class MatsimClassRouter implements LeastCostPathCalculator {

	private final Network network;

	
	public MatsimClassRouter(Network network, TravelDisutility travelCosts,
			TravelTime travelTimes) {
		this.network = network;

	}

	@Override
	public Path calcLeastCostPath(Node fromNode, Node toNode, double starttime,
			Person person, Vehicle vehicle) {
		
		
		
		ArrayList<Node> nodes = new ArrayList<Node>();
		ArrayList<Link> links = new ArrayList<Link>();
		
		nodes.add(network.getNodes().get(Id.createNodeId(13)));
		//the first node is the toNode of the previous activity's link
		nodes.add(network.getNodes().get(Id.createNodeId(14)));
		nodes.add(network.getNodes().get(Id.createNodeId(25)));
		nodes.add(network.getNodes().get(Id.createNodeId(36)));
		nodes.add(network.getNodes().get(Id.createNodeId(47)));
		//the last node is the fromNode of the next activity's link
		
		
		//the first link is the first link after the first Node
		links.add(network.getLinks().get(Id.createLinkId(112)));
		links.add(network.getLinks().get(Id.createLinkId(213)));
		links.add(network.getLinks().get(Id.createLinkId(224)));
		links.add(network.getLinks().get(Id.createLinkId(235)));
		//the last link is the last link before the last node
		
		//we don't provide any traveltimes or costs here
		return new Path(nodes,links,0.0,0.0);
	}

}
