/* ******************o***************************************************** *
 * project: org.matsimde.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package org.matsim.api.core.v01.network;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;

/**
 * Contains several test-cases for API-compliant Networks.
 *
 * @author mrieser
 */
public abstract class AbstractNetworkTest {

	private final static Logger log = LogManager.getLogger(AbstractNetworkTest.class);

	public abstract Network getEmptyTestNetwork();

	@Test
	void removeLink() {
		Fixture f = new Fixture(getEmptyTestNetwork());

		Assertions.assertTrue(f.network.getLinks().containsKey(f.linkIds[1]));
		Assertions.assertEquals(1, f.network.getNodes().get(f.nodeIds[1]).getInLinks().size());
		f.network.removeLink(f.linkIds[1]);
		Assertions.assertFalse(f.network.getLinks().containsKey(f.linkIds[1]));
		Assertions.assertEquals(0, f.network.getNodes().get(f.nodeIds[1]).getInLinks().size());
		Assertions.assertEquals(1, f.network.getNodes().get(f.nodeIds[1]).getOutLinks().size());

		Assertions.assertTrue(f.network.getLinks().containsKey(f.linkIds[2]));
		f.network.removeLink(f.linkIds[2]);
		Assertions.assertFalse(f.network.getLinks().containsKey(f.linkIds[2]));

		Assertions.assertTrue(f.network.getNodes().containsKey(f.nodeIds[1]));
		Assertions.assertEquals(0, f.network.getNodes().get(f.nodeIds[1]).getOutLinks().size());

		Assertions.assertEquals(2, f.network.getNodes().get(f.nodeIds[5]).getOutLinks().size());
		Assertions.assertEquals(2, f.network.getNodes().get(f.nodeIds[8]).getInLinks().size());
		f.network.removeLink(f.linkIds[10]);
		Assertions.assertEquals(1, f.network.getNodes().get(f.nodeIds[5]).getOutLinks().size());
		Assertions.assertEquals(1, f.network.getNodes().get(f.nodeIds[8]).getInLinks().size());
	}

	@Test
	void removeNode() {
		Fixture f = new Fixture(getEmptyTestNetwork());

		Assertions.assertEquals(8, f.network.getNodes().size());
		Assertions.assertEquals(12, f.network.getLinks().size());
		Assertions.assertTrue(f.network.getLinks().containsKey(f.linkIds[1]));
		Assertions.assertTrue(f.network.getLinks().containsKey(f.linkIds[2]));
		Assertions.assertTrue(f.network.getNodes().containsKey(f.nodeIds[1]));
		f.network.removeNode(f.nodeIds[1]);
		Assertions.assertEquals(7, f.network.getNodes().size());
		Assertions.assertEquals(10, f.network.getLinks().size());
		Assertions.assertFalse(f.network.getLinks().containsKey(f.linkIds[1]));
		Assertions.assertFalse(f.network.getLinks().containsKey(f.linkIds[2]));
		Assertions.assertFalse(f.network.getNodes().containsKey(f.nodeIds[1]));
		Assertions.assertFalse(f.network.getNodes().get(f.nodeIds[4]).getOutLinks().containsKey(f.linkIds[1]));
		Assertions.assertTrue(f.network.getNodes().get(f.nodeIds[4]).getOutLinks().containsKey(f.linkIds[5]));
		Assertions.assertTrue(f.network.getNodes().get(f.nodeIds[4]).getOutLinks().containsKey(f.linkIds[7]));

		f.network.removeNode(f.nodeIds[8]);
		Assertions.assertEquals(6, f.network.getNodes().size());
		Assertions.assertEquals(6, f.network.getLinks().size());
		Assertions.assertFalse(f.network.getLinks().containsKey(f.linkIds[8]));
		Assertions.assertFalse(f.network.getLinks().containsKey(f.linkIds[9]));
		Assertions.assertFalse(f.network.getNodes().containsKey(f.nodeIds[10]));
		Assertions.assertFalse(f.network.getNodes().containsKey(f.nodeIds[11]));
		Assertions.assertFalse(f.network.getNodes().get(f.nodeIds[5]).getOutLinks().containsKey(f.linkIds[10]));
		Assertions.assertTrue(f.network.getNodes().get(f.nodeIds[5]).getOutLinks().containsKey(f.linkIds[6]));
		Assertions.assertFalse(f.network.getNodes().get(f.nodeIds[5]).getInLinks().containsKey(f.linkIds[9]));
		Assertions.assertTrue(f.network.getNodes().get(f.nodeIds[5]).getInLinks().containsKey(f.linkIds[3]));
	}

