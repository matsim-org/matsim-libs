/*
 * Copyright 2018 Gunnar Flötteröd
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * contact: gunnar.flotterod@gmail.com
 *
 */
package org.matsim.contrib.pseudosimulation.searchacceleration.listeners;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.pseudosimulation.MobSimSwitcher;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.api.experimental.events.handler.VehicleDepartsAtFacilityEventHandler;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleCapacity;
import org.matsim.vehicles.Vehicles;

import floetteroed.utilities.Tuple;
import floetteroed.utilities.math.BasicStatistics;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class TransitStopInteractionListener
		implements TransitDriverStartsEventHandler, VehicleArrivesAtFacilityEventHandler,
		VehicleDepartsAtFacilityEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler {

	// -------------------- CONSTANTS / CLASSES --------------------

	private static class Route {

		protected final Id<TransitLine> lineId;
		protected final Id<TransitRoute> routeId;

		private Route(Id<TransitLine> lineId, final Id<TransitRoute> routeId) {
			this.lineId = lineId;
			this.routeId = routeId;
		}

		@Override
		public boolean equals(final Object obj) {
			if (obj instanceof Route) {
				final Route otherRoute = (Route) obj;
				return (this.lineId.equals(otherRoute.lineId) && this.routeId.equals(otherRoute.routeId));
			} else {
				return false;
			}
		}

		@Override
		public int hashCode() {
			// same recipe as in AbstractList
			int hashCode = 1;
			hashCode = 31 * hashCode + this.lineId.hashCode();
			hashCode = 31 * hashCode + this.routeId.hashCode();
			return hashCode;
		}
	}

	private static class Trip extends Route {

		protected final Id<Departure> departureId;

		private int passengerCnt = 0;

		public Trip(Id<TransitLine> lineId, final Id<TransitRoute> routeId, final Id<Departure> departureId) {
			super(lineId, routeId);
			this.departureId = departureId;
		}

		private void addPassenger() {
			this.passengerCnt++;
		}

		private void removePassenger() {
			this.passengerCnt--;
		}

		@Override
		public boolean equals(final Object obj) {
			if (obj instanceof Trip) {
				final Trip otherTrip = (Trip) obj;
				return (super.equals(otherTrip) && this.departureId.equals(otherTrip.departureId));
			} else {
				return false;
			}
		}

		@Override
		public int hashCode() {
			// same recipe as in AbstractList
			int hashCode = 1;
			hashCode = 31 * hashCode + super.hashCode();
			hashCode = 31 * hashCode + this.departureId.hashCode();
			return hashCode;
		}
	}

	public static class RouteStop extends Route {

		protected final Id<TransitStopFacility> stopId;

		public RouteStop(Id<TransitLine> lineId, final Id<TransitRoute> routeId, final Id<TransitStopFacility> stopId) {
			super(lineId, routeId);
			this.stopId = stopId;
		}

		@Override
		public boolean equals(final Object obj) {
			if (obj instanceof RouteStop) {
				final RouteStop otherRouteStop = (RouteStop) obj;
				return (super.equals(otherRouteStop) && this.stopId.equals(otherRouteStop.stopId));
			} else {
				return false;
			}
		}

		@Override
		public int hashCode() {
			// same recipe as in AbstractList
			int hashCode = 1;
			hashCode = 31 * hashCode + super.hashCode();
			hashCode = 31 * hashCode + this.stopId.hashCode();
			return hashCode;
		}
	}

	public static class TripStopPerformance implements Comparable<TripStopPerformance> {

		public final Id<Departure> departureId;
		public final double stopArrivalTime_s;

		public double stopDepartureTime_s = Double.POSITIVE_INFINITY;
		public int egressCnt = 0;
		public int accessCnt = 0;
		public int freeSpaceAtDeparture = 0;

		private TripStopPerformance(final Id<Departure> departureId, final double stopArrivaTime_s) {
			this.departureId = departureId;
			this.stopArrivalTime_s = stopArrivaTime_s;
		}

		private void addEgress() {
			this.egressCnt++;
		}

		private void addAccess() {
			this.accessCnt++;
		}

		private void setDeparturePerformance(final double stopDepartureTime_s, final int freeSpaceAtDeparture) {
			this.stopDepartureTime_s = stopDepartureTime_s;
			this.freeSpaceAtDeparture = freeSpaceAtDeparture;
		}

		public double getMeanEgressTime(final double accessDurPP_s, final double egressDurPP_s) {
			return this.stopArrivalTime_s + 0.5 * this.egressCnt * egressDurPP_s;
		}

		// TODO could be made more precise based on actual arrival time
		public double getMeanAccessTime(final double accessDurPP_s, final double egressDurPP_s) {
			return this.stopArrivalTime_s + this.egressCnt * egressDurPP_s + 0.5 * this.accessCnt * accessDurPP_s;

		}

		@Override
		public int compareTo(final TripStopPerformance other) {
			return Double.compare(this.stopArrivalTime_s, other.stopArrivalTime_s);
		}
	}

	public static class RouteStopPerformances {

		private final List<TripStopPerformance> allTripStopPerformances = new LinkedList<>();

		private RouteStopPerformances() {
		}

		private void addArrival(final Id<Departure> departureId, final double arrivalTime_s) {
			final TripStopPerformance performance = new TripStopPerformance(departureId, arrivalTime_s);
			this.allTripStopPerformances.add(performance);
		}

		private void addDeparture(final Id<Departure> departureId, final double departureTime_s,
				final int freeSpaceAtDeparture) {
			this.get(departureId).setDeparturePerformance(departureTime_s, freeSpaceAtDeparture);
		}

		// private void add(final TripStopPerformance tripStopPerformance) {
		// this.allTripStopPerformances.add(tripStopPerformance);
		// }

		public TripStopPerformance getNextInTime(final double time_s) {
			TripStopPerformance result = null;
			for (TripStopPerformance candidate : this.allTripStopPerformances) {
				if ((candidate.stopDepartureTime_s >= time_s) // && (candidate.freeSpaceAtDeparture > 0)
						&& ((result == null) || (candidate.stopDepartureTime_s < result.stopDepartureTime_s))) {
					result = candidate;
				}
			}
			return result;
		}

		public TripStopPerformance get(final Id<Departure> departureId) {
			for (TripStopPerformance candidate : this.allTripStopPerformances) {
				if (departureId.equals(candidate.departureId)) {
					return candidate;
				}
			}
			return null;
		}

	}

	// -------------------- MEMBERS --------------------

	private final MobSimSwitcher mobsimSwitcher;

	private final Population population;

	private final Vehicles transitVehicles;

	private final TransitSchedule transitSchedule;

	private final Map<Id<Vehicle>, Trip> vehId2trip = new LinkedHashMap<>();

	// Concurrent because it will be accessed by multiple pSim threads
	// simultaneously.
	private final Map<RouteStop, RouteStopPerformances> routeStop2performances = new ConcurrentHashMap<>();

	// -------------------- CONSTRUCTION --------------------

	public TransitStopInteractionListener(final MobSimSwitcher mobsimSwitcher, final Population population,
			final Vehicles transitVehicles, final TransitSchedule transitSchedule) {
		this.mobsimSwitcher = mobsimSwitcher;
		this.population = population;
		this.transitVehicles = transitVehicles;
		this.transitSchedule = transitSchedule;
	}

	// -------------------- SETTERS AND GETTERS --------------------

	public Map<RouteStop, RouteStopPerformances> newRouteStop2PerformancesView() {
		return Collections.unmodifiableMap(this.routeStop2performances);
	}

	public void analyzeResult(final TransitSchedule transitSchedule) {

		final Logger log = Logger.getLogger(this.getClass());
		log.info("analyzing transit performance ...");

		int unidentifiedCnt = 0;
		final BasicStatistics delayIfOnTime_s = new BasicStatistics();
		final BasicStatistics delayIfDelayed_s = new BasicStatistics();
		final BasicStatistics freeSpaceIfOnTime = new BasicStatistics();
		final BasicStatistics freeSpaceIfDelayed = new BasicStatistics();
//		Set<Tuple<Id<TransitLine>, Id<TransitRoute>>> unidentified = new LinkedHashSet<>();
		for (TransitLine line : transitSchedule.getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				for (Departure departure : route.getDepartures().values()) {
					final double routeDptTime_s = departure.getDepartureTime();
					for (TransitRouteStop stop : route.getStops()) {
						final RouteStopPerformances performances = this.routeStop2performances
								.get(new RouteStop(line.getId(), route.getId(), stop.getStopFacility().getId()));
						if (performances != null) {
							final TripStopPerformance performance = performances.get(departure.getId());
							if (performance != null) {
								final double delay_s = performance.stopDepartureTime_s
										- (routeDptTime_s + stop.getDepartureOffset());
								if (delay_s <= 0) {
									delayIfOnTime_s.add(delay_s);
									freeSpaceIfOnTime.add(performance.freeSpaceAtDeparture);
								} else {
									delayIfDelayed_s.add(delay_s);
									freeSpaceIfDelayed.add(performance.freeSpaceAtDeparture);
								}
							} else {
								// log.info("unidentified: " + route.getTransportMode() + ", stop " +
								// stop.getStopFacility().getId());
								unidentifiedCnt++;
								// unidentified.add(new Tuple<>(line.getId(), route.getId()));
							}
						} else {
							// log.info("unidentified: " + route.getTransportMode() + ", stop " +
							// stop.getStopFacility().getId());
							unidentifiedCnt++;
						}
					}
				}
			}
		}

		log.info(unidentifiedCnt + " unidentified but scheduled trip/stop interactions.");
//		for (Tuple<?,?> tuple : unidentified) {
//			log.info("UNIDENTIFIED: line " + tuple.getA() + ", route " + tuple.getB());
//		}
		log.info(delayIfOnTime_s.size() + " punctual interactions with mean delay " + delayIfOnTime_s.getAvg()
				+ " s, stddev " + delayIfOnTime_s.getStddev() + " s; free space " + freeSpaceIfOnTime.getAvg()
				+ ", stddev " + freeSpaceIfOnTime.getStddev() + ".");
		log.info(delayIfDelayed_s.size() + " delayed interactions with mean delay " + delayIfDelayed_s.getAvg()
				+ " s, stddev " + delayIfDelayed_s.getStddev() + " s; free space " + freeSpaceIfDelayed.getAvg()
				+ ", stddev " + freeSpaceIfDelayed.getStddev() + ".");
	}

	// -------------------- IMPLEMENTATION OF *EventHandler --------------------

	@Override
	public void reset(final int iteration) {
		this.analyzeResult(this.transitSchedule);
		if (this.mobsimSwitcher.isQSimIteration()) {
			this.vehId2trip.clear();
			this.routeStop2performances.clear();
		}
	}

	@Override
	public void handleEvent(final TransitDriverStartsEvent event) {
		if (this.mobsimSwitcher.isQSimIteration()
				&& this.transitVehicles.getVehicles().containsKey(event.getVehicleId())) {
			this.vehId2trip.put(event.getVehicleId(),
					new Trip(event.getTransitLineId(), event.getTransitRouteId(), event.getDepartureId()));
		}
	}

	@Override
	public void handleEvent(VehicleArrivesAtFacilityEvent event) {
		if (this.mobsimSwitcher.isQSimIteration()
				&& this.transitVehicles.getVehicles().containsKey(event.getVehicleId())) {
			final Trip trip = this.vehId2trip.get(event.getVehicleId());
			if (trip != null) {
				// final VehicleCapacity capacity =
				// this.transitVehicles.getVehicles().get(event.getVehicleId()).getType()
				// .getCapacity();
				// final double relativeOccupancy = ((double) trip.passengerCnt)
				// / (capacity.getSeats() + capacity.getStandingRoom());
				final RouteStop routeStop = new RouteStop(trip.lineId, trip.routeId, event.getFacilityId());
				RouteStopPerformances routepStopPerformances = this.routeStop2performances.get(routeStop);
				if (routepStopPerformances == null) {
					routepStopPerformances = new RouteStopPerformances();
					this.routeStop2performances.put(routeStop, routepStopPerformances);
				}
				routepStopPerformances.addArrival(trip.departureId, event.getTime());
			}
		}
	}

	@Override
	public void handleEvent(final VehicleDepartsAtFacilityEvent event) {
		if (this.mobsimSwitcher.isQSimIteration()
				&& this.transitVehicles.getVehicles().containsKey(event.getVehicleId())) {
			final Trip trip = this.vehId2trip.get(event.getVehicleId());
			if (trip != null) {
				final VehicleCapacity capacity = this.transitVehicles.getVehicles().get(event.getVehicleId()).getType()
						.getCapacity();
				// final double relativeOccupancy = ((double) trip.passengerCnt)
				// / (capacity.getSeats() + capacity.getStandingRoom());
				final int freeSpace = (capacity.getSeats() + capacity.getStandingRoom()) - trip.passengerCnt;

				final RouteStop routeStop = new RouteStop(trip.lineId, trip.routeId, event.getFacilityId());
				RouteStopPerformances routepStopPerformances = this.routeStop2performances.get(routeStop);
				if (routepStopPerformances == null) {
					routepStopPerformances = new RouteStopPerformances();
					this.routeStop2performances.put(routeStop, routepStopPerformances);
				}
				routepStopPerformances.addDeparture(trip.departureId, event.getTime(), freeSpace);
			}
		}
	}

	@Override
	public void handleEvent(final PersonEntersVehicleEvent event) {
		if (this.mobsimSwitcher.isQSimIteration()
				&& this.population.getPersons().keySet().contains(event.getPersonId())) {
			final Trip trip = this.vehId2trip.get(event.getVehicleId());
			if (trip != null) {
				trip.addPassenger();
			}
		}
	}

	@Override
	public void handleEvent(final PersonLeavesVehicleEvent event) {
		if (this.mobsimSwitcher.isQSimIteration()
				&& this.population.getPersons().keySet().contains(event.getPersonId())) {
			final Trip trip = this.vehId2trip.get(event.getVehicleId());
			if (trip != null) {
				trip.removePassenger();
			}
		}
	}
}
