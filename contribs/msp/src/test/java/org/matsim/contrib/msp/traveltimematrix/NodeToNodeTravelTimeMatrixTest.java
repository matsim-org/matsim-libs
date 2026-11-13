package org.matsim.contrib.msp.traveltimematrix;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.dvrp.trafficmonitoring.QSimFreeSpeedTravelTime;
import org.matsim.contrib.zone.skims.TravelTimeMatrix;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.speedy.LeastCostPathTree;
import org.matsim.core.router.speedy.SpeedyGraphBuilder;
import org.matsim.contrib.dvrp.router.TimeAsTravelDisutility;
import org.matsim.core.utils.misc.OptionalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link NodeToNodeTravelTimeMatrix} against the router it is built from: a matrix with no zonal half
 * has no discretisation to be wrong about, so the test is that it is <b>exactly</b> right for every pair
 * and not merely close.
 */
class NodeToNodeTravelTimeMatrixTest {

	@Test
	void everyPairIsExactlyTheRoutedFreeSpeedTravelTime() {

		Network network = TestNetworks.grid(5);
		TravelTimeMatrix matrix = NodeToNodeTravelTimeMatrix.create(network, 2, 1);

		QSimFreeSpeedTravelTime travelTime = new QSimFreeSpeedTravelTime(1);
		LeastCostPathTree tree = new LeastCostPathTree(SpeedyGraphBuilder.build(network), travelTime,
			new TimeAsTravelDisutility(travelTime));

		int pairs = 0;
		for (Node from : network.getNodes().values()) {
			tree.calculate(from, 0, null, null);
			for (Node to : network.getNodes().values()) {
				OptionalTime routed = tree.getTime(tree.getNodeIndex(to));
				assertEquals((int) routed.seconds(), matrix.getTravelTime(from, to, 0),
					"node " + from.getId() + " to node " + to.getId());
				pairs++;
			}
		}

		assertEquals(25 * 25, pairs, "every pair of the grid was checked");
	}

	/**
	 * The furthest pair of the corridor, as a sanity check that the numbers above are the ones the
	 * network was built to have and not a tautology of two identically broken routers.
	 */
	@Test
	void theCorridorEndToEndIsTheSumOfItsLinks() {

		Network network = TestNetworks.corridor();
		TravelTimeMatrix matrix = NodeToNodeTravelTimeMatrix.create(network, 1, 1);

		assertEquals(101 + 21 + 101, matrix.getTravelTime(node(network, "n1"), node(network, "n4"), 0));
		assertEquals(21, matrix.getTravelTime(node(network, "n2"), node(network, "n3"), 0));
		assertEquals(0, matrix.getTravelTime(node(network, "n1"), node(network, "n1"), 0));
	}

	/**
	 * Free speed does not have a time of day, so the departure time of the query cannot change the
	 * answer - it is a {@link TimeVaryingTravelTimeMatrix} on top that gives it meaning.
	 */
	@Test
	void theDepartureTimeDoesNotChangeTheAnswer() {

		Network network = TestNetworks.corridor();
		TravelTimeMatrix matrix = NodeToNodeTravelTimeMatrix.create(network, 1, 1);
		Node n1 = node(network, "n1");
		Node n4 = node(network, "n4");

		assertEquals(matrix.getTravelTime(n1, n4, 0), matrix.getTravelTime(n1, n4, 8 * 3600));
		assertEquals(matrix.getTravelTime(n1, n4, 0), matrix.getTravelTime(n1, n4, 23 * 3600));
	}

	/**
	 * With no zonal half there is nothing to answer an unreachable pair with, so it has to say so. A
	 * zonal matrix would hand back the travel time between the central nodes of the two zones, which for
	 * a disconnected network is not a smaller error but a meaningless number.
	 */
	@Test
	void anUnreachablePairIsRefusedRatherThanGuessed() {

		Network network = TestNetworks.corridor();
		// an island: reachable from nothing and reaching nothing
		Node island = NetworkUtils.createAndAddNode(network, Id.createNodeId("island"),
			new Coord(50_000, 50_000));
		Node other = NetworkUtils.createAndAddNode(network, Id.createNodeId("island2"),
			new Coord(51_000, 50_000));
		TestNetworks.link(network, "island-island2", island, other, 10, "residential");

		TravelTimeMatrix matrix = NodeToNodeTravelTimeMatrix.create(network, 1, 1);

		assertEquals(101, matrix.getTravelTime(island, other, 0), "the island itself is routable");

		IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
			() -> matrix.getTravelTime(node(network, "n1"), island, 0));
		assertTrue(thrown.getMessage().contains("island"), thrown.getMessage());
		assertTrue(thrown.getMessage().contains("not connected"), thrown.getMessage());
	}

	private static Node node(Network network, String id) {
		return network.getNodes().get(Id.createNodeId(id));
	}
}
