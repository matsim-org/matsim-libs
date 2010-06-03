/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.mrieser.pt.analysis;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.events.PersonEntersVehicleEvent;
import org.matsim.core.events.PersonLeavesVehicleEvent;
import org.matsim.core.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.core.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.core.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.events.handler.VehicleDepartsAtFacilityEventHandler;
import org.matsim.core.utils.misc.Time;
import org.matsim.transitSchedule.api.Departure;
import org.matsim.transitSchedule.api.TransitLine;
import org.matsim.transitSchedule.api.TransitRoute;
import org.matsim.transitSchedule.api.TransitRouteStop;
import org.matsim.transitSchedule.api.TransitSchedule;
import org.matsim.transitSchedule.api.TransitStopFacility;

/**
 * Calculates the number of passenger that are in a transit vehicle when
 * the vehicle departs a stop location.
 *
 * @author mrieser
 */
public class TransitLoad implements VehicleArrivesAtFacilityEventHandler, VehicleDepartsAtFacilityEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler {

	private final static Logger log = Logger.getLogger(TransitLoad.class);

	private final Map<TransitLine, LineData> lineData = new HashMap<TransitLine, LineData>();

	private final TransitSchedule schedule;
	private final Map<Id, Id> vehicleFacilityMap = new HashMap<Id, Id>();
	private volatile Map<Id, VehicleData> vehicleData = null;

	public TransitLoad(final TransitSchedule schedule) {
		this.schedule = schedule;
	}

	public int getLoadAtDeparture(final TransitLine line, final TransitRoute route, final TransitStopFacility stopFacility, final Departure departure) {
		int nOfPassengers = 0;
		for (TransitRouteStop stop : route.getStops()) {
			StopInformation si = getStopInformation(line, route, stop.getStopFacility().getId(), departure, false);
			if (si != null) {
				nOfPassengers -= si.nOfLeaving;
				nOfPassengers += si.nOfEntering;
			}
			if (stop.getStopFacility() == stopFacility) {
				return nOfPassengers;
			}
		}
		return -1;
	}

	public StopInformation getDepartureStopInformation(final TransitLine line, final TransitRoute route, final TransitStopFacility stopFacility, final Departure departure) {
		return getStopInformation(line, route, stopFacility.getId(), departure, false);
	}

	public void handleEvent(VehicleArrivesAtFacilityEvent event) {
		this.vehicleFacilityMap.put(event.getVehicleId(), event.getFacilityId());
		if (this.vehicleData == null) {
			collectVehiclesInfo();
		}
		VehicleData vData = this.vehicleData.get(event.getVehicleId());
		StopInformation si = getStopInformation(vData.line, vData.route, this.vehicleFacilityMap.get(event.getVehicleId()), vData.departure, true);
		si.arrivalTime = event.getTime();
	}

	public void handleEvent(VehicleDepartsAtFacilityEvent event) {
		Id stopId = this.vehicleFacilityMap.remove(event.getVehicleId());
		if (this.vehicleData == null) {
			collectVehiclesInfo();
		}
		VehicleData vData = this.vehicleData.get(event.getVehicleId());
		StopInformation si = getStopInformation(vData.line, vData.route, stopId, vData.departure, true);
		si.departureTime = event.getTime();
	}

	@Override
	public void handleEvent(final PersonEntersVehicleEvent event) {
		if (this.vehicleData == null) {
			collectVehiclesInfo();
		}
		VehicleData vData = this.vehicleData.get(event.getVehicleId());
		StopInformation si = getStopInformation(vData.line, vData.route, this.vehicleFacilityMap.get(event.getVehicleId()), vData.departure, true);
		si.nOfEntering++;
	}

	@Override
	public void handleEvent(final PersonLeavesVehicleEvent event) {
		if (this.vehicleData == null) {
			collectVehiclesInfo();
		}
		VehicleData vData = this.vehicleData.get(event.getVehicleId());
		StopInformation si = getStopInformation(vData.line, vData.route, this.vehicleFacilityMap.get(event.getVehicleId()), vData.departure, true);
		si.nOfLeaving++;
	}

	public void reset(int iteration) {
		this.vehicleFacilityMap.clear();
		if (this.vehicleData != null) {
			this.vehicleData.clear();
		}
	}

	/**
	 * Lazy initialization, as the vehicle info may not be available from the beginning.
	 */
	private synchronized void collectVehiclesInfo() {
		if (this.vehicleData == null) {
			Map<Id, VehicleData> result = new HashMap<Id, VehicleData>(1000);
			for (TransitLine line : this.schedule.getTransitLines().values()) {
				for (TransitRoute route : line.getRoutes().values()) {
					for (Departure departure : route.getDepartures().values()) {
						if (departure.getVehicleId() != null) {
							VehicleData oldData = result.put(departure.getVehicleId(), new VehicleData(departure.getVehicleId(), line, route, departure));
							if (oldData != null) {
								log.warn("vehicle " + oldData.vehicleId + " is used for more than one departure. Cannot guarantee correct functioning of analysis!");
							}
						}
					}
				}
			}
			this.vehicleData = result;
		}
	}

	private StopInformation getStopInformation(final TransitLine line, final TransitRoute route, final Id stopFacilityId, final Departure departure, final boolean createIfMissing) {
		LineData ld = this.lineData.get(line);
		if (ld == null) {
			if (createIfMissing) {
				ld = new LineData();
				this.lineData.put(line, ld);
			} else {
				return null;
			}
		}

		RouteData rd = ld.routeData.get(route);
		if (rd == null) {
			if (createIfMissing) {
				rd = new RouteData();
				ld.routeData.put(route, rd);
			} else {
				return null;
			}
		}

		TransitStopFacility stop = this.schedule.getFacilities().get(stopFacilityId);
		StopData sd = rd.stopData.get(stop);
		if (sd == null) {
			if (createIfMissing) {
				sd = new StopData();
				rd.stopData.put(stop, sd);
			} else {
				return null;
			}
		}

		StopInformation si = sd.departureData.get(departure);
		if (si == null) {
			if (createIfMissing) {
				si = new StopInformation();
				sd.departureData.put(departure, si);
			} else {
				return null;
			}
		}
		return si;
	}

	private static class VehicleData {
		public final Id vehicleId;
		public final TransitLine line;
		public final TransitRoute route;
		public final Departure departure;

		public VehicleData(final Id vehicleId, final TransitLine line, final TransitRoute route, final Departure departure) {
			this.vehicleId = vehicleId;
			this.line = line;
			this.route = route;
			this.departure = departure;
		}
	}

	private static class LineData {
		public final Map<TransitRoute, RouteData> routeData = new HashMap<TransitRoute, RouteData>();
	}

	private static class RouteData {
		public final Map<TransitStopFacility, StopData> stopData = new HashMap<TransitStopFacility, StopData>();
	}

	private static class StopData {
		public final Map<Departure, StopInformation> departureData = new HashMap<Departure, StopInformation>();
	}

	public static class StopInformation {
		public short nOfEntering = 0;
		public short nOfLeaving = 0;
		public double arrivalTime = Time.UNDEFINED_TIME;
		public double departureTime = Time.UNDEFINED_TIME;
	}

}
