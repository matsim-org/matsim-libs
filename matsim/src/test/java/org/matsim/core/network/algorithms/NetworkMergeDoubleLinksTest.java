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

package org.matsim.core.network.algorithms;

import junit.framework.Assert;

import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.utils.misc.Time;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author mrieser
 */
public class NetworkMergeDoubleLinksTest {

	@Test
	public void testRun_remove() {
		Fixture f = new Fixture();
		NetworkMergeDoubleLinks merger = new NetworkMergeDoubleLinks(NetworkMergeDoubleLinks.MergeType.REMOVE);
		merger.run((NetworkImpl) f.network);

		Assert.assertEquals("wrong number of links.", 3, f.network.getLinks().size());
		Assert.assertNotNull(f.network.getLinks().get(f.ids[0]));
		Assert.assertNull(f.network.getLinks().get(f.ids[10]));
		Assert.assertNotNull(f.network.getLinks().get(f.ids[1]));
		Assert.assertNull(f.network.getLinks().get(f.ids[11]));
		Assert.assertNotNull(f.network.getLinks().get(f.ids[2]));
		Assert.assertNull(f.network.getLinks().get(f.ids[12]));

		// attributes should be unchanged
		Assert.assertEquals(100.0, f.network.getLinks().get(f.ids[0]).getLength(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(200.0, f.network.getLinks().get(f.ids[0]).getCapacity(Time.UNDEFINED_TIME), MatsimTestUtils.EPSILON);
		Assert.assertEquals(1, f.network.getLinks().get(f.ids[0]).getNumberOfLanes(Time.UNDEFINED_TIME), MatsimTestUtils.EPSILON);
		Assert.assertEquals(30.0/3.6, f.network.getLinks().get(f.ids[0]).getFreespeed(Time.UNDEFINED_TIME), MatsimTestUtils.EPSILON);

		Assert.assertEquals(500.0, f.network.getLinks().get(f.ids[1]).getLength(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(2000.0, f.network.getLinks().get(f.ids[1]).getCapacity(Time.UNDEFINED_TIME), MatsimTestUtils.EPSILON);
		Assert.assertEquals(2, f.network.getLinks().get(f.ids[1]).getNumberOfLanes(Time.UNDEFINED_TIME), MatsimTestUtils.EPSILON);
		Assert.assertEquals(70.0/3.6, f.network.getLinks().get(f.ids[1]).getFreespeed(Time.UNDEFINED_TIME), MatsimTestUtils.EPSILON);

		Assert.assertEquals(700.0, f.network.getLinks().get(f.ids[2]).getLength(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(500.0, f.network.getLinks().get(f.ids[2]).getCapacity(Time.UNDEFINED_TIME), MatsimTestUtils.EPSILON);
		Assert.assertEquals(2, f.network.getLinks().get(f.ids[2]).getNumberOfLanes(Time.UNDEFINED_TIME), MatsimTestUtils.EPSILON);
		Assert.assertEquals(50.0/3.6, f.network.getLinks().get(f.ids[2]).getFreespeed(Time.UNDEFINED_TIME), MatsimTestUtils.EPSILON);
	}

	@Test
	public void testRun_additive() {
		Fixture f = new Fixture();
		NetworkMergeDoubleLinks merger = new NetworkMergeDoubleLinks(NetworkMergeDoubleLinks.MergeType.ADDITIVE);
		merger.run((NetworkImpl) f.network);

		Assert.assertEquals("wrong number of links.", 3, f.network.getLinks().size());
		Assert.assertNotNull(f.network.getLinks().get(f.ids[0]));
		Assert.assertNull(f.network.getLinks().get(f.ids[10]));
		Assert.assertNotNull(f.network.getLinks().get(f.ids[1]));
		Assert.assertNull(f.network.getLinks().get(f.ids[11]));
		Assert.assertNotNull(f.network.getLinks().get(f.ids[2]));
		Assert.assertNull(f.network.getLinks().get(f.ids[12]));

		// additive merge (sum cap, max freespeed, sum lanes, max length)
		Assert.assertEquals(500.0, f.network.getLinks().get(f.ids[0]).getLength(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(2200.0, f.network.getLinks().get(f.ids[0]).getCapacity(Time.UNDEFINED_TIME), MatsimTestUtils.EPSILON);
		Assert.assertEquals(3, f.network.getLinks().get(f.ids[0]).getNumberOfLanes(Time.UNDEFINED_TIME), MatsimTestUtils.EPSILON);
		Assert.assertEquals(70.0/3.6, f.network.getLinks().get(f.ids[0]).getFreespeed(Time.UNDEFINED_TIME), MatsimTestUtils.EPSILON);

		Assert.assertEquals(500.0, f.network.getLinks().get(f.ids[1]).getLength(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(2200.0, f.network.getLinks().get(f.ids[1]).getCapacity(Time.UNDEFINED_TIME), MatsimTestUtils.EPSILON);
		Assert.assertEquals(3, f.network.getLinks().get(f.ids[1]).getNumberOfLanes(Time.UNDEFINED_TIME), MatsimTestUtils.EPSILON);
		Assert.assertEquals(70.0/3.6, f.network.getLinks().get(f.ids[1]).getFreespeed(Time.UNDEFINED_TIME), MatsimTestUtils.EPSILON);

		Assert.assertEquals(700.0, f.network.getLinks().get(f.ids[2]).getLength(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(1500.0, f.network.getLinks().get(f.ids[2]).getCapacity(Time.UNDEFINED_TIME), MatsimTestUtils.EPSILON);
		Assert.assertEquals(3, f.network.getLinks().get(f.ids[2]).getNumberOfLanes(Time.UNDEFINED_TIME), MatsimTestUtils.EPSILON);
		Assert.assertEquals(80.0/3.6, f.network.getLinks().get(f.ids[2]).getFreespeed(Time.UNDEFINED_TIME), MatsimTestUtils.EPSILON);
	}

	@Test
	public void testRun_maximum() {
		Fixture f = new Fixture();
		NetworkMergeDoubleLinks merger = new NetworkMergeDoubleLinks(NetworkMergeDoubleLinks.MergeType.MAXIMUM);
		merger.run((NetworkImpl) f.network);

		Assert.assertEquals("wrong number of links.", 3, f.network.getLinks().size());
		Assert.assertNotNull(f.network.getLinks().get(f.ids[0]));
		Assert.assertNull(f.network.getLinks().get(f.ids[10]));
		Assert.assertNotNull(f.network.getLinks().get(f.ids[1]));
		Assert.assertNull(f.network.getLinks().get(f.ids[11]));
		Assert.assertNotNull(f.network.getLinks().get(f.ids[2]));
		Assert.assertNull(f.network.getLinks().get(f.ids[12]));

		// max merge (max cap, max freespeed, max langes, max length
		Assert.assertEquals(500.0, f.network.getLinks().get(f.ids[0]).getLength(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(2000.0, f.network.getLinks().get(f.ids[0]).getCapacity(Time.UNDEFINED_TIME), MatsimTestUtils.EPSILON);
		Assert.assertEquals(2, f.network.getLinks().get(f.ids[0]).getNumberOfLanes(Time.UNDEFINED_TIME), MatsimTestUtils.EPSILON);
		Assert.assertEquals(70.0/3.6, f.network.getLinks().get(f.ids[0]).getFreespeed(Time.UNDEFINED_TIME), MatsimTestUtils.EPSILON);

		Assert.assertEquals(500.0, f.network.getLinks().get(f.ids[1]).getLength(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(2000.0, f.network.getLinks().get(f.ids[1]).getCapacity(Time.UNDEFINED_TIME), MatsimTestUtils.EPSILON);
		Assert.assertEquals(2, f.network.getLinks().get(f.ids[1]).getNumberOfLanes(Time.UNDEFINED_TIME), MatsimTestUtils.EPSILON);
		Assert.assertEquals(70.0/3.6, f.network.getLinks().get(f.ids[1]).getFreespeed(Time.UNDEFINED_TIME), MatsimTestUtils.EPSILON);

		Assert.assertEquals(700.0, f.network.getLinks().get(f.ids[2]).getLength(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(1000.0, f.network.getLinks().get(f.ids[2]).getCapacity(Time.UNDEFINED_TIME), MatsimTestUtils.EPSILON);
		Assert.assertEquals(2, f.network.getLinks().get(f.ids[2]).getNumberOfLanes(Time.UNDEFINED_TIME), MatsimTestUtils.EPSILON);
		Assert.assertEquals(80.0/3.6, f.network.getLinks().get(f.ids[2]).getFreespeed(Time.UNDEFINED_TIME), MatsimTestUtils.EPSILON);
	}

	private static class Fixture {
		/*package*/ final Scenario scenario;
		/*package*/ final Network network;
		/*package*/ final Id[] ids = new Id[13];

		public Fixture() {
			this.scenario = new ScenarioImpl();
			this.network = scenario.getNetwork();

			for (int i = 0; i < ids.length; i++) {
				this.ids[i] = this.scenario.createId(Integer.toString(i));
			}

			NetworkFactory factory = this.network.getFactory();
			Node n0 = factory.createNode(this.ids[0], this.scenario.createCoord(0, 0));
			Node n1 = factory.createNode(this.ids[1], this.scenario.createCoord(300, 0));
			Node n2 = factory.createNode(this.ids[2], this.scenario.createCoord(200, 100));
			Node n3 = factory.createNode(this.ids[3], this.scenario.createCoord(100, 400));
			this.network.addNode(n0);
			this.network.addNode(n1);
			this.network.addNode(n2);
			this.network.addNode(n3);
			Link l0 = factory.createLink(this.ids[0], this.ids[0], this.ids[1]);
			Link l1 = factory.createLink(this.ids[1], this.ids[1], this.ids[2]);
			Link l2 = factory.createLink(this.ids[2], this.ids[2], this.ids[3]);
			Link l10 = factory.createLink(this.ids[10], this.ids[0], this.ids[1]);
			Link l11 = factory.createLink(this.ids[11], this.ids[1], this.ids[2]);
			Link l12 = factory.createLink(this.ids[12], this.ids[2], this.ids[3]);
			l0.setLength(100); l0.setCapacity(200.0); l0.setNumberOfLanes(1); l0.setFreespeed(30.0/3.6);
			l10.setLength(500); l10.setCapacity(2000.0); l10.setNumberOfLanes(2); l10.setFreespeed(70.0/3.6);
			this.network.addLink(l0);
			this.network.addLink(l10);
			l1.setLength(500); l1.setCapacity(2000.0); l1.setNumberOfLanes(2); l1.setFreespeed(70.0/3.6);
			l11.setLength(100); l11.setCapacity(200.0); l11.setNumberOfLanes(1); l11.setFreespeed(30.0/3.6);
			this.network.addLink(l1);
			this.network.addLink(l11);
			l2.setLength(700); l2.setCapacity(500.0); l2.setNumberOfLanes(2); l2.setFreespeed(50.0/3.6);
			l12.setLength(300); l12.setCapacity(1000.0); l12.setNumberOfLanes(1); l12.setFreespeed(80.0/3.6);
			this.network.addLink(l2);
			this.network.addLink(l12);
		}
	}

}
