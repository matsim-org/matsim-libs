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

import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.utils.identifiers.IdI;

/**
 * Aggregates a PtNetworkLayer from HaltePunkte to HalteBereiche. In the
 * resulting network, only links from HalteBereich to HalteBereich exist, but
 * none from HaltePunkt to HaltePunkt.
 * 
 * @author mrieser
 */
public class PtNetworkAggregator {

	private NetworkLayer network = null;
	private TreeMap<IdI, IdI> linkMatching = null; // TreeMap<oldLinkId, newLinkId>

	public PtNetworkAggregator(NetworkLayer ptNetwork) {
		aggregate(ptNetwork);
	}

	public NetworkLayer getNetwork() {
		return this.network;
	}

	public IdI lookupNewLinkId(IdI oldLinkId) {
		return linkMatching.get(oldLinkId);
	}

	@SuppressWarnings("unchecked")
	private void aggregate(NetworkLayer ptNetwork) {
		this.network = new NetworkLayer();
		this.linkMatching = new TreeMap<IdI, IdI>();

		// first, add all Haltebereiche-Nodes
		for (Iterator<Node> iter = ptNetwork.getNodes().iterator(); iter.hasNext(); ) {
			Node ptNode = iter.next();
			if (PtNetworkLayer.PEDESTRIAN_TYPE.equals(ptNode.getType())) {
				this.network.createNode(ptNode.getId().toString(),
						Integer.toString((int) ptNode.getCoord().getX()), Integer.toString((int) ptNode.getCoord().getY()),
						ptNode.getType());
			}
		}

		// now add links between the Haltebereichen
		for (Iterator<Link> iter = ptNetwork.getLinks().iterator(); iter.hasNext(); ) {
			Link ptLink = iter.next();
			if (PtNetworkLayer.PEDESTRIAN_TYPE.equals(ptLink.getFromNode().getType())
					|| PtNetworkLayer.PEDESTRIAN_TYPE.equals(ptLink.getToNode().getType())) {
				continue;
			}
			Node fromHb = findHaltebereich(ptLink.getFromNode());
			Node toHb = findHaltebereich(ptLink.getToNode());
			Link link = findConnectingLink(fromHb, toHb);
			if (link == null) {
				link = this.network.createLink(ptLink.getId().toString(),
						fromHb.getId().toString(), toHb.getId().toString(),
						Double.toString(fromHb.getCoord().calcDistance(toHb.getCoord())),
						"2", "1000", "1", null, null);
			}
			linkMatching.put(ptLink.getId(), link.getId());
		}
	}

	private Node findHaltebereich(Node node) {
		for (Iterator iter = node.getOutNodes().iterator(); iter.hasNext(); ) {
			Node otherNode = (Node) iter.next();
			if (PtNetworkLayer.PEDESTRIAN_TYPE.equals(otherNode.getType())) {
				return otherNode;
			}
		}
		return null;
	}

	private Link findConnectingLink(Node fromNode, Node toNode) {
		for (Iterator iter = fromNode.getOutLinks().iterator(); iter.hasNext(); ) {
			Link link = (Link)iter.next();
			if (link.getToNode() == toNode) {
				return link;
			}
		}
		return null;
	}

}
