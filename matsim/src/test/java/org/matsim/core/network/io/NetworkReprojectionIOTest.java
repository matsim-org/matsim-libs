
/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkReprojectionIOTest.java
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

 package org.matsim.core.network.io;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ProjectionUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.testcases.MatsimTestUtils;

	/**
 * @author thibautd
 */
public class NetworkReprojectionIOTest {
    private static final String INITIAL_CRS = "EPSG:3857";
	private static final String TARGET_CRS = "WGS84";
	private static final CoordinateTransformation transformation =
			TransformationFactory.getCoordinateTransformation(
					INITIAL_CRS,
					TARGET_CRS);

	@RegisterExtension
	public final MatsimTestUtils utils = new MatsimTestUtils();

	 @Test
	 void testInput() {
		final String networkFile = utils.getOutputDirectory()+"/network.xml";

		final Network initialNetwork = createInitialNetwork();

		new NetworkWriter( initialNetwork ).write( networkFile );

		final Network readNetwork = NetworkUtils.createNetwork();
		new MatsimNetworkReader(
				INITIAL_CRS, TARGET_CRS,
				readNetwork ).readFile( networkFile );

		Assertions.assertEquals(
				2,
				readNetwork.getNodes().size(),
				"unexpected network size" );

		for ( Node n : readNetwork.getNodes().values() ) {
			Node initialNode = initialNetwork.getNodes().get(n.getId());

			Assertions.assertEquals( transformation.transform(initialNode.getCoord()),
					n.getCoord(),
					"Unexpected coordinate" );
		}
	}

	 @Test
	 void testOutput() {
		final String networkFile = utils.getOutputDirectory()+"/network.xml";

		final Network initialNetwork = createInitialNetwork();

		new NetworkWriter(
				transformation,
				initialNetwork ).write( networkFile );

        final Network readNetwork = NetworkUtils.createNetwork();
		new MatsimNetworkReader(
				readNetwork ).readFile( networkFile );

		Assertions.assertEquals(
				2,
				readNetwork.getNodes().size(),
				"unexpected network size" );

		for ( Node n : readNetwork.getNodes().values() ) {
			Node initialNode = initialNetwork.getNodes().get(n.getId());
			Assertions.assertEquals(
					transformation.transform(initialNode.getCoord()),
					n.getCoord(),
					"Unexpected coordinate" );
		}
	}

	 @Test
	 void testWithControlerAndAttributes() {
		final String networkFile = utils.getOutputDirectory()+"/network.xml";

		final Network initialNetwork = createInitialNetwork();
		ProjectionUtils.putCRS(initialNetwork, INITIAL_CRS);

		new NetworkWriter( initialNetwork ).write( networkFile );

		final Config config = ConfigUtils.createConfig();
		config.network().setInputFile( networkFile );

		// web mercator. This would be a pretty silly choice for simulation,
		// but does not matter for tests. Just makes sure that (almost) every
		// coordinate can be projected
		config.global().setCoordinateSystem( TARGET_CRS );

		// TODO: test also with loading from Controler C'tor?
		final Scenario scenario = ScenarioUtils.loadScenario( config );
		for ( Id<Node> id : initialNetwork.getNodes().keySet() ) {
			final Coord originalCoord = initialNetwork.getNodes().get( id ).getCoord();
			final Coord internalCoord = scenario.getNetwork().getNodes().get( id ).getCoord();

			Assertions.assertEquals(
					transformation.transform(originalCoord),
					internalCoord,
					"No coordinates transform performed!");
		}

		Assertions.assertEquals(
				TARGET_CRS,
				ProjectionUtils.getCRS(scenario.getNetwork()),
				"wrong CRS information after loading");

		config.controller().setLastIteration( 0 );
		final String outputDirectory = utils.getOutputDirectory()+"/output/";
		config.controller().setOutputDirectory( outputDirectory );
		final Controler controler = new Controler( scenario );
		controler.run();

        final Network dumpedNetwork = NetworkUtils.createNetwork();
		new MatsimNetworkReader( dumpedNetwork ).readFile( outputDirectory+"/output_network.xml.gz" );

		for ( Id<Node> id : scenario.getNetwork().getNodes().keySet() ) {
			final Coord internalCoord = scenario.getNetwork().getNodes().get( id ).getCoord();
			final Coord dumpedCoord = dumpedNetwork.getNodes().get( id ).getCoord();

			Assertions.assertEquals(
					internalCoord,
					dumpedCoord,
					"coordinates were reprojected for dump");
		}
	}

	 @Test
	 void testWithControlerAndConfigParameters() {
		final String networkFile = utils.getOutputDirectory()+"/network.xml";

		final Network initialNetwork = createInitialNetwork();
		new NetworkWriter( initialNetwork ).write( networkFile );

		final Config config = ConfigUtils.createConfig();
		config.network().setInputFile( networkFile );

		config.network().setInputCRS( INITIAL_CRS );
		// web mercator. This would be a pretty silly choice for simulation,
		// but does not matter for tests. Just makes sure that (almost) every
		// coordinate can be projected
		config.global().setCoordinateSystem( TARGET_CRS );

		// TODO: test also with loading from Controler C'tor?
		final Scenario scenario = ScenarioUtils.loadScenario( config );
		for ( Id<Node> id : initialNetwork.getNodes().keySet() ) {
			final Coord originalCoord = initialNetwork.getNodes().get( id ).getCoord();
			final Coord internalCoord = scenario.getNetwork().getNodes().get( id ).getCoord();

			Assertions.assertNotEquals(
					originalCoord.getX(),
					internalCoord.getX(),
					MatsimTestUtils.EPSILON,
					"No coordinates transform performed!" );
			Assertions.assertNotEquals(
					originalCoord.getY(),
					internalCoord.getY(),
					MatsimTestUtils.EPSILON,
					"No coordinates transform performed!" );
		}

		config.controller().setLastIteration( 0 );
		final String outputDirectory = utils.getOutputDirectory()+"/output/";
		config.controller().setOutputDirectory( outputDirectory );
		final Controler controler = new Controler( scenario );
		controler.run();

        final Network dumpedNetwork = NetworkUtils.createNetwork();
		new MatsimNetworkReader( dumpedNetwork ).readFile( outputDirectory+"/output_network.xml.gz" );

		for ( Id<Node> id : initialNetwork.getNodes().keySet() ) {
			final Coord originalCoord = initialNetwork.getNodes().get( id ).getCoord();
			final Coord dumpedCoord = dumpedNetwork.getNodes().get( id ).getCoord();

			Assertions.assertNotEquals(
					originalCoord.getX(),
					dumpedCoord.getX(),
					MatsimTestUtils.EPSILON,
					"coordinates were reprojected for dump" );
			Assertions.assertNotEquals(
					originalCoord.getY(),
					dumpedCoord.getY(),
					MatsimTestUtils.EPSILON,
					"coordinates were reprojected for dump" );
		}
	}

	private Network createInitialNetwork() {
        final Network network = NetworkUtils.createNetwork();

		final Node node1 =
				network.getFactory().createNode(
						Id.createNodeId( 1 ),
						new Coord( 45 , 45 ) );
		network.addNode( node1 );

		final Node node2 =
				network.getFactory().createNode(
						Id.createNodeId( 2 ),
						new Coord( 20  , 20 ) );
		network.addNode( node2 );

		final Link l =
				network.getFactory().createLink(
						Id.createLinkId( "l" ),
						node1,
						node2 );
		network.addLink( l );

		return network;
	}
}
