package org.matsim.contrib.msp.traveltimematrix.evaluation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.msp.traveltimematrix.TestNetworks;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the sample an evaluation is carried out on: that a sample by distance bands really does put one
 * destination in each band, that a seed reproduces a sample, and that a written sample reads back.
 */
class ODSampleTest {

	@RegisterExtension
	private final MatsimTestUtils utils = new MatsimTestUtils();

	private static final double[] BANDS = {1000, 2000, 4000};
	private static final double[] NOON = {12 * 3600.0};

	@Test
	void everyDestinationLiesInTheBandItWasDrawnFor() {

		Network network = TestNetworks.grid(9);
		ODSample sample = ODSample.byDistanceBands(network, 10, BANDS, NOON, 1234);

		for (Node origin : sample.getOrigins()) {

			List<Node> destinations = sample.getDestinations(origin);
			assertEquals(BANDS.length, destinations.size(),
				"a grid of 9 by 9 km holds a node in each of the bands " + List.of(1000, 2000, 4000) + " m");

			for (int band = 0; band < destinations.size(); band++) {
				double distance = CoordUtils.calcEuclideanDistance(origin.getCoord(),
					destinations.get(band).getCoord());
				double lower = BANDS[band];
				double upper = band + 1 < BANDS.length ? BANDS[band + 1] : 2 * lower;
				assertTrue(distance >= lower && distance < upper,
					"the destination of band " + lower + " m is " + distance + " m away");
			}
		}
	}

	@Test
	void theSameSeedGivesTheSameSample() {

		Network network = TestNetworks.grid(9);

		ODSample first = ODSample.byDistanceBands(network, 5, BANDS, NOON, 42);
		ODSample second = ODSample.byDistanceBands(network, 5, BANDS, NOON, 42);
		ODSample other = ODSample.byDistanceBands(network, 5, BANDS, NOON, 43);

		assertEquals(first.getOrigins(), second.getOrigins());
		first.getOrigins().forEach(origin -> assertEquals(first.getDestinations(origin),
			second.getDestinations(origin)));
		assertEquals(first.getNumberOfPairs(), second.getNumberOfPairs());

		// not a property of the sampling, but of this test being able to tell two samples apart at all
		assertTrue(!first.getOrigins().equals(other.getOrigins())
			|| !first.getDestinations(first.getOrigins().get(0))
			.equals(other.getDestinations(other.getOrigins().get(0))), "another seed gives another sample");
	}

	@Test
	void aUniformSampleHasAsManyPairsAsItWasAskedFor() {

		Network network = TestNetworks.grid(5);
		ODSample sample = ODSample.uniform(network, 4, 3, ODSample.hourly(7, 10), 7);

		assertEquals(4, sample.getOrigins().size());
		assertEquals(12, sample.getNumberOfPairs());
		assertEquals(3, sample.getNumberOfDepartureTimes());
		assertEquals(36, sample.getNumberOfQueries());

		sample.getOrigins().forEach(origin -> {
			List<Node> destinations = sample.getDestinations(origin);
			assertEquals(3, destinations.size());
			assertTrue(!destinations.contains(origin), "a pair of a node with itself is not a query");
			assertEquals(3, Set.copyOf(destinations).size(), "the destinations of one origin are distinct");
		});
	}

	@Test
	void aSampleIsNotAskedForMoreOriginsOrDestinationsThanTheNetworkHasNodes() {

		Network network = TestNetworks.corridor();
		ODSample sample = ODSample.uniform(network, 100, 100, NOON, 1);

		assertEquals(4, sample.getOrigins().size(), "the corridor has four nodes");
		sample.getOrigins().forEach(origin -> assertEquals(3, sample.getDestinations(origin).size(),
			"every node but the origin itself, and no more than that"));
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
		ODSample sample = ODSample.byDistanceBands(network, 20, new double[]{1000, 4000}, NOON, 3);

		assertTrue(sample.getNumberOfPairs() >= sample.getOrigins().size(),
			"every origin that made it into the sample has at least one destination");
		assertTrue(sample.getOrigins().size() <= 20);
	}

	@Test
	void aWrittenSampleReadsBackAsTheSameQueries() {

		Network network = TestNetworks.grid(5);
		ODSample written = ODSample.byDistanceBands(network, 6, BANDS, ODSample.hourly(8, 10), 99);

		Path file = Path.of(utils.getOutputDirectory(), "od_sample.csv");
		written.write(file.toString());

		ODSample read = ODSample.read(file.toString(), network);

		assertEquals(written.getOrigins(), read.getOrigins());
		assertEquals(written.getNumberOfPairs(), read.getNumberOfPairs());
		assertArrayEquals(written.getDepartureTimes(), read.getDepartureTimes());
		written.getOrigins().forEach(origin -> assertEquals(written.getDestinations(origin),
			read.getDestinations(origin)));
	}

	@Test
	void aSampleOfNodesTheNetworkDoesNotHoldIsRefused() {

		Network grid = TestNetworks.grid(5);
		Path file = Path.of(utils.getOutputDirectory(), "od_sample.csv");
		ODSample.uniform(grid, 3, 2, NOON, 5).write(file.toString());

		// the same sample, read over a different network - which is what cutting the study area
		// differently does, and what would otherwise silently drop queries
		IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
			() -> ODSample.read(file.toString(), TestNetworks.corridor()));
		assertTrue(thrown.getMessage().contains("which the network does not hold"));
	}

	@Test
	void aSampleWithoutQueriesIsRefused() {

		Network network = TestNetworks.corridor();
		Node n1 = network.getNodes().get(Id.createNodeId("n1"));
		Node n2 = network.getNodes().get(Id.createNodeId("n2"));

		assertThrows(IllegalArgumentException.class, () -> ODSample.of(Map.of(), NOON),
			"no origin at all");
		assertThrows(IllegalArgumentException.class, () -> ODSample.of(Map.of(n1, List.of()), NOON),
			"an origin without destinations costs a tree and answers nothing");
		assertThrows(IllegalArgumentException.class, () -> ODSample.of(Map.of(n1, List.of(n2)), new double[0]),
			"no departure time");
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
}
