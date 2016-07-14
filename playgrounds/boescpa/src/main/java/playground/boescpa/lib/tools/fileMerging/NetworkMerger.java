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

package playground.boescpa.lib.tools.fileMerging;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * Merges two networks to a new one.
 *
 * @author boescpa
 */
public class NetworkMerger {
	private static Logger log = Logger.getLogger(NetworkMerger.class);

	public static Network mergeNetworks(final Network networkA, final Network networkB) {
		final Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		scenario.getConfig().transit().setUseTransit(true);
		final Network mergedNetwork = scenario.getNetwork();
		final NetworkFactory factory = mergedNetwork.getFactory();

		log.info("Merging networks...");
		// Nodes
		for (Node node : networkA.getNodes().values()) {
			Node newNode = factory.createNode(Id.create(node.getId().toString(), Node.class), node.getCoord());
			mergedNetwork.addNode(newNode);
		}
		for (Node node : networkB.getNodes().values()) {
			Node newNode = factory.createNode(Id.create(node.getId().toString(), Node.class), node.getCoord());
			mergedNetwork.addNode(newNode);
		}

		// Links
		double capacityFactor = mergedNetwork.getCapacityPeriod() / networkA.getCapacityPeriod();
		for (Link link : networkA.getLinks().values()) {
			Id<Node> fromNodeId = Id.create(link.getFromNode().getId().toString(), Node.class);
			Id<Node> toNodeId = Id.create(link.getToNode().getId().toString(), Node.class);
			Link newLink = factory.createLink(Id.create(link.getId().toString(), Link.class),
					mergedNetwork.getNodes().get(fromNodeId), mergedNetwork.getNodes().get(toNodeId));
			newLink.setAllowedModes(link.getAllowedModes());
			newLink.setCapacity(link.getCapacity() * capacityFactor);
			newLink.setFreespeed(link.getFreespeed());
			newLink.setLength(link.getLength());
			newLink.setNumberOfLanes(link.getNumberOfLanes());
			mergedNetwork.addLink(newLink);
		}
		capacityFactor = mergedNetwork.getCapacityPeriod() / networkB.getCapacityPeriod();
		for (Link link : networkB.getLinks().values()) {
			Id<Node> fromNodeId = Id.create(link.getFromNode().getId().toString(), Node.class);
			Id<Node> toNodeId = Id.create(link.getToNode().getId().toString(), Node.class);
			Link newLink = factory.createLink(Id.create(link.getId().toString(), Link.class),
					mergedNetwork.getNodes().get(fromNodeId), mergedNetwork.getNodes().get(toNodeId));
			newLink.setAllowedModes(link.getAllowedModes());
			newLink.setCapacity(link.getCapacity() * capacityFactor);
			newLink.setFreespeed(link.getFreespeed());
			newLink.setLength(link.getLength());
			newLink.setNumberOfLanes(link.getNumberOfLanes());
			mergedNetwork.addLink(newLink);
		}

		log.info(" Merging Stats:");
		log.info("  Number of links network A: " + networkA.getLinks().size());
		log.info("  Number of nodes network A: " + networkA.getNodes().size());
		log.info("  Number of links network B: " + networkB.getLinks().size());
		log.info("  Number of nodes network B: " + networkB.getNodes().size());
		log.info("  Sum of links: " + (networkA.getLinks().size() + networkB.getLinks().size()));
		log.info("  Sum of nodes: " + (networkA.getNodes().size() + networkB.getNodes().size()));
		log.info("  Number of links merged: " + mergedNetwork.getLinks().size());
		log.info("  Number of nodes merged: " + mergedNetwork.getNodes().size());

		log.info("Merging networks... done.");
		return mergedNetwork;
	}

	/**
	 * Integrates network B into network A so that after the method network A contains all links and nodes of both networks.
	 *
	 * @param networkA
	 * @param networkB
	 */
	public static void integrateNetwork(final Network networkA, final Network networkB, final String prefixNodesNetworkB, final String prefixLinksNetworkB) {
		final NetworkFactory factory = networkA.getFactory();

		log.info("Merging networks...");
		final int numberOfLinksNetworkA = networkA.getLinks().size();
		final int numberOfNodesNetworkA = networkA.getNodes().size();

		// Nodes
		for (Node node : networkB.getNodes().values()) {
			Id<Node> nodeId = Id.create(prefixNodesNetworkB + node.getId().toString(), Node.class);
			if (!networkA.getNodes().containsKey(nodeId)) {
				Node newNode = factory.createNode(nodeId, node.getCoord());
				networkA.addNode(newNode);
			}
		}

		// Links
		double capacityFactor = networkA.getCapacityPeriod() / networkB.getCapacityPeriod();
		for (Link link : networkB.getLinks().values()) {
			Id<Link> linkId = Id.create(prefixLinksNetworkB + link.getId().toString(), Link.class);
			if (!networkA.getLinks().containsKey(linkId)) {
				Id<Node> fromNodeId = Id.create(prefixNodesNetworkB + link.getFromNode().getId().toString(), Node.class);
				Id<Node> toNodeId = Id.create(prefixNodesNetworkB + link.getToNode().getId().toString(), Node.class);
				Link newLink = factory.createLink(linkId, networkA.getNodes().get(fromNodeId), networkA.getNodes().get(toNodeId));
				newLink.setAllowedModes(link.getAllowedModes());
				newLink.setCapacity(link.getCapacity() * capacityFactor);
				newLink.setFreespeed(link.getFreespeed());
				newLink.setLength(link.getLength());
				newLink.setNumberOfLanes(link.getNumberOfLanes());
				networkA.addLink(newLink);
			}
		}

		log.info(" Merging Stats:");
		log.info("  Number of links network A: " + numberOfLinksNetworkA);
		log.info("  Number of nodes network A: " + numberOfNodesNetworkA);
		log.info("  Number of links network B: " + networkB.getLinks().size());
		log.info("  Number of nodes network B: " + networkB.getNodes().size());
		log.info("  Sum of links: " + (numberOfLinksNetworkA + networkB.getLinks().size()));
		log.info("  Sum of nodes: " + (numberOfNodesNetworkA + networkB.getNodes().size()));
		log.info("  Number of links merged: " + networkA.getLinks().size());
		log.info("  Number of nodes merged: " + networkA.getNodes().size());

		log.info("Merging networks... done.");
	}
}
