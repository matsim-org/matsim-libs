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

import java.util.EnumSet;
import java.util.Set;

import junit.framework.Assert;

import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.core.network.NetworkImpl;

/**
 * @author mrieser
 */
public class MultimodalNetworkCleanerTest {

	@Test
	public void testRun_singleMode() {
		Fixture f = new Fixture();
		Network network = f.scenario.getNetwork();
		MultimodalNetworkCleaner cleaner = new MultimodalNetworkCleaner((NetworkImpl) network);

		cleaner.run(EnumSet.of(TransportMode.car));
		Assert.assertEquals("wrong number of links.", 8, network.getLinks().size());
		Assert.assertEquals("wrong number of nodes.", 6, network.getNodes().size());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[1]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[2]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[3]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[4]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[5]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[6]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[7]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[8]).getAllowedModes());
	}

	@Test
	public void testRun_singleMode_separateLink() {
		Fixture f = new Fixture();
		Network network = f.scenario.getNetwork();
		network.addLink(network.getFactory().createLink(f.ids[10], f.ids[1], f.ids[4]));
		network.addLink(network.getFactory().createLink(f.ids[11], f.ids[6], f.ids[3]));
		network.getLinks().get(f.ids[10]).setAllowedModes(f.modesW);
		network.getLinks().get(f.ids[11]).setAllowedModes(f.modesC);

		MultimodalNetworkCleaner cleaner = new MultimodalNetworkCleaner((NetworkImpl) network);

		cleaner.run(EnumSet.of(TransportMode.car));
		Assert.assertEquals("wrong number of links.", 9, network.getLinks().size());
		Assert.assertEquals("wrong number of nodes.", 6, network.getNodes().size());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[1]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[2]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[3]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[4]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[5]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[6]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[7]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[8]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[10]).getAllowedModes());
		Assert.assertNull(network.getLinks().get(f.ids[11]));

		cleaner.run(EnumSet.of(TransportMode.walk));
		Assert.assertEquals("wrong number of links.", 8, network.getLinks().size());
		Assert.assertEquals("wrong number of nodes.", 6, network.getNodes().size());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[1]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[2]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[3]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[4]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[5]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[6]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[7]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[8]).getAllowedModes());
		Assert.assertNull(network.getLinks().get(f.ids[10]));
		Assert.assertNull(network.getLinks().get(f.ids[11]));
	}

	@Test
	public void testRun_singleInexistantMode() {
		Fixture f = new Fixture();
		Network network = f.scenario.getNetwork();

		MultimodalNetworkCleaner cleaner = new MultimodalNetworkCleaner((NetworkImpl) network);

		cleaner.run(EnumSet.of(TransportMode.other));
		Assert.assertEquals("wrong number of links.", 8, network.getLinks().size());
		Assert.assertEquals("wrong number of nodes.", 6, network.getNodes().size());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[1]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[2]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[3]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[4]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[5]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[6]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[7]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[8]).getAllowedModes());
	}

	@Test
	public void testRun_singleMode_singleSink() {
		Fixture f = new Fixture();
		Network network = f.scenario.getNetwork();
		network.addNode(network.getFactory().createNode(f.ids[10], f.scenario.createCoord(0, 200)));
		network.addNode(network.getFactory().createNode(f.ids[11], f.scenario.createCoord(200, 200)));
		network.addLink(network.getFactory().createLink(f.ids[10], f.ids[1], f.ids[10]));
		network.addLink(network.getFactory().createLink(f.ids[11], f.ids[3], f.ids[11]));
		network.getLinks().get(f.ids[10]).setAllowedModes(f.modesC);
		network.getLinks().get(f.ids[11]).setAllowedModes(f.modesW);

		MultimodalNetworkCleaner cleaner = new MultimodalNetworkCleaner((NetworkImpl) network);
		Assert.assertEquals("wrong number of links.", 10, network.getLinks().size());
		Assert.assertEquals("wrong number of nodes.", 8, network.getNodes().size());

		cleaner.run(EnumSet.of(TransportMode.car));
		Assert.assertEquals("wrong number of links.", 9, network.getLinks().size());
		Assert.assertEquals("wrong number of nodes.", 7, network.getNodes().size());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[1]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[2]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[3]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[4]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[5]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[6]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[7]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[8]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[11]).getAllowedModes());
		Assert.assertNull(network.getLinks().get(f.ids[10]));

		cleaner.run(EnumSet.of(TransportMode.walk));
		Assert.assertEquals("wrong number of links.", 8, network.getLinks().size());
		Assert.assertEquals("wrong number of nodes.", 6, network.getNodes().size());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[1]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[2]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[3]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[4]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[5]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[6]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[7]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[8]).getAllowedModes());
		Assert.assertNull(network.getLinks().get(f.ids[11]));
	}

	@Test
	public void testRun_singleMode_singleSinkIntegrated() {
		Fixture f = new Fixture();
		Network network = f.scenario.getNetwork();
		network.getLinks().get(f.ids[1]).setAllowedModes(f.modesCW); // integrate the sinks into the existing network
		network.getLinks().get(f.ids[8]).setAllowedModes(f.modesCW);

		MultimodalNetworkCleaner cleaner = new MultimodalNetworkCleaner((NetworkImpl) network);
		Assert.assertEquals("wrong number of links.", 8, network.getLinks().size());
		Assert.assertEquals("wrong number of nodes.", 6, network.getNodes().size());

		cleaner.run(EnumSet.of(TransportMode.car));
		Assert.assertEquals("wrong number of links.", 8, network.getLinks().size());
		Assert.assertEquals("wrong number of nodes.", 6, network.getNodes().size());
		Assert.assertEquals(f.modesCW, network.getLinks().get(f.ids[1]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[2]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[3]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[4]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[5]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[6]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[7]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[8]).getAllowedModes()); // only remove mode, not link!

		cleaner.run(EnumSet.of(TransportMode.walk));
		Assert.assertEquals("wrong number of links.", 8, network.getLinks().size());
		Assert.assertEquals("wrong number of nodes.", 6, network.getNodes().size());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[1]).getAllowedModes()); // only remove mode, not link!
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[2]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[3]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[4]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[5]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[6]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[7]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[8]).getAllowedModes());
	}

	@Test
	public void testRun_singleMode_doubleSink() {
		Fixture f = new Fixture();
		Network network = f.scenario.getNetwork();
		network.addNode(network.getFactory().createNode(f.ids[10], f.scenario.createCoord(0, 200)));
		network.addNode(network.getFactory().createNode(f.ids[11], f.scenario.createCoord(200, 200)));
		network.addLink(network.getFactory().createLink(f.ids[10], f.ids[1], f.ids[10]));
		network.addLink(network.getFactory().createLink(f.ids[11], f.ids[2], f.ids[10]));
		network.addLink(network.getFactory().createLink(f.ids[12], f.ids[2], f.ids[11]));
		network.addLink(network.getFactory().createLink(f.ids[13], f.ids[3], f.ids[11]));
		network.getLinks().get(f.ids[10]).setAllowedModes(f.modesC);
		network.getLinks().get(f.ids[11]).setAllowedModes(f.modesC);
		network.getLinks().get(f.ids[12]).setAllowedModes(f.modesW);
		network.getLinks().get(f.ids[13]).setAllowedModes(f.modesW);

		MultimodalNetworkCleaner cleaner = new MultimodalNetworkCleaner((NetworkImpl) network);
		Assert.assertEquals("wrong number of links.", 12, network.getLinks().size());
		Assert.assertEquals("wrong number of nodes.", 8, network.getNodes().size());

		cleaner.run(EnumSet.of(TransportMode.walk));

		Assert.assertEquals("wrong number of links.", 10, network.getLinks().size());
		Assert.assertEquals("wrong number of nodes.", 7, network.getNodes().size());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[1]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[2]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[3]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[4]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[5]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[6]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[7]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[8]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[10]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[11]).getAllowedModes());
		Assert.assertNull(network.getLinks().get(f.ids[12]));
		Assert.assertNull(network.getLinks().get(f.ids[13]));

		cleaner.run(EnumSet.of(TransportMode.car));
		Assert.assertEquals("wrong number of links.", 8, network.getLinks().size());
		Assert.assertEquals("wrong number of nodes.", 6, network.getNodes().size());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[1]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[2]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[3]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[4]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[5]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[6]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[7]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[8]).getAllowedModes());
		Assert.assertNull(network.getLinks().get(f.ids[10]));
		Assert.assertNull(network.getLinks().get(f.ids[11]));
	}


	@Test
	public void testRun_singleMode_singleSource() {
		Fixture f = new Fixture();
		Network network = f.scenario.getNetwork();
		network.addNode(network.getFactory().createNode(f.ids[10], f.scenario.createCoord(0, 200)));
		network.addNode(network.getFactory().createNode(f.ids[11], f.scenario.createCoord(200, 200)));
		network.addLink(network.getFactory().createLink(f.ids[10], f.ids[10], f.ids[1]));
		network.addLink(network.getFactory().createLink(f.ids[11], f.ids[11], f.ids[3]));
		network.getLinks().get(f.ids[10]).setAllowedModes(f.modesC);
		network.getLinks().get(f.ids[11]).setAllowedModes(f.modesW);

		MultimodalNetworkCleaner cleaner = new MultimodalNetworkCleaner((NetworkImpl) network);
		Assert.assertEquals("wrong number of links.", 10, network.getLinks().size());
		Assert.assertEquals("wrong number of nodes.", 8, network.getNodes().size());

		cleaner.run(EnumSet.of(TransportMode.car));
		Assert.assertEquals("wrong number of links.", 9, network.getLinks().size());
		Assert.assertEquals("wrong number of nodes.", 7, network.getNodes().size());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[1]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[2]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[3]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[4]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[5]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[6]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[7]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[8]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[11]).getAllowedModes());
		Assert.assertNull(network.getLinks().get(f.ids[10]));

		cleaner.run(EnumSet.of(TransportMode.walk));
		Assert.assertEquals("wrong number of links.", 8, network.getLinks().size());
		Assert.assertEquals("wrong number of nodes.", 6, network.getNodes().size());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[1]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[2]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[3]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[4]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[5]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[6]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[7]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[8]).getAllowedModes());
		Assert.assertNull(network.getLinks().get(f.ids[11]));
	}

	@Test
	public void testRun_singleMode_doubleSource() {
		Fixture f = new Fixture();
		Network network = f.scenario.getNetwork();
		network.addNode(network.getFactory().createNode(f.ids[10], f.scenario.createCoord(0, 200)));
		network.addNode(network.getFactory().createNode(f.ids[11], f.scenario.createCoord(200, 200)));
		network.addLink(network.getFactory().createLink(f.ids[10], f.ids[10], f.ids[1]));
		network.addLink(network.getFactory().createLink(f.ids[11], f.ids[10], f.ids[2]));
		network.addLink(network.getFactory().createLink(f.ids[12], f.ids[11], f.ids[2]));
		network.addLink(network.getFactory().createLink(f.ids[13], f.ids[11], f.ids[3]));
		network.getLinks().get(f.ids[10]).setAllowedModes(f.modesC);
		network.getLinks().get(f.ids[11]).setAllowedModes(f.modesC);
		network.getLinks().get(f.ids[12]).setAllowedModes(f.modesW);
		network.getLinks().get(f.ids[13]).setAllowedModes(f.modesW);

		MultimodalNetworkCleaner cleaner = new MultimodalNetworkCleaner((NetworkImpl) network);
		Assert.assertEquals("wrong number of links.", 12, network.getLinks().size());
		Assert.assertEquals("wrong number of nodes.", 8, network.getNodes().size());

		cleaner.run(EnumSet.of(TransportMode.walk));

		Assert.assertEquals("wrong number of links.", 10, network.getLinks().size());
		Assert.assertEquals("wrong number of nodes.", 7, network.getNodes().size());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[1]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[2]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[3]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[4]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[5]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[6]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[7]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[8]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[10]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[11]).getAllowedModes());
		Assert.assertNull(network.getLinks().get(f.ids[12]));
		Assert.assertNull(network.getLinks().get(f.ids[13]));

		cleaner.run(EnumSet.of(TransportMode.car));
		Assert.assertEquals("wrong number of links.", 8, network.getLinks().size());
		Assert.assertEquals("wrong number of nodes.", 6, network.getNodes().size());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[1]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[2]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[3]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[4]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[5]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[6]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[7]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[8]).getAllowedModes());
		Assert.assertNull(network.getLinks().get(f.ids[10]));
		Assert.assertNull(network.getLinks().get(f.ids[11]));
	}

	@Test
	public void testRun_multipleModes() {
		Fixture f = new MultimodeFixture();
		Network network = f.scenario.getNetwork();

		MultimodalNetworkCleaner cleaner = new MultimodalNetworkCleaner((NetworkImpl) network);

		cleaner.run(EnumSet.of(TransportMode.car, TransportMode.walk));
		Assert.assertEquals("wrong number of links.", 12, network.getLinks().size());
		Assert.assertEquals("wrong number of nodes.", 9, network.getNodes().size());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[1]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[2]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[3]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[4]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[5]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[6]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[7]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[8]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[9]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[10]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[11]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[12]).getAllowedModes());
	}

	@Test
	public void testRun_multipleModes_doubleSink() {
		Fixture f = new MultimodeFixture();
		Network network = f.scenario.getNetwork();

		network.addNode(network.getFactory().createNode(f.ids[10], f.scenario.createCoord(200, 200)));
		network.addLink(network.getFactory().createLink(f.ids[18], f.ids[2], f.ids[10]));
		network.addLink(network.getFactory().createLink(f.ids[19], f.ids[3], f.ids[10]));

		Assert.assertEquals("wrong number of links.", 14, network.getLinks().size());
		Assert.assertEquals("wrong number of nodes.", 10, network.getNodes().size());

		MultimodalNetworkCleaner cleaner = new MultimodalNetworkCleaner((NetworkImpl) network);

		cleaner.run(EnumSet.of(TransportMode.car, TransportMode.walk));
		Assert.assertEquals("wrong number of links.", 12, network.getLinks().size());
		Assert.assertEquals("wrong number of nodes.", 9, network.getNodes().size());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[1]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[2]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[3]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[4]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[5]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[6]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[7]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[8]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[9]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[10]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[11]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[12]).getAllowedModes());
	}

	@Test
	public void testRun_multipleModes_doubleSource() {
		Fixture f = new MultimodeFixture();
		Network network = f.scenario.getNetwork();

		network.addNode(network.getFactory().createNode(f.ids[10], f.scenario.createCoord(200, 200)));
		network.addLink(network.getFactory().createLink(f.ids[18], f.ids[10], f.ids[2]));
		network.addLink(network.getFactory().createLink(f.ids[19], f.ids[10], f.ids[3]));

		Assert.assertEquals("wrong number of links.", 14, network.getLinks().size());
		Assert.assertEquals("wrong number of nodes.", 10, network.getNodes().size());

		MultimodalNetworkCleaner cleaner = new MultimodalNetworkCleaner((NetworkImpl) network);

		cleaner.run(EnumSet.of(TransportMode.car, TransportMode.walk));
		Assert.assertEquals("wrong number of links.", 12, network.getLinks().size());
		Assert.assertEquals("wrong number of nodes.", 9, network.getNodes().size());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[1]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[2]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[3]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[4]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[5]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[6]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[7]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[8]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[9]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[10]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[11]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[12]).getAllowedModes());
	}

	@Test
	public void testRun_emptyModes() {
		Fixture f = new Fixture();
		Network network = f.scenario.getNetwork();

		MultimodalNetworkCleaner cleaner = new MultimodalNetworkCleaner((NetworkImpl) network);

		cleaner.run(EnumSet.noneOf(TransportMode.class));
		// nothing should have changed from the initialization
		Assert.assertEquals("wrong number of links.", 8, network.getLinks().size());
		Assert.assertEquals("wrong number of nodes.", 6, network.getNodes().size());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[1]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[2]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[3]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[4]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[5]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[6]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[7]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[8]).getAllowedModes());
	}

	@Test
	public void testRun_unknownMode() {
		Fixture f = new Fixture();
		Network network = f.scenario.getNetwork();

		MultimodalNetworkCleaner cleaner = new MultimodalNetworkCleaner((NetworkImpl) network);

		cleaner.run(EnumSet.of(TransportMode.pt));
		// nothing should have changed from the initialization
		Assert.assertEquals("wrong number of links.", 8, network.getLinks().size());
		Assert.assertEquals("wrong number of nodes.", 6, network.getNodes().size());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[1]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[2]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[3]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[4]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[5]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[6]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[7]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[8]).getAllowedModes());
	}

	@Test
	public void testRun_singleLinkNetwork() {
		Scenario scenario = new ScenarioImpl();
		Network network = scenario.getNetwork();
		NetworkFactory factory = network.getFactory();

		Id id1 = scenario.createId("1");
		Id id2 = scenario.createId("2");

		network.addNode(factory.createNode(id1, scenario.createCoord(  0, 100)));
		network.addNode(factory.createNode(id2, scenario.createCoord(100, 100)));
		network.addLink(factory.createLink(id1, id1, id2));
		network.getLinks().get(id1).setAllowedModes(EnumSet.of(TransportMode.car));

		new MultimodalNetworkCleaner((NetworkImpl) network).run(EnumSet.of(TransportMode.car));
		/* a single link is no complete network, as the link's
		 * from-node cannot be reached by the link's to-node
		 * */
		Assert.assertEquals("wrong number of links.", 0, network.getLinks().size());
		Assert.assertEquals("wrong number of nodes.", 0, network.getNodes().size());
	}

	/**
	 * Creates a simple, multi-modal network as the basis for tests.
	 * <pre>
	 *
	 *   (1)------1------->(2)-------2------>(3)
	 *    ^       c        | ^       w        |
	 *    |                | |                |
	 *    |               c4 |                |
	 *    3c               | |                6w
	 *    |                | 5w               |
	 *    |                | |                |
	 *    |                v |                v
	 *   (4)<------7-------(5)<-------8------(6)
	 *             c                  w
	 *
	 * Legend: c = car, w = walk
	 *
	 * </pre>
	 *
	 * @author mrieser
	 */
	private static class Fixture {

		/*package*/ final Scenario scenario = new ScenarioImpl();
		/*package*/ final Id[] ids = new Id[21];
		/*package*/ final Set<TransportMode> modesC = EnumSet.of(TransportMode.car);
		/*package*/ final Set<TransportMode> modesW = EnumSet.of(TransportMode.walk);
		/*package*/ final Set<TransportMode> modesCW = EnumSet.of(TransportMode.car, TransportMode.walk);

		/*package*/ Fixture() {

			for (int i = 0; i < this.ids.length; i++) {
				this.ids[i] = this.scenario.createId(Integer.toString(i));
			}

			Network network = this.scenario.getNetwork();
			NetworkFactory factory = network.getFactory();
			network.addNode(factory.createNode(this.ids[1], this.scenario.createCoord(  0, 100)));
			network.addNode(factory.createNode(this.ids[2], this.scenario.createCoord(100, 100)));
			network.addNode(factory.createNode(this.ids[3], this.scenario.createCoord(200, 100)));
			network.addNode(factory.createNode(this.ids[4], this.scenario.createCoord(  0, 100)));
			network.addNode(factory.createNode(this.ids[5], this.scenario.createCoord(100,   0)));
			network.addNode(factory.createNode(this.ids[6], this.scenario.createCoord(200,   0)));
			network.addLink(factory.createLink(this.ids[1], this.ids[1], this.ids[2]));
			network.addLink(factory.createLink(this.ids[2], this.ids[2], this.ids[3]));
			network.addLink(factory.createLink(this.ids[3], this.ids[4], this.ids[1]));
			network.addLink(factory.createLink(this.ids[4], this.ids[2], this.ids[5]));
			network.addLink(factory.createLink(this.ids[5], this.ids[5], this.ids[2]));
			network.addLink(factory.createLink(this.ids[6], this.ids[3], this.ids[6]));
			network.addLink(factory.createLink(this.ids[7], this.ids[5], this.ids[4]));
			network.addLink(factory.createLink(this.ids[8], this.ids[6], this.ids[5]));
			network.getLinks().get(this.ids[1]).setAllowedModes(this.modesC);
			network.getLinks().get(this.ids[2]).setAllowedModes(this.modesW);
			network.getLinks().get(this.ids[3]).setAllowedModes(this.modesC);
			network.getLinks().get(this.ids[4]).setAllowedModes(this.modesC);
			network.getLinks().get(this.ids[5]).setAllowedModes(this.modesW);
			network.getLinks().get(this.ids[6]).setAllowedModes(this.modesW);
			network.getLinks().get(this.ids[7]).setAllowedModes(this.modesC);
			network.getLinks().get(this.ids[8]).setAllowedModes(this.modesW);
		}
	}

	/**
	 * Extends Fixture to create the following network for multiple modes tests:
 	 * <pre>
	 *
	 *   (1)------1------->(2)-------2------>(3)
	 *    ^       c        | ^       w        |
	 *    |                | |                |
	 *    |               c4 |                |
	 *    3c               | |                6w
	 *    |                | 5w               |
	 *    |                | |                |
	 *    |                v |                v
	 *   (4)<------7-------(5)<-------8------(6)
	 *    ^        c                  w       |
	 *    |                                   |
	 *    |                                   |
	 *    9c                                 10w
	 *    |                                   |
	 *    |                                   |
	 *    |                                   v
	 *   (7)<-----11-------(8)<------12------(9)
	 *             c                  w
	 *
	 * Legend: c = car, w = walk
	 *
	 * </pre>
	 *
	 * @author mrieser
	 */
	private static class MultimodeFixture extends Fixture {
		/*package*/ MultimodeFixture() {
			super();
			Network network = this.scenario.getNetwork();
			NetworkFactory factory = network.getFactory();
			network.addNode(factory.createNode(this.ids[7], this.scenario.createCoord(  0, -100)));
			network.addNode(factory.createNode(this.ids[8], this.scenario.createCoord(100, -100)));
			network.addNode(factory.createNode(this.ids[9], this.scenario.createCoord(200, -100)));

			network.addLink(factory.createLink(this.ids[ 9], this.ids[7], this.ids[4]));
			network.addLink(factory.createLink(this.ids[10], this.ids[6], this.ids[9]));
			network.addLink(factory.createLink(this.ids[11], this.ids[8], this.ids[7]));
			network.addLink(factory.createLink(this.ids[12], this.ids[9], this.ids[8]));
			network.getLinks().get(this.ids[ 9]).setAllowedModes(this.modesC);
			network.getLinks().get(this.ids[10]).setAllowedModes(this.modesW);
			network.getLinks().get(this.ids[11]).setAllowedModes(this.modesC);
			network.getLinks().get(this.ids[12]).setAllowedModes(this.modesW);
		}
	}
}
