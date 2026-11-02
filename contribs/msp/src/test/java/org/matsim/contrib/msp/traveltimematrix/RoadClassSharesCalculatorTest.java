package org.matsim.contrib.msp.traveltimematrix;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.common.zones.ZoneSystem;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests that the road class split of an origin-destination pair is the split of the free-speed path
 * between them.
 * <p>
 * The corridor of {@link TestNetworks#corridor()} is built so that the expected answer can be written
 * down: n1 to n4 is 101 s of residential street, 21 s of motorway and 101 s of residential street again,
 * so 202 of 223 s are residential.
 */
class RoadClassSharesCalculatorTest {

	/**
	 * The resolution of one stored byte.
	 */
	private static final double TOLERANCE = 1.0 / RoadClassShares.SCALE;

	private static final double RESIDENTIAL_LINK = 101;
	private static final double MOTORWAY_LINK = 21;

	@Test
	void theSharesAreTheSplitOfTheFreeSpeedPath() {

		Network network = TestNetworks.corridor();
		ZoneSystem zoneSystem = TestNetworks.oneZonePerNode(network);
		RoadClassification roadClasses = RoadClassification.byHighwayType(network);

		RoadClassShares shares = RoadClassSharesCalculator.calculateFreeSpeedShares(network, zoneSystem, roadClasses,
			1, 1);

		// byHighwayType over this network gives [motorway, residential, other]
		assertEquals(List.of("motorway", "residential", RoadClassification.OTHER), shares.getRoadClasses());

		double total = 2 * RESIDENTIAL_LINK + MOTORWAY_LINK;
		double[] endToEnd = shares.get(node(network, "n1"), node(network, "n4"));
		assertEquals(MOTORWAY_LINK / total, endToEnd[0], TOLERANCE, "motorway");
		assertEquals(2 * RESIDENTIAL_LINK / total, endToEnd[1], TOLERANCE, "residential");
		assertEquals(0.0, endToEnd[2], "nothing of this path is of any other road class");
	}

	@Test
	void aPairOnOneRoadClassOnlyIsEntirelyThatClass() {

		Network network = TestNetworks.corridor();
		ZoneSystem zoneSystem = TestNetworks.oneZonePerNode(network);
		RoadClassShares shares = RoadClassSharesCalculator.calculateFreeSpeedShares(network, zoneSystem,
			RoadClassification.byHighwayType(network), 1, 1);

		// n2 to n3 is the motorway link and nothing else
		double[] motorwayOnly = shares.get(node(network, "n2"), node(network, "n3"));
		assertEquals(1.0, motorwayOnly[0], 1e-12);

		// n1 to n2 is one residential link and nothing else
		double[] residentialOnly = shares.get(node(network, "n1"), node(network, "n2"));
		assertEquals(1.0, residentialOnly[1], 1e-12);
	}

	@Test
	void theSharesAreSymmetricOnASymmetricNetwork() {

		Network network = TestNetworks.corridor();
		ZoneSystem zoneSystem = TestNetworks.oneZonePerNode(network);
		RoadClassShares shares = RoadClassSharesCalculator.calculateFreeSpeedShares(network, zoneSystem,
			RoadClassification.byHighwayType(network), 1, 1);

		double[] there = shares.get(node(network, "n1"), node(network, "n4"));
		double[] back = shares.get(node(network, "n4"), node(network, "n1"));

		for (int k = 0; k < there.length; k++) {
			assertEquals(there[k], back[k], 1e-12, "class " + shares.getRoadClasses().get(k));
		}
	}

	@Test
	void everyPairOfTheZoneSystemHasShares() {

		Network network = TestNetworks.corridor();
		ZoneSystem zoneSystem = TestNetworks.oneZonePerNode(network);
		RoadClassShares shares = RoadClassSharesCalculator.calculateFreeSpeedShares(network, zoneSystem,
			RoadClassification.byDefaultGroups(network), 1, 1);

		for (Node from : network.getNodes().values()) {
			for (Node to : network.getNodes().values()) {
				double[] pair = shares.get(from, to);
				double sum = 0;
				for (double share : pair) {
					sum += share;
				}
				assertEquals(1.0, sum, 1e-12, from.getId() + " -> " + to.getId());
			}
		}
	}

	@Test
	void aSingleRoadClassHasNothingToSplit() {

		Network network = TestNetworks.corridor();
		ZoneSystem zoneSystem = TestNetworks.oneZonePerNode(network);

		assertThrows(IllegalArgumentException.class, () -> RoadClassSharesCalculator.calculateFreeSpeedShares(network,
			zoneSystem, RoadClassification.single(), 1, 1));
	}

	@Test
	void theResultIsTheSameWithSeveralThreads() {

		Network network = TestNetworks.corridor();
		ZoneSystem zoneSystem = TestNetworks.oneZonePerNode(network);
		RoadClassification roadClasses = RoadClassification.byHighwayType(network);

		RoadClassShares single = RoadClassSharesCalculator.calculateFreeSpeedShares(network, zoneSystem, roadClasses,
			1, 1);
		RoadClassShares parallel = RoadClassSharesCalculator.calculateFreeSpeedShares(network, zoneSystem, roadClasses,
			4, 1);

		for (Node from : network.getNodes().values()) {
			for (Node to : network.getNodes().values()) {
				assertArrayEquals(single.get(from, to), parallel.get(from, to), from.getId() + " -> " + to.getId());
			}
		}
	}

	private static Node node(Network network, String id) {
		return network.getNodes().get(Id.createNodeId(id));
	}

	private static void assertArrayEquals(double[] expected, double[] actual, String message) {
		assertEquals(expected.length, actual.length, message);
		for (int k = 0; k < expected.length; k++) {
			assertEquals(expected[k], actual[k], 1e-12, message + ", class " + k);
		}
	}
}
