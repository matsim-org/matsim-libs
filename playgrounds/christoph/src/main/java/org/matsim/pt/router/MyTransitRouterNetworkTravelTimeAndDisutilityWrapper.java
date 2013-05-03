/* *********************************************************************** *
 * project: org.matsim.*
 * MyTransitRouterNetworkTravelTimeAndDisutilityWrapper.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package org.matsim.pt.router;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;

/**
 * Wraps a MyTransitRouterNetworkTravelTimeAndDisutility into a TransitTravelDisutility object.
 *
 * @author cdobler
 */
public class MyTransitRouterNetworkTravelTimeAndDisutilityWrapper extends MyTransitRouterNetworkTravelTimeAndDisutility
		implements TransitTravelDisutility {

	public MyTransitRouterNetworkTravelTimeAndDisutilityWrapper(final TransitRouterConfig config, PreparedTransitSchedule preparedTransitSchedule) {
		super(config, preparedTransitSchedule);
	}

	@Override
	public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle, CustomDataManager dataManager) {
		return super.getLinkTravelDisutility(link, time, person, vehicle);
	}

}
