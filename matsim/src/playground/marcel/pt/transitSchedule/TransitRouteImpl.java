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
import org.matsim.core.api.population.NetworkRoute;
import org.matsim.transitSchedule.TransitStopFacility;

/**
 * Describes a route of a transit line, including its stops and the departures along this route.
 * 
 * @author mrieser
 */
public class TransitRouteImpl {

	private final Id routeId;
	private NetworkRoute route;
	private final List<TransitRouteStopImpl> stops = new ArrayList<TransitRouteStopImpl>();
	private String description = null;
	private final Map<Id, DepartureImpl> departures = new HashMap<Id, DepartureImpl>();
	private TransportMode transportMode;

	public TransitRouteImpl(final Id id, final NetworkRoute route, final List<TransitRouteStopImpl> stops, final TransportMode mode) {
		this.routeId = id;
		this.route = route;
		this.stops.addAll(stops);
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

	public void addDeparture(final DepartureImpl departure) {
		final Id id = departure.getId();
		if (this.departures.containsKey(id)) {
			throw new IllegalArgumentException("There is already a departure with id " + id.toString());
		}
		this.departures.put(id, departure);
	}

	public Map<Id, DepartureImpl> getDepartures() {
		return Collections.unmodifiableMap(this.departures);
	}

	public NetworkRoute getRoute() {
		return this.route;
	}

	public void setRoute(final NetworkRoute route) {
		this.route = route;
	}

	public List<TransitRouteStopImpl> getStops() {
		if (this.stops == null) {
			return Collections.unmodifiableList(new ArrayList<TransitRouteStopImpl>(0));
		}
		return Collections.unmodifiableList(this.stops);
	}

	public TransitRouteStopImpl getStop(final TransitStopFacility stop) {
		for (TransitRouteStopImpl trStop : this.stops) {
			if (stop == trStop.getStopFacility()) {
				return trStop;
			}
		}
		return null;
	}

}
