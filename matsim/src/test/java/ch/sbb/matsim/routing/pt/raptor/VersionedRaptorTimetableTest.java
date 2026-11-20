package ch.sbb.matsim.routing.pt.raptor;

import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorData.RRoute;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorData.RRouteStop;
import ch.sbb.matsim.routing.pt.raptor.VersionedRaptorTimetable.TripUpdate;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the versions of times that may differ from the schedule, without routing.
 */
public class VersionedRaptorTimetableTest {

	private static final Id<TransitRoute> RED_ROUTE = Id.create("red C > G", TransitRoute.class);

	@Test
	void fromScheduleHasTheTimesOfTheSchedule() {
		SwissRailRaptorData data = createData(true);
		VersionedRaptorTimetable timetable = VersionedRaptorTimetable.fromSchedule(data);

		for (RRoute route : data.routes) {
			for (int dep = route.indexFirstDeparture; dep < route.indexFirstDeparture + route.countDepartures; dep++) {
				for (int rs = route.indexFirstRouteStop; rs < route.indexFirstRouteStop + route.countRouteStops; rs++) {
					RRouteStop routeStop = data.routeStops[rs];
					assertEquals(data.scheduleTimetable.getArrivalTime(dep, routeStop), timetable.getArrivalTime(dep, routeStop));
					assertEquals(data.scheduleTimetable.getDepartureTime(dep, routeStop), timetable.getDepartureTime(dep, routeStop));
				}
			}
		}
		assertNextDepartureIsTheEarliest(data, timetable);
	}

	/**
	 * The red line has two departures at 7:00, r>02 and r>02b. Java orders them by id, snapshot 0 the other way round.
	 * The search follows snapshot 0, and only delays that keep this order are accepted.
	 */
	@Test
	void theOrderOfSnapshotZeroDecides() {
		SwissRailRaptorData data = createData(true);
		// first id, second in tt
		int dep02 = departure(data, "r>02");
		// second id, first in tt
		int dep02b = departure(data, "r>02b");

		Snapshot snapshot = snapshotWithR02bFirst(data);
		RRoute redRoute = data.routes[routeIndex(data, RED_ROUTE)];
		RRouteStop firstStop = data.routeStops[redRoute.indexFirstRouteStop];

		// 2b is first in tt, so assert that this will be found
		assertEquals(dep02b, snapshot.timetable.findNextDepartureIndex(redRoute, firstStop, 7 * 3600));

		VersionedRaptorTimetable delayed = snapshot.timetable.apply(List.of(delay(snapshot, dep02, 0, 60)));
		assertEquals(7 * 3600 + 60, delayed.getDepartureTime(dep02, firstStop));
		assertEquals(dep02b, delayed.findNextDepartureIndex(redRoute, firstStop, 7 * 3600));
		assertEquals(dep02, delayed.findNextDepartureIndex(redRoute, firstStop, 7 * 3600 + 1));

		assertThrows(IllegalArgumentException.class, () -> snapshot.timetable.apply(List.of(delay(snapshot, dep02b, 0, 60))));
	}

	@Test
	void theBuilderRejectsTimesThatDoNotMatchTheSchedule() {
		SwissRailRaptorData data = createData(true);
		RRoute route = data.routes[routeIndex(data, RED_ROUTE)];
		Id<TransitLine> line = data.routeStops[route.indexFirstRouteStop].line.getId();
		List<Id<TransitStopFacility>> stops = stops(data, route);
		int dep01 = departure(data, "r>01");
		int[] arrivals = arrivals(data, dep01);
		int[] departures = departures(data, dep01);

		// unknown route, wrong number of stops, wrong stop
		assertThrows(IllegalArgumentException.class, () -> builder(data).addRoute(line, Id.create("unknown", TransitRoute.class), stops));
		assertThrows(IllegalArgumentException.class, () -> builder(data).addRoute(line, RED_ROUTE, stops.subList(0, stops.size() - 1)));
		List<Id<TransitStopFacility>> wrongStops = new ArrayList<>(stops);
		wrongStops.set(wrongStops.size() - 1, Id.create("unknown", TransitStopFacility.class));
		assertThrows(IllegalArgumentException.class, () -> builder(data).addRoute(line, RED_ROUTE, wrongStops));

		// unknown, repeated and missing departures, missing routes
		assertThrows(IllegalArgumentException.class,
			() -> builder(data).addRoute(line, RED_ROUTE, stops).addDeparture(Id.create("unknown", Departure.class), arrivals, departures));
		assertThrows(IllegalArgumentException.class,
			() -> builder(data).addRoute(line, RED_ROUTE, stops).addDeparture(data.departureIds[dep01], arrivals, departures)
				.addDeparture(data.departureIds[dep01], arrivals, departures));
		assertThrows(IllegalArgumentException.class,
			() -> builder(data).addRoute(line, RED_ROUTE, stops).addDeparture(data.departureIds[dep01], arrivals, departures).build());
		assertThrows(IllegalArgumentException.class, () -> builder(data).build());

		// arrival after departure at a stop, arrival before the departure from the stop before
		int[] lateArrivals = arrivals.clone();
		lateArrivals[0] = departures[0] + 1;
		assertThrows(IllegalArgumentException.class, () -> builder(data).addRoute(line, RED_ROUTE, stops).addDeparture(data.departureIds[dep01], lateArrivals, departures));
		int[] earlyArrivals = arrivals.clone();
		earlyArrivals[1] = departures[0] - 1;
		assertThrows(IllegalArgumentException.class, () -> builder(data).addRoute(line, RED_ROUTE, stops).addDeparture(data.departureIds[dep01], earlyArrivals, departures));

		// a later departure of the route leaves before the one added before it
		int dep02 = departure(data, "r>02");
		assertThrows(IllegalArgumentException.class, () -> builder(data).addRoute(line, RED_ROUTE, stops)
			.addDeparture(data.departureIds[dep02], arrivals(data, dep02), departures(data, dep02))
			.addDeparture(data.departureIds[dep01], arrivals, departures));
	}

