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
import java.util.Objects;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.pt.transitSchedule.api.*;


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
		return new TransitRouteStopImpl.Builder().stop(stop).arrivalOffset(arrivalDelay).departureOffset(departureDelay).build();
	}

	@Override
	public TransitRouteStop createTransitRouteStop(final TransitStopFacility stop, final OptionalTime arrivalDelay, final OptionalTime departureDelay) {
		return new TransitRouteStopImpl.Builder().stop(stop).arrivalOffset(arrivalDelay).departureOffset(departureDelay).build();
	}

	@Override
	public TransitRouteStopImpl.Builder createTransitRouteStopBuilder(TransitStopFacility stop) {
		return new TransitRouteStopImpl.Builder().stop(stop);
	}

	@Override
	public TransitSchedule createTransitSchedule() {
		return new TransitScheduleImpl(this);
	}

	@Override
	public TransitStopFacility createTransitStopFacility(final Id<TransitStopFacility> facilityId, final Coord coordinate, final boolean blocksLane) {
		return new TransitStopFacilityImpl(facilityId, coordinate, blocksLane);
	}

	@Override
	public Departure createDeparture(final Id<Departure> departureId, final double time) {
		return new DepartureImpl(departureId, time);
	}

	@Override
	public ChainedDeparture createChainedDeparture(final Id<TransitLine> transitLineId, final Id<TransitRoute> transitRouteId, final Id<Departure> departureId) {
		return new ChainedDepartureImpl(transitLineId, transitRouteId, departureId);
	}

}
