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

package playground.marcel.pt.transitSchedule.api;

import java.util.List;
import java.util.Map;

import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.Identifiable;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.core.api.population.NetworkRoute;
import org.matsim.transitSchedule.TransitStopFacility;

/**
 * Describes a route of a transit line, including its stops and the departures along this route.
 * 
 * @author mrieser
 */
public interface TransitRoute extends Identifiable {

	public abstract void setDescription(final String description);

	public abstract String getDescription();

	/**
	 * Sets the transport mode with which this transit route is handled, e.g.
	 * {@link TransportMode#bus} or {@link TransportMode#train}.
	 *
	 * @param mode
	 */
	public abstract void setTransportMode(final TransportMode mode);

	public abstract TransportMode getTransportMode();

	public abstract void addDeparture(final Departure departure);

	public abstract Map<Id, Departure> getDepartures();

	public abstract NetworkRoute getRoute();

	public abstract void setRoute(final NetworkRoute route);

	public abstract List<TransitRouteStop> getStops();

	public abstract TransitRouteStop getStop(final TransitStopFacility stop);

}