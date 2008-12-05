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

package org.matsim.network.algorithms;

import junit.framework.TestCase;

import org.matsim.basic.v01.IdImpl;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;

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
		NetworkLayer network = new NetworkLayer();
		Node node1 = network.createNode("1",   "0",   "0", null);
		Node node2 = network.createNode("2", "100",   "0", null);
		Node node3 = network.createNode("3", "100", "100", null);
		Node node4 = network.createNode("4",   "0", "100", null);
		Node node5 = network.createNode("5", "200", "200", null);
		network.createLink(new IdImpl("1"), node1, node2, 100, 100, 100, 1);
		network.createLink(new IdImpl("2"), node2, node3, 100, 100, 100, 1);
		network.createLink(new IdImpl("3"), node3, node4, 100, 100, 100, 1);
		network.createLink(new IdImpl("4"), node4, node1, 100, 100, 100, 1);
		network.createLink(new IdImpl("5"), node3, node5, 100, 100, 100, 1);
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
		NetworkLayer network = new NetworkLayer();
		Node node1 = network.createNode("1",   "0",   "0", null);
		Node node2 = network.createNode("2", "100",   "0", null);
		Node node3 = network.createNode("3", "100", "100", null);
		Node node4 = network.createNode("4",   "0", "100", null);
		Node node5 = network.createNode("5", "200", "200", null);
		network.createLink(new IdImpl("1"), node1, node2, 100, 100, 100, 1);
		network.createLink(new IdImpl("2"), node2, node3, 100, 100, 100, 1);
		network.createLink(new IdImpl("3"), node3, node4, 100, 100, 100, 1);
		network.createLink(new IdImpl("4"), node4, node1, 100, 100, 100, 1);
		network.createLink(new IdImpl("5"), node3, node5, 100, 100, 100, 1);
		network.createLink(new IdImpl("6"), node2, node5, 100, 100, 100, 1);
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
		NetworkLayer network = new NetworkLayer();
		Node node1 = network.createNode("1",   "0",   "0", null);
		Node node2 = network.createNode("2", "100",   "0", null);
		Node node3 = network.createNode("3", "100", "100", null);
		Node node4 = network.createNode("4",   "0", "100", null);
		Node node5 = network.createNode("5", "200", "200", null);
		network.createLink(new IdImpl("1"), node1, node2, 100, 100, 100, 1);
		network.createLink(new IdImpl("2"), node2, node3, 100, 100, 100, 1);
		network.createLink(new IdImpl("3"), node3, node4, 100, 100, 100, 1);
		network.createLink(new IdImpl("4"), node4, node1, 100, 100, 100, 1);
		network.createLink(new IdImpl("5"), node5, node3, 100, 100, 100, 1);
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
		NetworkLayer network = new NetworkLayer();
		Node node1 = network.createNode("1",   "0",   "0", null);
		Node node2 = network.createNode("2", "100",   "0", null);
		Node node3 = network.createNode("3", "100", "100", null);
		Node node4 = network.createNode("4",   "0", "100", null);
		Node node5 = network.createNode("5", "200", "200", null);
		network.createLink(new IdImpl("1"), node1, node2, 100, 100, 100, 1);
		network.createLink(new IdImpl("2"), node2, node3, 100, 100, 100, 1);
		network.createLink(new IdImpl("3"), node3, node4, 100, 100, 100, 1);
		network.createLink(new IdImpl("4"), node4, node1, 100, 100, 100, 1);
		network.createLink(new IdImpl("5"), node5, node3, 100, 100, 100, 1);
		network.createLink(new IdImpl("6"), node5, node4, 100, 100, 100, 1);
		// link 5 is a source / dead end!

		assertEquals("# nodes", 5, network.getNodes().size());
		assertEquals("# links", 6, network.getLinks().size());

		NetworkCleaner cleaner = new NetworkCleaner();
		cleaner.run(network);

		assertEquals("# nodes", 4, network.getNodes().size());
		assertEquals("# links", 4, network.getLinks().size());
	}

}
