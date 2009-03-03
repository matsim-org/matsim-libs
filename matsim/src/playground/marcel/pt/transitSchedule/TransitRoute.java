/* *********************************************************************** *
 * project: org.matsim.*
 * TransitRoute.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.marcel.pt.transitSchedule;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.interfaces.basic.v01.Id;
import org.matsim.interfaces.core.v01.Facility;
import org.matsim.interfaces.core.v01.Route;

public class TransitRoute {

	private final Id routeId;
	private final Route route;
	private final List<TransitRouteStop> stops;
	private String description = null;
	private final Map<Id, Departure> departures = new HashMap<Id, Departure>();

	public TransitRoute(final Id id, final Route route, final List<TransitRouteStop> stops) {
		this.routeId = id;
		this.route = route;
		this.stops = stops;
	}

	public Id getId() {
		return this.routeId;
	}

	public void setDescription(final String description) {
		this.description = description;
	}

	public String getDescription() {
		return this.description;
	}

	public void addDeparture(final Id id, final Departure departure) {
		if (this.departures.containsKey(id)) {
			throw new IllegalArgumentException("There is already a departure with id " + id.toString());
		}
		this.departures.put(id, departure);
	}

	public Map<Id, Departure> getDepartures() {
		return Collections.unmodifiableMap(this.departures);
	}

	public Route getRoute() {
		return this.route;
	}

	public List<TransitRouteStop> getStops() {
		return Collections.unmodifiableList(this.stops);
	}



	public TransitRouteStop getStop(final Facility stop) {
		for (TransitRouteStop trStop : this.stops) {
			if (stop == trStop.getStopFacility()) {
				return trStop;
			}
		}
		return null;
	}

}
