/* *********************************************************************** *
 * project: org.matsim.*
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

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.geometry.CoordImpl;

/**
 * Contains several test-cases for API-compliant Networks.
 *
 * @author mrieser
 */
public abstract class AbstractNetworkTest {

	private final static Logger log = Logger.getLogger(AbstractNetworkTest.class);

	public abstract Network getEmptyTestNetwork();

	@Test
	public void removeLink() {
		Fixture f = new Fixture(getEmptyTestNetwork());

		Assert.assertTrue(f.network.getLinks().containsKey(f.ids[1]));
		Assert.assertEquals(1, f.network.getNodes().get(f.ids[1]).getInLinks().size());
		f.network.removeLink(f.ids[1]);
		Assert.assertFalse(f.network.getLinks().containsKey(f.ids[1]));
		Assert.assertEquals(0, f.network.getNodes().get(f.ids[1]).getInLinks().size());
		Assert.assertEquals(1, f.network.getNodes().get(f.ids[1]).getOutLinks().size());

		Assert.assertTrue(f.network.getLinks().containsKey(f.ids[2]));
		f.network.removeLink(f.ids[2]);
		Assert.assertFalse(f.network.getLinks().containsKey(f.ids[2]));

		Assert.assertTrue(f.network.getNodes().containsKey(f.ids[1]));
		Assert.assertEquals(0, f.network.getNodes().get(f.ids[1]).getOutLinks().size());

		Assert.assertEquals(2, f.network.getNodes().get(f.ids[5]).getOutLinks().size());
		Assert.assertEquals(2, f.network.getNodes().get(f.ids[8]).getInLinks().size());
		f.network.removeLink(f.ids[10]);
		Assert.assertEquals(1, f.network.getNodes().get(f.ids[5]).getOutLinks().size());
		Assert.assertEquals(1, f.network.getNodes().get(f.ids[8]).getInLinks().size());
	}

	@Test
	public void removeNode() {
		Fixture f = new Fixture(getEmptyTestNetwork());

		Assert.assertEquals(8, f.network.getNodes().size());
		Assert.assertEquals(12, f.network.getLinks().size());
		Assert.assertTrue(f.network.getLinks().containsKey(f.ids[1]));
		Assert.assertTrue(f.network.getLinks().containsKey(f.ids[2]));
		Assert.assertTrue(f.network.getNodes().containsKey(f.ids[1]));
		f.network.removeNode(f.ids[1]);
		Assert.assertEquals(7, f.network.getNodes().size());
		Assert.assertEquals(10, f.network.getLinks().size());
		Assert.assertFalse(f.network.getLinks().containsKey(f.ids[1]));
		Assert.assertFalse(f.network.getLinks().containsKey(f.ids[2]));
		Assert.assertFalse(f.network.getNodes().containsKey(f.ids[1]));
		Assert.assertFalse(f.network.getNodes().get(f.ids[4]).getOutLinks().containsKey(f.ids[1]));
		Assert.assertTrue(f.network.getNodes().get(f.ids[4]).getOutLinks().containsKey(f.ids[5]));
		Assert.assertTrue(f.network.getNodes().get(f.ids[4]).getOutLinks().containsKey(f.ids[7]));

		f.network.removeNode(f.ids[8]);
		Assert.assertEquals(6, f.network.getNodes().size());
		Assert.assertEquals(6, f.network.getLinks().size());
		Assert.assertFalse(f.network.getLinks().containsKey(f.ids[8]));
		Assert.assertFalse(f.network.getLinks().containsKey(f.ids[9]));
		Assert.assertFalse(f.network.getNodes().containsKey(f.ids[10]));
		Assert.assertFalse(f.network.getNodes().containsKey(f.ids[11]));
		Assert.assertFalse(f.network.getNodes().get(f.ids[5]).getOutLinks().containsKey(f.ids[10]));
		Assert.assertTrue(f.network.getNodes().get(f.ids[5]).getOutLinks().containsKey(f.ids[6]));
		Assert.assertFalse(f.network.getNodes().get(f.ids[5]).getInLinks().containsKey(f.ids[9]));
		Assert.assertTrue(f.network.getNodes().get(f.ids[5]).getInLinks().containsKey(f.ids[3]));
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
		public final Id[] ids = new Id[13];

		public Fixture(final Network network) {
			this.network = network;

			for (int i = 0; i < ids.length; i++) {
				this.ids[i] = new IdImpl(Integer.toString(i));
			}

			NetworkFactory f = this.network.getFactory();
			this.network.addNode(f.createNode(this.ids[1], new CoordImpl(100, 200)));
			this.network.addNode(f.createNode(this.ids[2], new CoordImpl(200, 200)));
			this.network.addNode(f.createNode(this.ids[3], new CoordImpl(  0,   0)));
			this.network.addNode(f.createNode(this.ids[4], new CoordImpl(100, 100)));
			this.network.addNode(f.createNode(this.ids[5], new CoordImpl(200, 100)));
			this.network.addNode(f.createNode(this.ids[6], new CoordImpl(300, 100)));
			this.network.addNode(f.createNode(this.ids[7], new CoordImpl(100,   0)));
			this.network.addNode(f.createNode(this.ids[8], new CoordImpl(200,   0)));
			this.network.addLink(f.createLink(this.ids[1], this.ids[4], this.ids[1]));
			this.network.addLink(f.createLink(this.ids[2], this.ids[1], this.ids[2]));
			this.network.addLink(f.createLink(this.ids[3], this.ids[2], this.ids[5]));
			this.network.addLink(f.createLink(this.ids[4], this.ids[3], this.ids[4]));
			this.network.addLink(f.createLink(this.ids[5], this.ids[4], this.ids[5]));
			this.network.addLink(f.createLink(this.ids[6], this.ids[5], this.ids[6]));
			this.network.addLink(f.createLink(this.ids[7], this.ids[4], this.ids[7]));
			this.network.addLink(f.createLink(this.ids[8], this.ids[7], this.ids[8]));
			this.network.addLink(f.createLink(this.ids[9], this.ids[8], this.ids[5]));
			this.network.addLink(f.createLink(this.ids[10], this.ids[5], this.ids[8]));
			this.network.addLink(f.createLink(this.ids[11], this.ids[8], this.ids[7]));
			this.network.addLink(f.createLink(this.ids[12], this.ids[7], this.ids[4]));
		}

	}
}
