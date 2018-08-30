package org.matsim.core.network.io;

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
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
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

	@Rule
	public final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testInput() {
		final String networkFile = utils.getOutputDirectory()+"/network.xml";

		final Network initialNetwork = createInitialNetwork();

		new NetworkWriter( initialNetwork ).write( networkFile );

		final Network readNetwork = NetworkUtils.createNetwork();
		new MatsimNetworkReader(
				INITIAL_CRS, TARGET_CRS,
				readNetwork ).readFile( networkFile );

		Assert.assertEquals(
				"unexpected network size",
				2,
				readNetwork.getNodes().size() );

		for ( Node n : readNetwork.getNodes().values() ) {
			Node initialNode = initialNetwork.getNodes().get(n.getId());

			Assert.assertEquals( "Unexpected coordinate",
					transformation.transform(initialNode.getCoord()),
					n.getCoord() );
		}
	}

	@Test
	public void testOutput() {
		final String networkFile = utils.getOutputDirectory()+"/network.xml";

		final Network initialNetwork = createInitialNetwork();

		new NetworkWriter(
				transformation,
				initialNetwork ).write( networkFile );

		final Network readNetwork = NetworkUtils.createNetwork();
		new MatsimNetworkReader(
				readNetwork ).readFile( networkFile );

		Assert.assertEquals(
				"unexpected network size",
				2,
				readNetwork.getNodes().size() );

		for ( Node n : readNetwork.getNodes().values() ) {
			Node initialNode = initialNetwork.getNodes().get(n.getId());
			Assert.assertEquals(
					"Unexpected coordinate",
					transformation.transform(initialNode.getCoord()),
					n.getCoord() );
		}
	}

	@Test
	public void testWithControlerAndAttributes() {
		final String networkFile = utils.getOutputDirectory()+"/network.xml";

		final Network initialNetwork = createInitialNetwork();
		initialNetwork.getAttributes().putAttribute(CoordUtils.INPUT_CRS_ATT, INITIAL_CRS);

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

			Assert.assertEquals(
					"No coordinates transform performed!",
					transformation.transform(originalCoord),
					internalCoord);
		}

		config.controler().setLastIteration( 0 );
		final String outputDirectory = utils.getOutputDirectory()+"/output/";
		config.controler().setOutputDirectory( outputDirectory );
		final Controler controler = new Controler( scenario );
		controler.run();

		final Network dumpedNetwork = NetworkUtils.createNetwork();
		new MatsimNetworkReader( dumpedNetwork ).readFile( outputDirectory+"/output_network.xml.gz" );

		for ( Id<Node> id : scenario.getNetwork().getNodes().keySet() ) {
			final Coord internalCoord = scenario.getNetwork().getNodes().get( id ).getCoord();
			final Coord dumpedCoord = dumpedNetwork.getNodes().get( id ).getCoord();

			Assert.assertEquals(
					"coordinates were reprojected for dump",
					internalCoord,
					dumpedCoord);
		}
	}

	@Test
	public void testWithControlerAndConfigParameters() {
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

			Assert.assertNotEquals(
					"coordinates were reprojected for dump",
					originalCoord.getX(),
					dumpedCoord.getX(),
					MatsimTestUtils.EPSILON );
			Assert.assertNotEquals(
					"coordinates were reprojected for dump",
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
