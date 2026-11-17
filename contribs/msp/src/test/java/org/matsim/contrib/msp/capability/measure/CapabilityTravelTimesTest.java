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
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.Facility;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;
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

	/** 1 m/s, so a walking time in seconds is a walking distance in metres. */
	private static final double WALK_SPEED = 1.0;

	private static final double WALK_DETOUR_FACTOR = 1.3;

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
		assertEquals(2 * 1000 / LINK_SPEED, byMode.get(TransportMode.car).travel(start, end, DEPARTURE).travelTime(), 1e-6);

		// bike may not use link 1-2, so it goes the long way round over three links and then the
		// destination link - and every one of the four at 15 km/h rather than at the links' 36 km/h
		assertEquals(4 * 1000 / BIKE_SPEED, byMode.get(TransportMode.bike).travel(start, end, DEPARTURE).travelTime(), 1e-6);
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
		assertEquals(1000 / 2.0, byMode.get(TransportMode.bike).travel(onLink("0-1"), slow, DEPARTURE).travelTime(), 1e-6);
		assertEquals(1000 / 2.0, byMode.get(TransportMode.car).travel(onLink("0-1"), slow, DEPARTURE).travelTime(), 1e-6);
	}

	@Test
	void aNetworkModeWithoutAVehicleTypeRunsAtLinkFreeSpeed() {

		// no vehicle type at all, so nothing says how fast a bike is and the links alone decide. The way
		// round is still the way a bike has to go.
		Scenario scenario = scenario(TransportMode.car, TransportMode.bike);

		Map<String, CapabilityTravelTimes> byMode = CapabilityTravelTimes.of(scenario,
			config(TransportMode.car, TransportMode.bike));

		assertEquals(4 * 1000 / LINK_SPEED,
			byMode.get(TransportMode.bike).travel(onLink("0-1"), onLink("2-5"), DEPARTURE).travelTime(), 1e-6);
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
		assertEquals(4 * 1000 / BIKE_SPEED, bike.travel(onLink("0-1"), onLink("2-5"), DEPARTURE).travelTime(), 1e-6);
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
		assertEquals(5000 * 1.3, walk.travel(at(0, 0), at(3000, 4000), DEPARTURE).travelTime(), 1e-6);
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
			byMode.get(TransportMode.bike).travel(place, onLink("2-5"), DEPARTURE).travelTime(), 1e-6);

		// car cannot reach node 3 at all, so it falls back to the nearest link of its own network - which
		// is a position, not an error: the car is parked on the nearest street
		assertTrue(Double.isFinite(byMode.get(TransportMode.car).travel(place, onLink("2-5"), DEPARTURE).travelTime()));
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
			byMode.get(TransportMode.car).travel(onLink("0-1"), onCarLinkOnly, DEPARTURE).travelTime(), 1e-6);

		// bike may not use that link and has no coordinate to snap from, so the place is out of its reach
		assertEquals(Double.POSITIVE_INFINITY,
			byMode.get(TransportMode.bike).travel(onLink("0-1"), onCarLinkOnly, DEPARTURE).travelTime());
	}

	@Test
	void publicTransportIsRoutedOnTheScheduleAndWaitingCounts() {

		// one line along the x axis, 0 -> 3000, leaving every ten minutes from 08:00 and taking five
		// minutes. The places sit on the stops, so there is no access or egress walk to confuse the
		// numbers and the travel time is the wait plus the ride.
		Scenario scenario = transitScenario();

		CapabilityTravelTimes pt = CapabilityTravelTimes.of(scenario, config(TransportMode.pt))
			.get(TransportMode.pt);

		// five minutes before the first departure: 300 s of waiting and 300 s of riding
		assertEquals(600, pt.travel(at(0, 0), at(3000, 0), 8 * 3600 - 300).travelTime(), 1e-6);

		// a minute after it: the next one leaves at 08:10, so nine minutes of waiting and the same ride.
		// Waiting is what makes pt different from every other mode here - the travel time depends on when
		// the agent leaves, so nothing can be cached per pair of places.
		assertEquals(840, pt.travel(at(0, 0), at(3000, 0), 8 * 3600 + 60).travelTime(), 1e-6);
	}

	@Test
	void aPublicTransportTripWithoutAConnectionIsWalkedInstead() {

		Scenario scenario = transitScenario();
		CapabilityTravelTimes pt = CapabilityTravelTimes.of(scenario, config(TransportMode.pt))
			.get(TransportMode.pt);

		// after the last departure of the day nothing runs any more. The agent still has legs, so the
		// trip is walked rather than called unreachable - the same teleported walk the walk mode uses.
		CapabilityTravelTimes.Trip trip = pt.travel(at(0, 0), at(3000, 0), 23 * 3600);

		assertEquals(3000 * WALK_DETOUR_FACTOR / WALK_SPEED, trip.travelTime(), 1e-6);
		assertEquals(3000 * WALK_DETOUR_FACTOR, trip.walkDistance(), 1e-6,
			"a walked pt trip is walked for its whole length");
	}

	@Test
	void aPublicTransportTripRecordsTheWalkToAndFromTheStops() {

		// the places sit 400 m north of the western stop and 300 m south of the eastern one, so the trip
		// is: walk to the stop, wait for the 08:00 departure, ride, walk off
		Scenario scenario = transitScenario();
		CapabilityTravelTimes pt = CapabilityTravelTimes.of(scenario, config(TransportMode.pt))
			.get(TransportMode.pt);

		CapabilityTravelTimes.Trip trip = pt.travel(at(0, 400), at(3000, -300), 8 * 3600 - 1000);

		// beeline x detour factor, the same formula as the walk mode and as MATSim's own teleportation
		assertEquals((400 + 300) * WALK_DETOUR_FACTOR, trip.walkDistance(), 1e-6);

		// 520 s of access walk from 07:43:20 gets the agent to the stop at 07:52:00, the ride leaves at
		// 08:00 and arrives at 08:05, and 390 s of egress walk land at 08:11:30. The raptor rounds each
		// walk up to whole seconds, so allow a second or two of that.
		assertEquals(8 * 3600 + 690 - (8 * 3600 - 1000), trip.travelTime(), 2);
	}

	@Test
	void walkIsWalkedWholeAndTheNetworkModesWalkNothing() {

		Scenario scenario = scenario(TransportMode.car, TransportMode.bike);
		teleportedWalk(scenario);
		addBikeVehicleType(scenario);

		Map<String, CapabilityTravelTimes> byMode = CapabilityTravelTimes.of(scenario,
			config(TransportMode.car, TransportMode.bike, TransportMode.walk));

		// 3-4-5 triangle, so 5000 m of beeline and 6500 m of walking
		assertEquals(5000 * WALK_DETOUR_FACTOR,
			byMode.get(TransportMode.walk).travel(at(0, 0), at(3000, 4000), DEPARTURE).walkDistance(), 1e-6);

		// car and bike walk nothing: the walk to where the vehicle is parked is not modelled
		assertEquals(0, byMode.get(TransportMode.car)
			.travel(onLink("0-1"), onLink("2-5"), DEPARTURE).walkDistance(), 1e-6);
		assertEquals(0, byMode.get(TransportMode.bike)
			.travel(onLink("0-1"), onLink("2-5"), DEPARTURE).walkDistance(), 1e-6);
	}

	@Test
	void walkIsTeleportedEvenWhereTheRunRoutesItOnTheNetwork() {

		// no link of this network allows walk, so routing walk on it would fail outright - which is the
		// point: walking is always teleported, beeline x detour factor, whatever the run does with it
		Scenario scenario = scenario(TransportMode.car, TransportMode.walk);
		teleportedWalk(scenario);

		CapabilityTravelTimes walk = CapabilityTravelTimes.of(scenario, config(TransportMode.walk))
			.get(TransportMode.walk);

		assertEquals(5000 * WALK_DETOUR_FACTOR / WALK_SPEED,
			walk.travel(at(0, 0), at(3000, 4000), DEPARTURE).travelTime(), 1e-6);
	}

	@Test
	void publicTransportNeedsAWalkSpeedToFallBackOn() {

		Scenario scenario = transitScenario();
		scenario.getConfig().routing().clearTeleportedModeParams();

		IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
			() -> CapabilityTravelTimes.of(scenario, config(TransportMode.pt)));
		assertTrue(e.getMessage().contains("teleported walk speed"));
	}

	@Test
	void beingATransitModeBeatsHavingTeleportedParameters() {

		// MATSim's own config defaults route pt by a free speed factor, so pt is a "teleported" mode as
		// far as the routing config is concerned - here it is even given a teleported speed outright. A
		// scenario that carries a schedule is meant to be analysed on the schedule all the same.
		Scenario scenario = transitScenario();
		scenario.getConfig().routing().getOrCreateModeRoutingParams(TransportMode.pt)
			.setTeleportedModeSpeed(10.0);
		assertTrue(scenario.getConfig().routing().getTeleportedModeParams().containsKey(TransportMode.pt),
			"this test is only about something if pt does carry teleported parameters");

		assertEquals(600, CapabilityTravelTimes.of(scenario, config(TransportMode.pt)).get(TransportMode.pt)
			.travel(at(0, 0), at(3000, 0), 8 * 3600 - 300).travelTime(), 1e-6);
	}

	@Test
	void aTransitModeWithoutAScheduleFails() {

		Scenario scenario = scenario(TransportMode.car);
		scenario.getConfig().transit().setUseTransit(true);

		// an empty schedule would make every person unable to reach anything by pt, which looks like a
		// result rather than like the missing input it is
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
			() -> CapabilityTravelTimes.of(scenario, config(TransportMode.pt)));
		assertTrue(e.getMessage().contains(TransportMode.pt));
		assertTrue(e.getMessage().contains("transit schedule"));
	}

	@Test
	void everyModeCountsItsQueries() {

		Scenario scenario = scenario(TransportMode.car);
		Map<String, CapabilityTravelTimes> byMode = CapabilityTravelTimes.of(scenario, config(TransportMode.car));

		CapabilityTravelTimes.Metered car = (CapabilityTravelTimes.Metered) byMode.get(TransportMode.car);
		assertEquals(0, car.queries());

		byMode.get(TransportMode.car).travel(onLink("0-1"), onLink("2-5"), DEPARTURE).travelTime();
		byMode.get(TransportMode.car).travel(onLink("0-1"), onLink("1-2"), DEPARTURE).travelTime();

		assertEquals(2, car.queries(), "the analysis has to be able to say how much routing each mode cost");
		assertTrue(car.seconds() >= 0);
	}

	private static CapabilityConfigGroup config(String... modes) {
		CapabilityConfigGroup config = new CapabilityConfigGroup();
		config.setModes(new java.util.LinkedHashSet<>(List.of(modes)));
		return config;
	}

	/**
	 * The same network, plus a transit line running along the x axis from a stop at (0,0) to one at
	 * (3000,0): five minutes of riding, a departure every ten minutes from 08:00 to 09:00.
	 */
	private static Scenario transitScenario() {

		Scenario scenario = scenario(TransportMode.car);
		scenario.getConfig().transit().setUseTransit(true);
		teleportedWalk(scenario);

		TransitSchedule schedule = scenario.getTransitSchedule();
		TransitScheduleFactory factory = schedule.getFactory();

		TransitStopFacility west = factory.createTransitStopFacility(
			Id.create("west", TransitStopFacility.class), new Coord(0, 0), false);
		west.setLinkId(Id.createLinkId("0-1"));
		TransitStopFacility east = factory.createTransitStopFacility(
			Id.create("east", TransitStopFacility.class), new Coord(3000, 0), false);
		east.setLinkId(Id.createLinkId("2-5"));
		schedule.addStopFacility(west);
		schedule.addStopFacility(east);

		TransitRouteStop first = factory.createTransitRouteStop(west, 0, 0);
		TransitRouteStop last = factory.createTransitRouteStop(east, 300, 300);

		// the raptor measures the distance of a ride along the links the line runs on, so the route needs
		// one: 0-1, 1-2, 2-5, the straight way along the x axis
		NetworkRoute along = RouteUtils.createNetworkRoute(
			List.of(Id.createLinkId("0-1"), Id.createLinkId("1-2"), Id.createLinkId("2-5")));

		TransitRoute route = factory.createTransitRoute(Id.create("east-bound", TransitRoute.class), along,
			List.of(first, last), TransportMode.pt);
		for (int i = 0; i < 7; i++) {
			Departure departure = factory.createDeparture(
				Id.create("dep-" + i, Departure.class), 8 * 3600 + i * 600);
			departure.setVehicleId(Id.create("bus-" + i, Vehicle.class));
			route.addDeparture(departure);
		}

		TransitLine line = factory.createTransitLine(Id.create("line", TransitLine.class));
		line.addRoute(route);
		schedule.addTransitLine(line);

		return scenario;
	}

	private static Scenario scenario(String... networkModes) {
		Config config = ConfigUtils.createConfig();
		config.routing().setNetworkModes(List.of(networkModes));
		Scenario scenario = ScenarioUtils.createScenario(config);
		buildNetwork(scenario.getNetwork());
		return scenario;
	}

	/**
	 * Walk at {@link #WALK_SPEED} with a detour factor of {@link #WALK_DETOUR_FACTOR}, which is also
	 * where the raptor takes its own beeline walk from.
	 */
	private static void teleportedWalk(Scenario scenario) {
		// the defaults are cleared first, the way the Dresden config does it, so that 'walk' is the only
		// walk the config holds: the raptor looks for 'transit_walk' and 'non_network_walk' before
		// 'walk', and with those around its access and egress walks would run at a different speed from
		// the walk mode of the analysis
		scenario.getConfig().routing().clearTeleportedModeParams();
		scenario.getConfig().routing().getOrCreateModeRoutingParams(TransportMode.walk)
			.setTeleportedModeSpeed(WALK_SPEED);
		scenario.getConfig().routing().getOrCreateModeRoutingParams(TransportMode.walk)
			.setBeelineDistanceFactor(WALK_DETOUR_FACTOR);
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
