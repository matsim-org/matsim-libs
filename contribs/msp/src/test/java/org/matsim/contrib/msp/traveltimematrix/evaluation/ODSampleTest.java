package org.matsim.contrib.msp.traveltimematrix.evaluation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.msp.traveltimematrix.TestNetworks;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the generated queries an evaluation can run on: that a sample by distance bands really does put
 * one destination in each band, that a seed reproduces a sample, and that a written sample reads back.
 */
class ODSampleTest {

	@RegisterExtension
	private final MatsimTestUtils utils = new MatsimTestUtils();

	private static final double[] BANDS = {1000, 2000, 4000};
	private static final double[] NOON = {12 * 3600.0};

	@Test
	void everyDestinationLiesInTheBandItWasDrawnFor() {

		Network network = TestNetworks.grid(9);
		List<TravelTimeQuery> queries = ODSample.byDistanceBands(network, 10, BANDS, NOON, 1234);

		for (TravelTimeQuery query : queries) {
			double distance = CoordUtils.calcEuclideanDistance(query.fromNode().getCoord(),
				query.toNode().getCoord());
			assertTrue(distance >= BANDS[0] && distance < 2 * BANDS[BANDS.length - 1],
				"the pair is " + distance + " m apart, which is in none of the bands " + List.of(1000, 2000, 4000));
		}

		// a grid of 9 by 9 km holds a node in each band for every origin, so every origin gets one pair
		// per band and no more
		assertEquals(10 * BANDS.length, queries.size());
	}

	@Test
	void aGeneratedQueryHasNoReferenceTravelTimeOfItsOwn() {

		Network network = TestNetworks.grid(5);
		List<TravelTimeQuery> queries = ODSample.uniform(network, 2, 2, NOON, 1);

		assertTrue(queries.stream().noneMatch(TravelTimeQuery::hasReference),
			"nobody drove a generated pair, so what it should have taken is still to be determined");
		assertTrue(queries.stream().allMatch(query -> Double.isNaN(query.distance())),
			"and no path has been computed for it either");
	}

	@Test
	void theSameSeedGivesTheSameSample() {

		Network network = TestNetworks.grid(9);

		List<TravelTimeQuery> first = ODSample.byDistanceBands(network, 5, BANDS, NOON, 42);
		List<TravelTimeQuery> second = ODSample.byDistanceBands(network, 5, BANDS, NOON, 42);
		List<TravelTimeQuery> other = ODSample.byDistanceBands(network, 5, BANDS, NOON, 43);

		assertEquals(first, second);
		assertFalse(first.equals(other), "another seed gives another sample");
	}

	@Test
	void everyPairIsAskedAtEveryDepartureTime() {

		Network network = TestNetworks.grid(5);
		List<TravelTimeQuery> queries = ODSample.uniform(network, 4, 3, ODSample.hourly(7, 10), 7);

		assertEquals(4 * 3 * 3, queries.size(), "four origins, three destinations each, three hours");
		assertEquals(4, queries.stream().map(TravelTimeQuery::fromNode).distinct().count());
		assertEquals(3, queries.stream().map(TravelTimeQuery::departureTime).distinct().count());
		assertTrue(queries.stream().noneMatch(query -> query.fromNode() == query.toNode()),
			"a pair of a node with itself is not a query");
	}

	@Test
	void aSampleIsNotAskedForMoreOriginsOrDestinationsThanTheNetworkHasNodes() {

		Network network = TestNetworks.corridor();
		List<TravelTimeQuery> queries = ODSample.uniform(network, 100, 100, NOON, 1);

		assertEquals(4, queries.stream().map(TravelTimeQuery::fromNode).distinct().count(),
			"the corridor has four nodes");
		assertEquals(4 * 3, queries.size(), "every node but the origin itself, and no more than that");
	}

