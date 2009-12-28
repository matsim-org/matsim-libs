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

package org.matsim.transitSchedule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.population.routes.NetworkRouteWRefs;
import org.matsim.transitSchedule.api.Departure;
import org.matsim.transitSchedule.api.TransitRoute;
import org.matsim.transitSchedule.api.TransitRouteStop;
import org.matsim.transitSchedule.api.TransitStopFacility;


/**
 * Describes a route of a transit line, including its stops and the departures along this route.
 * 
 * @author mrieser
 */
public class TransitRouteImpl implements TransitRoute {

	private final Id routeId;
	private NetworkRouteWRefs route;
	private final List<TransitRouteStop> stops = new ArrayList<TransitRouteStop>();
	private String description = null;
	private final Map<Id, Departure> departures = new TreeMap<Id, Departure>();
	private TransportMode transportMode;
	private String lineRouteName;
	private String direction;

	protected TransitRouteImpl(final Id id, final NetworkRouteWRefs route, final List<TransitRouteStop> stops, final TransportMode mode) {
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

	public void addDeparture(final Departure departure) {
		final Id id = departure.getId();
		if (this.departures.containsKey(id)) {
			throw new IllegalArgumentException("There is already a departure with id " + id.toString());
		}
		this.departures.put(id, departure);
	}

	public boolean removeDeparture(final Departure departure) {
		return null != this.departures.remove(departure.getId());
	}
	
	public Map<Id, Departure> getDepartures() {
		return Collections.unmodifiableMap(this.departures);
	}

	public NetworkRouteWRefs getRoute() {
		return this.route;
	}

	public void setRoute(final NetworkRouteWRefs route) {
		this.route = route;
	}

	public List<TransitRouteStop> getStops() {
		return Collections.unmodifiableList(this.stops);
	}

	public TransitRouteStop getStop(final TransitStopFacility stop) {
		for (TransitRouteStop trStop : this.stops) {
			if (stop == trStop.getStopFacility()) {
				return trStop;
			}
		}
		return null;
	}

	public String getLineRouteName() {
		return lineRouteName;
	}

	public String getDirection() {
		return direction;
	}

	public void setLineRouteName(String lineRouteName) {
		this.lineRouteName = lineRouteName;
	}

	public void setDirection(String direction) {
		this.direction = direction;
	}

}
