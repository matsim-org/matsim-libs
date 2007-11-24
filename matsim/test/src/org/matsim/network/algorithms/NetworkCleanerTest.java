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

import org.matsim.network.NetworkLayer;

/**
 * A very simple test for the NetworkCleaner, it doesn't test by far all possible cases.
 * I used it to debug the NetworkCleaner and thought that instead of deleting it, I'll
 * make a test case out of it, maybe someone else feels like extending it some time.
 *
 * @author mrieser
 */
public class NetworkCleanerTest extends TestCase {

	public void testNetworkCleaner() {
		// create a simple network
		NetworkLayer network = new NetworkLayer();
		network.createNode("1",   "0",   "0", null);
		network.createNode("2", "100",   "0", null);
		network.createNode("3", "100", "100", null);
		network.createNode("4",   "0", "100", null);
		network.createNode("5", "200", "200", null);
		network.createLink("1", "1", "2", "100", "100", "100", "1", null, null);
		network.createLink("2", "2", "3", "100", "100", "100", "1", null, null);
		network.createLink("3", "3", "4", "100", "100", "100", "1", null, null);
		network.createLink("4", "4", "1", "100", "100", "100", "1", null, null);
		network.createLink("5", "3", "5", "100", "100", "100", "1", null, null);
		// link 5 is a dead end!

		assertEquals("# nodes", 5, network.getNodes().size());
		assertEquals("# links", 5, network.getLinks().size());

		NetworkCleaner cleaner = new NetworkCleaner();
		cleaner.run(network);

		assertEquals("# nodes", 4, network.getNodes().size());
		assertEquals("# links", 4, network.getLinks().size());
	}

}
