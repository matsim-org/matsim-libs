package ch.sbb.matsim.routing.pt.raptor;

import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorData.RRoute;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorData.RRouteStop;

/**
 * The times at which the departures of {@link SwissRailRaptorData} arrive at and leave their route stops, as
 * {@link SwissRailRaptorCore} reads them. Departures are indices into {@link SwissRailRaptorData#departures},
 * route stops are elements of {@link SwissRailRaptorData#routeStops}; a departure is only asked for the route
 * stops of its own route. Times are seconds since midnight.
 * <p>
 * The departures of a route must not overtake each other: there is an order of them in which, at every route
 * stop, no departure arrives or departs earlier than the one before. This order need not be the order of the
 * indices; each implementation searches the next departure in its own order. {@link SwissRailRaptorCore}
 * does not rely on the order of the indices.
 */
interface RaptorTimetable {

	int getArrivalTime(int departureIndex, RRouteStop routeStop);

	int getDepartureTime(int departureIndex, RRouteStop routeStop);

	/**
	 * @return the index of a departure of {@code route} that leaves {@code routeStop} at the earliest departure
	 * time that is not before {@code time}, or -1 if there is none. If several departures leave at that time,
	 * any of them may be returned.
	 */
	int findNextDepartureIndex(RRoute route, RRouteStop routeStop, int time);
}
