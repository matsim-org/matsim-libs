/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkCleanerTest.java
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

package org.matsim.core.network.algorithms;

import junit.framework.TestCase;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;

/**
 * A very simple test for the NetworkCleaner, it doesn't test by far all possible cases.
 * I used it to debug the NetworkCleaner and thought that instead of deleting it, I'll
 * make a test case out of it, maybe someone else feels like extending it some time.
 *
 * @author mrieser
 */
public class NetworkCleanerTest extends TestCase {

	public void testSink() {
		// create a simple network
		Network network = NetworkUtils.createNetwork();
		Node node1 = NetworkUtils.createAndAddNode(network, Id.create("1", Node.class), new Coord((double) 0, (double) 0));
		Node node2 = NetworkUtils.createAndAddNode(network, Id.create("2", Node.class), new Coord((double) 100, (double) 0));
		Node node3 = NetworkUtils.createAndAddNode(network, Id.create("3", Node.class), new Coord((double) 100, (double) 100));
		Node node4 = NetworkUtils.createAndAddNode(network, Id.create("4", Node.class), new Coord((double) 0, (double) 100));
		Node node5 = NetworkUtils.createAndAddNode(network, Id.create("5", Node.class), new Coord((double) 200, (double) 200));
		final Node fromNode = node1;
		final Node toNode = node2;
		NetworkUtils.createAndAddLink(network,Id.create("1", Link.class), fromNode, toNode, (double) 100, (double) 100, (double) 100, (double) 1 );
		final Node fromNode1 = node2;
		final Node toNode1 = node3;
		NetworkUtils.createAndAddLink(network,Id.create("2", Link.class), fromNode1, toNode1, (double) 100, (double) 100, (double) 100, (double) 1 );
		final Node fromNode2 = node3;
		final Node toNode2 = node4;
		NetworkUtils.createAndAddLink(network,Id.create("3", Link.class), fromNode2, toNode2, (double) 100, (double) 100, (double) 100, (double) 1 );
		final Node fromNode3 = node4;
		final Node toNode3 = node1;
		NetworkUtils.createAndAddLink(network,Id.create("4", Link.class), fromNode3, toNode3, (double) 100, (double) 100, (double) 100, (double) 1 );
		final Node fromNode4 = node3;
		final Node toNode4 = node5;
		NetworkUtils.createAndAddLink(network,Id.create("5", Link.class), fromNode4, toNode4, (double) 100, (double) 100, (double) 100, (double) 1 );
		// link 5 is a sink / dead end!

		assertEquals("# nodes", 5, network.getNodes().size());
		assertEquals("# links", 5, network.getLinks().size());

		NetworkCleaner cleaner = new NetworkCleaner();
		cleaner.run(network);

		assertEquals("# nodes", 4, network.getNodes().size());
		assertEquals("# links", 4, network.getLinks().size());
	}

	public void testDoubleSink() {
		// create a simple network
		Network network = NetworkUtils.createNetwork();
		Node node1 = NetworkUtils.createAndAddNode(network, Id.create("1", Node.class), new Coord((double) 0, (double) 0));
		Node node2 = NetworkUtils.createAndAddNode(network, Id.create("2", Node.class), new Coord((double) 100, (double) 0));
		Node node3 = NetworkUtils.createAndAddNode(network, Id.create("3", Node.class), new Coord((double) 100, (double) 100));
		Node node4 = NetworkUtils.createAndAddNode(network, Id.create("4", Node.class), new Coord((double) 0, (double) 100));
		Node node5 = NetworkUtils.createAndAddNode(network, Id.create("5", Node.class), new Coord((double) 200, (double) 200));
		final Node fromNode = node1;
		final Node toNode = node2;
		NetworkUtils.createAndAddLink(network,Id.create("1", Link.class), fromNode, toNode, (double) 100, (double) 100, (double) 100, (double) 1 );
		final Node fromNode1 = node2;
		final Node toNode1 = node3;
		NetworkUtils.createAndAddLink(network,Id.create("2", Link.class), fromNode1, toNode1, (double) 100, (double) 100, (double) 100, (double) 1 );
		final Node fromNode2 = node3;
		final Node toNode2 = node4;
		NetworkUtils.createAndAddLink(network,Id.create("3", Link.class), fromNode2, toNode2, (double) 100, (double) 100, (double) 100, (double) 1 );
		final Node fromNode3 = node4;
		final Node toNode3 = node1;
		NetworkUtils.createAndAddLink(network,Id.create("4", Link.class), fromNode3, toNode3, (double) 100, (double) 100, (double) 100, (double) 1 );
		final Node fromNode4 = node3;
		final Node toNode4 = node5;
		NetworkUtils.createAndAddLink(network,Id.create("5", Link.class), fromNode4, toNode4, (double) 100, (double) 100, (double) 100, (double) 1 );
		final Node fromNode5 = node2;
		final Node toNode5 = node5;
		NetworkUtils.createAndAddLink(network,Id.create("6", Link.class), fromNode5, toNode5, (double) 100, (double) 100, (double) 100, (double) 1 );
		// link 5 is a sink / dead end!

		assertEquals("# nodes", 5, network.getNodes().size());
		assertEquals("# links", 6, network.getLinks().size());

		NetworkCleaner cleaner = new NetworkCleaner();
		cleaner.run(network);

		assertEquals("# nodes", 4, network.getNodes().size());
		assertEquals("# links", 4, network.getLinks().size());
	}

