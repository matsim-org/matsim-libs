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

package org.matsim.pt.transitSchedule.api;

import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.core.population.routes.NetworkRoute;

/**
 * Describes a route of a transit line, including its stops and the departures along this route.
 *
 * @author mrieser
 */
public interface TransitRoute extends Identifiable<TransitRoute> {

	public abstract void setDescription(final String description);

	public abstract String getDescription();

	/**
	 * Sets the transport mode with which this transit route is handled, e.g.
	 * <code>bus</code> or <code>train</code>.
	 *
	 * @param mode
	 */
	public abstract void setTransportMode(final String mode);

	public abstract String getTransportMode();

	public abstract void addDeparture(final Departure departure);

	public abstract boolean removeDeparture(final Departure departure);

	/**
	 * @return an immutable Map of all departures assigned to this route
	 */
	public abstract Map<Id<Departure>, Departure> getDepartures();

	public abstract NetworkRoute getRoute();

	public abstract void setRoute(final NetworkRoute route);

	/**
	 * @return an immutable list of all stops of this route in the order along the route
	 */
	public abstract List<TransitRouteStop> getStops();

	/**
	 * @param stop
	 * @return the {@link TransitRouteStop} of this route that stops at the specified
	 * {@link TransitStopFacility}. <code>null</code> if this route does not stop at the
	 * specified location.
	 */
	public abstract TransitRouteStop getStop(final TransitStopFacility stop);

}