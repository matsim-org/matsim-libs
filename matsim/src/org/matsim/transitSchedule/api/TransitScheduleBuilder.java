/* *********************************************************************** *
 * project: org.matsim.*
 * TransitScheduleBuilder.java
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

package org.matsim.transitSchedule.api;

import java.util.List;

import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.core.population.routes.NetworkRouteWRefs;

/**
 * @author mrieser
 */
public interface TransitScheduleBuilder {

	public abstract TransitSchedule createTransitSchedule();

	public abstract TransitLine createTransitLine(final Id lineId);

	public abstract TransitRoute createTransitRoute(final Id routeId, final NetworkRouteWRefs route, final List<TransitRouteStop> stops, final TransportMode mode);

	public abstract TransitRouteStop createTransitRouteStop(final TransitStopFacility stop, final double arrivalDelay, final double departureDelay);

	public abstract TransitStopFacility createTransitStopFacility(final Id facilityId, final Coord coordinate, final boolean blocksLane);

	public abstract Departure createDeparture(final Id departureId, final double time);

}
