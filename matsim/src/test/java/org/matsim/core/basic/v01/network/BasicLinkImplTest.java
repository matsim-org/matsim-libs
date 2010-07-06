/* *********************************************************************** *
 * project: org.matsim.*
 * BasicLinkImplTest.java
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

package org.matsim.core.basic.v01.network;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.testcases.MatsimTestCase;

public class BasicLinkImplTest extends MatsimTestCase {

	/**
	 * Tests setting and getting allowed modes for links.
	 *
	 * @author mrieser
	 */
	public void testAllowedModes() {
		NetworkLayer network = new NetworkLayer();
		Node n1 = network.createAndAddNode(new IdImpl(1), new CoordImpl(0, 0));
		Node n2 = network.createAndAddNode(new IdImpl(2), new CoordImpl(1000, 0));
		Link l = network.createAndAddLink(new IdImpl(1), n1, n2, 1000, 10, 3600, 1);

		// test default
		Set<String> modes = l.getAllowedModes();
		assertEquals("wrong number of default entries.", 1, modes.size());
		assertTrue("wrong default.", modes.contains(TransportMode.car));

		// test set/get empty list
		l.setAllowedModes(new HashSet<String>());
		modes = l.getAllowedModes();
		assertEquals("wrong number of allowed modes.", 0, modes.size());

		// test set/get list with entries
		modes = new HashSet<String>();
		modes.add(TransportMode.walk);
		modes.add(TransportMode.car);
		modes.add(TransportMode.bike);
		l.setAllowedModes(modes);
		modes = l.getAllowedModes();
		assertEquals("wrong number of allowed modes", 3, modes.size());
		assertTrue(modes.contains(TransportMode.walk));
		assertTrue(modes.contains(TransportMode.car));
		assertTrue(modes.contains(TransportMode.bike));
	}

}
