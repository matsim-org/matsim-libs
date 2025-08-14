/* *********************************************************************** *
 * project: org.matsim.*
 * ExperimentalTransitRoute.java
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

package org.matsim.pt.routes;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

public interface TransitPassengerRoute extends Route {
	Id<TransitStopFacility> getAccessStopId();

	Id<TransitStopFacility> getEgressStopId();

	Id<TransitLine> getLineId();

	Id<TransitRoute> getRouteId();

	TransitPassengerRoute getChainedRoute();

	OptionalTime getBoardingTime();

}
