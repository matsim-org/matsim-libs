/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkReaderMatsimV1Test.java
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

import java.util.Stack;

import org.matsim.api.basic.v01.TransportMode;
import org.matsim.core.api.network.Link;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.testcases.MatsimTestCase;
import org.xml.sax.helpers.AttributesImpl;

public class NetworkReaderMatsimV1Test extends MatsimTestCase {

	/**
	 * @author mrieser
	 */
	public void testAllowedModes_singleMode() {
		Link link = prepareTestAllowedModes("car");
		TransportMode[] modes = link.getAllowedModes();
		assertEquals("wrong number of allowed modes.", 1, modes.length);
		assertEquals("wrong mode.", TransportMode.car, modes[0]);

		// make sure we do not just get some default-value back...
		link = prepareTestAllowedModes("bike");
		modes = link.getAllowedModes();
		assertEquals("wrong number of allowed modes.", 1, modes.length);
		assertEquals("wrong mode.", TransportMode.bike, modes[0]);
	}

	/**
	 * @author mrieser
	 */
	public void testAllowedModes_emptyMode() {
		Link link = prepareTestAllowedModes("");
		TransportMode[] modes = link.getAllowedModes();
		assertEquals("wrong number of allowed modes.", 0, modes.length);
	}
	
	/**
	 * @author mrieser
	 */
	public void testAllowedModes_multipleModes() {
		Link link = prepareTestAllowedModes("car,bus");
		TransportMode[] modes = link.getAllowedModes();
		assertEquals("wrong number of allowed modes.", 2, modes.length);
		assertEquals("wrong mode.", TransportMode.car, modes[0]);
		assertEquals("wrong mode.", TransportMode.bus, modes[1]);

		link = prepareTestAllowedModes("bike,bus,walk");
		modes = link.getAllowedModes();
		assertEquals("wrong number of allowed modes.", 3, modes.length);
		assertEquals("wrong mode.", TransportMode.bike, modes[0]);
		assertEquals("wrong mode.", TransportMode.bus, modes[1]);
		assertEquals("wrong mode.", TransportMode.walk, modes[2]);

		link = prepareTestAllowedModes("pt, train"); // test with space after comma
		modes = link.getAllowedModes();
		assertEquals("wrong number of allowed modes.", 2, modes.length);
		assertEquals("wrong mode.", TransportMode.pt, modes[0]);
		assertEquals("wrong mode.", TransportMode.train, modes[1]);
	}

	/**
	 * Helper method to set up a fixture that is used more than once.
	 * It creates a single link by specifying the XML-Attributes and using
	 * {@link NetworkReaderMatsimV1} to "read" the attributes and create
	 * a link out of it.
	 * 
	 * @param modes The allowed transportation modes for the link 
	 * @return the created link
	 * 
	 * @author mrieser
	 */
	private Link prepareTestAllowedModes(final String modes) {
		NetworkLayer network = new NetworkLayer();
		network.createNode(new IdImpl("1"), new CoordImpl(0, 0));
		network.createNode(new IdImpl("2"), new CoordImpl(1000, 0));

		NetworkReaderMatsimV1 reader = new NetworkReaderMatsimV1(network);
		Stack<String> context = new Stack<String>();
		AttributesImpl atts = new AttributesImpl();
		atts.addAttribute(null, null, "id", null, "1");
		atts.addAttribute(null, null, "from", null, "1");
		atts.addAttribute(null, null, "to", null, "2");
		atts.addAttribute(null, null, "length", null, "1000");
		atts.addAttribute(null, null, "freespeed", null, "10");
		atts.addAttribute(null, null, "capacity", null, "3600");
		atts.addAttribute(null, null, "permlanes", null, "1");

		// specific test setup
		atts.addAttribute(null, null, "modes", null, modes);
		reader.startTag("link", atts, context);

		// start test
		assertEquals("expected one link.", 1, network.getLinks().size());
		Link link = network.getLink(new IdImpl("1"));
		assertNotNull("expected link with id=1.", link);
		
		return link;
	}
}
