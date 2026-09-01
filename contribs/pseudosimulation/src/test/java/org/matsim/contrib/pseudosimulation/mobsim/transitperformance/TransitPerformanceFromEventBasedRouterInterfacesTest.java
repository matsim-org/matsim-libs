package org.matsim.contrib.pseudosimulation.mobsim.transitperformance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.contrib.pseudosimulation.trafficinfo.StopStopTime;
import org.matsim.contrib.pseudosimulation.trafficinfo.WaitTime;
import org.matsim.core.population.PopulationUtils;
import org.matsim.pt.routes.DefaultTransitPassengerRoute;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * This class used to return a zero-duration transit leg for every query, because the wait-time and
 * stop-to-stop-time sources it reads were commented out when the contrib supplying them was
 * removed. The previous version of this test pinned that as
 * {@code adjacentStopsRetainLegacyZeroTransitTime}. Now that the sources exist again, these tests
 * assert what the emulator is actually supposed to compute.
 */
class TransitPerformanceFromEventBasedRouterInterfacesTest {

	private static final Id<TransitStopFacility> A = Id.create("a", TransitStopFacility.class);
	private static final Id<TransitStopFacility> B = Id.create("b", TransitStopFacility.class);
	private static final Id<TransitStopFacility> C = Id.create("c", TransitStopFacility.class);
	private static final Id<TransitLine> LINE = Id.create("line", TransitLine.class);
	private static final Id<TransitRoute> ROUTE = Id.create("route", TransitRoute.class);

	private record Fixture(TransitSchedule schedule, Leg leg) {
	}

	/** A three-stop route, so a trip can span more than one stop-to-stop segment. */
	private static Fixture threeStopRoute(Id<TransitStopFacility> egressStopId) {
		TransitScheduleFactory factory = new TransitScheduleFactoryImpl();
		TransitSchedule schedule = factory.createTransitSchedule();
		TransitStopFacility a = factory.createTransitStopFacility(A, new Coord(0, 0), false);
		TransitStopFacility b = factory.createTransitStopFacility(B, new Coord(1, 0), false);
		TransitStopFacility c = factory.createTransitStopFacility(C, new Coord(2, 0), false);
		schedule.addStopFacility(a);
		schedule.addStopFacility(b);
		schedule.addStopFacility(c);

		TransitRoute transitRoute = factory.createTransitRoute(ROUTE, null,
				List.of(factory.createTransitRouteStop(a, 0, 0), factory.createTransitRouteStop(b, 100, 100),
						factory.createTransitRouteStop(c, 200, 200)),
				"pt");
		TransitLine line = factory.createTransitLine(LINE);
		line.addRoute(transitRoute);
		schedule.addTransitLine(line);

		Leg leg = PopulationUtils.createLeg("pt");
		leg.setRoute(new DefaultTransitPassengerRoute(a, line, transitRoute,
				schedule.getFacilities().get(egressStopId)));
		return new Fixture(schedule, leg);
	}

	private static WaitTime waitOf(double seconds) {
		return (lineId, routeId, stopId, time) -> seconds;
	}

	@Test
	void addsTheWaitAtTheAccessStopToTheStopToStopTimes() {
		Fixture fixture = threeStopRoute(C);
		StopStopTime segments = (from, to, time) -> 60.0;

		TransitEmulator.Trip trip = new TransitPerformanceFromEventBasedRouterInterfaces(
				waitOf(30.0), segments, fixture.schedule()).findTrip(fixture.leg(), 100.0);

		assertNull(trip.vehicleId());
		assertEquals(130.0, trip.accessTime_s(), 1e-9, "boarding is the departure time plus the wait");
		assertEquals(250.0, trip.egressTime_s(), 1e-9, "two 60s segments follow boarding at 130s");
	}

	@Test
	void looksUpEachSegmentAtTheTimeTheVehicleReachesIt() {
		Fixture fixture = threeStopRoute(C);
		// Returns the query time itself, so the assertion below pins how the clock was advanced.
		// The previous implementation added the running total rather than the segment just taken,
		// which made the lookup time drift quadratically along the route.
		StopStopTime segments = (from, to, time) -> time;

		TransitEmulator.Trip trip = new TransitPerformanceFromEventBasedRouterInterfaces(
				waitOf(0.0), segments, fixture.schedule()).findTrip(fixture.leg(), 100.0);

		// First segment is looked up at t=100 and takes 100s; the second is therefore looked up at
		// t=200 and takes 200s. Total in-vehicle time is 300s.
		assertEquals(400.0, trip.egressTime_s(), 1e-9);
	}

	@Test
	void stopsAtTheEgressStopRatherThanRidingToTheEndOfTheRoute() {
		Fixture fixture = threeStopRoute(B);
		StopStopTime segments = (from, to, time) -> 60.0;

		TransitEmulator.Trip trip = new TransitPerformanceFromEventBasedRouterInterfaces(
				waitOf(0.0), segments, fixture.schedule()).findTrip(fixture.leg(), 100.0);

		assertEquals(160.0, trip.egressTime_s(), 1e-9, "one segment only, since B is the second stop");
	}

	@Test
	void reportsNoTripWhenTheEgressStopIsNotOnTheRoute() {
		Fixture fixture = threeStopRoute(C);
		TransitScheduleFactory factory = new TransitScheduleFactoryImpl();
		TransitStopFacility elsewhere = factory.createTransitStopFacility(
				Id.create("elsewhere", TransitStopFacility.class), new Coord(9, 9), false);
		Leg leg = PopulationUtils.createLeg("pt");
		leg.setRoute(new DefaultTransitPassengerRoute(fixture.schedule().getFacilities().get(A),
				fixture.schedule().getTransitLines().get(LINE),
				fixture.schedule().getTransitLines().get(LINE).getRoutes().get(ROUTE), elsewhere));

		TransitEmulator.Trip trip = new TransitPerformanceFromEventBasedRouterInterfaces(
				waitOf(0.0), (from, to, time) -> 60.0, fixture.schedule()).findTrip(leg, 100.0);

		assertNull(trip, "an unreachable egress stop must not yield a negative-duration leg");
	}
}