	public void testSource() {
		// create a simple network
		Network network = NetworkUtils.createNetwork();
		Node node1 = NetworkUtils.createAndAddNode(network, Id.create("1", Node.class), new Coord((double) 0, (double) 0));
		Node node2 = NetworkUtils.createAndAddNode(network, Id.create("2", Node.class), new Coord((double) 100, (double) 0));
		Node node3 = NetworkUtils.createAndAddNode(network, Id.create("3", Node.class), new Coord((double) 100, (double) 100));
		Node node4 = NetworkUtils.createAndAddNode(network, Id.create("4", Node.class), new Coord((double) 0, (double) 100));
		Node node5 = NetworkUtils.createAndAddNode(network, Id.create("5", Node.class), new Coord((double) 200, (double) 200));
		final Node fromNode = node1;
		final Node toNode = node2;
		NetworkUtils.createAndAddLink(network,Id.create("1", Link.class), fromNode, toNode, (double) 100, (double) 100, (double) 100, (double) 1 );
		final Node fromNode1 = node2;
		final Node toNode1 = node3;
		NetworkUtils.createAndAddLink(network,Id.create("2", Link.class), fromNode1, toNode1, (double) 100, (double) 100, (double) 100, (double) 1 );
		final Node fromNode2 = node3;
		final Node toNode2 = node4;
		NetworkUtils.createAndAddLink(network,Id.create("3", Link.class), fromNode2, toNode2, (double) 100, (double) 100, (double) 100, (double) 1 );
		final Node fromNode3 = node4;
		final Node toNode3 = node1;
		NetworkUtils.createAndAddLink(network,Id.create("4", Link.class), fromNode3, toNode3, (double) 100, (double) 100, (double) 100, (double) 1 );
		final Node fromNode4 = node5;
		final Node toNode4 = node3;
		NetworkUtils.createAndAddLink(network,Id.create("5", Link.class), fromNode4, toNode4, (double) 100, (double) 100, (double) 100, (double) 1 );
		// link 5 is a source / dead end!

		assertEquals("# nodes", 5, network.getNodes().size());
		assertEquals("# links", 5, network.getLinks().size());

		NetworkCleaner cleaner = new NetworkCleaner();
		cleaner.run(network);

		assertEquals("# nodes", 4, network.getNodes().size());
		assertEquals("# links", 4, network.getLinks().size());
	}

	public void testDoubleSource() {
		// create a simple network
		Network network = NetworkUtils.createNetwork();
		Node node1 = NetworkUtils.createAndAddNode(network, Id.create("1", Node.class), new Coord((double) 0, (double) 0));
		Node node2 = NetworkUtils.createAndAddNode(network, Id.create("2", Node.class), new Coord((double) 100, (double) 0));
		Node node3 = NetworkUtils.createAndAddNode(network, Id.create("3", Node.class), new Coord((double) 100, (double) 100));
		Node node4 = NetworkUtils.createAndAddNode(network, Id.create("4", Node.class), new Coord((double) 0, (double) 100));
		Node node5 = NetworkUtils.createAndAddNode(network, Id.create("5", Node.class), new Coord((double) 200, (double) 200));
		final Node fromNode = node1;
		final Node toNode = node2;
		NetworkUtils.createAndAddLink(network,Id.create("1", Link.class), fromNode, toNode, (double) 100, (double) 100, (double) 100, (double) 1 );
		final Node fromNode1 = node2;
		final Node toNode1 = node3;
		NetworkUtils.createAndAddLink(network,Id.create("2", Link.class), fromNode1, toNode1, (double) 100, (double) 100, (double) 100, (double) 1 );
		final Node fromNode2 = node3;
		final Node toNode2 = node4;
		NetworkUtils.createAndAddLink(network,Id.create("3", Link.class), fromNode2, toNode2, (double) 100, (double) 100, (double) 100, (double) 1 );
		final Node fromNode3 = node4;
		final Node toNode3 = node1;
		NetworkUtils.createAndAddLink(network,Id.create("4", Link.class), fromNode3, toNode3, (double) 100, (double) 100, (double) 100, (double) 1 );
		final Node fromNode4 = node5;
		final Node toNode4 = node3;
		NetworkUtils.createAndAddLink(network,Id.create("5", Link.class), fromNode4, toNode4, (double) 100, (double) 100, (double) 100, (double) 1 );
		final Node fromNode5 = node5;
		final Node toNode5 = node4;
		NetworkUtils.createAndAddLink(network,Id.create("6", Link.class), fromNode5, toNode5, (double) 100, (double) 100, (double) 100, (double) 1 );
		// link 5 is a source / dead end!

		assertEquals("# nodes", 5, network.getNodes().size());
		assertEquals("# links", 6, network.getLinks().size());

		NetworkCleaner cleaner = new NetworkCleaner();
		cleaner.run(network);

		assertEquals("# nodes", 4, network.getNodes().size());
		assertEquals("# links", 4, network.getLinks().size());
	}

}
