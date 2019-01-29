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
import org.matsim.utils.objectattributes.attributable.Attributable;

/**
 * Describes a route of a transit line, including its stops and the departures along this route.
 *
 * @author mrieser
 */
public interface TransitRoute extends Identifiable<TransitRoute>, Attributable {

	void setDescription(final String description);

	String getDescription();

	/**
	 * Sets the transport mode with which this transit route is handled, e.g.
	 * <code>bus</code> or <code>train</code>.
	 *
	 * @param mode
	 */
	void setTransportMode(final String mode);

	String getTransportMode();

	void addDeparture(final Departure departure);

	boolean removeDeparture(final Departure departure);

	/**
	 * @return an immutable Map of all departures assigned to this route
	 */
	Map<Id<Departure>, Departure> getDepartures();

	NetworkRoute getRoute();

	void setRoute(final NetworkRoute route);

	/**
	 * @return an immutable list of all stops of this route in the order along the route
	 */
	List<TransitRouteStop> getStops();

	/**
	 * @param stop
	 * @return the {@link TransitRouteStop} of this route that stops at the specified
	 * {@link TransitStopFacility}. <code>null</code> if this route does not stop at the
	 * specified location.
	 */
	TransitRouteStop getStop(final TransitStopFacility stop);

}