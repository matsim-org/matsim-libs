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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.core.api.facilities.Facility;
import org.matsim.core.api.population.NetworkRoute;

public class TransitRoute {

	private final Id routeId;
	private NetworkRoute route;
	private final List<TransitRouteStop> stops;
	private String description = null;
	private final Map<Id, Departure> departures = new HashMap<Id, Departure>();
	private TransportMode transportMode;

	public TransitRoute(final Id id, final NetworkRoute route, final List<TransitRouteStop> stops, final TransportMode mode) {
		this.routeId = id;
		this.route = route;
		this.stops = stops;
		this.transportMode = mode;
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

	/**
	 * Sets the transport mode with which this transit route is handled, e.g.
	 * {@link TransportMode#bus} or {@link TransportMode#train}.
	 * 
	 * @param mode
	 */
	public void setTransportMode(final TransportMode mode) {
		this.transportMode = mode;
	}
	
	public TransportMode getTransportMode() {
		return this.transportMode;
	}
	
	public void addDeparture(final Departure departure) {
		final Id id = departure.getId();
		if (this.departures.containsKey(id)) {
			throw new IllegalArgumentException("There is already a departure with id " + id.toString());
		}
		this.departures.put(id, departure);
	}

	public Map<Id, Departure> getDepartures() {
		return Collections.unmodifiableMap(this.departures);
	}

	public NetworkRoute getRoute() {
		return this.route;
	}
	
	public void setRoute(NetworkRoute route) {
		this.route = route;
	}

	public List<TransitRouteStop> getStops() {
		if (this.stops == null) {
			return Collections.unmodifiableList(new ArrayList<TransitRouteStop>(0));
		}
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