	@Test
	void applyReturnsANewVersionAndKeepsTheOldOne() {
		SwissRailRaptorData data = createData(true);
		VersionedRaptorTimetable original = VersionedRaptorTimetable.fromSchedule(data);
		int dep03 = departure(data, "r>03");
		int dep04 = departure(data, "r>04");
		int[] before = original.getDepartureTimes(dep03).clone();

		// the external index of fromSchedule is the departure index
		VersionedRaptorTimetable updated = original.apply(List.of(new TripUpdate(dep03, 1, new int[]{before[2] + 60}, new int[]{before[3] + 60})));

		assertTrue(Arrays.equals(before, original.getDepartureTimes(dep03)), "the old version must keep its times");
		assertEquals(before[0], updated.getDepartureTimes(dep03)[0]);
		assertEquals(before[1], updated.getDepartureTimes(dep03)[1]);
		assertEquals(before[2] + 60, updated.getDepartureTimes(dep03)[2]);
		assertEquals(before[3] + 60, updated.getDepartureTimes(dep03)[3]);
		assertNotSame(original.getDepartureTimes(dep03), updated.getDepartureTimes(dep03));
		assertSame(original.getDepartureTimes(dep04), updated.getDepartureTimes(dep04), "unchanged departures share their times");
	}

	/**
	 * Delays as the simulation would forecast them: a delay carries over to the following stops, and no departure
	 * arrives or departs earlier than the one before it. After each update, the binary search must find the same
	 * departure time as a linear search.
	 */
	@Test
	void theSearchFindsTheEarliestDepartureAfterRandomDelays() {
		SwissRailRaptorData data = createData(true);
		Snapshot snapshot = snapshotWithR02bFirst(data);
		Random random = new Random(4711);
		VersionedRaptorTimetable timetable = snapshot.timetable;

		for (int round = 0; round < 20; round++) {
			List<TripUpdate> updates = new ArrayList<>();
			for (int r = 0; r < data.routes.length; r++) {
				int[] previous = null;
				for (int dep : snapshot.order[r]) {
					int[] row = timetable.getDepartureTimes(dep);
					int stops = row.length / 2;
					boolean delayed = random.nextDouble() < 0.3;
					int fromStop = random.nextInt(stops);
					int delay = random.nextInt(600);
					int[] newRow = new int[row.length];
					for (int i = 0; i < stops; i++) {
						int extra = delayed && i >= fromStop ? delay : 0;
						int arrival = row[2 * i] + extra;
						if (i > 0) {
							arrival = Math.max(arrival, newRow[2 * i - 1]);
						}
						if (previous != null) {
							arrival = Math.max(arrival, previous[2 * i]);
						}
						int departure = Math.max(row[2 * i + 1] + extra, arrival);
						if (previous != null) {
							departure = Math.max(departure, previous[2 * i + 1]);
						}
						newRow[2 * i] = arrival;
						newRow[2 * i + 1] = departure;
					}
					int firstChange = firstDifference(row, newRow);
					if (firstChange < stops) {
						updates.add(update(snapshot.externalOf[dep], newRow, firstChange));
					}
					previous = newRow;
				}
			}
			timetable = timetable.apply(updates);
			assertNextDepartureIsTheEarliest(data, timetable);
		}
	}

