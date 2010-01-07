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

import junit.framework.TestCase;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.geometry.CoordImpl;

/**
 * @author mrieser
 */
public class NetworkImplTest extends TestCase {

	private static final Logger log = Logger.getLogger(NetworkImplTest.class);

	/**
	 * Tests that if a link is added with an id that already exists as link in the network,
	 * an exception is thrown. No exception should be thrown when the same link is added a
	 * second time.
	 */
	public void testAddLink_existingId() {
		NetworkLayer network = new NetworkLayer(); // TODO should be NetworkImpl, but that doesn't work
		Id id1 = new IdImpl(1);
		Id id2 = new IdImpl(2);
		Id id3 = new IdImpl(3);
		Id id4 = new IdImpl(4);
		NodeImpl node1 = new NodeImpl(id1, new CoordImpl(0, 0));
		NodeImpl node2 = new NodeImpl(id2, new CoordImpl(1000, 0));
		NodeImpl node3 = new NodeImpl(id3, new CoordImpl(2000, 500));
		NodeImpl node4 = new NodeImpl(id4, new CoordImpl(2000, -500));
		network.addNode(node1);
		network.addNode(node2);
		network.addNode(node3);
		network.addNode(node4);

		LinkImpl link1 = new LinkImpl(id1, node1, node2, network, 1000, 100.0, 2000.0, 1.0);
		LinkImpl link1b = new LinkImpl(id1, node2, node3, network, 1000, 100.0, 2000.0, 1.0);
		LinkImpl link2 = new LinkImpl(id2, node2, node4, network, 1000, 100.0, 2000.0, 1.0);
		network.addLink(link1);
		assertEquals(1, network.getLinks().size());
		try {
			network.addLink(link1b);
			fail("missing exception. Should not be able to add different link with existing id.");
		}
		catch (RuntimeException e) {
			log.info("catched expected exception.", e);
		}
		assertEquals(1, network.getLinks().size());
		network.addLink(link2);
		assertEquals(2, network.getLinks().size());
		network.addLink(link2); // adding the same link again should just be ignored
		assertEquals(2, network.getLinks().size());
	}

}
