/* *********************************************************************** *
 * project: org.matsim.*
 * PtNetworkAggregator.java
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

package playground.marcel.ptnetwork;

import java.util.Iterator;
import java.util.TreeMap;

import org.matsim.basic.v01.Id;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;

/**
 * Aggregates a PtNetworkLayer from HaltePunkte to HalteBereiche. In the
 * resulting network, only links from HalteBereich to HalteBereich exist, but
 * none from HaltePunkt to HaltePunkt.
 *
 * @author mrieser
 */
public class PtNetworkAggregator {

	private NetworkLayer network = null;
	private TreeMap<Id, Id> linkMatching = null; // TreeMap<oldLinkId, newLinkId>

	public PtNetworkAggregator(NetworkLayer ptNetwork) {
		aggregate(ptNetwork);
	}

	public NetworkLayer getNetwork() {
		return this.network;
	}

	public Id lookupNewLinkId(Id oldLinkId) {
		return this.linkMatching.get(oldLinkId);
	}

	@SuppressWarnings("unchecked")
	private void aggregate(NetworkLayer ptNetwork) {
		this.network = new NetworkLayer();
		this.linkMatching = new TreeMap<Id, Id>();

		// first, add all Haltebereiche-Nodes
		for (Iterator<? extends Node> iter = ptNetwork.getNodes().values().iterator(); iter.hasNext(); ) {
			Node ptNode = iter.next();
			if (PtNetworkLayer.PEDESTRIAN_TYPE.equals(ptNode.getType())) {
				this.network.createNode(ptNode.getId().toString(),
						Integer.toString((int) ptNode.getCoord().getX()), Integer.toString((int) ptNode.getCoord().getY()),
						ptNode.getType());
			}
		}

		// now add links between the Haltebereichen
		for (Iterator<? extends Link> iter = ptNetwork.getLinks().values().iterator(); iter.hasNext(); ) {
			Link ptLink = iter.next();
			if (PtNetworkLayer.PEDESTRIAN_TYPE.equals(ptLink.getFromNode().getType())
					|| PtNetworkLayer.PEDESTRIAN_TYPE.equals(ptLink.getToNode().getType())) {
				continue;
			}
			Node fromHb = findHaltebereich(ptLink.getFromNode());
			Node toHb = findHaltebereich(ptLink.getToNode());
			Link link = findConnectingLink(fromHb, toHb);
			if (link == null) {
				link = this.network.createLink(ptLink.getId(),
						this.network.getNode(fromHb.getId()), this.network.getNode(toHb.getId()),
						fromHb.getCoord().calcDistance(toHb.getCoord()), 2, 1000, 1);
			}
			this.linkMatching.put(ptLink.getId(), link.getId());
		}
	}

	private Node findHaltebereich(Node node) {
		for (Iterator iter = node.getOutNodes().values().iterator(); iter.hasNext(); ) {
			Node otherNode = (Node) iter.next();
			if (PtNetworkLayer.PEDESTRIAN_TYPE.equals(otherNode.getType())) {
				return otherNode;
			}
		}
		return null;
	}

	private Link findConnectingLink(Node fromNode, Node toNode) {
		for (Iterator iter = fromNode.getOutLinks().values().iterator(); iter.hasNext(); ) {
			Link link = (Link)iter.next();
			if (link.getToNode() == toNode) {
				return link;
			}
		}
		return null;
	}

}
