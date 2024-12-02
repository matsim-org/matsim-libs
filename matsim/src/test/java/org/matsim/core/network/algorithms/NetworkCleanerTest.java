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

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;

/**
 * A very simple test for the NetworkCleaner, it doesn't test by far all possible cases.
 * I used it to debug the NetworkCleaner and thought that instead of deleting it, I'll
 * make a test case out of it, maybe someone else feels like extending it some time.
 *
 * @author mrieser
 */
public class NetworkCleanerTest {

	@Test
	void testSink() {
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

		assertEquals(5, network.getNodes().size(), "# nodes");
		assertEquals(5, network.getLinks().size(), "# links");

		NetworkCleaner cleaner = new NetworkCleaner();
		cleaner.run(network);

		assertEquals(4, network.getNodes().size(), "# nodes");
		assertEquals(4, network.getLinks().size(), "# links");
	}

	@Test
	void testDoubleSink() {
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

		assertEquals(5, network.getNodes().size(), "# nodes");
		assertEquals(6, network.getLinks().size(), "# links");

		NetworkCleaner cleaner = new NetworkCleaner();
		cleaner.run(network);

		assertEquals(4, network.getNodes().size(), "# nodes");
		assertEquals(4, network.getLinks().size(), "# links");
	}

	@Test
	void testSource() {
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

		assertEquals(5, network.getNodes().size(), "# nodes");
		assertEquals(5, network.getLinks().size(), "# links");

		NetworkCleaner cleaner = new NetworkCleaner();
		cleaner.run(network);

		assertEquals(4, network.getNodes().size(), "# nodes");
		assertEquals(4, network.getLinks().size(), "# links");
	}

	@Test
	void testDoubleSource() {
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

		assertEquals(5, network.getNodes().size(), "# nodes");
		assertEquals(6, network.getLinks().size(), "# links");

		NetworkCleaner cleaner = new NetworkCleaner();
		cleaner.run(network);

		assertEquals(4, network.getNodes().size(), "# nodes");
		assertEquals(4, network.getLinks().size(), "# links");
	}

	/**
	 * This test essentially does the same by modifying the equil scenario.
	 * Visualization:
	 *		  /...                  ...\
	 * ------o------------o------------o-----------
	 * l1  (n2)   l6    (n7)   l15  (n12)   l20
	 */
	@ParameterizedTest
	@CsvSource({"6,15", "15,6"})
	void testNetworkCleaner(String removedBefore, String expectedRemovedAfter){
		Network net = NetworkUtils.readNetwork(IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("equil"), "network.xml").toString());
		net.removeLink(Id.createLinkId(removedBefore));
		int size = net.getLinks().size();

		NetworkUtils.runNetworkCleaner(net);
		Assertions.assertFalse(net.getLinks().containsKey(Id.createLinkId(expectedRemovedAfter)));
		assertEquals(net.getLinks().size(), size - 1);
	}

}
