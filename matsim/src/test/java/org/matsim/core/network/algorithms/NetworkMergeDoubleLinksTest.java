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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.util.List;

/**
 * @author mrieser
 */
public class NetworkMergeDoubleLinksTest {

	@Test
	void testRun_remove() {
		Fixture f = new Fixture();
		NetworkMergeDoubleLinks merger = new NetworkMergeDoubleLinks(NetworkMergeDoubleLinks.MergeType.REMOVE);
		merger.run(f.network);

		Assertions.assertEquals(3, f.network.getLinks().size(), "wrong number of links.");
		Assertions.assertNotNull(f.network.getLinks().get(f.linkIds[0]));
		Assertions.assertNull(f.network.getLinks().get(f.linkIds[10]));
		Assertions.assertNotNull(f.network.getLinks().get(f.linkIds[1]));
		Assertions.assertNull(f.network.getLinks().get(f.linkIds[11]));
		Assertions.assertNotNull(f.network.getLinks().get(f.linkIds[2]));
		Assertions.assertNull(f.network.getLinks().get(f.linkIds[12]));

		// attributes should be unchanged
		Assertions.assertEquals(100.0, f.network.getLinks().get(f.linkIds[0]).getLength(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(200.0, f.network.getLinks().get(f.linkIds[0]).getCapacity(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(1, f.network.getLinks().get(f.linkIds[0]).getNumberOfLanes(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(30.0/3.6, f.network.getLinks().get(f.linkIds[0]).getFreespeed(), MatsimTestUtils.EPSILON);

		Assertions.assertEquals(500.0, f.network.getLinks().get(f.linkIds[1]).getLength(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(2000.0, f.network.getLinks().get(f.linkIds[1]).getCapacity(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(2, f.network.getLinks().get(f.linkIds[1]).getNumberOfLanes(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(70.0/3.6, f.network.getLinks().get(f.linkIds[1]).getFreespeed(), MatsimTestUtils.EPSILON);

		Assertions.assertEquals(700.0, f.network.getLinks().get(f.linkIds[2]).getLength(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(500.0, f.network.getLinks().get(f.linkIds[2]).getCapacity(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(2, f.network.getLinks().get(f.linkIds[2]).getNumberOfLanes(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(50.0/3.6, f.network.getLinks().get(f.linkIds[2]).getFreespeed(), MatsimTestUtils.EPSILON);
	}

	@Test
	void testRun_additive() {
		Fixture f = new Fixture();
		NetworkMergeDoubleLinks merger = new NetworkMergeDoubleLinks(NetworkMergeDoubleLinks.MergeType.ADDITIVE);
		merger.run(f.network);

		Assertions.assertEquals(3, f.network.getLinks().size(), "wrong number of links.");
		Assertions.assertNotNull(f.network.getLinks().get(f.linkIds[0]));
		Assertions.assertNull(f.network.getLinks().get(f.linkIds[10]));
		Assertions.assertNotNull(f.network.getLinks().get(f.linkIds[1]));
		Assertions.assertNull(f.network.getLinks().get(f.linkIds[11]));
		Assertions.assertNotNull(f.network.getLinks().get(f.linkIds[2]));
		Assertions.assertNull(f.network.getLinks().get(f.linkIds[12]));

		// additive merge (sum cap, max freespeed, sum lanes, max length)
		Assertions.assertEquals(500.0, f.network.getLinks().get(f.linkIds[0]).getLength(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(2200.0, f.network.getLinks().get(f.linkIds[0]).getCapacity(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(3, f.network.getLinks().get(f.linkIds[0]).getNumberOfLanes(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(70.0/3.6, f.network.getLinks().get(f.linkIds[0]).getFreespeed(), MatsimTestUtils.EPSILON);

		Assertions.assertEquals(500.0, f.network.getLinks().get(f.linkIds[1]).getLength(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(2200.0, f.network.getLinks().get(f.linkIds[1]).getCapacity(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(3, f.network.getLinks().get(f.linkIds[1]).getNumberOfLanes(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(70.0/3.6, f.network.getLinks().get(f.linkIds[1]).getFreespeed(), MatsimTestUtils.EPSILON);

		Assertions.assertEquals(700.0, f.network.getLinks().get(f.linkIds[2]).getLength(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(1500.0, f.network.getLinks().get(f.linkIds[2]).getCapacity(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(3, f.network.getLinks().get(f.linkIds[2]).getNumberOfLanes(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(80.0/3.6, f.network.getLinks().get(f.linkIds[2]).getFreespeed(), MatsimTestUtils.EPSILON);

		// no restriction, as one of the duplicates allowed transition
		Assertions.assertNull(NetworkUtils.getDisallowedNextLinks(f.network.getLinks().get(f.linkIds[0])));

		// restriction remains, as both links have it
		Assertions.assertNotNull(NetworkUtils.getDisallowedNextLinks(f.network.getLinks().get(f.linkIds[1])));
	}

	@Test
	void testRun_maximum() {
		Fixture f = new Fixture();
		NetworkMergeDoubleLinks merger = new NetworkMergeDoubleLinks(NetworkMergeDoubleLinks.MergeType.MAXIMUM);
		merger.run(f.network);

		Assertions.assertEquals(3, f.network.getLinks().size(), "wrong number of links.");
		Assertions.assertNotNull(f.network.getLinks().get(f.linkIds[0]));
		Assertions.assertNull(f.network.getLinks().get(f.linkIds[10]));
		Assertions.assertNotNull(f.network.getLinks().get(f.linkIds[1]));
		Assertions.assertNull(f.network.getLinks().get(f.linkIds[11]));
		Assertions.assertNotNull(f.network.getLinks().get(f.linkIds[2]));
		Assertions.assertNull(f.network.getLinks().get(f.linkIds[12]));

		// max merge (max cap, max freespeed, max langes, max length
		Assertions.assertEquals(500.0, f.network.getLinks().get(f.linkIds[0]).getLength(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(2000.0, f.network.getLinks().get(f.linkIds[0]).getCapacity(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(2, f.network.getLinks().get(f.linkIds[0]).getNumberOfLanes(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(70.0/3.6, f.network.getLinks().get(f.linkIds[0]).getFreespeed(), MatsimTestUtils.EPSILON);

		Assertions.assertEquals(500.0, f.network.getLinks().get(f.linkIds[1]).getLength(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(2000.0, f.network.getLinks().get(f.linkIds[1]).getCapacity(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(2, f.network.getLinks().get(f.linkIds[1]).getNumberOfLanes(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(70.0/3.6, f.network.getLinks().get(f.linkIds[1]).getFreespeed(), MatsimTestUtils.EPSILON);

		Assertions.assertEquals(700.0, f.network.getLinks().get(f.linkIds[2]).getLength(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(1000.0, f.network.getLinks().get(f.linkIds[2]).getCapacity(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(2, f.network.getLinks().get(f.linkIds[2]).getNumberOfLanes(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(80.0/3.6, f.network.getLinks().get(f.linkIds[2]).getFreespeed(), MatsimTestUtils.EPSILON);

		// no restriction, as one of the duplicates allowed transition
		Assertions.assertNull(NetworkUtils.getDisallowedNextLinks(f.network.getLinks().get(f.linkIds[0])));

		// restriction remains, as both links have it
		Assertions.assertNotNull(NetworkUtils.getDisallowedNextLinks(f.network.getLinks().get(f.linkIds[1])));

	}

	private static class Fixture {
		/*package*/ final Scenario scenario;
		/*package*/ final Network network;
		/*package*/ final Id<Node>[] nodeIds = new Id[13];
		/*package*/ final Id<Link>[] linkIds = new Id[13];

		public Fixture() {
			this.scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
			this.network = this.scenario.getNetwork();

			for (int i = 0; i < this.nodeIds.length; i++) {
				this.nodeIds[i] = Id.create(i, Node.class);
			}
			for (int i = 0; i < this.linkIds.length; i++) {
				this.linkIds[i] = Id.create(i, Link.class);
			}

			NetworkFactory factory = this.network.getFactory();
			Node n0 = factory.createNode(this.nodeIds[0], new Coord((double) 0, (double) 0));
			Node n1 = factory.createNode(this.nodeIds[1], new Coord((double) 300, (double) 0));
			Node n2 = factory.createNode(this.nodeIds[2], new Coord((double) 200, (double) 100));
			Node n3 = factory.createNode(this.nodeIds[3], new Coord((double) 100, (double) 400));
			this.network.addNode(n0);
			this.network.addNode(n1);
			this.network.addNode(n2);
			this.network.addNode(n3);
			Link l0 = factory.createLink(this.linkIds[0], n0, n1);
			Link l1 = factory.createLink(this.linkIds[1], n1, n2);
			Link l2 = factory.createLink(this.linkIds[2], n2, n3);
			Link l10 = factory.createLink(this.linkIds[10], n0, n1);
			Link l11 = factory.createLink(this.linkIds[11], n1, n2);
			Link l12 = factory.createLink(this.linkIds[12], n2, n3);
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

			NetworkUtils.getOrCreateDisallowedNextLinks(l0).addDisallowedLinkSequence(TransportMode.car, List.of(l1.getId()));

			NetworkUtils.getOrCreateDisallowedNextLinks(l1).addDisallowedLinkSequence(TransportMode.car, List.of(l12.getId()));
			NetworkUtils.getOrCreateDisallowedNextLinks(l11).addDisallowedLinkSequence(TransportMode.car, List.of(l12.getId()));

		}
	}

}
