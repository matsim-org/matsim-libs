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

import java.io.IOException;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkChangeEventsParser;
import org.matsim.core.network.NetworkChangeEventsWriter;
import org.matsim.core.network.NetworkFactoryImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.TimeVariantLinkFactory;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.misc.CRCChecksum;
import org.matsim.testcases.MatsimTestCase;
import org.xml.sax.SAXException;

public class NetworkChangeEventsParserWriterTest  extends MatsimTestCase{

	public void testChangeEventsParserWriter(){
		String input = getInputDirectory() + "testNetworkChangeEvents.xml";
		String output  = getOutputDirectory() + "outputTestNetworkChangeEvents.xml";
		NetworkFactoryImpl nf = new NetworkFactoryImpl();
		nf.setLinkFactory(new TimeVariantLinkFactory());
		final NetworkLayer network = new NetworkLayer(nf);
		NodeImpl node1 = network.createAndAddNode(new IdImpl("1"), new CoordImpl(0, 0));
		NodeImpl node2 = network.createAndAddNode(new IdImpl("2"), new CoordImpl(0, 1000));
		NodeImpl node3 = network.createAndAddNode(new IdImpl("3"), new CoordImpl(1000, 2000));
		network.createAndAddLink(new IdImpl("1"), node1, node2, 1000, 1.667, 3600, 1);
		network.createAndAddLink(new IdImpl("2"), node2, node3, 1500, 1.667, 3600, 1);

		NetworkChangeEventsParser parser = new NetworkChangeEventsParser(network);
		try {
			parser.parse(input);
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		List<NetworkChangeEvent> events  = parser.getEvents();
		new NetworkChangeEventsWriter().write(output, events);

		long checksum_ref = CRCChecksum.getCRCFromFile(input);
		long checksum_run = CRCChecksum.getCRCFromFile(output);
		assertEquals(checksum_ref, checksum_run);

	}
}
