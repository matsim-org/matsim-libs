package ch.sbb.matsim.routing.pt.raptor;

import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorData.RRouteStop;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorData.RTransfer;
import com.google.common.base.VerifyException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.DefaultRoutingRequest;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.testcases.MatsimTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the option {@code useTimetableUpdates}: which transfers it keeps, and which features it rejects.
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
	private static final class TestSchedule {
		final TransitSchedule schedule = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getTransitSchedule();
		final TransitScheduleFactory factory = schedule.getFactory();
		final Network network = NetworkUtils.createNetwork();
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
}
