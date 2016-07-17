/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkDoubleLinks.java
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

package playground.balmermi.modules;

import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;

public class NetworkDoubleLinks {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private static final Logger log = Logger.getLogger(NetworkDoubleLinks.class);
	private final String suffix;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public NetworkDoubleLinks(final String idSuffix) {
		super();
		this.suffix = idSuffix;
	}

	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////

	private final void handleDoubleLink(Link l, Network network) {
		Node fn = l.getFromNode();
		Node tn = l.getToNode();
		Coord nc = new Coord(0.5 * (fn.getCoord().getX() + tn.getCoord().getX()), 0.5 * (fn.getCoord().getY() + tn.getCoord().getY()));
		Node r = ((Node) fn);
		final Coord coord = nc;
		Node n1 = NetworkUtils.createAndAddNode(network, Id.create(l.getId()+this.suffix, Node.class), coord);
		NetworkUtils.setType(n1,(String) NetworkUtils.getType( r ));
		Node n = n1;
		network.removeLink(l.getId());
		final Node toNode = n;
		Link l1new = NetworkUtils.createAndAddLink(network,l.getId(), l.getFromNode(), toNode, 0.5*l.getLength(), l.getFreespeed(), l.getCapacity(), l.getNumberOfLanes(), (String) NetworkUtils.getOrigId( l ), (String) NetworkUtils.getType(l));
		final Node fromNode = n;
		Link l2new = NetworkUtils.createAndAddLink(network,Id.create(l.getId()+this.suffix, Link.class), fromNode, l.getToNode(), 0.5*l.getLength(), l.getFreespeed(), l.getCapacity(), l.getNumberOfLanes(), (String) NetworkUtils.getOrigId( l ), (String) NetworkUtils.getType(l));
		log.info("    lid="+l.getId()+" split into lids="+l1new.getId()+","+l2new.getId()+" with additional nid="+n.getId());
	}

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	public void run(Network network) {
		log.info("running "+this.getClass().getName()+" module...");
		log.info("  init number of links: "+network.getLinks().size());
		log.info("  init number of nodes: "+network.getNodes().size());
		Set<Id> linkIds = new TreeSet<Id>();
		for (Node n : network.getNodes().values()) {
			Object [] outLinks = n.getOutLinks().values().toArray();
			for (int i=0; i<outLinks.length; i++) {
				Link refLink = (Link)outLinks[i];
				for (int j=i+1; j<outLinks.length; j++) {
					Link candidateLink = (Link)outLinks[j];
					if (refLink.getToNode().equals(candidateLink.getToNode())) {
						linkIds.add(candidateLink.getId());
					}
				}
			}
		}
		log.info("  number of links to handle: "+linkIds.size());

		for (Id id : linkIds) { handleDoubleLink((Link) network.getLinks().get(id),network); }

		log.info("  final number of links: "+network.getLinks().size());
		log.info("  final number of nodes: "+network.getNodes().size());
		log.info("done. ("+this.getClass().getName()+")");
	}
}
