package org.matsim.core.network;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author thibautd
 */
public class NetworkReprojectionIOTest {
	@Rule
	public final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testInput() {
		final String networkFile = utils.getOutputDirectory()+"/network.xml";

		final Network initialNetwork = createInitialNetwork();

		new NetworkWriter( initialNetwork ).write( networkFile );

		final Network readNetwork = NetworkUtils.createNetwork();
		new MatsimNetworkReader(
				new CoordinateTransformation() {
					@Override
					public Coord transform(Coord coord) {
						return new Coord( 1000 , 1000 );
					}
				},
				readNetwork ).readFile( networkFile );

		Assert.assertEquals(
				"unexpected network size",
				2,
				readNetwork.getNodes().size() );

		for ( Node n : readNetwork.getNodes().values() ) {
			Assert.assertEquals(
					"Unexpected coordinate",
					new Coord( 1000 , 1000 ),
					n.getCoord() );
		}
	}

	@Test
	public void testOutput() {
		final String networkFile = utils.getOutputDirectory()+"/network.xml";

		final Network initialNetwork = createInitialNetwork();

		new NetworkWriter(
				new CoordinateTransformation() {
					@Override
					public Coord transform(Coord coord) {
						return new Coord( 9999 , 9999 );
					}
				},
				initialNetwork ).write( networkFile );

		final Network readNetwork = NetworkUtils.createNetwork();
		new MatsimNetworkReader(
				readNetwork ).readFile( networkFile );

		Assert.assertEquals(
				"unexpected network size",
				2,
				readNetwork.getNodes().size() );

		for ( Node n : readNetwork.getNodes().values() ) {
			Assert.assertEquals(
					"Unexpected coordinate",
					new Coord( 9999 , 9999 ),
					n.getCoord() );
		}
	}

	@Test
	public void testWithControlerAndConfigParameters() {
		final String networkFile = utils.getOutputDirectory()+"/network.xml";

		final Network initialNetwork = createInitialNetwork();
		new NetworkWriter( initialNetwork ).write( networkFile );

		final Config config = ConfigUtils.createConfig();
		config.network().setInputFile( networkFile );

		config.network().setInputCRS( "WGS84" );
		// web mercator. This would be a pretty silly choice for simulation,
		// but does not matter for tests. Just makes sure that (almost) every
		// coordinate can be projected
		config.global().setCoordinateSystem(  "EPSG:3857" );

		// TODO: test also with loading from Controler C'tor?
		final Scenario scenario = ScenarioUtils.loadScenario( config );
		for ( Id<Node> id : initialNetwork.getNodes().keySet() ) {
			final Coord originalCoord = initialNetwork.getNodes().get( id ).getCoord();
			final Coord internalCoord = scenario.getNetwork().getNodes().get( id ).getCoord();

			Assert.assertNotEquals(
					"No coordinates transform performed!",
					originalCoord.getX(),
					internalCoord.getX(),
					MatsimTestUtils.EPSILON );
			Assert.assertNotEquals(
					"No coordinates transform performed!",
					originalCoord.getY(),
					internalCoord.getY(),
					MatsimTestUtils.EPSILON );
		}

		config.controler().setLastIteration( 0 );
		final String outputDirectory = utils.getOutputDirectory()+"/output/";
		config.controler().setOutputDirectory( outputDirectory );
		final Controler controler = new Controler( scenario );
		controler.run();

		final Network dumpedNetwork = NetworkUtils.createNetwork();
		new MatsimNetworkReader( dumpedNetwork ).readFile( outputDirectory+"/output_network.xml.gz" );

		for ( Id<Node> id : initialNetwork.getNodes().keySet() ) {
			final Coord originalCoord = initialNetwork.getNodes().get( id ).getCoord();
			final Coord dumpedCoord = dumpedNetwork.getNodes().get( id ).getCoord();

			Assert.assertEquals(
					"coordinates were not reprojected for dump",
					originalCoord.getX(),
					dumpedCoord.getX(),
					MatsimTestUtils.EPSILON );
			Assert.assertEquals(
					"coordinates were not reprojected for dump",
					originalCoord.getY(),
					dumpedCoord.getY(),
					MatsimTestUtils.EPSILON );
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
