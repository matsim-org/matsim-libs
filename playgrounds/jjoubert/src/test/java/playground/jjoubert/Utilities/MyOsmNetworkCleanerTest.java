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

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestCase;
import org.xml.sax.SAXException;

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

		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimNetworkReader nr = new MatsimNetworkReader(sc);
		nr.parse(getOutputDirectory() + "network.xml.gz");

		assertEquals("Network must have 8 links.", 8, sc.getNetwork().getLinks().size());

		MyOsmNetworkCleaner monc = new MyOsmNetworkCleaner();
		monc.cleanNetwork(sc.getNetwork(), mp);
		/* The resulting network should have 5 nodes and 6 links before the 
		 * NetworkCleaner is invoked. The largest cluster, however, is the one
		 * with nodes 3, 4 & 5, and contains 3 nodes and 4 links. 
		 */
		assertEquals("Wrong number of remaining links.", 4, monc.getNewNetwork().getLinks().size());
		Network n = monc.getNewNetwork();
		assertEquals("Link 3-4 is not in network.", true, n.getLinks().containsKey(Id.create("34", Link.class)));
		assertEquals("Link 4-3 is not in network.", true, n.getLinks().containsKey(Id.create("43", Link.class)));
		assertEquals("Link 4-5 is not in network.", true, n.getLinks().containsKey(Id.create("45", Link.class)));
		assertEquals("Link 5-4 is not in network.", true, n.getLinks().containsKey(Id.create("54", Link.class)));
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

		MyShapefileReader msr = new MyShapefileReader(getInputDirectory() + "Test.shp");
		MultiPolygon mp = msr.readMultiPolygon();

		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimNetworkReader nr = new MatsimNetworkReader(sc);
		nr.parse(getOutputDirectory() + "network.xml.gz");

		monc.cleanNetwork(sc.getNetwork(), mp);
		assertNotNull("Cleaned network should exists.", monc.getNewNetwork());
	}
	

	/**
	 * Overwrite the basic setUp() method so that a new MATSim network is created.
	 * 					
	 * 				 (6,13) 5<---->6 (8,13)
	 * 						^
	 * 						|
	 * 						|
	 * 						|
	 * 						V	
	 * 						4 (6,8)
	 * 						^
	 *        				|       
	 * (0,5) 1<-------------+--------------->2 (11,5)
	 * 						|
	 * 						V
	 * 						3 (6,2)
	 * 
	 * All nodes are connected with bi-directional links, i.e. one in eacj
	 * direction. 
	 * 
	 * @throws Exception
	 */
	@Override
	public void setUp() throws Exception {
		// this method is automatically called by JUnit, no need to call it manually
		super.setUp();

		// Build the test network.
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network n = sc.getNetwork();
		NetworkFactory nf = n.getFactory();
		// Nodes
		Node n1 = nf.createNode(Id.create("1", Node.class), new Coord(0.0, 5.0));
		n.addNode(n1);
		Node n2 = nf.createNode(Id.create("2", Node.class), new Coord(11.0, 5.0));
		n.addNode(n2);
		Node n3 = nf.createNode(Id.create("3", Node.class), new Coord(6.0, 2.0));
		n.addNode(n3);
		Node n4 = nf.createNode(Id.create("4", Node.class), new Coord(6.0, 8.0));
		n.addNode(n4);
		Node n5 = nf.createNode(Id.create("5", Node.class), new Coord(6.0, 13.0));
		n.addNode(n5);
		Node n6 = nf.createNode(Id.create("6", Node.class), new Coord(8.0, 13.0));
		n.addNode(n6);

		Link l12 = nf.createLink(Id.create("12", Link.class), n1, n2);
		n.addLink(l12);
		Link l21 = nf.createLink(Id.create("21", Link.class), n2, n1);
		n.addLink(l21);

		Link l34 = nf.createLink(Id.create("34", Link.class), n3, n4);
		n.addLink(l34);
		Link l43 = nf.createLink(Id.create("43", Link.class), n4, n3);
		n.addLink(l43);
		
		
		Link l45 = nf.createLink(Id.create("45", Link.class), n4, n5);
		n.addLink(l45);
		Link l54 = nf.createLink(Id.create("54", Link.class), n5, n4);
		n.addLink(l54);
		
		Link l56 = nf.createLink(Id.create("56", Link.class), n5, n6);
		n.addLink(l56);
		Link l65 = nf.createLink(Id.create("65", Link.class), n6, n5);
		n.addLink(l65);

		// Write network to file.
		NetworkWriter nw = new NetworkWriter(n);
		nw.write(getOutputDirectory() + "network.xml.gz");

	}


}