	@Test
	void bandsThatNoNodeCanFillAreRefusedRatherThanSearchedForEver() {

		// the nodes of the corridor are 1 km apart, so no node lies within 1 km of another one: the
		// nearest node to a point 500 m from the origin is the origin itself
		Network network = TestNetworks.corridor();

		IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
			() -> ODSample.byDistanceBands(network, 2, new double[]{0}, NOON, 1));
		assertTrue(thrown.getMessage().contains("The bands have to fit the study area"));
	}

	@Test
	void aBandThatOnlySomeOriginsCanFillStillGivesASample() {

		// 4 to 8 km in a grid of 5 by 5 km: the corners can fill it, the middle cannot
		Network network = TestNetworks.grid(6);
		List<TravelTimeQuery> queries = ODSample.byDistanceBands(network, 20, new double[]{1000, 4000}, NOON, 3);

		assertTrue(!queries.isEmpty());
		assertTrue(queries.stream().map(TravelTimeQuery::fromNode).distinct().count() <= 20);
	}

	@Test
	void theDistanceBandsHaveToAscend() {
		Network network = TestNetworks.grid(5);
		assertThrows(IllegalArgumentException.class,
			() -> ODSample.byDistanceBands(network, 2, new double[]{2000, 1000}, NOON, 1));
	}

	@Test
	void hourlyDepartureTimesAreSecondsAfterMidnight() {
		assertArrayEquals(new double[]{7 * 3600.0, 8 * 3600.0, 9 * 3600.0}, ODSample.hourly(7, 10));
		assertEquals(24, ODSample.HOURLY_DEPARTURE_TIMES.length);
		assertThrows(IllegalArgumentException.class, () -> ODSample.hourly(10, 10));
	}

	@Test
	void writtenQueriesReadBackAsTheSameQuestions() {

		Network network = TestNetworks.grid(5);
		List<TravelTimeQuery> written = ODSample.byDistanceBands(network, 6, BANDS, ODSample.hourly(8, 10), 99);

		Path file = Path.of(utils.getOutputDirectory(), "od_queries.csv");
		TravelTimeQuery.write(written, file.toString());
		List<TravelTimeQuery> read = TravelTimeQuery.read(file.toString(), network);

		assertEquals(written.size(), read.size());
		for (int i = 0; i < written.size(); i++) {
			assertEquals(written.get(i).fromNode(), read.get(i).fromNode());
			assertEquals(written.get(i).toNode(), read.get(i).toNode());
			assertEquals(written.get(i).departureTime(), read.get(i).departureTime(), 1e-9);
			assertFalse(read.get(i).hasReference(), "the sample had no reference travel time to write");
		}
	}

	@Test
	void aWrittenReferenceTravelTimeComesBackWithTheQuery() {

		Network network = TestNetworks.grid(5);
		Node from = network.getNodes().values().iterator().next();
		Node to = network.getNodes().values().stream().filter(node -> node != from).findFirst().orElseThrow();

		// the column is what makes the csv an interchange: a travel time fetched from a routing API can
		// be written into it and evaluated as if it had been measured
		Path file = Path.of(utils.getOutputDirectory(), "with_reference.csv");
		TravelTimeQuery.write(List.of(new TravelTimeQuery(from, to, 28800, 612.5, 4200, Double.NaN)),
			file.toString());

		TravelTimeQuery read = TravelTimeQuery.read(file.toString(), network).get(0);
		assertTrue(read.hasReference());
		assertEquals(612.5, read.referenceTravelTime(), 1e-9);
		assertEquals(4200, read.distance(), 1e-9);
	}

	@Test
	void queriesOfNodesTheNetworkDoesNotHoldAreRefused() {

		Network grid = TestNetworks.grid(5);
		Path file = Path.of(utils.getOutputDirectory(), "od_queries.csv");
		TravelTimeQuery.write(ODSample.uniform(grid, 3, 2, NOON, 5), file.toString());

		// the same queries, read over a different network - which is what cutting the study area
		// differently does, and what would otherwise silently drop them
		IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
			() -> TravelTimeQuery.read(file.toString(), TestNetworks.corridor()));
		assertTrue(thrown.getMessage().contains("which the network does not hold"));
	}

	@Test
	void aSampleNeedsADepartureTime() {
		Network network = TestNetworks.grid(5);
		assertThrows(IllegalArgumentException.class, () -> ODSample.uniform(network, 2, 2, new double[0], 1));
	}
}
