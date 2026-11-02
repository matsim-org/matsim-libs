package org.matsim.contrib.msp.traveltimematrix.congestion;

import org.junit.jupiter.api.Test;
import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.application.analysis.traffic.TrafficStatsCalculator;
import org.matsim.contrib.dvrp.trafficmonitoring.QSimFreeSpeedTravelTime;
import org.matsim.contrib.msp.traveltimematrix.RoadClassification;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the congestion index measured on a run.
 * <p>
 * The test that matters most is {@link #theNetworkWideIndexIsTheOneOfTrafficStatsCalculator()}: this
 * class is a rewrite of {@link TrafficStatsCalculator#getTomTomNetworkCongestionIndex(int, int, String)}
 * that computes every road class and time bin in one pass, and the rewrite is only worth anything if it
 * returns the same numbers. The rest of the tests pin down what the road class split and the volume
 * weighting do.
 */
class CongestionIndexCalculatorTest {

	private static final int TIME_BIN_SIZE = 900;
	private static final int END_TIME = 3600;

	/**
	 * A network of two parallel links of 1 km, one motorway and one residential street, so that a road
	 * class index can differ from the network-wide one.
	 */
	private static Network twoRoadClasses() {
		Network network = NetworkUtils.createNetwork();
		var a = NetworkUtils.createAndAddNode(network, Id.createNodeId("a"), new org.matsim.api.core.v01.Coord(0, 0));
		var b = NetworkUtils.createAndAddNode(network, Id.createNodeId("b"), new org.matsim.api.core.v01.Coord(1000, 0));
		var c = NetworkUtils.createAndAddNode(network, Id.createNodeId("c"), new org.matsim.api.core.v01.Coord(0, 500));
		var d = NetworkUtils.createAndAddNode(network, Id.createNodeId("d"), new org.matsim.api.core.v01.Coord(1000, 500));

		Link motorway = NetworkUtils.createAndAddLink(network, Id.createLinkId("motorway"), a, b, 1000, 25, 1800, 1);
		NetworkUtils.setType(motorway, "motorway");
		Link residential = NetworkUtils.createAndAddLink(network, Id.createLinkId("residential"), c, d, 1000, 10, 600, 1);
		NetworkUtils.setType(residential, "residential");

		return network;
	}

	@Test
	void theNetworkWideIndexIsTheOneOfTrafficStatsCalculator() {

		Network network = twoRoadClasses();
		TravelTime travelTime = congestedBy(Map.of("motorway", 1.5, "residential", 1.1));
		VolumesAnalyzer volumes = volumes(network, Map.of("motorway", 100, "residential", 20));

		CongestionIndexProfile profile = new CongestionIndexCalculator(network, travelTime, volumes,
			RoadClassification.single(), 1).calculate(TIME_BIN_SIZE, END_TIME);

		// the reference, constructed so that one of its queries covers exactly one bin of the profile
		TrafficStatsCalculator reference = new TrafficStatsCalculator(network, travelTime, volumes, TIME_BIN_SIZE);

		for (int bin = 0; bin < profile.getNumberOfBins(); bin++) {
			int from = bin * TIME_BIN_SIZE;
			assertEquals(reference.getTomTomNetworkCongestionIndex(from, from + TIME_BIN_SIZE, null),
				profile.get(from), 1e-12, "bin starting at " + from + " s");
		}
	}

	@Test
	void theIndexOfOneRoadClassIsTheOneOfTrafficStatsCalculatorForThatType() {

		Network network = twoRoadClasses();
		TravelTime travelTime = congestedBy(Map.of("motorway", 1.5, "residential", 1.1));
		VolumesAnalyzer volumes = volumes(network, Map.of("motorway", 100, "residential", 20));

		// one class per highway type, so that a class is exactly what the reference can filter on
		CongestionIndexProfile profile = new CongestionIndexCalculator(network, travelTime, volumes,
			RoadClassification.byHighwayType(network), 1).calculate(TIME_BIN_SIZE, END_TIME);

		TrafficStatsCalculator reference = new TrafficStatsCalculator(network, travelTime, volumes, TIME_BIN_SIZE);

		for (String roadClass : new String[]{"motorway", "residential"}) {
			assertEquals(reference.getTomTomNetworkCongestionIndex(0, TIME_BIN_SIZE, roadClass),
				profile.get(0, roadClass), 1e-12, roadClass);
		}
	}

	@Test
	void aRoadClassThatIsHalfAsSlowHasHalfTheIndex() {

		Network network = twoRoadClasses();
		TravelTime travelTime = congestedBy(Map.of("motorway", 1.4, "residential", 1.2));
		VolumesAnalyzer volumes = volumes(network, Map.of("motorway", 50, "residential", 50));

		CongestionIndexProfile profile = new CongestionIndexCalculator(network, travelTime, volumes,
			RoadClassification.byHighwayType(network), 1).calculate(TIME_BIN_SIZE, END_TIME);

		// the link travel times are a clean multiple of the free-speed ones, so the index of a class is
		// exactly that multiple minus one, whatever the volumes are
		assertEquals(0.4, profile.get(0, "motorway"), 1e-9);
		assertEquals(0.2, profile.get(0, "residential"), 1e-9);
	}

	@Test
	void theNetworkWideIndexIsWeightedByVolume() {

		Network network = twoRoadClasses();
		TravelTime travelTime = congestedBy(Map.of("motorway", 1.4, "residential", 1.2));

		// all the traffic on the motorway: the network-wide index is the motorway's
		CongestionIndexProfile motorwayOnly = new CongestionIndexCalculator(network, travelTime,
			volumes(network, Map.of("motorway", 100, "residential", 0)), RoadClassification.single(), 1)
			.calculate(TIME_BIN_SIZE, END_TIME);
		assertEquals(0.4, motorwayOnly.get(0), 1e-9);

		// all of it on the residential street: the index is the street's, although the motorway is just
		// as congested. The index answers "how much longer does the average vehicle take", so an empty
		// road does not dilute it.
		CongestionIndexProfile residentialOnly = new CongestionIndexCalculator(network, travelTime,
			volumes(network, Map.of("motorway", 0, "residential", 100)), RoadClassification.single(), 1)
			.calculate(TIME_BIN_SIZE, END_TIME);
		assertEquals(0.2, residentialOnly.get(0), 1e-9);
	}

	@Test
	void aRoadClassWithoutTrafficGetsTheNetworkWideIndex() {

		Network network = twoRoadClasses();
		TravelTime travelTime = congestedBy(Map.of("motorway", 1.4, "residential", 1.2));
		VolumesAnalyzer volumes = volumes(network, Map.of("motorway", 100, "residential", 0));

		CongestionIndexProfile profile = new CongestionIndexCalculator(network, travelTime, volumes,
			RoadClassification.byDefaultGroups(network), 1).calculate(TIME_BIN_SIZE, END_TIME);

		assertEquals(0.4, profile.get(0, "motorway"), 1e-9);
		// nothing drove here, so there is nothing to measure and the network-wide value stands in
		assertEquals(profile.get(0, RoadClassification.ALL), profile.get(0, "residential"), 1e-12);
		// a class the network has no links of at all is in the profile as well, for the same reason
		assertEquals(profile.get(0, RoadClassification.ALL), profile.get(0, "primary"), 1e-12);
	}

	@Test
	void anEmptyNetworkIsNotCongested() {

		Network network = twoRoadClasses();
		TravelTime travelTime = congestedBy(Map.of("motorway", 1.0, "residential", 1.0));
		VolumesAnalyzer volumes = volumes(network, Map.of("motorway", 100, "residential", 100));

		CongestionIndexProfile profile = new CongestionIndexCalculator(network, travelTime, volumes,
			RoadClassification.single(), 1).calculate(TIME_BIN_SIZE, END_TIME);

		assertEquals(0.0, profile.getDailyMean(RoadClassification.ALL), 1e-12);
	}

	@Test
	void theProfileCoversTheWholeDayAsked() {

		Network network = twoRoadClasses();
		CongestionIndexProfile profile = new CongestionIndexCalculator(network,
			congestedBy(Map.of("motorway", 1.2, "residential", 1.2)),
			volumes(network, Map.of("motorway", 10, "residential", 10)), RoadClassification.single(), 1)
			.calculate(900, 86400);

		assertEquals(96, profile.getNumberOfBins());
		assertEquals(900, profile.getTimeBinSize());
		assertTrue(profile.getRoadClasses().contains(RoadClassification.ALL));
	}

	/**
	 * Travel times that are a fixed multiple of the free-speed ones, per highway type. A multiple rather
	 * than a difference, so that the index - which is a ratio - comes out as a round number the test can
	 * assert on without reproducing the arithmetic of the class under test.
	 */
	private static TravelTime congestedBy(Map<String, Double> factorByHighwayType) {
		QSimFreeSpeedTravelTime freeSpeed = new QSimFreeSpeedTravelTime(1);
		return new TravelTime() {
			@Override
			public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
				double factor = factorByHighwayType.getOrDefault(NetworkUtils.getHighwayType(link), 1.0);
				return freeSpeed.getLinkTravelTime(link, time, person, vehicle) * factor;
			}
		};
	}

	/**
	 * A {@link VolumesAnalyzer} holding the given number of vehicles per link in the first hour, fed the
	 * way a run would feed it - one {@link LinkLeaveEvent} per vehicle.
	 */
	private static VolumesAnalyzer volumes(Network network, Map<String, Integer> volumeByLink) {
		VolumesAnalyzer volumes = new VolumesAnalyzer(3600, 86400, network, false);
		for (Map.Entry<String, Integer> entry : volumeByLink.entrySet()) {
			for (int vehicle = 0; vehicle < entry.getValue(); vehicle++) {
				volumes.handleEvent(new LinkLeaveEvent(1800, Id.create(vehicle, Vehicle.class),
					Id.createLinkId(entry.getKey())));
			}
		}
		return volumes;
	}
}
