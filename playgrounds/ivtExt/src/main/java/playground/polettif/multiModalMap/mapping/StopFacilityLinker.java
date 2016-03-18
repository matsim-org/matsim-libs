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


package playground.polettif.multiModalMap.mapping;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.LinkIdComparator;
import org.matsim.core.network.NetworkFactoryImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.utils.geometry.CoordUtils;

import java.util.*;

public class StopFacilityLinker {

	protected static Logger log = Logger.getLogger(StopFacilityLinker.class);

	// param
	private String prefix;

	private int linkIdIter;
	private int nodeIdIter;
	private Network network;
	private NetworkFactoryImpl networkFactory;
	private SortedMap<Link, Link> splitLinkOperations;


	Map<Integer, Map<Integer, Node>> map = new TreeMap<>();
	private Id<Link> newLinkId;
	
	
	public static void main(final String[] args) {
		
	}

	public StopFacilityLinker(Network network, String newObjectPrefix) {
		this.network = network;
		this.networkFactory = new NetworkFactoryImpl(network);
		this.prefix = newObjectPrefix;
		this.linkIdIter = 0;
		this.nodeIdIter = 0;
		this.splitLinkOperations = new TreeMap<>(new LinkIdComparator());
	}

	/**
	 * Splits the given link into two. Places a new node at splitPointCoordinates, changes the toNode of the original
	 * link, changes its length and creates a new link (with the same attributes except length).
	 *
	 * @param link which should be split
	 * @param splitPointCoordinates point where the new node is set. It is not checked, whether the coordinates are actually
	 *                              on the link.
	 */
	public void split(Link link, Coord splitPointCoordinates) {
		Id<Link> linkId = link.getId();
		Link oppositeLink = Tools.getOppositeLink(link);

		Node nodeA = link.getFromNode();
		Node nodeB = link.getToNode();

		// get coordinates on the link
		Coord coordinatesOnLink = Tools.getClosestPointOnLine(link, splitPointCoordinates);

		// new names
		Id<Node> newNodeId = Id.createNodeId(prefix + "node_" + nodeIdIter++);
		newLinkId = Id.createLinkId(prefix + linkIdIter++);

		// add node and links, remove original link
		Node newNode = networkFactory.createNode(newNodeId, coordinatesOnLink);
		Link newLink = networkFactory.createLink(
				newLinkId,
				newNode,
				nodeB,
				(NetworkImpl) network,
				CoordUtils.calcEuclideanDistance(newNode.getCoord(), nodeB.getCoord()),
				link.getFreespeed(),
				link.getCapacity(),
				link.getNumberOfLanes());

		network.addNode(newNode);
		network.addLink(newLink);
		network.getLinks().get(linkId).setToNode(newNode);
		network.getLinks().get(linkId).setLength(CoordUtils.calcEuclideanDistance(nodeA.getCoord(), newNode.getCoord()));

		splitLinkOperations.put(newLink, link);

		// split opposite link as well
		if(oppositeLink != null) {
			Id<Link> oppositeLinkId = oppositeLink.getId();

			Id<Node> oppositeNewNodeId = Id.createNodeId(prefix+"node_"+nodeIdIter++);
			Id<Link> oppositeNewLinkId = Id.createLinkId(prefix+linkIdIter++);

			Node oppositeNewNode = networkFactory.createNode(oppositeNewNodeId, coordinatesOnLink);
			Link oppositeNewLink = networkFactory.createLink(
					oppositeNewLinkId,
					oppositeNewNode,
					network.getLinks().get(oppositeLinkId).getToNode(),
					(NetworkImpl) network,
					CoordUtils.calcEuclideanDistance(oppositeNewNode.getCoord(), nodeA.getCoord()),
					oppositeLink.getFreespeed(),
					oppositeLink.getCapacity(),
					oppositeLink.getNumberOfLanes());

			network.addNode(oppositeNewNode);
			network.addLink(oppositeNewLink);
			network.getLinks().get(oppositeLinkId).setToNode(oppositeNewNode);
			network.getLinks().get(oppositeLinkId).setLength(CoordUtils.calcEuclideanDistance(nodeB.getCoord(), newNode.getCoord()));

			splitLinkOperations.put(oppositeNewLink, link);
		}
	}

	public void undoSplit(Link undoLink) {
		Link originalLink = splitLinkOperations.get(undoLink);

		// reset ToNode
		network.getLinks().get(originalLink.getId()).setToNode(network.getLinks().get(undoLink.getId()).getToNode());

		// remove intermediate node
		network.removeNode(undoLink.getFromNode().getId());

		// remove undoLink
		network.removeLink(undoLink.getId());
	}

	public Network getNetwork() {
		return network;
	}

	public SortedMap<Link, Link> getSplitLinkOperations() {
		return splitLinkOperations;
	}
	
	public Id<Link> getNewLinkId() {
		return newLinkId;
	}

}