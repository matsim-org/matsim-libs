/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkImplTest.java
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

package org.matsim.core.network;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.AbstractNetworkTest;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.gbl.Gbl;
import org.matsim.api.core.v01.network.Node;

/**
 * @author mrieser
 */
public class NetworkImplTest extends AbstractNetworkTest {

	private static final Logger log = Logger.getLogger(NetworkImplTest.class);

	@Override
	public Network getEmptyTestNetwork() {
		return new NetworkImpl();
	}
	
	/**
	 * Tests if the default values of a network instance are the same as the defaults specified in the network_v1.dtd
	 */
	@Test
	public void testDefaultValues(){
		Network net = new NetworkImpl();
		Assert.assertEquals(7.5, net.getEffectiveCellSize(), 0.0);
		Assert.assertEquals(3.75, net.getEffectiveLaneWidth(), 0.0);
		Assert.assertEquals(3600.0, net.getCapacityPeriod(), 0.0);

		Node node1 = NetworkUtils.createNode(Id.create(1, Node.class), new Coord((double) 0, (double) 0));
		Node node2 = NetworkUtils.createNode(Id.create(2, Node.class), new Coord((double) 1000, (double) 0));
		net.addNode(node1);
		net.addNode(node2);
		final NetworkFactory nf = net.getFactory();
		Gbl.assertNotNull(nf);
		Link link = nf.createLink(Id.create(1, Link.class), node1, node2);
		Assert.assertEquals(1, link.getAllowedModes().size());
		Assert.assertEquals("car", link.getAllowedModes().iterator().next());
	}

	/**
	 * Tests that if a link is added with an id that already exists as link in the network,
	 * an exception is thrown. No exception should be thrown when the same link is added a
	 * second time.
	 */
	@Test
	public void testAddLink_existingId() {
		Network network = new NetworkImpl();
		Node node1 = NetworkUtils.createNode(Id.create(1, Node.class), new Coord((double) 0, (double) 0));
		Node node2 = NetworkUtils.createNode(Id.create(2, Node.class), new Coord((double) 1000, (double) 0));
		Node node3 = NetworkUtils.createNode(Id.create(3, Node.class), new Coord((double) 2000, (double) 500));
		final double y = -500;
		Node node4 = NetworkUtils.createNode(Id.create(4, Node.class), new Coord((double) 2000, y));
		network.addNode(node1);
		network.addNode(node2);
		network.addNode(node3);
		network.addNode(node4);

		Link link1 = NetworkUtils.createLink(Id.create(1, Link.class), node1, node2, network, 1000, 100.0, 2000.0, 1.0);
		Link link1b = NetworkUtils.createLink(Id.create(1, Link.class), node2, node3, network, 1000, 100.0, 2000.0, 1.0);
		Link link2 = NetworkUtils.createLink(Id.create(2, Link.class), node2, node4, network, 1000, 100.0, 2000.0, 1.0);
		network.addLink(link1);
		Assert.assertEquals(1, network.getLinks().size());
		try {
			network.addLink(link1b);
			Assert.fail("missing exception. Should not be able to add different link with existing id.");
		}
		catch (IllegalArgumentException e) {
			log.info("catched expected exception.", e);
		}
		Assert.assertEquals(1, network.getLinks().size());
		network.addLink(link2);
		Assert.assertEquals(2, network.getLinks().size());
		network.addLink(link2); // adding the same link again should just be ignored
		Assert.assertEquals(2, network.getLinks().size());
	}
	
