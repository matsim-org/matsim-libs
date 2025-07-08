/* *********************************************************************** *
 * project: org.matsim.* 												   *
 *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2023 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package ch.sbb.matsim.routing.pt.raptor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.scoring.functions.ModeUtilityParameters;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

/**
 * @author mrieser / Simunto GmbH
 */
public class OccupancyData {

	private final static Logger LOG = LogManager.getLogger(OccupancyData.class);

	final IdMap<TransitLine, LineData> lineData = new IdMap<>(TransitLine.class);
	final Map<Id<Vehicle>, VehicleData> vehicleData = new HashMap<>();
	final Map<Id<Person>, PassengerData> paxData = new HashMap<>();
	private CacheData cache = null;

	public void reset() {
		LOG.info("[SwissRailRaptor] Resetting ExecutionData");
		this.lineData.clear();
		this.vehicleData.clear();
		this.paxData.clear();
		this.cache = null;
	}

	public DepartureData getNextAvailableDeparture(Id<TransitLine> transitLine, Id<TransitRoute> transitRoute, Id<TransitStopFacility> stopFacility, double time) {
		LineData line = this.lineData.get(transitLine);
		if (line == null) {
			return null;
		}
		RouteData route = line.routeData.get(transitRoute);
		if (route == null) {
			return null;
		}
		StopData stop = route.stopData.get(stopFacility);
		if (stop == null) {
			return null;
		}
		List<DepartureData> departures = stop.getSortedDepartures();
		if (departures.isEmpty()) {
			return null;
		}

		for (DepartureData dep : departures) {
			if (!Double.isInfinite(dep.earliestWaitStart) && Double.isInfinite(dep.latestWaitStart)) {
				// the vehicle is at a stop, there was at least one agent that already boarded,
				// but the vehicle has not left yet. Assume everybody can board this vehicle.
				return dep;
			}
			if (time <= dep.latestWaitStart) {
				return dep;
			}
		}
		return null;
	}

	public DepartureData getDepartureData(Id<TransitLine> transitLine, Id<TransitRoute> transitRoute, Id<TransitStopFacility> stopFacility, Id<Departure> departure) {
		LineData line = this.lineData.get(transitLine);
		if (line == null) {
			return null;
		}
		RouteData route = line.routeData.get(transitRoute);
		if (route == null) {
			return null;
		}
		StopData stop = route.stopData.get(stopFacility);
		if (stop == null) {
			return null;
		}
		for (DepartureData d : stop.depList) {
			if (d.departureId.equals(departure)) {
				return d;
			}
		}
		return null;
	}

	public RouteData getRouteData(Id<TransitLine> transitLine, Id<TransitRoute> transitRoute) {
		LineData line = this.lineData.get(transitLine);
		if (line == null) {
			return null;
		}
		return line.routeData.get(transitRoute);
	}

//	public Vehicle getVehicle(Id<TransitLine> transitLine, Id<TransitRoute> transitRoute, Id<Departure> departure) {
//		LineData line = this.lineData.get(transitLine);
//		if (line == null) {
//			return null;
//		}
//		RouteData route = line.routeData.get(transitRoute);
//		if (route == null) {
//			return null;
//		}
//		return route.vehicles.get(departure);
//	}

	public int getNextAvailableDeparture(SwissRailRaptorData data, SwissRailRaptorData.RRouteStop routeStop, double time) {
		CacheData cache = getCache(data);

		int[] latestWaitStartTimes = cache.latestWaitingTimeStartPerRouteStopPerDeparture[routeStop.index];
		int toIndex = latestWaitStartTimes.length;
		int offset = latestWaitStartTimes[0] - 1;
		int pos = Arrays.binarySearch(latestWaitStartTimes, 1, toIndex, (int) time);
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
		return offset + pos;
	}

	private CacheData getCache(SwissRailRaptorData data) {
		CacheData cache = this.cache;
		if (cache == null) {
			cache = buildCache(data);
		}
		return cache;
	}

	private synchronized CacheData buildCache(SwissRailRaptorData data) {
		CacheData cache = this.cache;
		if (cache != null) {
			return cache;
		}
		LOG.info("[SwissRailRaptor] build query-cache for ExecutionData");

		cache = new CacheData(data.routeStops.length);
		SwissRailRaptorData.RRouteStop[] routeStops = data.routeStops;
		for (int routeStopIdx = 0; routeStopIdx < routeStops.length; routeStopIdx++) {
			SwissRailRaptorData.RRouteStop routeStop = routeStops[routeStopIdx];
			SwissRailRaptorData.RRoute route = data.routes[routeStop.transitRouteIndex];
			int departuresCount = route.countDepartures;
			int[] departures = new int[departuresCount + 1];
			departures[0] = route.indexFirstDeparture;

			LineData lineData = this.lineData.get(routeStop.line.getId());
			RouteData routeData = lineData == null ? null : lineData.routeData.get(routeStop.route.getId());
			StopData stopData = routeData == null ? null : routeData.stopData.get(routeStop.routeStop.getStopFacility().getId());


			List<Departure> origDepartures = new ArrayList<>(routeStop.route.getDepartures().values());
			origDepartures.sort(Comparator.comparingDouble(Departure::getDepartureTime));

			int lastValue = -1;
			for (int depIdx = 0; depIdx < origDepartures.size(); depIdx++) {
				Departure departure = origDepartures.get(depIdx);
				DepartureData dd = stopData == null ? null : stopData.depData.get(departure.getId());
				int latestWaitStart;
				if (dd == null) {
					latestWaitStart = (int) (departure.getDepartureTime() + routeStop.departureOffset);
				} else {
					latestWaitStart = Double.isFinite(dd.latestWaitStart) ? (int) dd.latestWaitStart : lastValue;
				}
				departures[depIdx + 1] = latestWaitStart;
				lastValue = latestWaitStart;
			}
			cache.latestWaitingTimeStartPerRouteStopPerDeparture[routeStopIdx] = departures;
		}
		this.cache = cache;
		LOG.info("[SwissRailRaptor] done (build query-cache for ExecutionData)");
		return cache;
	}

