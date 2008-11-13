/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkExpandNode.java
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

package org.matsim.network.algorithms;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.IdImpl;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.utils.collections.Tuple;
import org.matsim.utils.geometry.Coord;
import org.matsim.utils.geometry.CoordImpl;
import org.matsim.utils.misc.Time;

public class NetworkExpandNode {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final static Logger log = Logger.getLogger(NetworkExpandNode.class);

	private Node node = null;
	private ArrayList<Tuple<Id,Id>> turns = null;
	private double radius = Double.NaN;
	private double offset = Double.NaN;
	
	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public NetworkExpandNode() {
		log.info("init " + this.getClass().getName() + " module...");
		log.info("done.");
	}

	//////////////////////////////////////////////////////////////////////
	// set methods
	//////////////////////////////////////////////////////////////////////
	
	public final void setNode(Node node) {
		this.node = node;
	}

	public final void setTurns(ArrayList<Tuple<Id,Id>> turns) {
		this.turns = turns;
	}

	public final void setNodeExpansionRadius(double radius) {
		this.radius = radius;
	}

	public final void setNodeExpansionOffset(double offset) {
		this.offset = offset;
	}

	//////////////////////////////////////////////////////////////////////
	// run method
	//////////////////////////////////////////////////////////////////////

	public void run(final NetworkLayer network) {
		log.info("running " + this.getClass().getName() + " module...");
		this.expandNode(network,node.getId(),turns,radius,offset);
		log.info("running " + this.getClass().getName() + " module...");
	}
	
	//////////////////////////////////////////////////////////////////////
	// expand method
	//////////////////////////////////////////////////////////////////////

	public final Tuple<ArrayList<Node>,ArrayList<Link>> expandNode(final NetworkLayer network, final Id nodeId, final ArrayList<Tuple<Id,Id>> turns, final double r, final double e) {
		// check the input
		if (Double.isNaN(r)) { throw new IllegalArgumentException("nodeid="+nodeId+": expansion radius is NaN."); }
		if (Double.isNaN(e)) { throw new IllegalArgumentException("nodeid="+nodeId+": expansion radius is NaN."); }
		if (network == null) { throw new IllegalArgumentException("network not defined."); }
		Node node = network.getNode(nodeId);
		if (node == null) { throw new IllegalArgumentException("nodeid="+nodeId+": not found in the network."); }
		if (turns == null) {throw new IllegalArgumentException("nodeid="+nodeId+": turn list not defined!"); }
		for (int i=0; i<turns.size(); i++) {
			Id first = turns.get(i).getFirst();
			if (first == null) { throw new IllegalArgumentException("given list contains 'null' values."); }
			if (!node.getInLinks().containsKey(first)) { throw new IllegalArgumentException("nodeid="+nodeId+", linkid="+first+": link not an inlink of given node."); }
			Id second = turns.get(i).getSecond();
			if (second == null) { throw new IllegalArgumentException("given list contains 'null' values."); }
			if (!node.getOutLinks().containsKey(second)) { throw new IllegalArgumentException("nodeid="+nodeId+", linkid="+second+": link not an outlink of given node."); }
		}
		
		// remove the node
		Map<Id,Link> inlinks = new TreeMap<Id, Link>(node.getInLinks());
		Map<Id,Link> outlinks = new TreeMap<Id, Link>(node.getOutLinks());
		if (!network.removeNode(node)) { throw new RuntimeException("nodeid="+nodeId+": Failed to remove node from the network."); }

		ArrayList<Node> newNodes = new ArrayList<Node>(inlinks.size()+outlinks.size());
		ArrayList<Link> newLinks = new ArrayList<Link>(turns.size());
		// add new nodes and connect them with the in and out links
		int nodeIdCnt = 0;
		double d = Math.sqrt(r*r-e*e);
		for (Link inlink : inlinks.values()) {
			Coord c = node.getCoord();
			Coord p = inlink.getFromNode().getCoord();
			Coord pc = new CoordImpl(c.getX()-p.getX(),c.getY()-p.getY());
			double lpc = Math.sqrt(pc.getX()*pc.getX()+pc.getY()*pc.getY());
			double x = p.getX()+(1-d/lpc)*pc.getX()+e/lpc*pc.getY();
			double y = p.getY()+(1-d/lpc)*pc.getY()-e/lpc*pc.getX();
			Node n = network.createNode(new IdImpl(node.getId()+"-"+nodeIdCnt),new CoordImpl(x,y),node.getType());
			newNodes.add(n);
			nodeIdCnt++;
			network.createLink(inlink.getId(),inlink.getFromNode(),n,inlink.getLength(),inlink.getFreespeed(Time.UNDEFINED_TIME),inlink.getCapacity(Time.UNDEFINED_TIME),inlink.getLanes(Time.UNDEFINED_TIME),inlink.getOrigId(),inlink.getType());
		}
		for (Link outlink : outlinks.values()) {
			Coord c = node.getCoord();
			Coord p = outlink.getToNode().getCoord();
			Coord cp = new CoordImpl(p.getX()-c.getX(),p.getY()-c.getY());
			double lcp = Math.sqrt(cp.getX()*cp.getX()+cp.getY()*cp.getY());
			double x = c.getX()+d/lcp*cp.getX()+e/lcp*cp.getY();
			double y = c.getY()+d/lcp*cp.getY()-e/lcp*cp.getX();
			Node n = network.createNode(new IdImpl(node.getId()+"-"+nodeIdCnt),new CoordImpl(x,y),node.getType());
			newNodes.add(n);
			nodeIdCnt++;
			network.createLink(outlink.getId(),n,outlink.getToNode(),outlink.getLength(),outlink.getFreespeed(Time.UNDEFINED_TIME),outlink.getCapacity(Time.UNDEFINED_TIME),outlink.getLanes(Time.UNDEFINED_TIME),outlink.getOrigId(),outlink.getType());
		}
		
		// add virtual links for the turn restrictions
		for (int i=0; i<turns.size(); i++) {
			Tuple<Id,Id> turn = turns.get(i);
			Link fromLink = network.getLink(turn.getFirst());
			Link toLink = network.getLink(turn.getSecond());
			Link l = network.createLink(new IdImpl(fromLink.getId()+"-"+i),fromLink.getToNode(),toLink.getFromNode(),toLink.getFromNode().getCoord().calcDistance(fromLink.getToNode().getCoord()),fromLink.getFreespeed(Time.UNDEFINED_TIME),fromLink.getCapacity(Time.UNDEFINED_TIME),fromLink.getLanes(Time.UNDEFINED_TIME));
			newLinks.add(l);
		}
		return new Tuple<ArrayList<Node>, ArrayList<Link>>(newNodes,newLinks);
	}
}
