/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkSegmentDoubleLinks.java
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

package org.matsim.network.algorithms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.IdImpl;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.utils.misc.Time;


/**
 * This algorithm handles double links (two links with same from and to node) by splitting
 * one of the link and adding an additional node. This is necessary, since the routes in MATSim
 * are node based.
 *
 * @author laemmel
 */
public class NetworkSegmentDoubleLinks {
	private static final Logger log = Logger.getLogger(NetworkSegmentDoubleLinks.class);

	private NetworkLayer network;

	private int dblLinks = 0;
	private int trblLinks = 0; // what does trbl stand for?? please document! TODO [GL] documentation
	private int qtblLinks = 0; // what does qtbl stand for?? please document!
	private int ntblLinks = 0; // what does ntbl stand for?? pleaes document!

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	public void run(final NetworkLayer network) {
		this.network = network;
		log.info("    running " + this.getClass().getName() + " algorithm...");

		Queue<Node> nodes = new LinkedList<Node>(network.getNodes().values());
		while (nodes.peek() != null) {
			Node n = nodes.poll();
			HashMap<Id, List<Link>> toNodesMap = new HashMap<Id, List<Link>>();
			for (Link l : n.getOutLinks().values()) {
				List<Link> links = toNodesMap.get(l.getToNode().getId());
				if (links != null) {
					links.add(l);
				} else {
					links = new ArrayList<Link>();
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

	private void handleDblLinks(HashMap<Id, List<Link>> toNodesMap) {
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
		this.network.removeLink(link);
		String from = link.getFromNode().getId().toString();
		String to = link.getToNode().getId().toString();
		String length = Double.toString(link.getLength()/2);
		String freespeed = Double.toString(link.getFreespeed(Time.UNDEFINED_TIME));
		String capacity = Double.toString(link.getCapacity(Time.UNDEFINED_TIME));
		String permlanes = Double.toString(link.getLanes(Time.UNDEFINED_TIME));
		String type = null;
		String median = this.network.createNode(getNewNodeId().toString(), Double.toString(link.getCenter().getX()), Double.toString(link.getCenter().getY()), null).getId().toString();
		String l1Id = link.getId().toString();
		this.network.createLink(l1Id, from, median, length, freespeed, capacity, permlanes, l1Id, type);
		String l2Id = getNewLinkId().toString();
		this.network.createLink(l2Id, median, to, length, freespeed, capacity, permlanes, l2Id, type);
	}

	private Id getNewLinkId() {
		Random r = new Random();
		Id id = new IdImpl(r.nextInt(Integer.MAX_VALUE));
		while (this.network.getLink(id) != null) {
			id = new IdImpl(r.nextInt(Integer.MAX_VALUE));
		}
		return id;
	}

	private Id getNewNodeId() {
		Random r = new Random();
		Id id = new IdImpl(r.nextInt(Integer.MAX_VALUE));
		while (this.network.getNode(id) != null) {
			id = new IdImpl(r.nextInt(Integer.MAX_VALUE));
		}
		return id;
	}

}
