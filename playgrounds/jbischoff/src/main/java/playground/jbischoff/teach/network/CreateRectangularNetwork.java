/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
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

/**
 * 
 */
package playground.jbischoff.teach.network;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class CreateRectangularNetwork {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Network network = NetworkUtils.createNetwork();
		
		Node n1 = network.getFactory().createNode(Id.createNodeId("1"), new Coord(0,0));
		Node n2 = network.getFactory().createNode(Id.createNodeId("2"), new Coord(0,100));
		Node n3 = network.getFactory().createNode(Id.createNodeId("3"), new Coord(100,0));
		Node n4 = network.getFactory().createNode(Id.createNodeId("4"), new Coord(100,100));
		
		network.addNode(n1);
		network.addNode(n2);
		network.addNode(n3);
		network.addNode(n4);
		
		Link l1 = network.getFactory().createLink(Id.createLinkId(1), n1, n2);
		Link l2 = network.getFactory().createLink(Id.createLinkId(2), n2, n1);
		Link l3 = network.getFactory().createLink(Id.createLinkId(3), n2, n4);
		Link l4 = network.getFactory().createLink(Id.createLinkId(4), n4, n2);
		Link l5 = network.getFactory().createLink(Id.createLinkId(5), n3, n4);
		Link l6 = network.getFactory().createLink(Id.createLinkId(6), n4, n3);
		Link l7 = network.getFactory().createLink(Id.createLinkId(7), n3, n1);
		Link l8 = network.getFactory().createLink(Id.createLinkId(8), n1, n3);
		
		network.addLink(l1);
		network.addLink(l2);
		network.addLink(l3);
		network.addLink(l4);
		network.addLink(l5);
		network.addLink(l6);
		network.addLink(l7);
		network.addLink(l8);
	
		for (Link l : network.getLinks().values()){
			l.setCapacity(2000);
			l.setFreespeed(16.66666);
			Set<String> modes = new HashSet<String>();
			modes.add("car");
			l.setAllowedModes(modes);
			l.setLength(100);
			l.setNumberOfLanes(2.0);
		}
		new NetworkWriter(network).write("network.xml");
	}

}
