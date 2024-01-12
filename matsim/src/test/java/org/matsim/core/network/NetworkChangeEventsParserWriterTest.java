/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkChangeEventsParserWriterTest.java
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

package org.matsim.core.network;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.io.NetworkChangeEventsParser;
import org.matsim.core.network.io.NetworkChangeEventsWriter;
import org.matsim.core.utils.misc.CRCChecksum;
import org.matsim.testcases.MatsimTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class NetworkChangeEventsParserWriterTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils() ;

	@Test
	void testChangeEventsParserWriter() {
		String input = utils.getInputDirectory() + "testNetworkChangeEvents.xml";
		String output  = utils.getOutputDirectory() + "outputTestNetworkChangeEvents.xml";
		final Network network = new NetworkImpl(new VariableIntervalTimeVariantLinkFactory());
		Node node1 = NetworkUtils.createAndAddNode(network, Id.create("1", Node.class), new Coord((double) 0, (double) 0));
		Node node2 = NetworkUtils.createAndAddNode(network, Id.create("2", Node.class), new Coord((double) 0, (double) 1000));
		Node node3 = NetworkUtils.createAndAddNode(network, Id.create("3", Node.class), new Coord((double) 1000, (double) 2000));
        NetworkUtils.createAndAddLink(network,Id.create("1", Link.class), node1, node2, (double) 1000, 1.667, (double) 3600, (double) 1 );
        NetworkUtils.createAndAddLink(network,Id.create("2", Link.class), node2, node3, (double) 1500, 1.667, (double) 3600, (double) 1 );

		List<NetworkChangeEvent> events = new ArrayList<>();
		NetworkChangeEventsParser parser = new NetworkChangeEventsParser(network, events );
		parser.readFile(input);
		new NetworkChangeEventsWriter().write(output, events);

		long checksum_ref = CRCChecksum.getCRCFromFile(input);
		long checksum_run = CRCChecksum.getCRCFromFile(output);
		assertEquals(checksum_ref, checksum_run);
	}

	@Test
	void testWriteChangeEventWithoutLinkDoesntWork() {
		assertThrows(Exception.class, () -> {
			final String fileName = utils.getOutputDirectory() + "wurst.xml";

			List<NetworkChangeEvent> events = new ArrayList<>();
			final NetworkChangeEvent e = new NetworkChangeEvent(0.0);
			e.setFlowCapacityChange(new NetworkChangeEvent.ChangeValue(NetworkChangeEvent.ChangeType.ABSOLUTE_IN_SI_UNITS, 10));
			events.add(e);
			new NetworkChangeEventsWriter().write(fileName, events);
		});
	}

	@Test
	void testWriteChangeEventWithSmallValueAndReadBack() {
		final String fileName = utils.getOutputDirectory() + "wurst.xml";

		final Network network = NetworkUtils.createNetwork();
        Node node1 = NetworkUtils.createAndAddNode(network, Id.create("1", Node.class), new Coord((double) 0, (double) 0));
		Node node2 = NetworkUtils.createAndAddNode(network, Id.create("2", Node.class), new Coord((double) 0, (double) 1000));
		final Link link = NetworkUtils.createAndAddLink(network, Id.create("2", Link.class), node1, node2, (double) 1500, 1.667, (double) 3600, (double) 1);

		List<NetworkChangeEvent> outputEvents = new ArrayList<>();
		final NetworkChangeEvent event = new NetworkChangeEvent(0.0);
		event.setFlowCapacityChange(new NetworkChangeEvent.ChangeValue(NetworkChangeEvent.ChangeType.ABSOLUTE_IN_SI_UNITS, 0.000000000004));
		event.addLink(link);
		outputEvents.add(event);
		new NetworkChangeEventsWriter().write(fileName, outputEvents);

		List<NetworkChangeEvent> inputEvents = new ArrayList<>();
		NetworkChangeEventsParser parser = new NetworkChangeEventsParser(network, inputEvents);
		parser.readFile(fileName);

		assertThat(inputEvents, hasItem(event));
	}

	// see MATSIM-770
	@Test
	void testWriteReadZeroChangeEvents() {
		final String fileName = this.utils.getOutputDirectory() + "zeroChanges.xml";
		List<NetworkChangeEvent> changeEvents = new ArrayList<>();
		new NetworkChangeEventsWriter().write(fileName, changeEvents);

        Network network = NetworkUtils.createNetwork();
        List<NetworkChangeEvent> changeEvents2 = new ArrayList<>();
		new NetworkChangeEventsParser(network, changeEvents2).readFile(fileName);

		// the main test is that there is no exception
		Assertions.assertTrue(changeEvents2.isEmpty());
	}

	@Test
	void testAbsoluteChangeEvents() {
        final Network network = NetworkUtils.createNetwork();
        Node node1 = NetworkUtils.createAndAddNode(network, Id.create("1", Node.class), new Coord((double) 0, (double) 0));
		Node node2 = NetworkUtils.createAndAddNode(network, Id.create("2", Node.class), new Coord((double) 0, (double) 1000));
		final Link link = NetworkUtils.createAndAddLink(network, Id.create("2", Link.class), node1, node2, (double) 1500, 1.667, (double) 3600, (double) 1);

		final String fileName = this.utils.getOutputDirectory() + "absoluteChanges.xml";
		List<NetworkChangeEvent> changeEvents = new ArrayList<>();

		final NetworkChangeEvent event = new NetworkChangeEvent(100.0);
		event.setFlowCapacityChange(new NetworkChangeEvent.ChangeValue(NetworkChangeEvent.ChangeType.ABSOLUTE_IN_SI_UNITS, 0.000000000005));
		event.addLink(link);
		changeEvents.add(event);

		new NetworkChangeEventsWriter().write(fileName, changeEvents);

		List<NetworkChangeEvent> changeEvents2 = new ArrayList<>();
		new NetworkChangeEventsParser(network, changeEvents2).readFile(fileName);

		Assertions.assertFalse(changeEvents2.isEmpty());
		Assertions.assertEquals(1, changeEvents2.size());
		NetworkChangeEvent event2 = changeEvents2.get(0);
		Assertions.assertEquals(event.getStartTime(), event2.getStartTime(), 0.0);
		Assertions.assertEquals(NetworkChangeEvent.ChangeType.ABSOLUTE_IN_SI_UNITS, event2.getFlowCapacityChange().getType());
		Assertions.assertEquals(event.getFlowCapacityChange().getValue(), event2.getFlowCapacityChange().getValue(), 1e-10);
	}

	@Test
	void testScaleFactorChangeEvents() {
        final Network network = NetworkUtils.createNetwork();
        Node node1 = NetworkUtils.createAndAddNode(network, Id.create("1", Node.class), new Coord((double) 0, (double) 0));
		Node node2 = NetworkUtils.createAndAddNode(network, Id.create("2", Node.class), new Coord((double) 0, (double) 1000));
		final Link link = NetworkUtils.createAndAddLink(network, Id.create("2", Link.class), node1, node2, (double) 1500, 1.667, (double) 3600, (double) 1);

		final String fileName = this.utils.getOutputDirectory() + "scalefactorChanges.xml";
		List<NetworkChangeEvent> changeEvents = new ArrayList<>();

		final NetworkChangeEvent event = new NetworkChangeEvent(200.0);
		event.setLanesChange(new NetworkChangeEvent.ChangeValue(NetworkChangeEvent.ChangeType.FACTOR, 2.5));
		event.addLink(link);
		changeEvents.add(event);

		new NetworkChangeEventsWriter().write(fileName, changeEvents);

		List<NetworkChangeEvent> changeEvents2 = new ArrayList<>();
		new NetworkChangeEventsParser(network, changeEvents2).readFile(fileName);

		Assertions.assertFalse(changeEvents2.isEmpty());
		Assertions.assertEquals(1, changeEvents2.size());
		NetworkChangeEvent event2 = changeEvents2.get(0);
		Assertions.assertEquals(event.getStartTime(), event2.getStartTime(), 0.0);
		Assertions.assertEquals(NetworkChangeEvent.ChangeType.FACTOR, event2.getLanesChange().getType());
		Assertions.assertEquals(event.getLanesChange().getValue(), event2.getLanesChange().getValue(), 1e-10);
	}

	@Test
	void testPositiveOffsetChangeEvents() {
        final Network network = NetworkUtils.createNetwork();
        Node node1 = NetworkUtils.createAndAddNode(network, Id.create("1", Node.class), new Coord((double) 0, (double) 0));
		Node node2 = NetworkUtils.createAndAddNode(network, Id.create("2", Node.class), new Coord((double) 0, (double) 1000));
		final Link link = NetworkUtils.createAndAddLink(network, Id.create("2", Link.class), node1, node2, (double) 1500, 1.667, (double) 3600, (double) 1);

		final String fileName = this.utils.getOutputDirectory() + "offsetChanges.xml";
		List<NetworkChangeEvent> changeEvents = new ArrayList<>();

		final NetworkChangeEvent event = new NetworkChangeEvent(300.0);
		event.setFreespeedChange(new NetworkChangeEvent.ChangeValue(NetworkChangeEvent.ChangeType.OFFSET_IN_SI_UNITS, +3.6));
		event.addLink(link);
		changeEvents.add(event);

		new NetworkChangeEventsWriter().write(fileName, changeEvents);

		List<NetworkChangeEvent> changeEvents2 = new ArrayList<>();
		new NetworkChangeEventsParser(network, changeEvents2).readFile(fileName);

		Assertions.assertFalse(changeEvents2.isEmpty());
		Assertions.assertEquals(1, changeEvents2.size());
		NetworkChangeEvent event2 = changeEvents2.get(0);
		Assertions.assertEquals(event.getStartTime(), event2.getStartTime(), 0.0);
		Assertions.assertEquals(NetworkChangeEvent.ChangeType.OFFSET_IN_SI_UNITS, event2.getFreespeedChange().getType());
		Assertions.assertEquals(event.getFreespeedChange().getValue(), event2.getFreespeedChange().getValue(), 1e-10);
	}

	@Test
	void testNegativeOffsetChangeEvents() {
        final Network network = NetworkUtils.createNetwork();
        Node node1 = NetworkUtils.createAndAddNode(network, Id.create("1", Node.class), new Coord((double) 0, (double) 0));
		Node node2 = NetworkUtils.createAndAddNode(network, Id.create("2", Node.class), new Coord((double) 0, (double) 1000));
		final Link link = NetworkUtils.createAndAddLink(network, Id.create("2", Link.class), node1, node2, (double) 1500, 1.667, (double) 3600, (double) 1);

		final String fileName = this.utils.getOutputDirectory() + "offsetChanges.xml";
		List<NetworkChangeEvent> changeEvents = new ArrayList<>();

		final NetworkChangeEvent event = new NetworkChangeEvent(300.0);
		event.setFreespeedChange(new NetworkChangeEvent.ChangeValue(NetworkChangeEvent.ChangeType.OFFSET_IN_SI_UNITS, -3.6));
		event.addLink(link);
		changeEvents.add(event);

		new NetworkChangeEventsWriter().write(fileName, changeEvents);

		List<NetworkChangeEvent> changeEvents2 = new ArrayList<>();
		new NetworkChangeEventsParser(network, changeEvents2).readFile(fileName);

		Assertions.assertFalse(changeEvents2.isEmpty());
		Assertions.assertEquals(1, changeEvents2.size());
		NetworkChangeEvent event2 = changeEvents2.get(0);
		Assertions.assertEquals(event.getStartTime(), event2.getStartTime(), 0.0);
		Assertions.assertEquals(NetworkChangeEvent.ChangeType.OFFSET_IN_SI_UNITS, event2.getFreespeedChange().getType());
		Assertions.assertEquals(event.getFreespeedChange().getValue(), event2.getFreespeedChange().getValue(), 1e-10);
	}

}