	private static class CacheData {
		int[][] latestWaitingTimeStartPerRouteStopPerDeparture;

		private CacheData(int routeStopsCount) {
			this.latestWaitingTimeStartPerRouteStopPerDeparture = new int[routeStopsCount][];
		}
	}

	static class LineData {
		Map<Id<TransitRoute>, RouteData> routeData = new HashMap<>();
	}

	static class RouteData {
		Map<Id<TransitStopFacility>, StopData> stopData = new HashMap<>();
		final TransitRoute route;

		public RouteData(TransitRoute route) {
			this.route = route;
		}
	}

	static class StopData {
		Map<Id<Departure>, DepartureData> depData = new HashMap<>();
		boolean sorted = true;
		List<DepartureData> depList = new ArrayList<>();

		DepartureData getOrCreate(Id<Departure> depId) {
			DepartureData data = this.depData.get(depId);
			if (data == null) {
				data = new DepartureData(depId);
				this.depList.add(data);
				this.depData.put(depId, data);
				this.sorted = false;
			}
			return data;
		}

		List<DepartureData> getSortedDepartures() {
			if (!this.sorted) {
				this.depList.sort((o1, o2) -> {
					double depTime1 = o1.vehDepTime;
					double depTime2 = o2.vehDepTime;
					if (Double.isInfinite(depTime1)) {
						depTime1 = Double.POSITIVE_INFINITY;
					}
					if (Double.isInfinite(depTime2)) {
						depTime2 = Double.POSITIVE_INFINITY;
					}
					return Double.compare(depTime1, depTime2);
				});
				this.sorted = true;
			}

			ListIterator<DepartureData> li = this.depList.listIterator(this.depList.size());
			DepartureData laterDep = null;
			while (li.hasPrevious()) {
				DepartureData dep = li.previous();
				if (laterDep == null) {
					dep.latestWaitStart = dep.vehDepTime;
					laterDep = dep;
				} else {
					if (laterDep.earliestWaitStart < dep.vehDepTime) {
						// there were agents that could not board

						if (Double.isFinite(dep.latestWaitStart)) {
							// but at least some agents could board
							laterDep.earliestWaitStart = dep.latestWaitStart;
							laterDep = dep;
						}
					} else {
						// looks like everybody who wanted could board
						laterDep.earliestWaitStart = dep.vehDepTime;
						dep.latestWaitStart = dep.vehDepTime;
						laterDep = dep;
					}
				}
			}
			return this.depList;
		}
	}

	public static final class DepartureData {
		final Id<Departure> departureId;
		double vehDepTime = Double.NEGATIVE_INFINITY;
		double earliestWaitStart = Double.POSITIVE_INFINITY;
		double latestWaitStart = Double.NEGATIVE_INFINITY;
		int paxCountAtDeparture = -1;

		public DepartureData(Id<Departure> departureId) {
			this.departureId = departureId;
		}

		void addWaitingPerson(double startWaitTime) {
			if (startWaitTime < this.earliestWaitStart) {
				this.earliestWaitStart = startWaitTime;
			}
			if (startWaitTime > this.latestWaitStart) {
				this.latestWaitStart = startWaitTime;
			}
		}
	}

	static class VehicleData {
		final Vehicle vehicle;
		final Id<TransitLine> lineId;
		final Id<TransitRoute> routeId;
		final Id<Departure> departureId;
		Id<TransitStopFacility> stopFacilityId = null;
		int currentPaxCount = 0;

		public VehicleData(Vehicle vehicle, Id<TransitLine> lineId, Id<TransitRoute> routeId, Id<Departure> departureId) {
			this.vehicle = vehicle;
			this.lineId = lineId;
			this.routeId = routeId;
			this.departureId = departureId;
		}
	}

	static class PassengerData {
		final Person person;
		final Map<String, ModeUtilityParameters> modeParams;
		String mode;
		double waitingStartTime;
		double vehBoardingTime;
		Id<TransitStopFacility> boardingStopId;
		Id<Departure> departureId;

		public PassengerData(Person person, Map<String, ModeUtilityParameters> modeParams) {
			this.person = person;
			this.modeParams = modeParams;
		}
	}

}
