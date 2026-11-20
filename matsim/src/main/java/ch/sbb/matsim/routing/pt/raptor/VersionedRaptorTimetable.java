package ch.sbb.matsim.routing.pt.raptor;

import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorData.RRoute;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorData.RRouteStop;
import it.unimi.dsi.fastutil.ints.Int2IntLinkedOpenHashMap;
import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Times of the departures at their route stops that may differ from the transit schedule, e.g. because of delays.
 * An instance is one immutable version of these times: {@link #apply} returns a new version and leaves this one
 * unchanged, so different threads can route on different versions at the same time.
 * <p>
 * A first version comes from the times of the schedule ({@link #fromSchedule}) or from a complete set of times
 * ({@link #builder}). The builder also defines the order of the departures of each route and the external index of
 * each departure: its position in the order in which they were added, over all routes. Updates refer to departures
 * by this external index. In that order, the departures of a route must not overtake each other: at every route
 * stop, no departure arrives or departs earlier than the one before. Every version checks this, and that each
 * departure arrives at a stop before it departs and departs before it arrives at the next stop.
 * <p>
 * The times can only be used with {@link RaptorStaticConfig#isUseTimetableUpdates()}, because the transfers are
 * otherwise filtered with the times of the schedule.
 */
public final class VersionedRaptorTimetable implements RaptorTimetable {

	/**
	 * New times of one departure from the route stop at position {@code fromStop} of its route to the end of the
	 * route. {@code departure} is the external index of the departure.
	 */
	public record TripUpdate(int departure, int fromStop, int[] arrivalTimes, int[] departureTimes) {
	}

	private final Base base;
	/**
	 * per departure index of {@link SwissRailRaptorData}: arrival and departure time at each stop of its route
	 * looks like this: [[routeDeparture1_arrival1, routeDeparture1_departure1, ...], [routeDeparture2_arrival1, routeDeparture2_departure1, ...]]
	 */
	private final int[][] times;

	private VersionedRaptorTimetable(Base base, int[][] times) {
		this.base = base;
		this.times = times;
	}

	/** A version with the times of the transit schedule. The external index of a departure is its index in {@code data}. */
	public static VersionedRaptorTimetable fromSchedule(SwissRailRaptorData data) {
		requireTimetableUpdates(data);

		// [[r1_d1, r1_d2, ...], [r2_d1, r2_d2, ...]]
		int[][] order = new int[data.routes.length][];

		// [[d1_arrival1, d1_departure1, ...], [d2_arrival1, d2_departure1, ...]]
		int[][] times = new int[data.departures.length][];
		for (int r = 0; r < data.routes.length; r++) {
			RRoute route = data.routes[r];
			order[r] = new int[route.countDepartures];
			for (int k = 0; k < route.countDepartures; k++) {
				int dep = route.indexFirstDeparture + k;
				order[r][k] = dep;
				int[] row = new int[2 * route.countRouteStops];
				for (int i = 0; i < route.countRouteStops; i++) {
					RRouteStop routeStop = data.routeStops[route.indexFirstRouteStop + i];
					row[2 * i] = data.scheduleTimetable.getArrivalTime(dep, routeStop);
					row[2 * i + 1] = data.scheduleTimetable.getDepartureTime(dep, routeStop);
				}
				times[dep] = row;
			}
		}
		int[] departureOfExternal = new int[data.departures.length];
		for (int dep = 0; dep < departureOfExternal.length; dep++) {
			departureOfExternal[dep] = dep;
		}
		return new VersionedRaptorTimetable(new Base(data, order, departureOfExternal), times);
	}

	/** Builds a first version from a complete set of times, see {@link Builder}. */
	public static Builder builder(SwissRailRaptorData data) {
		requireTimetableUpdates(data);
		return new Builder(data);
	}

	/**
	 * @return a new version with the times of {@code updates}; this version stays unchanged. If an update refers to a
	 * departure that an earlier update of the same call has changed, it applies on top of it.
	 * @throws IllegalArgumentException if an update refers to an unknown departure or stop, has arrays of the wrong
	 *                                  length, or if the new times break the order of the departures of a route
	 */
	public VersionedRaptorTimetable apply(Collection<TripUpdate> updates) {
		SwissRailRaptorData data = this.base.data;
		int[][] newTimes = this.times.clone();
		Int2IntLinkedOpenHashMap firstChangedStop = new Int2IntLinkedOpenHashMap(updates.size());
		for (TripUpdate update : updates) {
			if (update.departure() < 0 || update.departure() >= this.base.departureOfExternal.length) {
				throw new IllegalArgumentException("Unknown departure " + update.departure() + ", there are " + this.base.departureOfExternal.length + " departures.");
			}
			int dep = this.base.departureOfExternal[update.departure()];
			RRoute route = data.routes[this.base.routeOfDeparture[dep]];
			if (update.fromStop() < 0 || update.fromStop() >= route.countRouteStops) {
				throw new IllegalArgumentException("Departure " + describe(dep) + " has no stop " + update.fromStop() + ", its route has " + route.countRouteStops + " stops.");
			}
			int count = route.countRouteStops - update.fromStop();
			if (update.arrivalTimes().length != count || update.departureTimes().length != count) {
				throw new IllegalArgumentException("Departure " + describe(dep) + " from stop " + update.fromStop() + " needs " + count + " arrivals and departures, but got "
					+ update.arrivalTimes().length + " and " + update.departureTimes().length + ".");
			}
			int[] row = newTimes[dep] == this.times[dep] ? this.times[dep].clone() : newTimes[dep];
			for (int k = 0; k < count; k++) {
				row[2 * (update.fromStop() + k)] = update.arrivalTimes()[k];
				row[2 * (update.fromStop() + k) + 1] = update.departureTimes()[k];
			}
			newTimes[dep] = row;
			int previousFirst = firstChangedStop.getOrDefault(dep, Integer.MAX_VALUE);
			firstChangedStop.put(dep, Math.min(previousFirst, update.fromStop()));
		}
		for (var entry : firstChangedStop.int2IntEntrySet()) {
			int dep = entry.getIntKey();
			int fromStop = entry.getIntValue();
			checkTrip(newTimes, dep, fromStop);
			int[] order = this.base.order[this.base.routeOfDeparture[dep]];
			int position = this.base.positionInOrder[dep];
			if (position > 0) {
				checkOrder(newTimes, order[position - 1], dep, fromStop);
			}
			if (position + 1 < order.length) {
				checkOrder(newTimes, dep, order[position + 1], fromStop);
			}
		}
		return new VersionedRaptorTimetable(this.base, newTimes);
	}

	@Override
	public boolean belongsTo(SwissRailRaptorData data) {
		return this.base.data == data;
	}

	@Override
	public int getArrivalTime(int departureIndex, RRouteStop routeStop) {
		return this.times[departureIndex][2 * this.base.stopPosition[routeStop.index]];
	}

	@Override
	public int getDepartureTime(int departureIndex, RRouteStop routeStop) {
		return this.times[departureIndex][2 * this.base.stopPosition[routeStop.index] + 1];
	}

	@Override
	public int findNextDepartureIndex(RRoute route, RRouteStop routeStop, int time) {
		int[] order = this.base.order[routeStop.transitRouteIndex];
		int column = 2 * this.base.stopPosition[routeStop.index] + 1;
		int low = 0;
		int high = order.length;
		while (low < high) {
			int mid = (low + high) >>> 1;
			if (this.times[order[mid]][column] < time) {
				low = mid + 1;
			} else {
				high = mid;
			}
		}
		return low < order.length ? order[low] : -1;
	}

	/** The times of a departure, for tests. */
	int[] getDepartureTimes(int departureIndex) {
		return this.times[departureIndex];
	}

	private String describe(int dep) {
		return describe(this.base.data, dep);
	}

	private static String describe(SwissRailRaptorData data, int dep) {
		return data.departureIds[dep] + " of route " + firstRouteStop(data, dep).route.getId();
	}

	private static RRouteStop firstRouteStop(SwissRailRaptorData data, int dep) {
		for (RRoute route : data.routes) {
			if (dep >= route.indexFirstDeparture && dep < route.indexFirstDeparture + route.countDepartures) {
				return data.routeStops[route.indexFirstRouteStop];
			}
		}
		throw new IllegalArgumentException("No route has departure " + dep);
	}

	/** Each departure arrives at a stop before it departs there, and departs before it arrives at the next stop. */
	private void checkTrip(int[][] times, int dep, int fromStop) {
		checkTrip(this.base.data, times[dep], dep, fromStop);
	}

	private static void checkTrip(SwissRailRaptorData data, int[] row, int dep, int fromStop) {
		int stops = row.length / 2;
		for (int i = fromStop; i < stops; i++) {
			if (row[2 * i] > row[2 * i + 1]) {
				throw new IllegalArgumentException("Departure " + describe(data, dep) + " arrives at stop " + i + " at " + row[2 * i] + ", after it departs at " + row[2 * i + 1] + ".");
			}
		}
		for (int i = Math.max(1, fromStop); i < stops; i++) {
			if (row[2 * i] < row[2 * i - 1]) {
				throw new IllegalArgumentException("Departure " + describe(data, dep) + " arrives at stop " + i + " at " + row[2 * i] + ", before it departs from stop " + (i - 1) + " at " + row[2 * i - 1] + ".");
			}
		}
	}

	/** {@code dep} neither arrives nor departs earlier than {@code previous}, the departure before it in its route. */
	private void checkOrder(int[][] times, int previous, int dep, int fromStop) {
		checkOrder(this.base.data, times[previous], times[dep], previous, dep, fromStop);
	}

	private static void checkOrder(SwissRailRaptorData data, int[] previousRow, int[] row, int previous, int dep, int fromStop) {
		int stops = row.length / 2;
		for (int i = fromStop; i < stops; i++) {
			if (row[2 * i] < previousRow[2 * i] || row[2 * i + 1] < previousRow[2 * i + 1]) {
				throw new IllegalArgumentException("Departure " + describe(data, dep) + " overtakes " + data.departureIds[previous] + " at stop " + i + ": it arrives at "
					+ row[2 * i] + " and departs at " + row[2 * i + 1] + ", the departure before it at " + previousRow[2 * i] + " and " + previousRow[2 * i + 1] + ".");
			}
		}
	}

	private static void requireTimetableUpdates(SwissRailRaptorData data) {
		if (!data.config.isUseTimetableUpdates()) {
			throw new IllegalStateException("Versioned times need SwissRailRaptor's option useTimetableUpdates; without it, the transfers are filtered with the times of the schedule.");
		}
	}

	/**
	 * Builds a first version from a complete set of times. For each route of the schedule, call {@link #addRoute}
	 * and then {@link #addDeparture} for each of its departures, in the order of the route. The external index of a
	 * departure is the number of departures added before it.
	 */
	public static final class Builder {
		private final SwissRailRaptorData data;
		private final Map<Id<TransitLine>, Map<Id<TransitRoute>, Integer>> routeIndices = new HashMap<>();
		private final int[][] order;
		private final int[][] times;
		private final int[] departureOfExternal;
		private int countExternal = 0;

		private int currentRoute = -1;
		private int currentCount = 0;
		private final Map<Id<Departure>, Integer> openDepartures = new HashMap<>();

		private Builder(SwissRailRaptorData data) {
			this.data = data;
			this.order = new int[data.routes.length][];
			this.times = new int[data.departures.length][];
			int countDepartures = 0;
			for (int r = 0; r < data.routes.length; r++) {
				RRoute route = data.routes[r];
				if (route.countRouteStops == 0) {
					// a route without stops cannot be identified and has no times
					this.order[r] = new int[0];
					for (int dep = route.indexFirstDeparture; dep < route.indexFirstDeparture + route.countDepartures; dep++) {
						this.times[dep] = new int[0];
					}
					continue;
				}
				RRouteStop first = data.routeStops[route.indexFirstRouteStop];
				this.routeIndices.computeIfAbsent(first.line.getId(), _ -> new HashMap<>()).put(first.route.getId(), r);
				countDepartures += route.countDepartures;
			}
			this.departureOfExternal = new int[countDepartures];
		}

		/**
		 * Starts the departures of a route. {@code stops} are the stop facilities of the route, which must match
		 * the schedule.
		 */
		public Builder addRoute(Id<TransitLine> lineId, Id<TransitRoute> routeId, List<Id<TransitStopFacility>> stops) {
			finishRoute();
			Integer r = this.routeIndices.getOrDefault(lineId, Map.of()).get(routeId);
			if (r == null) {
				throw new IllegalArgumentException("Unknown route " + routeId + " of line " + lineId + ".");
			}
			if (this.order[r] != null) {
				throw new IllegalArgumentException("Route " + routeId + " of line " + lineId + " was added twice.");
			}
			RRoute route = this.data.routes[r];
			if (stops.size() != route.countRouteStops) {
				throw new IllegalArgumentException("Route " + routeId + " of line " + lineId + " has " + route.countRouteStops + " stops in the schedule, but " + stops.size() + " were given.");
			}
			for (int i = 0; i < stops.size(); i++) {
				Id<TransitStopFacility> expected = this.data.routeStops[route.indexFirstRouteStop + i].routeStop.getStopFacility().getId();
				if (!expected.equals(stops.get(i))) {
					throw new IllegalArgumentException("Stop " + i + " of route " + routeId + " of line " + lineId + " is " + expected + " in the schedule, but " + stops.get(i) + " was given.");
				}
			}
			this.currentRoute = r;
			this.currentCount = 0;
			this.order[r] = new int[route.countDepartures];
			for (int dep = route.indexFirstDeparture; dep < route.indexFirstDeparture + route.countDepartures; dep++) {
				this.openDepartures.put(this.data.departureIds[dep], dep);
			}
			return this;
		}

		/** Adds the next departure of the current route, with its arrival and departure time at each stop of the route. */
		public Builder addDeparture(Id<Departure> departureId, int[] arrivals, int[] departures) {
			if (this.currentRoute < 0) {
				throw new IllegalStateException("Add a route before its departures.");
			}
			RRoute route = this.data.routes[this.currentRoute];
			Integer dep = this.openDepartures.remove(departureId);
			if (dep == null) {
				throw new IllegalArgumentException("Departure " + departureId + " is not a departure of route " + currentRouteId() + ", or was added twice.");
			}
			if (arrivals.length != route.countRouteStops || departures.length != route.countRouteStops) {
				throw new IllegalArgumentException("Departure " + departureId + " of route " + currentRouteId() + " needs " + route.countRouteStops + " arrivals and departures, but got "
					+ arrivals.length + " and " + departures.length + ".");
			}
			int[] row = new int[2 * route.countRouteStops];
			for (int i = 0; i < route.countRouteStops; i++) {
				row[2 * i] = arrivals[i];
				row[2 * i + 1] = departures[i];
			}
			checkTrip(this.data, row, dep, 0);
			if (this.currentCount > 0) {
				int previous = this.order[this.currentRoute][this.currentCount - 1];
				checkOrder(this.data, this.times[previous], row, previous, dep, 0);
			}
			this.times[dep] = row;
			this.order[this.currentRoute][this.currentCount++] = dep;
			this.departureOfExternal[this.countExternal++] = dep;
			return this;
		}

		/**
		 * @return the first version
		 * @throws IllegalArgumentException if a route or a departure of the schedule is missing
		 */
		public VersionedRaptorTimetable build() {
			finishRoute();
			List<Id<TransitRoute>> missing = new ArrayList<>();
			for (int r = 0; r < this.order.length; r++) {
				if (this.order[r] == null) {
					missing.add(this.data.routeStops[this.data.routes[r].indexFirstRouteStop].route.getId());
				}
			}
			if (!missing.isEmpty()) {
				throw new IllegalArgumentException(missing.size() + " routes of the schedule are missing, e.g. " + missing.subList(0, Math.min(5, missing.size())) + ".");
			}
			return new VersionedRaptorTimetable(new Base(this.data, this.order, this.departureOfExternal), this.times);
		}

		private void finishRoute() {
			if (this.currentRoute >= 0 && !this.openDepartures.isEmpty()) {
				throw new IllegalArgumentException("Route " + currentRouteId() + " misses " + this.openDepartures.size() + " departures, e.g. "
					+ this.openDepartures.keySet().iterator().next() + ".");
			}
			this.currentRoute = -1;
		}

		private Id<TransitRoute> currentRouteId() {
			return this.data.routeStops[this.data.routes[this.currentRoute].indexFirstRouteStop].route.getId();
		}
	}

	/** What all versions built from the same first version share. */
	private static final class Base {
		final SwissRailRaptorData data;
		/** per route: its departures in the order of the route */
		final int[][] order;
		/** per departure: its position in the order of its route */
		final int[] positionInOrder;
		/** per departure: its route */
		final int[] routeOfDeparture;
		/** per route stop: its position in its route */
		final int[] stopPosition;
		/** per external index: the departure */
		final int[] departureOfExternal;

		Base(SwissRailRaptorData data, int[][] order, int[] departureOfExternal) {
			this.data = data;
			this.order = order;
			this.departureOfExternal = departureOfExternal;
			this.positionInOrder = new int[data.departures.length];
			this.routeOfDeparture = new int[data.departures.length];
			this.stopPosition = new int[data.routeStops.length];
			for (int r = 0; r < data.routes.length; r++) {
				RRoute route = data.routes[r];
				for (int k = 0; k < order[r].length; k++) {
					this.positionInOrder[order[r][k]] = k;
				}
				for (int dep = route.indexFirstDeparture; dep < route.indexFirstDeparture + route.countDepartures; dep++) {
					this.routeOfDeparture[dep] = r;
				}
				for (int i = 0; i < route.countRouteStops; i++) {
					this.stopPosition[route.indexFirstRouteStop + i] = i;
				}
			}
		}
	}
}
