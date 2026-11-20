package ch.sbb.matsim.routing.pt.raptor;

import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorData.RRoute;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorData.RRouteStop;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitRoute;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the timetable of the transit schedule that {@link SwissRailRaptorCore} reads its times from.
 */
public class RaptorScheduleTimetableTest {

	@Test
	void timesAreTheStartTimePlusTheOffsetsOfTheRouteStop() {
		SwissRailRaptorData data = createData();
		RaptorTimetable timetable = data.scheduleTimetable;

		for (RRoute route : data.routes) {
			for (int dep = route.indexFirstDeparture; dep < route.indexFirstDeparture + route.countDepartures; dep++) {
				for (int rs = route.indexFirstRouteStop; rs < route.indexFirstRouteStop + route.countRouteStops; rs++) {
					RRouteStop routeStop = data.routeStops[rs];
					assertEquals(data.departures[dep] + routeStop.arrivalOffset, timetable.getArrivalTime(dep, routeStop));
					assertEquals(data.departures[dep] + routeStop.departureOffset, timetable.getDepartureTime(dep, routeStop));
				}
			}
		}
	}

	/**
	 * Compares the search with a linear search over all departures of the route, at every route stop and for times
	 * on a grid of minutes, at every departure and one second before and after it. With several departures at the
	 * same time, any of them may be found, so the departure times are compared, not the indices.
	 */
	@Test
	void nextDepartureIsTheEarliestNotBeforeTheTime() {
		SwissRailRaptorData data = createData();
		RaptorTimetable timetable = data.scheduleTimetable;

		int checked = 0;
		for (RRoute route : data.routes) {
			int firstDep = route.indexFirstDeparture;
			int lastDep = firstDep + route.countDepartures;
			for (int rs = route.indexFirstRouteStop; rs < route.indexFirstRouteStop + route.countRouteStops; rs++) {
				RRouteStop routeStop = data.routeStops[rs];
				for (int time : times(data, route, routeStop)) {
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
					checked++;
				}
			}
		}
		assertTrue(checked > 0);
	}

	@Test
	void theFixtureHasDeparturesAtTheSameTime() {
		SwissRailRaptorData data = createData();
		boolean sameTime = false;
		for (RRoute route : data.routes) {
			for (int dep = route.indexFirstDeparture + 1; dep < route.indexFirstDeparture + route.countDepartures; dep++) {
				sameTime |= data.departures[dep] == data.departures[dep - 1];
			}
		}
		assertTrue(sameTime, "the search is not checked with departures at the same time");
	}

	private static int[] times(SwissRailRaptorData data, RRoute route, RRouteStop routeStop) {
		int[] grid = new int[13 * 60];
		for (int i = 0; i < grid.length; i++) {
			grid[i] = 3 * 3600 + i * 60;
		}
		int[] atDepartures = new int[3 * route.countDepartures];
		for (int k = 0; k < route.countDepartures; k++) {
			int depTime = data.scheduleTimetable.getDepartureTime(route.indexFirstDeparture + k, routeStop);
			atDepartures[3 * k] = depTime - 1;
			atDepartures[3 * k + 1] = depTime;
			atDepartures[3 * k + 2] = depTime + 1;
		}
		int[] all = Arrays.copyOf(grid, grid.length + atDepartures.length);
		System.arraycopy(atDepartures, 0, all, grid.length, atDepartures.length);
		return all;
	}

	/** The schedule of {@link Fixture}, plus a second departure of the red line at 7:00. */
	private static SwissRailRaptorData createData() {
		Fixture f = new Fixture();
		f.init();
		TransitRoute route = f.redLine.getRoutes().get(Id.create("red C > G", TransitRoute.class));
		route.addDeparture(f.schedule.getFactory().createDeparture(Id.create("r>02b", Departure.class), 7.0 * 3600));

		RaptorStaticConfig raptorConfig = RaptorUtils.createStaticConfig(f.config);
		return SwissRailRaptorData.create(f.schedule, null, raptorConfig, f.network, null);
	}
}
