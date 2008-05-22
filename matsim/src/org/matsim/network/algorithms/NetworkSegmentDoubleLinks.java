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

import java.util.HashMap;
import java.util.Random;
import java.util.Vector;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.IdImpl;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;


/**
 * This algorithm handles double links (two links with same from and to node) by splitting 
 * one of the link and adding an additional node. This is necessary, since the routes in MATSim
 * are node based.  
 * 
 * @author laemmel
 *
 */
public class NetworkSegmentDoubleLinks {
	private static final Logger log = Logger.getLogger(NetworkSegmentDoubleLinks.class);

	
	private NetworkLayer network; 
	
	private int dblLinks = 0;
	private int trblLinks = 0;
	private int qtblLinks = 0;
	private int ntblLinks = 0;
	
	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////
	
	public void run(NetworkLayer network) {
		this.network = network;
		log.info("    running " + this.getClass().getName() + " algorithm...");

		
		ConcurrentLinkedQueue<Node> nodes = new ConcurrentLinkedQueue<Node>(network.getNodes().values());
		while (nodes.peek() != null) {
			Node n = nodes.poll();
			HashMap<Id,Vector<Link>> toNodesMap = new HashMap<Id,Vector<Link>>();
			for (Link l : n.getOutLinks().values()) {
				Vector<Link> links = toNodesMap.get(l.getToNode().getId());
				if (links != null) {
					links.add(l);
				} else {
					links = new Vector<Link>();
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
		log.info("\t" + this.trblLinks + " trblLinks.");
		log.info("\t" + this.qtblLinks + " qtblLinks.");
		log.info("\t" + this.ntblLinks + " ntblLinks.");
		log.info("done.");

	}

	private void handleDblLinks(HashMap<Id, Vector<Link>> toNodesMap) {
		for (Vector<Link> vec : toNodesMap.values()) {
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
		String freespeed = Double.toString(link.getFreespeed(org.matsim.utils.misc.Time.UNDEFINED_TIME));
		String capacity = Double.toString(link.getCapacity(org.matsim.utils.misc.Time.UNDEFINED_TIME));
		String permlanes = Double.toString(link.getLanes(org.matsim.utils.misc.Time.UNDEFINED_TIME));
		String type = null;
		String median = this.network.createNode(getNewNodeId().toString(), Double.toString(link.getCenter().getX()), Double.toString(link.getCenter().getY()), null).getId().toString();
		String l1Id = link.getId().toString();
		this.network.createLink(l1Id, from, median, length, freespeed, capacity, permlanes, l1Id, type);
		String l2Id = getNewLinkId().toString();
		this.network.createLink(l2Id, median, to, length, freespeed, capacity, permlanes, l2Id, type);
	}

	private Id getNewLinkId() {
		Random r = new Random();
		Id id = new IdImpl(Math.abs(r.nextInt()));
		while (this.network.getLink(id) != null) {
			id = new IdImpl(Math.abs(r.nextInt()));
		}
		return id;		
	}

	private Id getNewNodeId() {
		Random r = new Random();
		Id id = new IdImpl(Math.abs(r.nextInt()));
		while (this.network.getNode(id) != null) {
			id = new IdImpl(Math.abs(r.nextInt()));
		}
		return id;		
	}
	
}