	/**
	 * Tests that if a link is added when its associated nodes are not in the network,
	 * an exception is thrown. If the node is already in the network, no exception 
	 * should be thrown. 
	 */
	@Test
	public void testAddLink_noNodes(){
		Network n = NetworkUtils.createNetwork();
		Node a = n.getFactory().createNode(Id.create("a", Node.class), new Coord(0.0, 0.0));
		Node b = n.getFactory().createNode(Id.create("b", Node.class), new Coord(1000.0, 0.0));
		Node c = n.getFactory().createNode(Id.create("c", Node.class), new Coord(0.0, 1000.0));
		
		Link ab = n.getFactory().createLink(Id.create("ab", Link.class), a, b);
		try{
			n.addLink(ab);
			Assert.fail("Should have thrown exception as fromNode was not in network.");
		} catch (IllegalArgumentException e){
			log.info("Caught expected exception: fromNode not in network.", e);
		}

		n.addNode(a);
		try{
			n.addLink(ab);
			Assert.fail("Should have thrown exception as toNode was not in network.");
		} catch (IllegalArgumentException e){
			log.info("Caught expected exception: toNode not in network.", e);
		}
		
		n.addNode(b);
		try{
			n.addLink(ab);
			log.info("Link added correctly. Both nodes in the network.");
		} catch (IllegalArgumentException e){
			Assert.fail("Should not have thrown exception as both nodes are in network.");
		}
		
		Link ac = n.getFactory().createLink(Id.create("ac", Link.class), a, c);
		try{
			n.addLink(ac);
			Assert.fail("Should have thrown exception as toNode was not in network.");
		} catch (IllegalArgumentException e){
			log.info("Caught expected exception: toNode not in network.", e);
		}
		
		n.addNode(c);
		try{
			n.addLink(ac);
			log.info("Link added correctly. Both nodes in the network.");
		} catch (IllegalArgumentException e){
			Assert.fail("Should not have thrown exception as both nodes are in network.");
		}
	}

	
	@Test
	public void testAddNode_existingId() {
		Network network = new NetworkImpl();
		Node node1 = NetworkUtils.createNode(Id.create(1, Node.class), new Coord((double) 0, (double) 0));
		Node node2 = NetworkUtils.createNode(Id.create(2, Node.class), new Coord((double) 1000, (double) 0));
		Node node3 = NetworkUtils.createNode(Id.create(3, Node.class), new Coord((double) 2000, (double) 500));
		Node node1b = NetworkUtils.createNode(Id.create(1, Node.class), new Coord((double) 2000, (double) 0));
		network.addNode(node1);
		network.addNode(node2);
		Assert.assertEquals(2, network.getNodes().size());
		try {
			network.addNode(node1b);
			Assert.fail("missing exception. Should not be able to add different node with existing id.");
		}
		catch (IllegalArgumentException e) {
			log.info("catched expected exception.", e);
		}
		Assert.assertEquals(2, network.getNodes().size());
		network.addNode(node1); // adding the same node again should just be ignored
		Assert.assertEquals(2, network.getNodes().size());
		network.addNode(node3);
		Assert.assertEquals(3, network.getNodes().size());
	}
	
	/**
	 * MATSIM-278, 10jun2015: adding a node if quadtree only contained one node
	 * 
	 * @author mrieser / Senozon AG
	 */
	@Test
	public void testAddNode_singleNodeFirstOnly() {
		Network network = new NetworkImpl();
		Node node1 = NetworkUtils.createNode(Id.create(1, Node.class), new Coord((double) 500, (double) 400));
		Node node2 = NetworkUtils.createNode(Id.create(2, Node.class), new Coord((double) 600, (double) 500));

		network.addNode(node1);
		Assert.assertEquals(1, network.getNodes().size());
		Node n = NetworkUtils.getNearestNode(network,new Coord((double) 550, (double) 450));
		Assert.assertEquals(node1, n);
		
		network.addNode(node2);
		Assert.assertEquals(2, network.getNodes().size());

		n = NetworkUtils.getNearestNode(network,new Coord((double) 590, (double) 490));
		Assert.assertEquals(node2, n);
	}

	/**
	 * MATSIM-278, 13jun2015: adding a node if quadtree is empty
	 * 
	 * @author droeder / Senozon Deutschland GmbH
	 */
	@Test
	public void testAddTwoNodes_initializedEmptyQuadtree() {
		Network network = new NetworkImpl();
		Node node1 = NetworkUtils.createNode(Id.create(1, Node.class), new Coord((double) 500, (double) 400));
		Node node2 = NetworkUtils.createNode(Id.create(2, Node.class), new Coord((double) 600, (double) 500));

		Node n = NetworkUtils.getNearestNode(network,new Coord((double) 550, (double) 450));
		Assert.assertNull(n);
		
		network.addNode(node1);
		Assert.assertEquals(1, network.getNodes().size());
		n = NetworkUtils.getNearestNode(network,new Coord((double) 550, (double) 450));
		Assert.assertEquals(node1, n);
		
		network.addNode(node2);
		Assert.assertEquals(2, network.getNodes().size());

		n = NetworkUtils.getNearestNode(network,new Coord((double) 590, (double) 490));
		Assert.assertEquals(node2, n);
	}

}
