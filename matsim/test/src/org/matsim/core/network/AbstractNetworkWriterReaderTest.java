/* *********************************************************************** *
 * project: org.matsim.*
 * AbstractNetworkWriterReaderTest.java
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

import java.io.File;

import org.matsim.api.basic.v01.population.BasicLeg;
import org.matsim.core.api.network.Link;
import org.matsim.core.api.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.testcases.MatsimTestCase;

/**
 * An abstract class that tests if certain features of networks (e.g. specific
 * settings/attributes for links or nodes) are written to file and afterwards
 * correctly read in. The class is abstract so the tests can be reused with
 * different file formats.
 *
 * @author mrieser
 */
public abstract class AbstractNetworkWriterReaderTest extends MatsimTestCase {

	/**
	 * Writes the given network to the specified file.
	 * 
	 * @param network
	 * @param filename
	 */
	protected abstract void writeNetwork(final NetworkLayer network, final String filename);
	
	/**
	 * Reads a network from the specified file into the given network data structure.
	 * 
	 * @param network
	 * @param filename
	 */
	protected abstract void readNetwork(final NetworkLayer network, final String filename);	
	public void testAllowedModes_multipleModes() {
		doTestAllowedModes(new BasicLeg.Mode[] {BasicLeg.Mode.bus, BasicLeg.Mode.train}, 
				getOutputDirectory() + "network.xml");
	}

	public void testAllowedModes_singleMode() {
		doTestAllowedModes(new BasicLeg.Mode[] {BasicLeg.Mode.miv}, 
				getOutputDirectory() + "network.xml");
	}

	public void testAllowedModes_noMode() {
		doTestAllowedModes(new BasicLeg.Mode[] {}, 
				getOutputDirectory() + "network.xml");
	}

	private void doTestAllowedModes(final BasicLeg.Mode[] modes, final String filename) {
		NetworkLayer network1 = new NetworkLayer();
		Node n1 = network1.createNode(new IdImpl("1"), new CoordImpl(0, 0));
		Node n2 = network1.createNode(new IdImpl("2"), new CoordImpl(1000, 0));
		Link l1 = network1.createLink(new IdImpl("1"), n1, n2, 1000.0, 10.0, 3600.0, 1.0);
		l1.setAllowedModes(modes);
		
		writeNetwork(network1, filename);
		
		File networkFile = new File(filename);
		assertTrue("written network file doesn't exist.", networkFile.exists());
		assertTrue("written network file seems to be empty.", networkFile.length() > 0);
		
		NetworkLayer network2 = new NetworkLayer();
		readNetwork(network2, filename);
		
		Link link1 = network2.getLinks().get(new IdImpl("1"));
		assertNotNull("link not found in read-in network.", link1);
		
		BasicLeg.Mode[] modes2 = link1.getAllowedModes();
		assertEquals("wrong number of allowed modes.", modes.length, modes2.length);
		for (int i = 0, n = modes.length; i < n; i++) {
			assertEquals("wrong mode.", modes[i], modes2[i]);
		}
	}
}
