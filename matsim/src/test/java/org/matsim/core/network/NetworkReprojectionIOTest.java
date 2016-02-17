package org.matsim.core.network;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
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

	private Network createInitialNetwork() {
		final Network network = NetworkUtils.createNetwork();

		final Node node1 =
				network.getFactory().createNode(
						Id.createNodeId( 1 ),
						new Coord( 0 , 0 ) );
		network.addNode( node1 );

		final Node node2 =
				network.getFactory().createNode(
						Id.createNodeId( 2 ),
						new Coord( 1 , 1 ) );
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
