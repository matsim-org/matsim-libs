/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkUtilsTest.java
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

package org.matsim.utils;

import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.IdImpl;
import org.matsim.interfaces.core.v01.Link;
import org.matsim.interfaces.core.v01.Node;
import org.matsim.network.NetworkLayer;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.utils.geometry.CoordImpl;

/**
 * @author mrieser
 */
public class NetworkUtilsTest extends MatsimTestCase {

	private final static Logger log = Logger.getLogger(NetworkUtilsTest.class);

	public void testGetNodes_Empty() {
		NetworkLayer network = getTestNetwork();
		List<Node> nodes = NetworkUtils.getNodes(network, "");
		assertEquals(0, nodes.size());

		List<Node> nodes2 = NetworkUtils.getNodes(network, " ");
		assertEquals(0, nodes2.size());

		List<Node> nodes3 = NetworkUtils.getNodes(network, " \t\r\n \t  \t ");
		assertEquals(0, nodes3.size());
	}

	public void testGetNodes_Null() {
		NetworkLayer network = getTestNetwork();
		List<Node> nodes = NetworkUtils.getNodes(network, null);
		assertEquals(0, nodes.size());
	}

	public void testGetNodes_mixedDelimiters() {
		NetworkLayer network = getTestNetwork();
		List<Node> nodes = NetworkUtils.getNodes(network, " 1\t\t2 \n4\t \t5      3 ");
		assertEquals(5, nodes.size());
		assertEquals(network.getNode(new IdImpl(1)), nodes.get(0));
		assertEquals(network.getNode(new IdImpl(2)), nodes.get(1));
		assertEquals(network.getNode(new IdImpl(4)), nodes.get(2));
		assertEquals(network.getNode(new IdImpl(5)), nodes.get(3));
		assertEquals(network.getNode(new IdImpl(3)), nodes.get(4));
	}

	public void testGetNodes_NonExistant() {
		NetworkLayer network = getTestNetwork();
		try {
			NetworkUtils.getNodes(network, "1 3 ab 5");
			fail("expected Exception, but didn't happen.");
		} catch (IllegalArgumentException e) {
			log.info("catched expected exception: " + e.getMessage());
		}
	}
	
	public void testGetLinks_Empty() {
		NetworkLayer network = getTestNetwork();
		List<Link> links = NetworkUtils.getLinks(network, "");
		assertEquals(0, links.size());
		
		List<Link> links2 = NetworkUtils.getLinks(network, " ");
		assertEquals(0, links2.size());
		
		List<Link> links3 = NetworkUtils.getLinks(network, " \t\r\n \t  \t ");
		assertEquals(0, links3.size());
	}
	
	public void testGetLinks_Null() {
		NetworkLayer network = getTestNetwork();
		List<Link> links = NetworkUtils.getLinks(network, null);
		assertEquals(0, links.size());
	}
	
	public void testGetLinks_mixedDelimiters() {
		NetworkLayer network = getTestNetwork();
		List<Link> links = NetworkUtils.getLinks(network, " 1\t\t2 \n4\t \t      3 ");
		assertEquals(4, links.size());
		assertEquals(network.getLink(new IdImpl(1)), links.get(0));
		assertEquals(network.getLink(new IdImpl(2)), links.get(1));
		assertEquals(network.getLink(new IdImpl(4)), links.get(2));
		assertEquals(network.getLink(new IdImpl(3)), links.get(3));
	}
	
	public void testGetLinks_NonExistant() {
		NetworkLayer network = getTestNetwork();
		try {
			NetworkUtils.getLinks(network, "1 3 ab 4");
			fail("expected Exception, but didn't happen.");
		} catch (IllegalArgumentException e) {
			log.info("catched expected exception: " + e.getMessage());
		}
	}
	
	private NetworkLayer getTestNetwork() {
		int numOfLinks = 5;
		
		NetworkLayer network = new NetworkLayer();
		Node[] nodes = new Node[numOfLinks+1];
		for (int i = 0; i <= numOfLinks; i++) {
			nodes[i] = network.createNode(new IdImpl(i), new CoordImpl(1000 * i, 0));
		}
		for (int i = 0; i < numOfLinks; i++) {
			network.createLink(new IdImpl(i), nodes[i], nodes[i+1], 1000.0, 10.0, 3600.0, 1);
		}
		return network;
	}
}
