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

package org.matsim.pt.transitSchedule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;


/**
 * Describes a route of a transit line, including its stops and the departures along this route.
 *
 * @author mrieser
 */
public class TransitRouteImpl implements TransitRoute {

	private final Id<TransitRoute> routeId;
	private NetworkRoute route;
	private final List<TransitRouteStop> stops = new ArrayList<TransitRouteStop>(8);
	private String description = null;
	private final Map<Id<Departure>, Departure> departures = new TreeMap<Id<Departure>, Departure>();
	private String transportMode;
	private String lineRouteName;
	private String direction;

	protected TransitRouteImpl(final Id<TransitRoute> id, final NetworkRoute route, final List<TransitRouteStop> stops, final String transportMode) {
		this.routeId = id;
		this.route = route;
		this.stops.addAll(stops);
		this.transportMode = transportMode;
	}

	@Override
	public Id<TransitRoute> getId() {
		return this.routeId;
	}

	@Override
	public void setDescription(final String description) {
		this.description = description;
	}

	@Override
	public String getDescription() {
		return this.description;
	}

	/**
	 * Sets the transport mode with which this transit route is handled, e.g.
	 * <code>bus</code> or <code>train</code>.
	 *
	 * @param mode
	 */
	@Override
	public void setTransportMode(final String mode) {
		this.transportMode = mode;
	}

	@Override
	public String getTransportMode() {
		return this.transportMode;
	}

	@Override
	public void addDeparture(final Departure departure) {
		final Id<Departure> id = departure.getId();
		if (this.departures.containsKey(id)) {
			throw new IllegalArgumentException("There is already a departure with id " + id.toString() + " in transit route " + this.routeId);
		}
		this.departures.put(id, departure);
	}

	@Override
	public boolean removeDeparture(final Departure departure) {
		return null != this.departures.remove(departure.getId());
	}

	@Override
	public Map<Id<Departure>, Departure> getDepartures() {
		return Collections.unmodifiableMap(this.departures);
	}

	@Override
	public NetworkRoute getRoute() {
		return this.route;
	}

	@Override
	public void setRoute(final NetworkRoute route) {
		this.route = route;
	}

	@Override
	public List<TransitRouteStop> getStops() {
		return Collections.unmodifiableList(this.stops);
	}

	@Override
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

	@Override
	public String toString() {
		return "[TransitRouteImpl: route=" + this.routeId.toString() + ", #departures=" + this.departures.size() + "]";
	}
	
}
