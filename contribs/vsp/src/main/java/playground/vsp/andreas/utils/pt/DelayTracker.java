/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.vsp.andreas.utils.pt;

import java.util.HashMap;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

/**
 * @author mrieser / senozon
 */
public class DelayTracker {

	private final TransitSchedule schedule;
	private final HashMap<Id, InternalData> vehicleTracker = new HashMap<Id, InternalData>();
	
	public DelayTracker(final TransitSchedule schedule) {
		this.schedule = schedule;
	}
	
	public void addVehicleAssignment(final Id vehicleId, final Id transitLineId, final Id transitRouteId, final Id departureId) {
		TransitLine line = this.schedule.getTransitLines().get(transitLineId);
		TransitRoute route = line.getRoutes().get(transitRouteId);
		Departure dep = route.getDepartures().get(departureId);
		
		this.vehicleTracker.put(vehicleId, new InternalData(dep.getDepartureTime(), route.getStops()));
	}
	
	public double vehicleArrivesAtStop(final Id vehicleId, final double arrivalTime, final Id stopFacilityId) {
		InternalData d = this.vehicleTracker.get(vehicleId);
		TransitRouteStop stop = d.stops.get(d.idx);
		if (!stop.getStopFacility().getId().equals(stopFacilityId)) {
			for (int idx = d.idx + 1; idx < d.stops.size(); idx++) {
				stop = d.stops.get(idx);
				if (stop.getStopFacility().getId().equals(stopFacilityId)) {
					// we found the stop, looks like we skipped some stations, or at least were not informed about the departure
					d.idx = idx;
					break;
				}
			}
		}
		
		if (stop.getStopFacility().getId().equals(stopFacilityId)) {
			double scheduledTime = d.departureTime + (stop.getArrivalOffset() == Time.UNDEFINED_TIME ? stop.getDepartureOffset() : stop.getArrivalOffset());
			return arrivalTime - scheduledTime;
		}
		return Double.NaN;
	}

	public double vehicleDepartsAtStop(final Id vehicleId, final double departureTime, final Id stopFacilityId) {
		InternalData d = this.vehicleTracker.get(vehicleId);
		TransitRouteStop stop = d.stops.get(d.idx);
		d.idx++;
		if (stop.getStopFacility().getId().equals(stopFacilityId)) {
			double scheduledTime = d.departureTime + stop.getDepartureOffset();
			return departureTime - scheduledTime;
		}
		return Double.NaN;
	}

	private static class InternalData {
		final double departureTime;
		final List<TransitRouteStop> stops;
		int idx = 0;
		
		public InternalData(final double departureTime, final List<TransitRouteStop> stops) {
			this.departureTime = departureTime;
			this.stops = stops;
		}
	}
	
}
