package org.matsim.contrib.msp.traveltimematrix.experiments;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.msp.traveltimematrix.TestNetworks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the estimate that decides whether a configuration is attempted at all.
 * <p>
 * The grid networks have a known geometry, so the neighbour counts can be written down: on a 5 by 5 grid
 * of nodes 1 km apart, a 1 km radius reaches a node's four neighbours plus itself, and an unbounded one
 * reaches all 25.
 */
class MatrixFootprintTest {

	@Test
	void anUnboundedNeighborhoodStoresEveryPair() {

		Network network = TestNetworks.grid(5);
		MatrixFootprint footprint = MatrixFootprint.estimate(network, 1000, Double.POSITIVE_INFINITY);

		assertEquals(25, footprint.meanNeighbors(), 1e-9,
			"every node of the 25 is a neighbour of every node, which needs no sampling to know");

		// 25 nodes x 25 neighbours x 6 bytes for the packed entries, plus one bit set per row over the
		// whole node index space, plus the array of rows. The index space is global to the jvm and other
		// tests have created nodes too, so it is at least the 25 of this network and usually more - which
		// is the point of it being a field.
		int space = footprint.nodeIdSpace();
		assertTrue(space >= 25, "the index space covers at least this network's nodes");
		assertEquals(25L * 25 * 6 + 25L * ((space + 7) / 8) + (long) space * 8, footprint.sparseBytes());
	}

	@Test
	void aSmallRadiusReachesOnlyTheNodesNextDoor() {

		Network network = TestNetworks.grid(5);
		MatrixFootprint footprint = MatrixFootprint.estimate(network, 1000, TestNetworks.SPACING);

		// a node in the middle reaches four, one on an edge three, a corner two - plus itself every time
		assertTrue(footprint.meanNeighbors() > 1, "a node is at least its own neighbour");
		assertTrue(footprint.meanNeighbors() <= 5, "and at most itself plus four, on a square grid "
			+ "(got " + footprint.meanNeighbors() + ")");
	}

	@Test
	void theNumberOfZonesIsBoundedByTheNumberOfNodes() {

		// the fact the whole memory story rests on: a cell with no node in it is not a zone, so making
		// the cells smaller cannot grow the zonal matrix past nodes squared
		Network network = TestNetworks.grid(5);

		assertEquals(25, MatrixFootprint.estimate(network, 1, 1000).zones(),
			"one zone per node at the limit, not one per square metre of the bounding box");
		assertTrue(MatrixFootprint.estimate(network, 10_000, 1000).zones() <= 25);
	}

	/**
	 * The unbounded corner has no zonal half - not an unused one - so it is reported as no zones and no
	 * bytes, whatever cell size is passed alongside it. Which also means no zone system is built in
	 * order to find that out, the cell size there describing nothing.
	 */
	@Test
	void anUnboundedRadiusHasNoZonalHalfAtAll() {

		Network network = TestNetworks.grid(5);

		MatrixFootprint fine = MatrixFootprint.estimate(network, 1, Double.POSITIVE_INFINITY);
		MatrixFootprint coarse = MatrixFootprint.estimate(network, 10_000, Double.POSITIVE_INFINITY);

		assertEquals(0, fine.zones());
		assertEquals(0, fine.zonalBytes());
		assertEquals(fine.totalBytes(), coarse.totalBytes(), "the cell size cannot change what is built");
		assertEquals(25, fine.meanNeighbors(), "every node is a neighbour of every node");
	}

	@Test
	void theZonalHalfGrowsWithTheSquareOfTheZones() {

		Network network = TestNetworks.grid(5);

		MatrixFootprint fine = MatrixFootprint.estimate(network, 1, 1000);
		MatrixFootprint coarse = MatrixFootprint.estimate(network, 10_000, 1000);

		assertEquals((long) fine.zones() * fine.zones() * Short.BYTES, fine.zonalBytes());
		assertTrue(fine.zonalBytes() > coarse.zonalBytes(), "smaller cells, more zones, more memory");
	}

	@Test
	void theBitSetsAreSizedByTheNodeIndexSpaceAndNotByTheStudyArea() {

		// the trap: cutting a small study area out of a large network leaves the node id indices spread
		// over the large one, and every sparse row keeps a bit set over that whole index space. Estimating
		// it from the study area instead would under-count by the ratio between the two - on Dresden cut
		// from Germany, by a factor of twenty, which is 1.3 GB.
		Network small = TestNetworks.grid(5);
		MatrixFootprint footprint = MatrixFootprint.estimate(small, 1000, Double.POSITIVE_INFINITY);

		long fromStudyArea = 25L * 25 * 6 + 25L * 25 / 8;
		assertTrue(footprint.sparseBytes() >= fromStudyArea,
			"the estimate is never below what the study area alone would suggest");
		assertTrue(footprint.nodeIdSpace() >= small.getNodes().size());
	}

	@Test
	void aFootprintOverTheBudgetDoesNotFit() {

		Network network = TestNetworks.grid(5);
		MatrixFootprint footprint = MatrixFootprint.estimate(network, 1, Double.POSITIVE_INFINITY);

		assertTrue(footprint.fitsIn(Long.MAX_VALUE, 1, Double.POSITIVE_INFINITY));
		assertFalse(footprint.fitsIn(1, 1, Double.POSITIVE_INFINITY));
		assertEquals(footprint.zonalBytes() + footprint.sparseBytes(), footprint.totalBytes());
	}
}
