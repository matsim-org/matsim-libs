package org.matsim.contrib.msp.capability.measure;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.msp.capability.CapabilityConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.Facility;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the travel times per mode on a small network that has a car-only short cut and a bike-only way
 * around it, so that a mode which used the wrong links would show up in the numbers.
 */
public class CapabilityTravelTimesTest {

	/** 15 km/h, the speed the bike vehicle type of these tests is given. */
	private static final double BIKE_SPEED = 15 / 3.6;

	/** the free speed of every link but the slow one: 36 km/h, well above what a bike does. */
	private static final double LINK_SPEED = 10;

	private static final double DEPARTURE = 8 * 3600;

	@Test
	void bikeTakesItsOwnLinksAtItsOwnSpeed() {

		Scenario scenario = scenario(TransportMode.car, TransportMode.bike);
		addBikeVehicleType(scenario);

		Map<String, CapabilityTravelTimes> byMode = CapabilityTravelTimes.of(scenario,
			config(TransportMode.car, TransportMode.bike));

		Facility start = onLink("0-1");
		Facility end = onLink("2-5");

		// car takes the direct link 1-2 and then the destination link, both 1000 m at the link free speed.
		// No vehicle type names car, so nothing caps it - free speed car is what it was before.
		assertEquals(2 * 1000 / LINK_SPEED, byMode.get(TransportMode.car).travelTime(start, end, DEPARTURE), 1e-6);

		// bike may not use link 1-2, so it goes the long way round over three links and then the
		// destination link - and every one of the four at 15 km/h rather than at the links' 36 km/h
		assertEquals(4 * 1000 / BIKE_SPEED, byMode.get(TransportMode.bike).travelTime(start, end, DEPARTURE), 1e-6);
	}

	@Test
	void aLinkSlowerThanTheBikeKeepsItsOwnSpeed() {

		Scenario scenario = scenario(TransportMode.car, TransportMode.bike);
		addBikeVehicleType(scenario);

		Map<String, CapabilityTravelTimes> byMode = CapabilityTravelTimes.of(scenario,
			config(TransportMode.car, TransportMode.bike));

		// the speed of the vehicle type is a cap, not a replacement: on a link whose free speed is below
		// it - a pedestrian zone, say - the bike is as slow as the link says, and no faster than the car
		Facility slow = onLink("1-slow");
		assertEquals(1000 / 2.0, byMode.get(TransportMode.bike).travelTime(onLink("0-1"), slow, DEPARTURE), 1e-6);
		assertEquals(1000 / 2.0, byMode.get(TransportMode.car).travelTime(onLink("0-1"), slow, DEPARTURE), 1e-6);
	}

	@Test
	void aNetworkModeWithoutAVehicleTypeRunsAtLinkFreeSpeed() {

		// no vehicle type at all, so nothing says how fast a bike is and the links alone decide. The way
		// round is still the way a bike has to go.
		Scenario scenario = scenario(TransportMode.car, TransportMode.bike);

		Map<String, CapabilityTravelTimes> byMode = CapabilityTravelTimes.of(scenario,
			config(TransportMode.car, TransportMode.bike));

		assertEquals(4 * 1000 / LINK_SPEED,
			byMode.get(TransportMode.bike).travelTime(onLink("0-1"), onLink("2-5"), DEPARTURE), 1e-6);
	}

	@Test
	void beingARoutedModeBeatsHavingATeleportedSpeed() {

		// MATSim's own config defaults give bike a teleported speed of 15 km/h, so a mode can be both.
		// A mode the run routes on the network has to be routed on the network here as well.
		Scenario scenario = scenario(TransportMode.car, TransportMode.bike);
		assertTrue(scenario.getConfig().routing().getTeleportedModeParams().containsKey(TransportMode.bike),
			"this test is only about something if bike does carry teleported parameters");
		addBikeVehicleType(scenario);

		CapabilityTravelTimes bike = CapabilityTravelTimes.of(scenario, config(TransportMode.bike))
			.get(TransportMode.bike);

		// teleported, this would be the beeline 2000 m / 4.17 m/s = 480 s; routed, it is the way round
		assertEquals(4 * 1000 / BIKE_SPEED, bike.travelTime(onLink("0-1"), onLink("2-5"), DEPARTURE), 1e-6);
	}

