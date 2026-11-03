package org.matsim.contrib.msp.traveltimematrix.evaluation;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.dvrp.trafficmonitoring.QSimFreeSpeedTravelTime;
import org.matsim.contrib.msp.traveltimematrix.TestNetworks;
import org.matsim.contrib.zone.skims.TravelTimeMatrix;
import org.matsim.core.router.util.TravelTime;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the reference the matrix is judged against: the time-dependent optimum (variant 4) and the
 * free-speed path timed under congestion (level 3).
 * <p>
 * The networks are built so that the expected answer can be written down. On the
 * {@link TestNetworks#corridor()} there is only one path, so the two rungs have to agree exactly, which
 * is the test that level 3 is the same journey as variant 4. On a {@link TestNetworks#grid(int)} there
 * are alternatives, so slowing down one link of the free-speed path makes the optimum reroute and level
 * 3 stay behind - the gap the evaluation exists to measure.
 */
class RoutedTravelTimesTest {

	/**
	 * At a 1 s time step, {@code QSimFreeSpeedTravelTime} gives a 1 km residential link at 10 m/s 101 s
	 * and the 1 km motorway link at 50 m/s 21 s, so the corridor end to end is 223 s.
	 */
	private static final double CORRIDOR_END_TO_END = 223;
	private static final double TIME_STEP = 1;

	private static final double MORNING = 8 * 3600;
	private static final double NIGHT = 3 * 3600;

	@Test
	void withoutCongestionTheReferenceIsTheFreeSpeedTravelTime() {

		Network network = TestNetworks.corridor();
		ODSample sample = corridorSample(network, new double[]{NIGHT, MORNING});

		RoutedTravelTimes routed = RoutedTravelTimes.compute(network, sample,
			new QSimFreeSpeedTravelTime(TIME_STEP), TIME_STEP, 1);

		RoutedTravelTimes.PairTravelTimes pair = routed.get(node(network, "n1"), node(network, "n4"));
		assertEquals(CORRIDOR_END_TO_END, pair.freeSpeedTravelTime(), 1e-9);
		assertEquals(3 * TestNetworks.SPACING, pair.distance(), 1e-9, "three links of 1 km");

		for (int t = 0; t < sample.getNumberOfDepartureTimes(); t++) {
			assertEquals(CORRIDOR_END_TO_END, pair.timeDependent()[t], 1e-9,
				"an empty network is equally empty at every time of day");
			assertEquals(CORRIDOR_END_TO_END, pair.fixedPath()[t], 1e-9);
		}
	}

	@Test
	void theReferenceFollowsTheTimeOfDay() {

		Network network = TestNetworks.corridor();
		ODSample sample = corridorSample(network, new double[]{NIGHT, MORNING});

		// everything takes twice as long during the morning hour, and free speed otherwise
		RoutedTravelTimes routed = RoutedTravelTimes.compute(network, sample, slowedDown(2, MORNING, null),
			TIME_STEP, 1);

		assertEquals(CORRIDOR_END_TO_END, routed.getTimeDependentTravelTime(node(network, "n1"),
			node(network, "n4"), 0), 1e-9, "at night");
		assertEquals(2 * CORRIDOR_END_TO_END, routed.getTimeDependentTravelTime(node(network, "n1"),
			node(network, "n4"), 1), 1e-9, "in the morning peak");
	}

	@Test
	void whenThereIsNoAlternativePathTheFixedPathIsTheOptimum() {

		Network network = TestNetworks.corridor();
		ODSample sample = corridorSample(network, new double[]{MORNING});

		// the motorway in the middle of the corridor is five times slower in the morning. There is no
		// way around it, so holding the path fixed cannot cost anything.
		RoutedTravelTimes routed = RoutedTravelTimes.compute(network, sample, slowedDown(5, MORNING, "2-3"),
			TIME_STEP, 1);

		RoutedTravelTimes.PairTravelTimes pair = routed.get(node(network, "n1"), node(network, "n4"));
		assertEquals(pair.timeDependent()[0], pair.fixedPath()[0], 1e-9);
		assertEquals(CORRIDOR_END_TO_END + 4 * 21, pair.timeDependent()[0], 1e-9,
			"the 21 s motorway link takes five times as long");
	}

	@Test
	void holdingThePathFixedCostsTimeWhenThereIsAWayAround() {

		Network network = TestNetworks.grid(3);
		Node origin = node(network, "n0-0");
		Node destination = node(network, "n2-0");
		ODSample sample = ODSample.of(Map.of(origin, List.of(destination)), new double[]{MORNING});

		// the free-speed path along the bottom row is the straight one; slow its first link down enough
		// that going around the block is quicker
		RoutedTravelTimes routed = RoutedTravelTimes.compute(network, sample,
			slowedDown(10, MORNING, "n0-0_n1-0"), TIME_STEP, 1);

		RoutedTravelTimes.PairTravelTimes pair = routed.get(origin, destination);
		assertTrue(pair.fixedPath()[0] > pair.timeDependent()[0],
			"the optimum reroutes around the slow link (" + pair.timeDependent()[0] + " s) while the free-speed "
				+ "path drives through it (" + pair.fixedPath()[0] + " s)");
		assertEquals(2 * 101 + 9 * 101, pair.fixedPath()[0], 1e-9,
			"two residential links, the first of them ten times slower");
	}

	@Test
	void theRungsAreHandedOutAsTravelTimeMatrices() {

		Network network = TestNetworks.corridor();
		ODSample sample = corridorSample(network, new double[]{MORNING});
		RoutedTravelTimes routed = RoutedTravelTimes.compute(network, sample, slowedDown(2, MORNING, null),
			TIME_STEP, 1);

		TravelTimeMatrix reference = routed.asTimeDependentMatrix();
		TravelTimeMatrix fixedPath = routed.asFixedPathMatrix();
		Node n1 = node(network, "n1");
		Node n4 = node(network, "n4");

		assertEquals(Math.round(2 * CORRIDOR_END_TO_END), reference.getTravelTime(n1, n4, MORNING));
		assertEquals(Math.round(2 * CORRIDOR_END_TO_END), fixedPath.getTravelTime(n1, n4, MORNING));

		// a lookup table, not a router: a departure time or a pair that was not sampled is an error and
		// not the nearest answer, which would compare two different questions
		assertThrows(IllegalArgumentException.class, () -> reference.getTravelTime(n1, n4, NIGHT));
		assertThrows(IllegalArgumentException.class,
			() -> reference.getTravelTime(n4, n1, MORNING));
	}

	/**
	 * Both ends of the corridor as one pair, which is the longest path it has.
	 */
	private static ODSample corridorSample(Network network, double[] departureTimes) {
		return ODSample.of(Map.of(node(network, "n1"), List.of(node(network, "n4"))), departureTimes);
	}

	/**
	 * Free-speed travel times, times a factor during the hour that starts at {@code from}.
	 *
	 * @param linkId the only link that is slowed down, or null for all of them
	 */
	private static TravelTime slowedDown(double factor, double from, String linkId) {
		TravelTime freeSpeed = new QSimFreeSpeedTravelTime(TIME_STEP);
		return (link, time, person, vehicle) -> {
			double freeSpeedTime = freeSpeed.getLinkTravelTime(link, time, person, vehicle);
			boolean slowLink = linkId == null || link.getId().equals(Id.createLinkId(linkId));
			boolean slowHour = time >= from && time < from + 3600;
			return slowLink && slowHour ? factor * freeSpeedTime : freeSpeedTime;
		};
	}

	private static Node node(Network network, String id) {
		return network.getNodes().get(Id.createNodeId(id));
	}
}
