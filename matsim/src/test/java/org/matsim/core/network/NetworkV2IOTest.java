package org.matsim.core.network;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
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
	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testNetworkAttributes() {
		final Scenario sc = createTestNetwork( false );

		new NetworkWriter( sc.getNetwork() ).writeV2( utils.getOutputDirectory()+"network.xml" );

		final Scenario read = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		new MatsimNetworkReader( read.getNetwork() ).readFile( utils.getOutputDirectory()+"network.xml" );

		Assert.assertEquals( "unexpected year in network metadata",
				sc.getNetwork().getAttributes().getAttribute( "year" ),
				read.getNetwork().getAttributes().getAttribute( "year" ) );
	}

	@Test
	public void testNodesAttributes() {
		final Scenario sc = createTestNetwork( false );

		new NetworkWriter( sc.getNetwork() ).writeV2( utils.getOutputDirectory()+"network.xml" );

		final Scenario read = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		new MatsimNetworkReader( read.getNetwork() ).readFile( utils.getOutputDirectory()+"network.xml" );

		final Id<Node> id = Id.createNodeId( "Zurich" );

		Assert.assertEquals( "unexpected internet attribute in node metadata",
				"good",
				read.getNetwork().getNodes().get( id ).getAttributes().getAttribute( "Internet" ) );

		Assert.assertEquals( "unexpected meeting attribute in node metadata",
				false,
				read.getNetwork().getNodes().get( id ).getAttributes().getAttribute( "Developper Meeting" ) );
	}

	@Test
	public void testNo3DCoord() {
		// should be done through once "mixed" network as soon as possible
		final Scenario sc = createTestNetwork( false );

		new NetworkWriter( sc.getNetwork() ).writeV2( utils.getOutputDirectory()+"network.xml" );

		final Scenario read = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		new MatsimNetworkReader( read.getNetwork() ).readFile( utils.getOutputDirectory()+"network.xml" );

		final Id<Node> zh = Id.createNodeId( "Zurich" );

		final Coord zhCoord = read.getNetwork().getNodes().get( zh ).getCoord();

		Assert.assertFalse( "did not expect Z",
				zhCoord.hasZ() );
	}

	@Test
	public void test3DCoord() {
		// should be done through once "mixed" network as soon as possible
		final Scenario sc = createTestNetwork(	true );

		new NetworkWriter( sc.getNetwork() ).writeV2( utils.getOutputDirectory()+"network.xml" );

		final Scenario read = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		new MatsimNetworkReader( read.getNetwork() ).readFile( utils.getOutputDirectory()+"network.xml" );

		final Id<Node> zh = Id.createNodeId( "Zurich" );

		final Coord zhCoord = read.getNetwork().getNodes().get( zh ).getCoord();

		Assert.assertTrue( "did expect Z",
				zhCoord.hasZ() );

		Assert.assertEquals( "unexpected Z value",
				400,
				zhCoord.getZ() ,
				MatsimTestUtils.EPSILON );
	}

	@Test
	public void testLinksAttributes() {
		final Scenario sc = createTestNetwork( false );

		new NetworkWriter( sc.getNetwork() ).writeV2( utils.getOutputDirectory()+"network.xml" );

		final Scenario read = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		new MatsimNetworkReader( read.getNetwork() ).readFile( utils.getOutputDirectory()+"network.xml" );

		final Id<Link> id = Id.createLinkId( "trip" );

		Assert.assertEquals( "unexpected mode attribute in link metadata",
				3,
				read.getNetwork().getLinks().get( id ).getAttributes().getAttribute( "number of modes" ) );
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
