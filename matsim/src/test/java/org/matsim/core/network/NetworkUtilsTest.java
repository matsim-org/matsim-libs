/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.util.List;
import java.util.TreeMap;

/**
 * @author nagel
 *
 */
public class NetworkUtilsTest {
	private static final Logger log = LogManager.getLogger( NetworkUtilsTest.class ) ;

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils() ;

	/**
	 * Test method for {@link org.matsim.core.network.NetworkUtils#isMultimodal(org.matsim.api.core.v01.network.Network)}.
	 */
	@Test
	final void testIsMultimodal() {

		Config config = utils.createConfigWithInputResourcePathAsContext();
		config.network().setInputFile("network.xml" );

		Scenario scenario = ScenarioUtils.loadScenario(config) ;

		Network network = scenario.getNetwork() ;

		Assertions.assertTrue( NetworkUtils.isMultimodal( network ) );

	}


	@SuppressWarnings("static-method")
	@Test
	final void getOutLinksSortedByAngleTest() {
		final Network network = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getNetwork();
		// (we need the network to properly connect the links)

		NetworkFactory nf = network.getFactory() ;

		// a number of potentially outgoing links:

		Node nd0 = nf.createNode(Id.createNodeId("0"), new Coord(0.,0.) ) ;
		Node ndN = nf.createNode(Id.createNodeId("N"), new Coord(0.,100.) ) ;
		Node ndNE = nf.createNode(Id.createNodeId("NE"), new Coord(100.,100.) ) ;
		Node ndE = nf.createNode(Id.createNodeId("E"), new Coord(100.,0.) ) ;
		Node ndSE = nf.createNode(Id.createNodeId("SE"), new Coord(100.,-100.) ) ;
		Node ndS = nf.createNode(Id.createNodeId("S"), new Coord(0.,-100.) ) ;
		Node ndSW = nf.createNode(Id.createNodeId("SW"), new Coord(-100.,-100.) ) ;
		Node ndW = nf.createNode(Id.createNodeId("W"), new Coord(-100.,0.) ) ;
		Node ndNW = nf.createNode(Id.createNodeId("NW"), new Coord(-100.,+100.) ) ;

		network.addNode( nd0 );
		network.addNode( ndN );
		network.addNode( ndNE );
		network.addNode( ndE );
		network.addNode( ndSE );
		network.addNode( ndS );
		network.addNode( ndSW );
		network.addNode( ndW );
		network.addNode( ndNW );

		Link liN = nf.createLink( Id.createLinkId("N"), nd0, ndN ) ;
		Link liNE = nf.createLink( Id.createLinkId("NE"), nd0, ndNE ) ;
		Link liE = nf.createLink( Id.createLinkId("E"), nd0, ndE ) ;
		Link liSE = nf.createLink( Id.createLinkId("SE"), nd0, ndSE ) ;
		Link liS = nf.createLink( Id.createLinkId("S"), nd0, ndS ) ;
		Link liSW = nf.createLink( Id.createLinkId("SW"), nd0, ndSW ) ;
		Link liW = nf.createLink( Id.createLinkId("W"), nd0, ndW ) ;
		Link liNW = nf.createLink( Id.createLinkId("NW"), nd0, ndNW ) ;

		network.addLink( liN );
		network.addLink( liNE );
		network.addLink( liE );
		network.addLink( liSE );
		network.addLink( liS );
		network.addLink( liSW );
		network.addLink( liW );
		network.addLink( liNW );

		log.info("===");
		// a link coming north to south:
		{
			Link inLink = nf.createLink( Id.createLinkId("fromNorth"), ndN, nd0 ) ;
			TreeMap<Double, Link> result = NetworkUtils.getOutLinksSortedClockwiseByAngle(inLink) ;
			for ( Link outLink : result.values() ) {
				log.info( outLink );
			}

			Link[] actuals = result.values().toArray( new Link[result.size()] ) ;
			Link[] expecteds = {liNE,liE,liSE,liS,liSW,liW,liNW} ;
			Assertions.assertArrayEquals(expecteds, actuals);

		}
		log.info("===");
		// a link coming south to north:
		{
			Link inLink = nf.createLink( Id.createLinkId("fromSouth"), ndS, nd0 ) ;
			TreeMap<Double, Link> result = NetworkUtils.getOutLinksSortedClockwiseByAngle(inLink) ;
			for ( Link outLink : result.values() ) {
				log.info( outLink );
			}
			Link[] actuals = result.values().toArray( new Link[result.size()] ) ;
			Link[] expecteds = {liSW,liW,liNW,liN,liNE,liE,liSE} ;
			Assertions.assertArrayEquals(expecteds, actuals);
		}
		log.info("===");
	}

