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

package org.matsim.pt.analysis;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.events.PersonEntersVehicleEvent;
import org.matsim.core.events.PersonLeavesVehicleEvent;
import org.matsim.core.events.TransitDriverStartsEvent;
import org.matsim.core.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.core.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.core.events.handler.TransitDriverStartsEventHandler;
import org.matsim.core.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.events.handler.VehicleDepartsAtFacilityEventHandler;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * Calculates the number of passenger that are in a transit vehicle when
 * the vehicle departs a stop location.
 *
 * @author mrieser
 */
public class TransitLoad implements TransitDriverStartsEventHandler, VehicleArrivesAtFacilityEventHandler, VehicleDepartsAtFacilityEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler {

	private final Map<Id, LineData> lineData = new HashMap<Id, LineData>();

	private final Map<Id, Id> vehicleFacilityMap = new HashMap<Id, Id>();
	private final Map<Id, VehicleData> vehicleData = new HashMap<Id, VehicleData>();

	public TransitLoad() {
	}

	@Deprecated
	public TransitLoad(final TransitSchedule schedule) {
	}

	public int getLoadAtDeparture(final TransitLine line, final TransitRoute route, final TransitStopFacility stopFacility, final Departure departure) {
		int nOfPassengers = 0;
		for (TransitRouteStop stop : route.getStops()) {
			StopInformation si = getStopInformation(line.getId(), route.getId(), stop.getStopFacility().getId(), departure.getId(), false);
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
		return getStopInformation(line.getId(), route.getId(), stopFacility.getId(), departure.getId(), false);
	}

	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		this.vehicleData.put(event.getVehicleId(), new VehicleData(event.getVehicleId(), event.getTransitLineId(), event.getTransitRouteId(), event.getDepartureId()));
	}

	@Override
	public void handleEvent(final VehicleArrivesAtFacilityEvent event) {
		this.vehicleFacilityMap.put(event.getVehicleId(), event.getFacilityId());
		VehicleData vData = this.vehicleData.get(event.getVehicleId());
		if (vData != null) {
			StopInformation si = getStopInformation(vData.lineId, vData.routeId, this.vehicleFacilityMap.get(event.getVehicleId()), vData.departureId, true);
			si.arrivalTime = event.getTime();
		}
	}

	@Override
	public void handleEvent(final VehicleDepartsAtFacilityEvent event) {
		Id stopId = this.vehicleFacilityMap.remove(event.getVehicleId());
		VehicleData vData = this.vehicleData.get(event.getVehicleId());
		if (vData != null) {
			StopInformation si = getStopInformation(vData.lineId, vData.routeId, stopId, vData.departureId, true);
			si.departureTime = event.getTime();
		}
	}

	@Override
	public void handleEvent(final PersonEntersVehicleEvent event) {
		VehicleData vData = this.vehicleData.get(event.getVehicleId());
		if (vData != null) {
			StopInformation si = getStopInformation(vData.lineId, vData.routeId, this.vehicleFacilityMap.get(event.getVehicleId()), vData.departureId, true);
			si.nOfEntering++;
		}
	}

	@Override
	public void handleEvent(final PersonLeavesVehicleEvent event) {
		VehicleData vData = this.vehicleData.get(event.getVehicleId());
		if (vData != null) {
			StopInformation si = getStopInformation(vData.lineId, vData.routeId, this.vehicleFacilityMap.get(event.getVehicleId()), vData.departureId, true);
			si.nOfLeaving++;
		}
	}

	@Override
	public void reset(final int iteration) {
		this.vehicleFacilityMap.clear();
		if (this.vehicleData != null) {
			this.vehicleData.clear();
		}
	}

	private StopInformation getStopInformation(final Id lineId, final Id routeId, final Id stopFacilityId, final Id departureId, final boolean createIfMissing) {
		LineData ld = this.lineData.get(lineId);
		if (ld == null) {
			if (createIfMissing) {
				ld = new LineData();
				this.lineData.put(lineId, ld);
			} else {
				return null;
			}
		}

		RouteData rd = ld.routeData.get(routeId);
		if (rd == null) {
			if (createIfMissing) {
				rd = new RouteData();
				ld.routeData.put(routeId, rd);
			} else {
				return null;
			}
		}

		StopData sd = rd.stopData.get(stopFacilityId);
		if (sd == null) {
			if (createIfMissing) {
				sd = new StopData();
				rd.stopData.put(stopFacilityId, sd);
			} else {
				return null;
			}
		}

		StopInformation si = sd.departureData.get(departureId);
		if (si == null) {
			if (createIfMissing) {
				si = new StopInformation();
				sd.departureData.put(departureId, si);
			} else {
				return null;
			}
		}
		return si;
	}

	private static class VehicleData {
		public final Id vehicleId;
		public final Id lineId;
		public final Id routeId;
		public final Id departureId;

		public VehicleData(final Id vehicleId, final Id lineId, final Id routeId, final Id departureId) {
			this.vehicleId = vehicleId;
			this.lineId = lineId;
			this.routeId = routeId;
			this.departureId = departureId;
		}
	}

	/*package*/ static class LineData {
		public final Map<Id, RouteData> routeData = new HashMap<Id, RouteData>();
	}

	/*package*/ static class RouteData {
		public final Map<Id, StopData> stopData = new HashMap<Id, StopData>(); // use stopFacilityId as key
	}

	/*package*/ static class StopData {
		public final Map<Id, StopInformation> departureData = new HashMap<Id, StopInformation>(); // use departure id as key
	}

	public static class StopInformation {
		public short nOfEntering = 0;
		public short nOfLeaving = 0;
		public double arrivalTime = Time.UNDEFINED_TIME;
		public double departureTime = Time.UNDEFINED_TIME;
	}

}
