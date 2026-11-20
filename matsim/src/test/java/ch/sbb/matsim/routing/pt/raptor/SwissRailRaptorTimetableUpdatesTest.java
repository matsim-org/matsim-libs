package ch.sbb.matsim.routing.pt.raptor;

import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorCore.TravelInfo;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorData.RRoute;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorData.RRouteStop;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorData.RTransfer;
import ch.sbb.matsim.routing.pt.raptor.VersionedRaptorTimetable.TripUpdate;
import com.google.common.base.VerifyException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.DefaultRoutingRequest;
import org.matsim.core.router.RoutingRequest;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.routes.TransitPassengerRoute;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.utils.objectattributes.attributable.AttributesImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the option {@code useTimetableUpdates}: which transfers it keeps, which features it rejects, and routing on
 * the timetable that a {@link RaptorTimetableProvider} chooses.
 */
public class SwissRailRaptorTimetableUpdatesTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	/**
	 * Route 1 reaches X only after the last departure of route 2 there. With the times of the schedule, the
	 * transfer can never be used, but with a delay of route 2 it might.
	 * This test doesn't apply updates but only checks if transfers are kept in data.
	 */
	@Test
	void keepsTransfersThatOnlyDelaysMakeReachable() {
		TestSchedule s = new TestSchedule();
		TransitStopFacility a = s.stop("A", 0, 0);
		TransitStopFacility x = s.stop("X", 1000, 0);
		TransitStopFacility b = s.stop("B", 2000, 0);
		TransitStopFacility c = s.stop("C", 1000, 1000);
		TransitRoute route1 = s.route("1", 10 * 3600, List.of(a, x, b), 0, 600, 1200);
		TransitRoute route2 = s.route("2", 9 * 3600, List.of(x, c), 0, 600);

		assertFalse(hasTransfer(s.data(false), route1, 1, route2, 0));
		assertTrue(hasTransfer(s.data(true), route1, 1, route2, 0));
	}

	/**
	 * Routes 1 and 2 serve the same stops after X. Without timetable updates, the raptor assumes that they do not
	 * overtake each other, so a transfer between them is useless. With timetable updates, only departures of the
	 * same route keep their order, so the transfer is kept.
	 */
	@Test
	void keepsTransfersBetweenRoutesWithTheSameUpcomingStops() {
		TestSchedule s = new TestSchedule();
		TransitStopFacility a = s.stop("A", 0, 0);
		TransitStopFacility x = s.stop("X", 1000, 0);
		TransitStopFacility b = s.stop("B", 2000, 0);
		TransitStopFacility d = s.stop("D", 1000, -1000);
		TransitRoute route1 = s.route("1", 8 * 3600, List.of(a, x, b), 0, 600, 1200);
		TransitRoute route2 = s.route("2", 8 * 3600 + 300, List.of(d, x, b), 0, 600, 1200);

		assertFalse(hasTransfer(s.data(false), route1, 1, route2, 1));
		assertTrue(hasTransfer(s.data(true), route1, 1, route2, 1));
	}

	/**
	 * Route 1 serves X Y X Y X Y. From the second X, the rest of the route after the third X is the same, so the
	 * transfer between them is useless, also with timetable updates: departures of the same route do not overtake
	 * each other. Route 2 serves X Y X Y X Z instead; there the rest differs, so the same transfer is kept, which
	 * shows that only the upcoming stops drop it on route 1.
	 */
	@Test
	void dropsTransfersWithinTheSameRouteWithTheSameUpcomingStops() {
		TestSchedule s = new TestSchedule();
		TransitStopFacility x = s.stop("X", 0, 0);
		TransitStopFacility y = s.stop("Y", 1000, 0);
		TransitStopFacility z = s.stop("Z", 0, 5000);
		TransitRoute route1 = s.route("1", 8 * 3600, List.of(x, y, x, y, x, y), 0, 600, 1200, 1800, 2400, 3000);
		TransitRoute route2 = s.route("2", 8 * 3600, List.of(x, y, x, y, x, z), 0, 600, 1200, 1800, 2400, 3000);

		assertFalse(hasTransfer(s.data(false), route1, 2, route1, 4));
		assertFalse(hasTransfer(s.data(true), route1, 2, route1, 4));
		assertTrue(hasTransfer(s.data(true), route2, 2, route2, 4));
	}

	@Test
	void rejectsCapacityConstraints() {
		TestSchedule s = new TestSchedule();
		s.route("1", 8 * 3600, List.of(s.stop("A", 0, 0), s.stop("B", 1000, 0)), 0, 600);
		RaptorStaticConfig config = new RaptorStaticConfig();
		config.setUseTimetableUpdates(true);
		config.setUseCapacityConstraints(true);

		assertThrows(IllegalArgumentException.class, () -> SwissRailRaptorData.create(s.schedule, null, config, s.network, null));
	}

	@Test
	void rejectsChainedDepartures() {
		String input = utils.getPackageInputDirectory();
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new TransitScheduleReader(scenario).readFile(input + "schedule.xml");
		new MatsimNetworkReader(scenario.getNetwork()).readFile(input + "network.xml");
		RaptorStaticConfig config = new RaptorStaticConfig();
		config.setUseTimetableUpdates(true);

		assertThrows(IllegalArgumentException.class,
			() -> SwissRailRaptorData.create(scenario.getTransitSchedule(), null, config, scenario.getNetwork(), null));
	}

	@Test
	void rejectsTheRangeQueryInTheConfig() {
		Config config = ConfigUtils.createConfig();
		SwissRailRaptorConfigGroup srrConfig = ConfigUtils.addOrGetModule(config, SwissRailRaptorConfigGroup.class);
		srrConfig.setUseRangeQuery(true);
		assertDoesNotThrow(config::checkConsistency);

		srrConfig.setUseTimetableUpdates(true);
		assertThrows(VerifyException.class, config::checkConsistency);
	}

	@Test
	void rejectsTheRangeQueryWhenRouting() {
		TestSchedule s = new TestSchedule();
		TransitStopFacility a = s.stop("A", 0, 0);
		TransitStopFacility b = s.stop("B", 1000, 0);
		s.route("1", 8 * 3600, List.of(a, b), 0, 600);
		Config config = ConfigUtils.createConfig();
		SwissRailRaptorConfigGroup srrConfig = ConfigUtils.addOrGetModule(config, SwissRailRaptorConfigGroup.class);
		srrConfig.setUseRangeQuery(true);
		SwissRailRaptor raptor = new SwissRailRaptor.Builder(s.data(true), config).build();

		assertThrows(IllegalStateException.class, () -> raptor.calcRoute(
			DefaultRoutingRequest.withoutAttributes(new FakeFacility(a.getCoord()), new FakeFacility(b.getCoord()), 7.5 * 3600, null)));
	}

	/**
	 * Route 1 reaches X at 10:10, after the last departure of route 2 there (9:00). On the schedule, C cannot be
	 * reached. On a version in which route 2 is 75 minutes late, the request boards it at X at 10:15.
	 */
	@Test
	void aDelayedLastBusCanBeReached() {
		LastBus s = new LastBus();
		SwissRailRaptorData data = s.data(true);
		SwissRailRaptor onSchedule = new SwissRailRaptor.Builder(data, s.config).build();

		VersionedRaptorTimetable delayed = delay(data, s.route2, 75 * 60);
		SwissRailRaptor onDelay = new SwissRailRaptor.Builder(data, s.config).with((d, time, person, attributes) -> delayed).build();

		// no delays => no route
		assertTrue(legsOn(s.route2, onSchedule.calcRoute(s.request(null))).isEmpty());

		// with delay => route
		List<Leg> legs = legsOn(s.route2, onDelay.calcRoute(s.request(null)));
		assertEquals(1, legs.size());
		assertEquals(10 * 3600 + 15 * 60, boardingTime(legs.getFirst()));
		assertEquals(10 * 3600 + 25 * 60, legs.getFirst().getDepartureTime().seconds() + legs.getFirst().getTravelTime().seconds());

		// trees from a facility ask the provider as well
		assertNull(onSchedule.calcTree(new FakeFacility(s.a.getCoord()), s.departureTime, null, new AttributesImpl()).get(s.c.getId()));
		TravelInfo toC = onDelay.calcTree(new FakeFacility(s.a.getCoord()), s.departureTime, null, new AttributesImpl()).get(s.c.getId());
		assertEquals(10 * 3600 + 25 * 60, toC.ptArrivalTime);
	}

	/**
	 * The provider chooses one of two versions by a routing attribute. Four threads route alternately on both, each
	 * with its own raptor on the same data.
	 */
	@Test
	void versionsInParallelThreads() throws Exception {
		LastBus s = new LastBus();
		SwissRailRaptorData data = s.data(true);
		List<VersionedRaptorTimetable> versions = List.of(delay(data, s.route2, 75 * 60), delay(data, s.route2, 90 * 60));
		RaptorTimetableProvider provider = (d, time, person, attributes) -> versions.get((Integer) attributes.getAttribute("version"));
		int[] expectedBoardingTimes = {10 * 3600 + 15 * 60, 10 * 3600 + 30 * 60};

		ExecutorService executor = Executors.newFixedThreadPool(4);
		try {
			List<Future<?>> results = new ArrayList<>();
			for (int t = 0; t < 4; t++) {
				SwissRailRaptor raptor = new SwissRailRaptor.Builder(data, s.config).with(provider).build();
				int thread = t;
				results.add(executor.submit(() -> {
					for (int i = 0; i < 200; i++) {
						int version = (thread + i) % 2;
						List<Leg> legs = legsOn(s.route2, raptor.calcRoute(s.request(version)));
						assertEquals(1, legs.size());
						assertEquals(expectedBoardingTimes[version], boardingTime(legs.getFirst()));
					}
				}));
			}
			for (Future<?> result : results) {
				result.get();
			}
		} finally {
			executor.shutdownNow();
		}
	}

	@Test
	void rejectsATimetableOfOtherData() {
		LastBus s = new LastBus();
		SwissRailRaptorData data = s.data(true);
		SwissRailRaptorData other = s.data(true);
		VersionedRaptorTimetable version = delay(other, s.route2, 75 * 60);
		SwissRailRaptor withVersion = new SwissRailRaptor.Builder(data, s.config).with((d, time, person, attributes) -> version).build();
		SwissRailRaptor withSchedule = new SwissRailRaptor.Builder(data, s.config).with((d, time, person, attributes) -> other.scheduleTimetable).build();

		assertThrows(IllegalArgumentException.class, () -> withVersion.calcRoute(s.request(null)));
		assertThrows(IllegalArgumentException.class, () -> withSchedule.calcRoute(s.request(null)));
	}

	@Test
	void rejectsRangeQueriesAndTreesOverTimeRanges() {
		LastBus s = new LastBus();
		SwissRailRaptor raptor = new SwissRailRaptor.Builder(s.data(true), s.config).build();
		RaptorParameters parameters = RaptorUtils.createParameters(s.config);

		assertThrows(IllegalStateException.class, () -> raptor.calcRoutes(new FakeFacility(s.a.getCoord()), new FakeFacility(s.c.getCoord()),
			9 * 3600, s.departureTime, 11 * 3600, null, new AttributesImpl()));
		assertThrows(IllegalStateException.class, () -> raptor.calcTreesObservable(s.a, 9 * 3600, 11 * 3600, parameters, null,
			(departureTime, stop, arrivalTime, transferCount, route) -> {
			}));
	}

	/** @return a version in which the departure of {@code route}, its only one, is {@code seconds} late at all stops */
	private static VersionedRaptorTimetable delay(SwissRailRaptorData data, TransitRoute route, int seconds) {
		VersionedRaptorTimetable schedule = VersionedRaptorTimetable.fromSchedule(data);
		for (RRoute r : data.routes) {
			if (r.countRouteStops > 0 && data.routeStops[r.indexFirstRouteStop].route == route) {
				int departure = r.indexFirstDeparture;
				int[] arrivalTimes = new int[r.countRouteStops];
				int[] departureTimes = new int[r.countRouteStops];
				for (int i = 0; i < r.countRouteStops; i++) {
					RRouteStop routeStop = data.routeStops[r.indexFirstRouteStop + i];
					arrivalTimes[i] = schedule.getArrivalTime(departure, routeStop) + seconds;
					departureTimes[i] = schedule.getDepartureTime(departure, routeStop) + seconds;
				}
				// the external index of fromSchedule is the departure index
				return schedule.apply(List.of(new TripUpdate(departure, 0, arrivalTimes, departureTimes)));
			}
		}
		throw new IllegalArgumentException("Unknown route " + route.getId());
	}

	/** @return the pt legs on {@code route}, none if there is no route at all */
	private static List<Leg> legsOn(TransitRoute route, List<? extends PlanElement> planElements) {
		List<Leg> legs = new ArrayList<>();
		if (planElements != null) {
			for (PlanElement planElement : planElements) {
				if (planElement instanceof Leg leg && leg.getRoute() instanceof TransitPassengerRoute ptRoute && ptRoute.getRouteId().equals(route.getId())) {
					legs.add(leg);
				}
			}
		}
		return legs;
	}

	private static double boardingTime(Leg leg) {
		return ((TransitPassengerRoute) leg.getRoute()).getBoardingTime().seconds();
	}

	private static boolean hasTransfer(SwissRailRaptorData data, TransitRoute fromRoute, int fromStop, TransitRoute toRoute, int toStop) {
		for (RTransfer transfer : data.transfers) {
			RRouteStop from = data.routeStops[transfer.fromRouteStop];
			RRouteStop to = data.routeStops[transfer.toRouteStop];
			if (from.route == fromRoute && from.routeStop == fromRoute.getStops().get(fromStop)
				&& to.route == toRoute && to.routeStop == toRoute.getStops().get(toStop)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * A schedule with one line per route and one departure per route. All stops lie on the same link, so the network
	 * needs no links.
	 */
	private static class TestSchedule {
		final Config config = ConfigUtils.createConfig();
		final Scenario scenario = ScenarioUtils.createScenario(this.config);
		final TransitSchedule schedule = this.scenario.getTransitSchedule();
		final TransitScheduleFactory factory = schedule.getFactory();
		final Network network = this.scenario.getNetwork();
		final Id<Link> link = Id.createLinkId("link");

		TransitStopFacility stop(String id, double x, double y) {
			TransitStopFacility stop = factory.createTransitStopFacility(Id.create(id, TransitStopFacility.class), new Coord(x, y), false);
			stop.setLinkId(link);
			schedule.addStopFacility(stop);
			return stop;
		}

		/** The stops are served at the start time plus the offsets, in seconds, without dwell time. */
		TransitRoute route(String id, double startTime, List<TransitStopFacility> stops, double... offsets) {
			List<TransitRouteStop> routeStops = new ArrayList<>();
			for (int i = 0; i < stops.size(); i++) {
				routeStops.add(factory.createTransitRouteStopBuilder(stops.get(i)).arrivalOffset(offsets[i]).departureOffset(offsets[i]).build());
			}
			TransitRoute route = factory.createTransitRoute(Id.create(id, TransitRoute.class), RouteUtils.createLinkNetworkRouteImpl(link, link),
				routeStops, "bus");
			route.addDeparture(factory.createDeparture(Id.create(id, Departure.class), startTime));
			TransitLine line = factory.createTransitLine(Id.create(id, TransitLine.class));
			line.addRoute(route);
			schedule.addTransitLine(line);
			return route;
		}

		SwissRailRaptorData data(boolean useTimetableUpdates) {
			RaptorStaticConfig config = new RaptorStaticConfig();
			config.setUseTimetableUpdates(useTimetableUpdates);
			return SwissRailRaptorData.create(schedule, null, config, network, null);
		}
	}

	/**
	 * Route 1 runs A-X-B at 10:00, route 2 runs X-C at 9:00, and the request goes from A to C at 9:55. The stops are
	 * 10 km apart, so walking never wins and no stop can be reached on foot from another one.
	 */
	private static final class LastBus extends TestSchedule {
		final TransitStopFacility a = stop("A", 0, 0);
		final TransitStopFacility x = stop("X", 10_000, 0);
		final TransitStopFacility b = stop("B", 20_000, 0);
		final TransitStopFacility c = stop("C", 10_000, 10_000);
		final TransitRoute route1 = route("1", 10 * 3600, List.of(a, x, b), 0, 600, 1200);
		final TransitRoute route2 = route("2", 9 * 3600, List.of(x, c), 0, 600);
		final double departureTime = 9 * 3600 + 55 * 60;

		/** @param version the routing attribute "version", if not null */
		RoutingRequest request(Integer version) {
			AttributesImpl attributes = new AttributesImpl();
			if (version != null) {
				attributes.putAttribute("version", version);
			}
			return DefaultRoutingRequest.of(new FakeFacility(a.getCoord()), new FakeFacility(c.getCoord()), departureTime, null, attributes);
		}
	}
}