	@Test
	void testfindNearestPointOnLink(){
		Network network = NetworkUtils.createNetwork();
        Coord n1 = new Coord(1, 1);
		Coord n2 = new Coord(100,100);
		Coord plainX = new Coord(1,100);
		Coord plainY = new Coord(100,1);
		Node node1 = NetworkUtils.createNode(Id.createNodeId(1),n1);
		Node node2 = NetworkUtils.createNode(Id.createNodeId(2),n2);
		Node plainXNode = NetworkUtils.createNode(Id.createNodeId(3),plainX);
		Node plainYNode = NetworkUtils.createNode(Id.createNodeId(4),plainY);

		Link link = NetworkUtils.createLink(Id.createLinkId("1-2"),node1,node2,network,150,1,20,1);
		Link plainXLink = NetworkUtils.createLink(Id.createLinkId("plainX"),node1,plainXNode,network,150,1,20,1);
		Link plainYLink = NetworkUtils.createLink(Id.createLinkId("plainY"),node1,plainYNode,network,150,1,20,1);
		Link loop = NetworkUtils.createLink(Id.createLinkId("1-1"),node1,node1,network,150,1,20,1);

		//fromNode
		Coord tp1 = new Coord(1,1);
		assertEquals(n1,NetworkUtils.findNearestPointOnLink(tp1,link));

		//toNode
		Coord tp2 = new Coord(100,100);
		assertEquals(n2,NetworkUtils.findNearestPointOnLink(tp2,link));

		//on the link
		Coord tp3 = new Coord(50,50);
		assertEquals(tp3,NetworkUtils.findNearestPointOnLink(tp3,link));

		//along the link vector, but before link
		Coord tp4 = new Coord(-50,-50);
		assertEquals(n1,NetworkUtils.findNearestPointOnLink(tp4,link));

		//along the link vector, but after link ends
		Coord tp5 = new Coord(150,150);
		assertEquals(n2,NetworkUtils.findNearestPointOnLink(tp5,link));

		//off the link, but before link start
		Coord tp6 = new Coord(1,0);
		assertEquals(new Coord(1,1),NetworkUtils.findNearestPointOnLink(tp6,link));

		//off the link, parallel to it
		Coord tp7 = new Coord(2,1);
		assertEquals(new Coord(1.5,1.5),NetworkUtils.findNearestPointOnLink(tp7,link));

		//off the link, parallel to it
		Coord tp8 = new Coord(30,45);
		assertEquals(new Coord(37.5,37.5),NetworkUtils.findNearestPointOnLink(tp8,link));

		//loop link
		assertEquals(n1,NetworkUtils.findNearestPointOnLink(tp8,loop));

		//link is plain in x & y
		assertEquals(new Coord(1,45),NetworkUtils.findNearestPointOnLink(tp8,plainXLink));
		assertEquals(new Coord(30,1),NetworkUtils.findNearestPointOnLink(tp8,plainYLink));

	}