	@Test
	void applyRejectsUpdatesThatDoNotFit() {
		SwissRailRaptorData data = createData(true);
		VersionedRaptorTimetable timetable = VersionedRaptorTimetable.fromSchedule(data);
		int dep01 = departure(data, "r>01");
		int[] row = timetable.getDepartureTimes(dep01);
		int stops = row.length / 2;

		// r>01 would leave two hours late, after r>02 and r>03
		assertThrows(IllegalArgumentException.class, () -> timetable.apply(List.of(delay(timetable, dep01, dep01, 0, 2 * 3600))));
		// unknown departures
		assertThrows(IllegalArgumentException.class, () -> timetable.apply(List.of(new TripUpdate(-1, 0, new int[stops], new int[stops]))));
		assertThrows(IllegalArgumentException.class, () -> timetable.apply(List.of(new TripUpdate(data.departures.length, 0, new int[stops], new int[stops]))));
		// stops outside the route
		assertThrows(IllegalArgumentException.class, () -> timetable.apply(List.of(new TripUpdate(dep01, stops, new int[0], new int[0]))));
		assertThrows(IllegalArgumentException.class, () -> timetable.apply(List.of(new TripUpdate(dep01, -1, new int[stops + 1], new int[stops + 1]))));
		// arrays of the wrong length
		assertThrows(IllegalArgumentException.class, () -> timetable.apply(List.of(new TripUpdate(dep01, 0, new int[stops - 1], new int[stops - 1]))));
	}

	@Test
	void versionsNeedTheOptionAndBelongToTheirData() {
		SwissRailRaptorData withoutOption = createData(false);
		assertThrows(IllegalStateException.class, () -> VersionedRaptorTimetable.fromSchedule(withoutOption));
		assertThrows(IllegalStateException.class, () -> VersionedRaptorTimetable.builder(withoutOption));

		SwissRailRaptorData data = createData(true);
		VersionedRaptorTimetable timetable = VersionedRaptorTimetable.fromSchedule(data);
		assertTrue(timetable.belongsTo(data));
		assertFalse(timetable.belongsTo(createData(true)));
	}

	/** Compares the search with a linear search over all departures of the route, as in {@link RaptorScheduleTimetableTest}. */
	private static void assertNextDepartureIsTheEarliest(SwissRailRaptorData data, VersionedRaptorTimetable timetable) {
		for (RRoute route : data.routes) {
			int firstDep = route.indexFirstDeparture;
			int lastDep = firstDep + route.countDepartures;
			for (int rs = route.indexFirstRouteStop; rs < route.indexFirstRouteStop + route.countRouteStops; rs++) {
				RRouteStop routeStop = data.routeStops[rs];
				List<Integer> times = new ArrayList<>();
				for (int t = 3 * 3600; t < 16 * 3600; t += 60) {
					times.add(t);
				}
				for (int dep = firstDep; dep < lastDep; dep++) {
					int depTime = timetable.getDepartureTime(dep, routeStop);
					times.addAll(List.of(depTime - 1, depTime, depTime + 1));
				}
				for (int time : times) {
					int expected = Integer.MAX_VALUE;
					for (int dep = firstDep; dep < lastDep; dep++) {
						int depTime = timetable.getDepartureTime(dep, routeStop);
						if (depTime >= time && depTime < expected) {
							expected = depTime;
						}
					}
					int found = timetable.findNextDepartureIndex(route, routeStop, time);
					if (expected == Integer.MAX_VALUE) {
						assertEquals(-1, found, "no departure at or after " + time);
					} else {
						assertTrue(found >= firstDep && found < lastDep, "departure " + found + " is not one of the route");
						assertEquals(expected, timetable.getDepartureTime(found, routeStop), "departure time of the next departure after " + time);
					}
				}
			}
		}
	}

	/** A first version from the builder, with the times of the schedule, and r>02b before r>02. */
	private record Snapshot(VersionedRaptorTimetable timetable, int[][] order, int[] externalOf) {
	}

	private static Snapshot snapshotWithR02bFirst(SwissRailRaptorData data) {
		int dep02b = departure(data, "r>02b");
		int[][] order = new int[data.routes.length][];
		int[] externalOf = new int[data.departures.length];
		int external = 0;
		VersionedRaptorTimetable.Builder builder = builder(data);
		for (int r = 0; r < data.routes.length; r++) {
			RRoute route = data.routes[r];
			RRouteStop first = data.routeStops[route.indexFirstRouteStop];
			List<Integer> deps = new ArrayList<>();
			for (int dep = route.indexFirstDeparture; dep < route.indexFirstDeparture + route.countDepartures; dep++) {
				deps.add(dep);
			}
			deps.sort(Comparator.<Integer>comparingInt(dep -> data.departures[dep]).thenComparingInt(dep -> dep == dep02b ? 0 : 1));
			order[r] = deps.stream().mapToInt(Integer::intValue).toArray();
			builder.addRoute(first.line.getId(), first.route.getId(), stops(data, route));
			for (int dep : order[r]) {
				builder.addDeparture(data.departureIds[dep], arrivals(data, dep), departures(data, dep));
				externalOf[dep] = external++;
			}
		}
		return new Snapshot(builder.build(), order, externalOf);
	}

