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

package playground.boescpa.lib.tools.spatialCutting;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkFactoryImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkReaderMatsimV1;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import playground.boescpa.lib.tools.NetworkUtils;

import java.util.HashSet;
import java.util.Set;

/**
 * Cuts a network to a predefined area.
 *
 * @author boescpa
 */
public class NetworkCutter {
	private final static Logger log = Logger.getLogger(NetworkCutter.class);
	private final Network network;

	public NetworkCutter(Network network) {
		this.network = network;
	}

	public static void main(String[] args) {
		// args 0: Path to config.
		final String pathToNetwork = args[0];
		// args 1: X-coord center (double)
		final double xCoord = Double.parseDouble(args[1]);
		// args 2: Y-coord center (double)
		final double yCoord = Double.parseDouble(args[2]);
		// args 3: Radius (int)
		final int radius = Integer.parseInt(args[3]);
		// args 4: Path to network-output
		final String pathToOutputNetwork = args[4];

		// For 30km around Zurich Center (Bellevue): X - (2)683518.0, Y - (1)246836.0, radius - 30000

		Network network = NetworkUtils.readNetwork(pathToNetwork);
		NetworkCutter cutter = new NetworkCutter(network);
		Coord center = new Coord(xCoord, yCoord);
		cutter.cutNetworkToArea(pathToOutputNetwork, center, radius);
	}

	public Set<Id<Link>> cutNetworkToArea(String pathToOutputNetwork, Coord center, int radius) {
		Network filteredNetwork = org.matsim.core.network.NetworkUtils.createNetwork();
		NetworkFactoryImpl factory = new NetworkFactoryImpl(filteredNetwork);

		log.info(" Area of interest (AOI): center=" + center + "; radius=" + radius);
		// Identify all nodes within area of interest:
		final Set<Id<Node>> nodesInAreaOfInterest = new HashSet<>();
		int nodesOnBorder = 0;
		for (Node node : network.getNodes().values()) {
			double distance = CoordUtils.calcEuclideanDistance(node.getCoord(), center);
			if (distance <= radius) {
				nodesInAreaOfInterest.add(node.getId());
				Node newNode = factory.createNode(node.getId(), node.getCoord());
				filteredNetwork.addNode(newNode);
			}
			if (distance == radius) {
				nodesOnBorder++;
			}
		}
		log.info(" AOI contains: " + nodesInAreaOfInterest.size() + " nodes.");
		log.info(" AOI contains: " + nodesOnBorder + " nodes on border.");

		log.info(" Filter network...");
		for (Link link : network.getLinks().values()) {
			// case link in area or crossing border:
			if (nodesInAreaOfInterest.contains(link.getToNode().getId())
					|| nodesInAreaOfInterest.contains(link.getFromNode().getId())) {
				if (!filteredNetwork.getNodes().containsKey(link.getToNode().getId())) {
					Node newNode = factory.createNode(link.getToNode().getId(), link.getToNode().getCoord());
					filteredNetwork.addNode(newNode);
				}
				if (!filteredNetwork.getNodes().containsKey(link.getFromNode().getId())) {
					Node newNode = factory.createNode(link.getFromNode().getId(), link.getFromNode().getCoord());
					filteredNetwork.addNode(newNode);
				}
				Link newLink = factory.createLink(link.getId(),
						filteredNetwork.getNodes().get(link.getFromNode().getId()),
						filteredNetwork.getNodes().get(link.getToNode().getId()),
						(NetworkImpl) filteredNetwork,
						link.getLength(),
						link.getFreespeed(),
						link.getCapacity(),
						link.getNumberOfLanes());
				newLink.setAllowedModes(link.getAllowedModes());
				filteredNetwork.addLink(newLink);
			}
		}
		if (pathToOutputNetwork != null) {
			new NetworkWriter(filteredNetwork).write(pathToOutputNetwork);
			// test network
			new NetworkReaderMatsimV1(ScenarioUtils.createScenario(ConfigUtils.createConfig()).getNetwork()).parse(pathToOutputNetwork);
		}
		log.info(" Filter network... done.");
		log.info(" Filtered " + filteredNetwork.getNodes().size() + " nodes of originally " + network.getNodes().size() + " nodes");
		log.info(" Filtered " + filteredNetwork.getLinks().size() + " links of originally " + network.getLinks().size() + " links");

		Set<Id<Link>> borderLinks = new HashSet<>();
		for(Link link : filteredNetwork.getLinks().values()) {
			Id<Node> fromNode = link.getFromNode().getId();
			Id<Node> toNode = link.getToNode().getId();
			if ((nodesInAreaOfInterest.contains(fromNode) && !nodesInAreaOfInterest.contains(toNode)) ||
					(!nodesInAreaOfInterest.contains(fromNode) && nodesInAreaOfInterest.contains(toNode))) {
				borderLinks.add(link.getId());
			}
		}
		log.info(" Of these " + borderLinks.size() + " are border links.");
		return borderLinks;
	}
}
