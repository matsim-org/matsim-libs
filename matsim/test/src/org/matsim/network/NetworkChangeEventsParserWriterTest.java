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

package org.matsim.network;

import java.io.IOException;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.testcases.MatsimTestCase;
import org.matsim.utils.CRCChecksum;
import org.xml.sax.SAXException;

public class NetworkChangeEventsParserWriterTest  extends MatsimTestCase{

	public void testChangeEventsParserWriter(){
		String input = getInputDirectory() + "testNetworkChangeEvents.xml";
		String output  = getOutputDirectory() + "outputTestNetworkChangeEvents.xml";
		NetworkFactory nf = new NetworkFactory();
		nf.setLinkPrototype(TimeVariantLinkImpl.class);
		final NetworkLayer network = new NetworkLayer(nf);
		network.createNode("1", "0", "0", null);
		network.createNode("2", "0", "1000", null);
		network.createNode("3", "1000", "2000", null);
		network.createLink("1", "1", "2", "1000", "1.667", "3600", "1", null, null);
		network.createLink("2", "2", "3", "1500", "1.667", "3600", "1", null, null);
		
		
		
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
