/* *********************************************************************** *
 * project: org.matsim.*
 * CreateMultiModalNetwork.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.christoph.matsim2030;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.scenario.ScenarioUtils;

public class CreateMultiModalNetwork {

	public static void main(String[] args) {
		
		Scenario streetScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Scenario transitScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		
		new MatsimNetworkReader(streetScenario).readFile("/data/matsim/cdobler/2030/network.xml.gz");
		new MatsimNetworkReader(transitScenario).readFile("/data/matsim/cdobler/2030/network.edited.xml.gz");
		
		Network streetNetwork = streetScenario.getNetwork();
		Network transitNetwork = transitScenario.getNetwork();

		merge(streetNetwork, "", transitNetwork);
		
		new NetworkWriter(streetNetwork).write("/data/matsim/cdobler/2030/network_multimodal.xml.gz");

	}
	
	/**
	 * Merges two networks into one, by copying all nodes and links from the addNetworks to the baseNetwork.
	 *
	 * @param baseNetwork
	 * @param addNetwork
	 */
	public static void merge(final Network baseNetwork, final String addPrefix, final Network addNetwork) {
		double capacityFactor = baseNetwork.getCapacityPeriod() / addNetwork.getCapacityPeriod();
		NetworkFactory factory = baseNetwork.getFactory();
		for (Node node : addNetwork.getNodes().values()) {
			NodeImpl node2 = (NodeImpl) factory.createNode(Id.create(addPrefix + node.getId().toString(), Node.class), node.getCoord());
			baseNetwork.addNode(node2);
		}
		for (Link link : addNetwork.getLinks().values()) {
			Id<Node> fromNodeId = Id.create(addPrefix + link.getFromNode().getId().toString(), Node.class);
			Id<Node> toNodeId = Id.create(addPrefix + link.getToNode().getId().toString(), Node.class);
			Link link2 = factory.createLink(Id.create(addPrefix + link.getId().toString(), Link.class),
					fromNodeId, toNodeId);
			link2.setAllowedModes(link.getAllowedModes());
			link2.setCapacity(link.getCapacity() * capacityFactor);
			link2.setFreespeed(link.getFreespeed());
			link2.setLength(link.getLength());
			link2.setNumberOfLanes(link.getNumberOfLanes());
			baseNetwork.addLink(link2);
		}
	}
}
