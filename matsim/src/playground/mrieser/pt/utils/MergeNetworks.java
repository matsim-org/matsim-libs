/* *********************************************************************** *
 * project: org.matsim.*
 * MergeNetworks.java
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

package playground.mrieser.pt.utils;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.utils.misc.Time;

/**
 * Merges two networks into one, by copying all nodes and links from the two
 * given networks to a third one.
 *
 * @author mrieser
 */
public class MergeNetworks {

	public static void merge(final Network networkA, final String prefixA, final Network networkB, final String prefixB, final NetworkImpl mergedNetwork) {
		double capacityFactor = mergedNetwork.getCapacityPeriod() / networkA.getCapacityPeriod();
		NetworkFactory builder = mergedNetwork.getFactory();
		for (Node node : networkA.getNodes().values()) {
			NodeImpl node2 = (NodeImpl) builder.createNode(new IdImpl(prefixA + node.getId().toString()));
			node2.setCoord(node.getCoord());
			mergedNetwork.getNodes().put(node2.getId(), node2);
		}
		for (Link link : networkA.getLinks().values()) {
			Id fromNodeId = new IdImpl(prefixA + link.getFromNode().getId().toString());
			Id toNodeId = new IdImpl(prefixA + link.getToNode().getId().toString());
			Link link2 = builder.createLink(new IdImpl(prefixA + link.getId().toString()),
					fromNodeId, toNodeId);
			link2.setAllowedModes(link.getAllowedModes());
			link2.setCapacity(link.getCapacity(Time.UNDEFINED_TIME) * capacityFactor);
			link2.setFreespeed(link.getFreespeed(Time.UNDEFINED_TIME));
			link2.setLength(link.getLength());
			link2.setNumberOfLanes(link.getNumberOfLanes(Time.UNDEFINED_TIME));
			mergedNetwork.getLinks().put(link2.getId(), (LinkImpl) link2);
			mergedNetwork.getNodes().get(fromNodeId).addOutLink(link2);
			mergedNetwork.getNodes().get(toNodeId).addInLink(link2);
		}
		capacityFactor = mergedNetwork.getCapacityPeriod() / networkB.getCapacityPeriod();
		for (Node node : networkB.getNodes().values()) {
			NodeImpl node2 = (NodeImpl) builder.createNode(new IdImpl(prefixB + node.getId().toString()));
			node2.setCoord(node.getCoord());
			mergedNetwork.getNodes().put(node2.getId(), node2);
		}
		for (Link link : networkB.getLinks().values()) {
			Id fromNodeId = new IdImpl(prefixB + link.getFromNode().getId().toString());
			Id toNodeId = new IdImpl(prefixB + link.getToNode().getId().toString());
			Link link2 = builder.createLink(new IdImpl(prefixB + link.getId().toString()),
					fromNodeId, toNodeId);
			link2.setAllowedModes(link.getAllowedModes());
			link2.setCapacity(link.getCapacity(Time.UNDEFINED_TIME) * capacityFactor);
			link2.setFreespeed(link.getFreespeed(Time.UNDEFINED_TIME));
			link2.setLength(link.getLength());
			link2.setNumberOfLanes(link.getNumberOfLanes(Time.UNDEFINED_TIME));
			mergedNetwork.getLinks().put(link2.getId(), (LinkImpl) link2);
			mergedNetwork.getNodes().get(fromNodeId).addOutLink(link2);
			mergedNetwork.getNodes().get(toNodeId).addInLink(link2);
		}
	}
}
