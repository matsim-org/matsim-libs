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
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.utils.geometry.CoordImpl;

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

	private final void handleDoubleLink(LinkImpl l, NetworkLayer network) {
		Node fn = l.getFromNode();
		Node tn = l.getToNode();
		Coord nc = new CoordImpl(0.5*(fn.getCoord().getX()+tn.getCoord().getX()),0.5*(fn.getCoord().getY()+tn.getCoord().getY()));
		Node n = network.createAndAddNode(new IdImpl(l.getId()+this.suffix),nc,((NodeImpl) fn).getType());
		network.removeLink(l.getId());
		LinkImpl l1new = network.createAndAddLink(l.getId(),l.getFromNode(),n,0.5*l.getLength(),l.getFreespeed(),l.getCapacity(),l.getNumberOfLanes(),l.getOrigId(),l.getType());
		LinkImpl l2new = network.createAndAddLink(new IdImpl(l.getId()+this.suffix),n,l.getToNode(),0.5*l.getLength(),l.getFreespeed(),l.getCapacity(),l.getNumberOfLanes(),l.getOrigId(),l.getType());
		log.info("    lid="+l.getId()+" split into lids="+l1new.getId()+","+l2new.getId()+" with additional nid="+n.getId());
	}

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	public void run(NetworkLayer network) {
		log.info("running "+this.getClass().getName()+" module...");
		log.info("  init number of links: "+network.getLinks().size());
		log.info("  init number of nodes: "+network.getNodes().size());
		Set<Id> linkIds = new TreeSet<Id>();
		for (Node n : network.getNodes().values()) {
			Object [] outLinks = n.getOutLinks().values().toArray();
			for (int i=0; i<outLinks.length; i++) {
				LinkImpl refLink = (LinkImpl)outLinks[i];
				for (int j=i+1; j<outLinks.length; j++) {
					LinkImpl candidateLink = (LinkImpl)outLinks[j];
					if (refLink.getToNode().equals(candidateLink.getToNode())) {
						linkIds.add(candidateLink.getId());
					}
				}
			}
		}
		log.info("  number of links to handle: "+linkIds.size());

		for (Id id : linkIds) { handleDoubleLink(network.getLinks().get(id),network); }

		log.info("  final number of links: "+network.getLinks().size());
		log.info("  final number of nodes: "+network.getNodes().size());
		log.info("done. ("+this.getClass().getName()+")");
	}
}
