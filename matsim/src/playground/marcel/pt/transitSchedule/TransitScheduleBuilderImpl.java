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

package playground.marcel.pt.transitSchedule;

import java.util.List;

import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.transitSchedule.TransitStopFacility;

import playground.marcel.pt.transitSchedule.api.Departure;
import playground.marcel.pt.transitSchedule.api.TransitLine;
import playground.marcel.pt.transitSchedule.api.TransitRoute;
import playground.marcel.pt.transitSchedule.api.TransitRouteStop;
import playground.marcel.pt.transitSchedule.api.TransitSchedule;
import playground.marcel.pt.transitSchedule.api.TransitScheduleBuilder;

public class TransitScheduleBuilderImpl implements TransitScheduleBuilder {

	public TransitLine createTransitLine(final Id lineId) {
		return new TransitLineImpl(lineId);
	}

	public TransitRoute createTransitRoute(final Id routeId, final NetworkRoute route, final List<TransitRouteStop> stops, final TransportMode mode) {
		return new TransitRouteImpl(routeId, route, stops, mode);
	}

	public TransitRouteStop createTransitRouteStop(final TransitStopFacility stop, final double arrivalDelay, final double departureDelay) {
		return new TransitRouteStopImpl(stop, arrivalDelay, departureDelay);
	}

	public TransitSchedule createTransitSchedule() {
		return new TransitScheduleImpl(this);
	}

	public TransitStopFacility createTransitStopFacility(final Id facilityId, final Coord coordinate) {
		return new TransitStopFacility(facilityId, coordinate);
	}
	
	public Departure createDeparture(final Id departureId, final double time) {
		return new DepartureImpl(departureId, time);
	}

}
