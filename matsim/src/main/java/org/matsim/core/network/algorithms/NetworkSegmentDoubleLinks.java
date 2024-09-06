/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkSegmentDoubleLinks.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.core.network.algorithms;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.internal.NetworkRunnable;

import java.util.*;

/**
 * This algorithm handles double links (two links with same from and to node) by splitting
 * one of the link and adding an additional node. This is necessary, since the routes in MATSim
 * are node based.
 *
 * @author laemmel
 */
public final class NetworkSegmentDoubleLinks implements NetworkRunnable {
	private static final Logger log = LogManager.getLogger(NetworkSegmentDoubleLinks.class);

	private Network network = null;

	private int dblLinks = 0;
	private int trblLinks = 0; // what does trbl stand for?? please document! TODO [GL] documentation
	private int qtblLinks = 0; // what does qtbl stand for?? please document!
	private int ntblLinks = 0; // what does ntbl stand for?? pleaes document!

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public void run(final Network network) {
		this.network = network;
		log.info("    running " + this.getClass().getName() + " algorithm...");

		Queue<Node> nodes = new LinkedList<>(network.getNodes().values());
		while (nodes.peek() != null) {
			Node n = nodes.poll();
			HashMap<Id<Node>, List<Link>> toNodesMap = new HashMap<>();
			for (Link l : n.getOutLinks().values()) {
				List<Link> links = toNodesMap.get(l.getToNode().getId());
				if (links != null) {
					links.add(l);
				} else {
					links = new ArrayList<>();
					links.add(l);
					toNodesMap.put(l.getToNode().getId(), links);
				}
			}
			if (toNodesMap.size() > 0) {
				handleDblLinks(toNodesMap);
			}
		}

		log.info("handled: ");
		log.info("\t" + this.dblLinks + " dblLinks.");
		log.info("\t" + this.trblLinks + " trblLinks."); // what does trbl stand for?? please document! TODO [GL] documentation
		log.info("\t" + this.qtblLinks + " qtblLinks.");
		log.info("\t" + this.ntblLinks + " ntblLinks.");
		log.info("done.");
	}

	private void handleDblLinks(HashMap<Id<Node>, List<Link>> toNodesMap) {
		for (List<Link> vec : toNodesMap.values()) {
			switch (vec.size()) {
				case 1:
					break;
				case 2:
					this.dblLinks++;
					break;
				case 3:
					this.trblLinks++;
					break;
				case 4:
					this.qtblLinks++;
					break;
				default:
					this.ntblLinks++;
			}

			for (int i = 1; i < vec.size(); i++) {
				splitLink(vec.get(i));
			}
		}
	}

	private void splitLink(Link link) {
		this.network.removeLink(link.getId());
		double length = link.getLength()/2.0;
		double freespeed = link.getFreespeed();
		double capacity = link.getCapacity();
		double permlanes = link.getNumberOfLanes();

		Node medianNode = this.network.getFactory().createNode(getNewNodeId(), link.getCoord());
		this.network.addNode(medianNode);

		Link tmpLink = this.network.getFactory().createLink(link.getId(), link.getFromNode(), medianNode);
		tmpLink.setLength(length);
		tmpLink.setFreespeed(freespeed);
		tmpLink.setCapacity(capacity);
		tmpLink.setNumberOfLanes(permlanes);
		this.network.addLink(tmpLink);

		tmpLink = this.network.getFactory().createLink(getNewLinkId(), medianNode, link.getToNode());
		tmpLink.setLength(length);
		tmpLink.setFreespeed(freespeed);
		tmpLink.setCapacity(capacity);
		tmpLink.setNumberOfLanes(permlanes);
		this.network.addLink(tmpLink);
	}

	private Id<Link> getNewLinkId() {
		Random r = new Random();
		Id<Link> id = Id.create(r.nextInt(Integer.MAX_VALUE), Link.class);
		while (this.network.getLinks().get(id) != null) {
			id = Id.create(r.nextInt(Integer.MAX_VALUE), Link.class);
		}
		return id;
	}

	private Id<Node> getNewNodeId() {
		Random r = new Random();
		Id<Node> id = Id.create(r.nextInt(Integer.MAX_VALUE), Node.class);
		while (this.network.getNodes().get(id) != null) {
			id = Id.create(r.nextInt(Integer.MAX_VALUE), Node.class);
		}
		return id;
	}

}