	/**
	 * Creates a simple text network.
	 *
	 * <pre>
	 *               (1)-----2---->(2)
	 *                ^             |
	 *                |             |
	 *                1             3
	 *                |             |
	 *                |             v
	 * (3)-----4---->(4)-----5---->(5)-----6---->(6)
	 *               | ^           | ^
	 *               | |           | |
	 *               7 12         10 9
	 *               | |           | |
	 *               v |           v |
	 *               (7)<----11----(8)
	 *               ( )-----8---->( )
	 * </pre>
	 *
	 * @author mrieser
	 */
	private static class Fixture {
		public final Network network;
		public final Id<Link>[] linkIds = new Id[13];
		public final Id<Node>[] nodeIds = new Id[13];

		public Fixture(final Network network) {
			this.network = network;

			for (int i = 0; i < linkIds.length; i++) {
				this.linkIds[i] = Id.create(i, Link.class);
			}
			for (int i = 0; i < nodeIds.length; i++) {
				this.nodeIds[i] = Id.create(i, Node.class);
			}

			NetworkFactory f = this.network.getFactory();
			Node[] nodes = new Node[9];
			this.network.addNode(nodes[1] = f.createNode(this.nodeIds[1], new Coord((double) 100, (double) 200)));
			this.network.addNode(nodes[2] = f.createNode(this.nodeIds[2], new Coord((double) 200, (double) 200)));
			this.network.addNode(nodes[3] = f.createNode(this.nodeIds[3], new Coord((double) 0, (double) 0)));
			this.network.addNode(nodes[4] = f.createNode(this.nodeIds[4], new Coord((double) 100, (double) 100)));
			this.network.addNode(nodes[5] = f.createNode(this.nodeIds[5], new Coord((double) 200, (double) 100)));
			this.network.addNode(nodes[6] = f.createNode(this.nodeIds[6], new Coord((double) 300, (double) 100)));
			this.network.addNode(nodes[7] = f.createNode(this.nodeIds[7], new Coord((double) 100, (double) 0)));
			this.network.addNode(nodes[8] = f.createNode(this.nodeIds[8], new Coord((double) 200, (double) 0)));
			this.network.addLink(f.createLink(this.linkIds[1], nodes[4], nodes[1]));
			this.network.addLink(f.createLink(this.linkIds[2], nodes[1], nodes[2]));
			this.network.addLink(f.createLink(this.linkIds[3], nodes[2], nodes[5]));
			this.network.addLink(f.createLink(this.linkIds[4], nodes[3], nodes[4]));
			this.network.addLink(f.createLink(this.linkIds[5], nodes[4], nodes[5]));
			this.network.addLink(f.createLink(this.linkIds[6], nodes[5], nodes[6]));
			this.network.addLink(f.createLink(this.linkIds[7], nodes[4], nodes[7]));
			this.network.addLink(f.createLink(this.linkIds[8], nodes[7], nodes[8]));
			this.network.addLink(f.createLink(this.linkIds[9], nodes[8], nodes[5]));
			this.network.addLink(f.createLink(this.linkIds[10], nodes[5], nodes[8]));
			this.network.addLink(f.createLink(this.linkIds[11], nodes[8], nodes[7]));
			this.network.addLink(f.createLink(this.linkIds[12], nodes[7], nodes[4]));
		}

	}
}
