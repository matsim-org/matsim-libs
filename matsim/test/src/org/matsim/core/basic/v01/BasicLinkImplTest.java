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

package org.matsim.core.basic.v01;

import org.matsim.api.basic.v01.network.BasicLink;
import org.matsim.api.basic.v01.network.BasicNode;
import org.matsim.api.basic.v01.population.BasicLeg;
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
		BasicNode n1 = new BasicNodeImpl(new IdImpl(1), new CoordImpl(0, 0));
		BasicNode n2 = new BasicNodeImpl(new IdImpl(1), new CoordImpl(1000, 0));
		BasicLink l = new BasicLinkImpl(network, new IdImpl(1), n1, n2);
		
		// test default
		BasicLeg.Mode[] modes = l.getAllowedModes();
		assertEquals("wrong number of default entries.", 1, modes.length);
		assertEquals("wrong default.", BasicLeg.Mode.car, modes[0]);
		
		// test set/get empty list
		l.setAllowedModes(new BasicLeg.Mode[] {});
		modes = l.getAllowedModes();
		assertEquals("wrong number of allowed modes.", 0, modes.length);
		
		// test set/get list with entries
		l.setAllowedModes(new BasicLeg.Mode[] {BasicLeg.Mode.bus, BasicLeg.Mode.car, BasicLeg.Mode.bike});
		modes = l.getAllowedModes();
		assertEquals("wrong number of allowed modes", 3, modes.length);
		assertEquals(BasicLeg.Mode.bus, modes[0]);
		assertEquals(BasicLeg.Mode.car, modes[1]);
		assertEquals(BasicLeg.Mode.bike, modes[2]);

	}

}
