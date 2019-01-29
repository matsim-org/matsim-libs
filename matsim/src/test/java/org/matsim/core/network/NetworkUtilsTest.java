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

import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.hamcrest.Matcher;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
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

/**
 * @author nagel
 *
 */
public class NetworkUtilsTest {
	private static final Logger log = Logger.getLogger( NetworkUtilsTest.class ) ;
	
	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils() ;

	/**
	 * Test method for {@link org.matsim.core.network.NetworkUtils#isMultimodal(org.matsim.api.core.v01.network.Network)}.
	 */
	@Test
	public final void testIsMultimodal() {

		Config config = utils.createConfigWithInputResourcePathAsContext();
		config.network().setInputFile("network.xml" );
		
		Scenario scenario = ScenarioUtils.loadScenario(config) ;
		
		Network network = scenario.getNetwork() ;
		
		Assert.assertTrue( NetworkUtils.isMultimodal( network ) );
		
	}
	
	@SuppressWarnings("static-method")
	@Test
	public final void getOutLinksSortedByAngleTest() {
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
			Assert.assertArrayEquals(expecteds, actuals);
			
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
			Assert.assertArrayEquals(expecteds, actuals);
		}
		log.info("===");
	}

}