	@Test
	void getOriginalGeometry() {

        var network = NetworkUtils.createNetwork();
        var fromNode = NetworkUtils.createAndAddNode(network, Id.createNodeId("from"), new Coord(0, 0));
		var toNode = NetworkUtils.createAndAddNode(network, Id.createNodeId("to"), new Coord(100, 100));
		var link = network.getFactory().createLink(Id.createLinkId("link"), fromNode, toNode);
		network.addLink(link);

		var intermediateNode1 = network.getFactory().createNode(Id.createNodeId("intermediate_1"), new Coord(20, 20));
		var intermediateNode2 = network.getFactory().createNode(Id.createNodeId("intermediate_2"), new Coord(70, 70));

		var intermediateString1 = intermediateNode1.getId().toString() + "," + intermediateNode1.getCoord().getX() + "," + intermediateNode1.getCoord().getY();
		var intermediateString2 = intermediateNode2.getId().toString() + "," + intermediateNode2.getCoord().getX() + "," + intermediateNode2.getCoord().getY();
		var geometryString = intermediateString1 + " " + intermediateString2;
		link.getAttributes().putAttribute(NetworkUtils.ORIG_GEOM, geometryString);

		var expectedNodeList = List.of(fromNode, intermediateNode1, intermediateNode2, toNode);
		var actualNodeList = NetworkUtils.getOriginalGeometry(link);

		assertEquals(expectedNodeList.size(), actualNodeList.size());

		for (var i = 0; i < expectedNodeList.size(); i++) {
			var expectedNode = expectedNodeList.get(i);
			var actualNode = actualNodeList.get(i);

			assertEquals(expectedNode.getId(), actualNode.getId());
			assertEquals(expectedNode.getCoord(), actualNode.getCoord());
		}
	}

	@Test
	void getOriginalGeometry_noGeometryStored() {

        var network = NetworkUtils.createNetwork();
        var fromNode = NetworkUtils.createAndAddNode(network, Id.createNodeId("from"), new Coord(0, 0));
		var toNode = NetworkUtils.createAndAddNode(network, Id.createNodeId("to"), new Coord(100, 100));
		var link = network.getFactory().createLink(Id.createLinkId("link"), fromNode, toNode);
		network.addLink(link);

		var expectedNodeList = List.of(fromNode, toNode);

		var actualNodeList = NetworkUtils.getOriginalGeometry(link);

		assertEquals(expectedNodeList.size(), actualNodeList.size());

		for (var i = 0; i < expectedNodeList.size(); i++) {
			var expectedNode = expectedNodeList.get(i);
			var actualNode = actualNodeList.get(i);

			assertEquals(expectedNode.getId(), actualNode.getId());
			assertEquals(expectedNode.getCoord(), actualNode.getCoord());
		}
	}

	/**
	 * Have this test here since I first had a bug where empty origgeom attributes would cause exceptions because String.split
	 * splits an empty string ("") into string[""]...
	 */
	@Test
	void getOriginalGeometry_emptyGeometryStored() {

        var network = NetworkUtils.createNetwork();
        var fromNode = NetworkUtils.createAndAddNode(network, Id.createNodeId("from"), new Coord(0, 0));
		var toNode = NetworkUtils.createAndAddNode(network, Id.createNodeId("to"), new Coord(100, 100));
		var link = network.getFactory().createLink(Id.createLinkId("link"), fromNode, toNode);
		link.getAttributes().putAttribute(NetworkUtils.ORIG_GEOM, "");
		network.addLink(link);

		var expectedNodeList = List.of(fromNode, toNode);

		var actualNodeList = NetworkUtils.getOriginalGeometry(link);

		assertEquals(expectedNodeList.size(), actualNodeList.size());

		for (var i = 0; i < expectedNodeList.size(); i++) {
			var expectedNode = expectedNodeList.get(i);
			var actualNode = actualNodeList.get(i);

			assertEquals(expectedNode.getId(), actualNode.getId());
			assertEquals(expectedNode.getCoord(), actualNode.getCoord());
		}

	}
}















