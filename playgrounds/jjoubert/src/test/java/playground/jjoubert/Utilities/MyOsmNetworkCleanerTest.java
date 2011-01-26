/* *********************************************************************** *
 * project: org.matsim.*
 * MyOsmNetworkCleanerTest.java
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

package playground.jjoubert.Utilities;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.testcases.MatsimTestCase;
import org.xml.sax.SAXException;

import playground.jjoubert.Utilities.MyOsmNetworkCleaner;
import playground.jjoubert.Utilities.MyShapefileReader;

import com.vividsolutions.jts.geom.MultiPolygon;


public class MyOsmNetworkCleanerTest extends MatsimTestCase{

	/**
	 * Test to check that the method keeps all (and only) links that have at
	 * least one of its nodes within a given polygon.
	 * @throws IOException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 */
	public void testCleanNetwork() throws SAXException, ParserConfigurationException, IOException{
		MyShapefileReader msr = new MyShapefileReader(getInputDirectory() + "Test.shp");
		MultiPolygon mp = msr.readMultiPolygon();

		assertEquals("Polygon is not a square.", 5, mp.getNumPoints());

		Scenario sc = new ScenarioImpl();
		MatsimNetworkReader nr = new MatsimNetworkReader(sc);
		nr.parse(getOutputDirectory() + "network.xml.gz");

		assertEquals("Network must have 4 links.", 4, sc.getNetwork().getLinks().size());

		MyOsmNetworkCleaner monc = new MyOsmNetworkCleaner();
		monc.cleanNetwork(sc.getNetwork(), mp);
		assertEquals("Wrong number of remaining links.", 2, monc.getNewNetwork().getLinks().size());

		Network n = monc.getNewNetwork();
		assertEquals("Link #2 not in network.", true, n.getLinks().containsKey(new IdImpl("2")));
		assertEquals("Link #3 not in network.", true, n.getLinks().containsKey(new IdImpl("3")));
	}

	/**
	 * Test to check that a new network can only be returned once the
	 * {@link MyOsmNetworkCleaner#cleanNetwork(Network, MultiPolygon)} method
	 * has been called. Otherwise <code>null</code> is returned.
	 * @throws IOException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 */
	public void testGetNewNetwork() throws SAXException, ParserConfigurationException, IOException{
		MyOsmNetworkCleaner monc = new MyOsmNetworkCleaner();
		assertNull("No cleaned network should exist.", monc.getNewNetwork());

		//TODO: Add bit to check not null network.

		MyShapefileReader msr = new MyShapefileReader(getInputDirectory() + "Test.shp");
		MultiPolygon mp = msr.readMultiPolygon();

		Scenario sc = new ScenarioImpl();
		MatsimNetworkReader nr = new MatsimNetworkReader(sc);
		nr.parse(getOutputDirectory() + "network.xml.gz");

		monc.cleanNetwork(sc.getNetwork(), mp);
		assertNotNull("Cleaned network should exists.", monc.getNewNetwork());
	}

	/**
	 * Overwrite the basic setUp() method so that a new MATSim network is created.
	 * @throws Exception
	 */
	@Override
	public void setUp() throws Exception {
		// this method is automatically called by JUnit, no need to call it manually
		super.setUp();

		// Build the test network.
		Scenario sc = new ScenarioImpl();
		Network n = sc.getNetwork();
		NetworkFactory nf = n.getFactory();

		// Link 1.
		n.addNode(nf.createNode(new IdImpl("1"), new CoordImpl(0.0, 5.0)));
		n.addNode(nf.createNode(new IdImpl("2"), new CoordImpl(11.0, 5.0)));
		n.addLink(nf.createLink(new IdImpl("1"), new IdImpl("1"), new IdImpl("2")));
		// Link 2.
		n.addNode(nf.createNode(new IdImpl("3"), new CoordImpl(6.0, 2.0)));
		n.addNode(nf.createNode(new IdImpl("4"), new CoordImpl(6.0, 7.0)));
		n.addLink(nf.createLink(new IdImpl("2"), new IdImpl("3"), new IdImpl("4")));
		// Link 3.
		n.addNode(nf.createNode(new IdImpl("5"), new CoordImpl(6.0, 12.0)));
		n.addLink(nf.createLink(new IdImpl("3"), new IdImpl("4"), new IdImpl("5")));
		// Link 4.
		n.addNode(nf.createNode(new IdImpl("6"), new CoordImpl(8.0, 12.0)));
		n.addLink(nf.createLink(new IdImpl("4"), new IdImpl("5"), new IdImpl("6")));

		// Write network to file.
		NetworkWriter nw = new NetworkWriter(n);
		nw.write(getOutputDirectory() + "network.xml.gz");

	}


}
