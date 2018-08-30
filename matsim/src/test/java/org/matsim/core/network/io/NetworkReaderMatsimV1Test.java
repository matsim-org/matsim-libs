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

package org.matsim.core.network.io;

import java.util.Set;
import java.util.Stack;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.NetworkReaderMatsimV1;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.testcases.utils.AttributesBuilder;
import org.xml.sax.Attributes;

public class NetworkReaderMatsimV1Test extends MatsimTestCase {

	/**
	 * @author mrieser
	 */
	public void testAllowedModes_singleMode() {
		Link link = prepareTestAllowedModes("car");
		Set<String> modes = link.getAllowedModes();
		assertEquals("wrong number of allowed modes.", 1, modes.size());
		assertTrue("wrong mode.", modes.contains(TransportMode.car));

		// make sure we do not just get some default-value back...
		link = prepareTestAllowedModes("bike");
		modes = link.getAllowedModes();
		assertEquals("wrong number of allowed modes.", 1, modes.size());
		assertTrue("wrong mode.", modes.contains(TransportMode.bike));
	}

	/**
	 * @author mrieser
	 */
	public void testAllowedModes_emptyMode() {
		Link link = prepareTestAllowedModes("");
		Set<String> modes = link.getAllowedModes();
		assertEquals("wrong number of allowed modes.", 0, modes.size());
	}

	/**
	 * @author mrieser
	 */
	public void testAllowedModes_multipleModes() {
		Link link = prepareTestAllowedModes("car,bus");
		Set<String> modes = link.getAllowedModes();
		assertEquals("wrong number of allowed modes.", 2, modes.size());
		assertTrue("wrong mode.", modes.contains(TransportMode.car));
		assertTrue("wrong mode.", modes.contains("bus"));

		link = prepareTestAllowedModes("bike,bus,walk");
		modes = link.getAllowedModes();
		assertEquals("wrong number of allowed modes.", 3, modes.size());
		assertTrue("wrong mode.", modes.contains(TransportMode.bike));
		assertTrue("wrong mode.", modes.contains("bus"));
		assertTrue("wrong mode.", modes.contains(TransportMode.walk));

		link = prepareTestAllowedModes("pt, train"); // test with space after comma
		modes = link.getAllowedModes();
		assertEquals("wrong number of allowed modes.", 2, modes.size());
		assertTrue("wrong mode.", modes.contains(TransportMode.pt));
		assertTrue("wrong mode.", modes.contains("train"));
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
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network network = scenario.getNetwork();
		network.addNode(network.getFactory().createNode(Id.create("1", Node.class), new Coord((double) 0, (double) 0)));
		network.addNode(network.getFactory().createNode(Id.create("2", Node.class), new Coord((double) 1000, (double) 0)));

		NetworkReaderMatsimV1 reader = new NetworkReaderMatsimV1(scenario.getNetwork());
		Stack<String> context = new Stack<String>();
		Attributes atts = new AttributesBuilder().
				add("id", "1").
				add("from", "1").
				add("to", "2").
				add("length", "1000").
				add("freespeed", "10").
				add("capacity", "3600").
				add("permlanes", "1").
				add("modes", modes).  // specific test setup
				get();

		reader.startTag("link", atts, context);

		// start test
		assertEquals("expected one link.", 1, network.getLinks().size());
		Link link = network.getLinks().get(Id.create("1", Link.class));
		assertNotNull("expected link with id=1.", link);

		return link;
	}
}
