/* *********************************************************************** *
 * project: org.matsim.*
 * TransitScheduleBuilderImpl.java
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

import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;



public class TransitScheduleFactoryImpl implements TransitScheduleFactory {

	@Override
	public TransitLine createTransitLine(final Id<TransitLine> lineId) {
		return new TransitLineImpl(lineId);
	}

	@Override
	public TransitRoute createTransitRoute(final Id<TransitRoute> routeId, final NetworkRoute route, final List<TransitRouteStop> stops, final String mode) {
		return new TransitRouteImpl(routeId, route, stops, mode);
	}

	@Override
	public TransitRouteStop createTransitRouteStop(final TransitStopFacility stop, final double arrivalDelay, final double departureDelay) {
		return new TransitRouteStopImpl(stop, arrivalDelay, departureDelay);
	}

	@Override
	public TransitSchedule createTransitSchedule() {
		return new TransitScheduleImpl(this);
	}

	@Override
	public TransitStopFacility createTransitStopFacility(final Id<org.matsim.facilities.Facility> facilityId, final Coord coordinate, final boolean blocksLane) {
		return new TransitStopFacilityImpl(facilityId, coordinate, blocksLane);
	}

	@Override
	public Departure createDeparture(final Id<Departure> departureId, final double time) {
		return new DepartureImpl(departureId, time);
	}

}