	private static TripUpdate delay(Snapshot snapshot, int dep, int fromStop, int seconds) {
		return delay(snapshot.timetable, snapshot.externalOf[dep], dep, fromStop, seconds);
	}

	private static TripUpdate delay(VersionedRaptorTimetable timetable, int external, int dep, int fromStop, int seconds) {
		int[] times = timetable.getDepartureTimes(dep).clone();
		for (int k = 2 * fromStop; k < times.length; k++) {
			times[k] += seconds;
		}
		return update(external, times, fromStop);
	}

	private static TripUpdate update(int external, int[] times, int fromStop) {
		int stops = times.length / 2;
		int[] arrivalTimes = new int[stops - fromStop];
		int[] departureTimes = new int[stops - fromStop];
		for (int i = fromStop; i < stops; i++) {
			arrivalTimes[i - fromStop] = times[2 * i];
			departureTimes[i - fromStop] = times[2 * i + 1];
		}
		return new TripUpdate(external, fromStop, arrivalTimes, departureTimes);
	}

	/** @return the first stop at which the times differ, or the number of stops */
	private static int firstDifference(int[] row, int[] newRow) {
		for (int i = 0; i < row.length; i++) {
			if (row[i] != newRow[i]) {
				return i / 2;
			}
		}
		return row.length / 2;
	}

	private static VersionedRaptorTimetable.Builder builder(SwissRailRaptorData data) {
		return VersionedRaptorTimetable.builder(data);
	}

	private static List<Id<TransitStopFacility>> stops(SwissRailRaptorData data, RRoute route) {
		List<Id<TransitStopFacility>> stops = new ArrayList<>();
		for (int i = 0; i < route.countRouteStops; i++) {
			stops.add(data.routeStops[route.indexFirstRouteStop + i].routeStop.getStopFacility().getId());
		}
		return stops;
	}

	private static int[] arrivals(SwissRailRaptorData data, int dep) {
		RRoute route = data.routes[data.routeStops[firstRouteStopOf(data, dep)].transitRouteIndex];
		int[] times = new int[route.countRouteStops];
		for (int i = 0; i < times.length; i++) {
			times[i] = data.scheduleTimetable.getArrivalTime(dep, data.routeStops[route.indexFirstRouteStop + i]);
		}
		return times;
	}

	private static int[] departures(SwissRailRaptorData data, int dep) {
		RRoute route = data.routes[data.routeStops[firstRouteStopOf(data, dep)].transitRouteIndex];
		int[] times = new int[route.countRouteStops];
		for (int i = 0; i < times.length; i++) {
			times[i] = data.scheduleTimetable.getDepartureTime(dep, data.routeStops[route.indexFirstRouteStop + i]);
		}
		return times;
	}

	private static int firstRouteStopOf(SwissRailRaptorData data, int dep) {
		for (RRoute route : data.routes) {
			if (dep >= route.indexFirstDeparture && dep < route.indexFirstDeparture + route.countDepartures) {
				return route.indexFirstRouteStop;
			}
		}
		throw new IllegalArgumentException("no route has departure " + dep);
	}

	private static int routeIndex(SwissRailRaptorData data, Id<TransitRoute> routeId) {
		for (int r = 0; r < data.routes.length; r++) {
			if (data.routeStops[data.routes[r].indexFirstRouteStop].route.getId().equals(routeId)) {
				return r;
			}
		}
		throw new IllegalArgumentException("no route " + routeId);
	}

	private static int departure(SwissRailRaptorData data, String id) {
		RRoute route = data.routes[routeIndex(data, RED_ROUTE)];
		for (int dep = route.indexFirstDeparture; dep < route.indexFirstDeparture + route.countDepartures; dep++) {
			if (data.departureIds[dep].toString().equals(id)) {
				return dep;
			}
		}
		throw new IllegalArgumentException("no departure " + id + " of the red line");
	}

	/** The schedule of {@link Fixture}, plus a second departure of the red line at 7:00. */
	private static SwissRailRaptorData createData(boolean useTimetableUpdates) {
		Fixture f = new Fixture();
		f.init();
		TransitRoute route = f.redLine.getRoutes().get(RED_ROUTE);
		route.addDeparture(f.schedule.getFactory().createDeparture(Id.create("r>02b", Departure.class), 7.0 * 3600));

		RaptorStaticConfig raptorConfig = RaptorUtils.createStaticConfig(f.config);
		raptorConfig.setUseTimetableUpdates(useTimetableUpdates);
		return SwissRailRaptorData.create(f.schedule, null, raptorConfig, f.network, null);
	}
}