	@Test
	void aModeThatIsNotRoutedOnTheNetworkIsTeleported() {

		Scenario scenario = scenario(TransportMode.car);
		scenario.getConfig().routing().getOrCreateModeRoutingParams(TransportMode.walk)
			.setTeleportedModeSpeed(1.0);
		scenario.getConfig().routing().getOrCreateModeRoutingParams(TransportMode.walk)
			.setBeelineDistanceFactor(1.3);

		CapabilityTravelTimes walk = CapabilityTravelTimes.of(scenario, config(TransportMode.walk))
			.get(TransportMode.walk);

		// 3-4-5 triangle: 5000 m of beeline, 1.3 of detour factor, 1 m/s
		assertEquals(5000 * 1.3, walk.travelTime(at(0, 0), at(3000, 4000), DEPARTURE), 1e-6);
	}

	@Test
	void aModeThatIsNeitherRoutedNorTeleportedFails() {
		Scenario scenario = scenario(TransportMode.car);
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
			() -> CapabilityTravelTimes.of(scenario, config("hovercraft")));
		assertTrue(e.getMessage().contains("hovercraft"));
	}

	@Test
	void aNetworkModeNoLinkAllowsFails() {
		Scenario scenario = scenario(TransportMode.car, TransportMode.truck);
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
			() -> CapabilityTravelTimes.of(scenario, config(TransportMode.truck)));
		assertTrue(e.getMessage().contains(TransportMode.truck));
	}

	@Test
	void aPlaceIsPositionedByItsCoordinateInEachModesOwnNetwork() {

		Scenario scenario = scenario(TransportMode.car, TransportMode.bike);
		addBikeVehicleType(scenario);
		Map<String, CapabilityTravelTimes> byMode = CapabilityTravelTimes.of(scenario,
			config(TransportMode.car, TransportMode.bike));

		// a place at node 3, which only bike-only links touch, naming the car-only link 1-2 the way a run
		// or a places csv would. The coordinate wins: bike starts from the cycleway at node 3, not from
		// the car link, which is the whole point for a car-free street.
		Facility place = new Place(Id.createLinkId("1-2"), new Coord(1000, 1000));

		// bike: the place sits on 1-3, whose toNode is node 3, so the way on is 3-4, 4-2 and then 2-5
		assertEquals(3 * 1000 / BIKE_SPEED,
			byMode.get(TransportMode.bike).travelTime(place, onLink("2-5"), DEPARTURE), 1e-6);

		// car cannot reach node 3 at all, so it falls back to the nearest link of its own network - which
		// is a position, not an error: the car is parked on the nearest street
		assertTrue(Double.isFinite(byMode.get(TransportMode.car).travelTime(place, onLink("2-5"), DEPARTURE)));
	}

	@Test
	void aPlaceWithoutACoordinateFallsBackToTheLinkItNames() {

		Scenario scenario = scenario(TransportMode.car, TransportMode.bike);
		addBikeVehicleType(scenario);
		Map<String, CapabilityTravelTimes> byMode = CapabilityTravelTimes.of(scenario,
			config(TransportMode.car, TransportMode.bike));

		// the places csv allows a link_id as the only location, and then there is nothing else to go on
		Facility onCarLinkOnly = new Place(Id.createLinkId("1-2"), null);

		assertEquals(1000 / LINK_SPEED,
			byMode.get(TransportMode.car).travelTime(onLink("0-1"), onCarLinkOnly, DEPARTURE), 1e-6);

		// bike may not use that link and has no coordinate to snap from, so the place is out of its reach
		assertEquals(Double.POSITIVE_INFINITY,
			byMode.get(TransportMode.bike).travelTime(onLink("0-1"), onCarLinkOnly, DEPARTURE));
	}

	private static CapabilityConfigGroup config(String... modes) {
		CapabilityConfigGroup config = new CapabilityConfigGroup();
		config.setModes(new java.util.LinkedHashSet<>(List.of(modes)));
		return config;
	}

	private static Scenario scenario(String... networkModes) {
		Config config = ConfigUtils.createConfig();
		config.routing().setNetworkModes(List.of(networkModes));
		Scenario scenario = ScenarioUtils.createScenario(config);
		buildNetwork(scenario.getNetwork());
		return scenario;
	}

	private static void addBikeVehicleType(Scenario scenario) {
		scenario.getVehicles().addVehicleType(VehicleUtils
			.createVehicleType(Id.create(TransportMode.bike, VehicleType.class))
			.setMaximumVelocity(BIKE_SPEED));
	}

	/**
	 * <pre>
	 *                   1-3        3-4
	 *            (1000,1000) --> (2000,1000)          bike only
	 *                 ^                |
	 *             1-3 |                | 4-2
	 *      0-1        |      1-2       v        2-5
	 * (0,0) --> (1000,0) ----------> (2000,0) --> (3000,0)
	 *                 |         car only
	 *          1-slow |
	 *                 v
	 *           (1000,-1000)                             2 m/s, car and bike
	 * </pre>
	 * Every link is 1000 m long and runs at {@link #LINK_SPEED}, except {@code 1-slow}.
	 */
	private static void buildNetwork(Network network) {

		node(network, "0", 0, 0);
		node(network, "1", 1000, 0);
		node(network, "2", 2000, 0);
		node(network, "3", 1000, 1000);
		node(network, "4", 2000, 1000);
		node(network, "5", 3000, 0);
		node(network, "slow", 1000, -1000);

		Set<String> car = Set.of(TransportMode.car);
		Set<String> bike = Set.of(TransportMode.bike);
		Set<String> both = Set.of(TransportMode.car, TransportMode.bike);

		link(network, "0-1", "0", "1", LINK_SPEED, both);
		link(network, "1-2", "1", "2", LINK_SPEED, car);
		link(network, "1-3", "1", "3", LINK_SPEED, bike);
		link(network, "3-4", "3", "4", LINK_SPEED, bike);
		link(network, "4-2", "4", "2", LINK_SPEED, bike);
		link(network, "2-5", "2", "5", LINK_SPEED, both);
		link(network, "1-slow", "1", "slow", 2, both);
	}

	private static void node(Network network, String id, double x, double y) {
		NetworkUtils.createAndAddNode(network, Id.createNodeId(id), new Coord(x, y));
	}

	private static void link(Network network, String id, String from, String to, double freeSpeed, Set<String> modes) {
		Node fromNode = network.getNodes().get(Id.createNodeId(from));
		Node toNode = network.getNodes().get(Id.createNodeId(to));
		Link link = NetworkUtils.createAndAddLink(network, Id.createLinkId(id), fromNode, toNode, 1000, freeSpeed, 1800, 1);
		link.setAllowedModes(modes);
	}

	private static Set<String> linkIds(Network network) {
		return network.getLinks().keySet().stream().map(Id::toString).collect(java.util.stream.Collectors.toSet());
	}

	private static Facility onLink(String linkId) {
		return new Place(Id.createLinkId(linkId), null);
	}

	private static Facility at(double x, double y) {
		return new Place(null, new Coord(x, y));
	}

	/**
	 * A place that is nothing but a link, a coordinate, or both - which is all the travel times read.
	 */
	private record Place(Id<Link> linkId, Coord coord) implements Facility {

		@Override
		public Coord getCoord() {
			return this.coord;
		}

		@Override
		public Id<Link> getLinkId() {
			return this.linkId;
		}

		@Override
		public Map<String, Object> getCustomAttributes() {
			return Map.of();
		}
	}
}
