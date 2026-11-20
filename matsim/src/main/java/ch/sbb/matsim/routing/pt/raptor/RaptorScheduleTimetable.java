package ch.sbb.matsim.routing.pt.raptor;

import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorData.RRoute;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorData.RRouteStop;

import java.util.Arrays;

/**
 * The times of the transit schedule: the departure time of a departure at the start of its route plus the
 * arrival or departure offset of the route stop. The departures of a route are sorted by their departure time.
 */
final class RaptorScheduleTimetable implements RaptorTimetable {

	private final int[] departures;

	RaptorScheduleTimetable(int[] departures) {
		this.departures = departures;
	}

	@Override
	public boolean belongsTo(SwissRailRaptorData data) {
		return this.departures == data.departures;
	}

	@Override
	public int getArrivalTime(int departureIndex, RRouteStop routeStop) {
		return this.departures[departureIndex] + routeStop.arrivalOffset;
	}

	@Override
	public int getDepartureTime(int departureIndex, RRouteStop routeStop) {
		return this.departures[departureIndex] + routeStop.departureOffset;
	}

	@Override
	public int findNextDepartureIndex(RRoute route, RRouteStop routeStop, int time) {
		int depTimeAtRouteStart = time - routeStop.departureOffset;
		int fromIndex = route.indexFirstDeparture;
		int toIndex = fromIndex + route.countDepartures;
		int pos = Arrays.binarySearch(this.departures, fromIndex, toIndex, depTimeAtRouteStart);
		if (pos < 0) {
			// binarySearch returns (-(insertion point) - 1) if the element was not found, which will happen most of the times.
			// insertion_point points to the next larger element, which is the next departure in our case
			// This can be transformed as follows:
			// retval = -(insertion point) - 1
			// ==> insertion point = -(retval+1) .
			pos = -(pos + 1);
		}
		if (pos >= toIndex) {
			// there is no later departure time
			return -1;
		}
		return pos;
	}
}
