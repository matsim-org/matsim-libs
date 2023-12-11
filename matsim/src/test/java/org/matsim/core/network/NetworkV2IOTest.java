
/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkV2IOTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

	/**
 * @author thibautd
 */
public class NetworkV2IOTest {
	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	 @Test
	 void testNetworkAttributes() {
		final Scenario sc = createTestNetwork( false );

		new NetworkWriter( sc.getNetwork() ).writeV2( utils.getOutputDirectory()+"network.xml" );

		final Scenario read = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		new MatsimNetworkReader( read.getNetwork() ).readFile( utils.getOutputDirectory()+"network.xml" );

		Assertions.assertEquals( sc.getNetwork().getAttributes().getAttribute( "year" ),
				read.getNetwork().getAttributes().getAttribute( "year" ),
				"unexpected year in network metadata" );
	}

	 @Test
	 void testNodesAttributes() {
		final Scenario sc = createTestNetwork( false );

		new NetworkWriter( sc.getNetwork() ).writeV2( utils.getOutputDirectory()+"network.xml" );

		final Scenario read = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		new MatsimNetworkReader( read.getNetwork() ).readFile( utils.getOutputDirectory()+"network.xml" );

		final Id<Node> id = Id.createNodeId( "Zurich" );

		Assertions.assertEquals( "good",
				read.getNetwork().getNodes().get( id ).getAttributes().getAttribute( "Internet" ),
				"unexpected internet attribute in node metadata" );

		Assertions.assertEquals( false,
				read.getNetwork().getNodes().get( id ).getAttributes().getAttribute( "Developper Meeting" ),
				"unexpected meeting attribute in node metadata" );
	}

	 @Test
	 void testNo3DCoord() {
		// should be done through once "mixed" network as soon as possible
		final Scenario sc = createTestNetwork( false );

		new NetworkWriter( sc.getNetwork() ).writeV2( utils.getOutputDirectory()+"network.xml" );

		final Scenario read = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		new MatsimNetworkReader( read.getNetwork() ).readFile( utils.getOutputDirectory()+"network.xml" );

		final Id<Node> zh = Id.createNodeId( "Zurich" );

		final Coord zhCoord = read.getNetwork().getNodes().get( zh ).getCoord();

		Assertions.assertFalse( zhCoord.hasZ(),
				"did not expect Z" );
	}

	 @Test
	 void test3DCoord() {
		// should be done through once "mixed" network as soon as possible
		final Scenario sc = createTestNetwork(	true );

		new NetworkWriter( sc.getNetwork() ).writeV2( utils.getOutputDirectory()+"network.xml" );

		final Scenario read = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		new MatsimNetworkReader( read.getNetwork() ).readFile( utils.getOutputDirectory()+"network.xml" );

		final Id<Node> zh = Id.createNodeId( "Zurich" );

		final Coord zhCoord = read.getNetwork().getNodes().get( zh ).getCoord();

		Assertions.assertTrue( zhCoord.hasZ(),
				"did expect Z" );

		Assertions.assertEquals( 400,
				zhCoord.getZ() ,
				MatsimTestUtils.EPSILON,
				"unexpected Z value" );
	}

	 @Test
	 void testLinksAttributes() {
		final Scenario sc = createTestNetwork( false );

		new NetworkWriter( sc.getNetwork() ).writeV2( utils.getOutputDirectory()+"network.xml" );

		final Scenario read = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		new MatsimNetworkReader( read.getNetwork() ).readFile( utils.getOutputDirectory()+"network.xml" );

		final Id<Link> id = Id.createLinkId( "trip" );

		Assertions.assertEquals( 3,
				read.getNetwork().getLinks().get( id ).getAttributes().getAttribute( "number of modes" ),
				"unexpected mode attribute in link metadata" );
	}

	private Scenario createTestNetwork( boolean threeD) {
		final Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig() );

		final Network network = scenario.getNetwork();
		final NetworkFactory factory = network.getFactory();

		network.getAttributes().putAttribute( "year" , 2016 );

		final Node zurichNode =
				factory.createNode(
						Id.createNodeId( "Zurich" ) ,
						threeD ?
								new Coord( 0 , 0 , 400 ) :
								new Coord( 0 , 0 ) );
		final Node teltowNode =
				factory.createNode(
						Id.createNodeId( "Teltow" ) ,
						threeD ?
								new Coord( 1 , 1 , 1 ) :
								new Coord( 1 , 1 ) );

		zurichNode.getAttributes().putAttribute( "Internet" , "good" );
		zurichNode.getAttributes().putAttribute( "Developper Meeting" , false );

		teltowNode.getAttributes().putAttribute( "Internet" , "not so good" );
		teltowNode.getAttributes().putAttribute( "Developper Meeting" , true );

		network.addNode( zurichNode );
		network.addNode( teltowNode );

		final Link link = factory.createLink( Id.createLinkId( "trip" ) ,
											zurichNode, teltowNode );
		link.getAttributes().putAttribute( "number of modes" , 3 );
		link.setLength( 5000 );
		network.addLink( link );

		return scenario;
	}
}
