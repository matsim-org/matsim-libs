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

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.io.NetworkChangeEventsParser;
import org.matsim.core.network.io.NetworkChangeEventsWriter;
import org.matsim.core.utils.misc.CRCChecksum;
import org.matsim.testcases.MatsimTestCase;

public class NetworkChangeEventsParserWriterTest  extends MatsimTestCase{

	public void testChangeEventsParserWriter(){
		String input = getInputDirectory() + "testNetworkChangeEvents.xml";
		String output  = getOutputDirectory() + "outputTestNetworkChangeEvents.xml";
		final Network network = NetworkUtils.createNetwork();
		NetworkFactory nf = network.getFactory();
		nf.setLinkFactory(new VariableIntervalTimeVariantLinkFactory());
		((NetworkImpl)network).setFactory(nf);
		Node node1 = NetworkUtils.createAndAddNode(network, Id.create("1", Node.class), new Coord((double) 0, (double) 0));
		Node node2 = NetworkUtils.createAndAddNode(network, Id.create("2", Node.class), new Coord((double) 0, (double) 1000));
		Node node3 = NetworkUtils.createAndAddNode(network, Id.create("3", Node.class), new Coord((double) 1000, (double) 2000));
		final Node fromNode = node1;
		final Node toNode = node2;
		NetworkUtils.createAndAddLink(network,Id.create("1", Link.class), fromNode, toNode, (double) 1000, 1.667, (double) 3600, (double) 1 );
		final Node fromNode1 = node2;
		final Node toNode1 = node3;
		NetworkUtils.createAndAddLink(network,Id.create("2", Link.class), fromNode1, toNode1, (double) 1500, 1.667, (double) 3600, (double) 1 );

		List<NetworkChangeEvent> events = new ArrayList<>() ;
		NetworkChangeEventsParser parser = new NetworkChangeEventsParser(network, events );
		parser.readFile(input);
		new NetworkChangeEventsWriter().write(output, events);

		long checksum_ref = CRCChecksum.getCRCFromFile(input);
		long checksum_run = CRCChecksum.getCRCFromFile(output);
		assertEquals(checksum_ref, checksum_run);

	}
}
