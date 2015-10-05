/* *********************************************************************** *
 * project: org.matsim.*
 * RoutingContextImpl.java
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

package org.matsim.core.router;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import java.util.HashMap;
import java.util.Map;

public class RoutingContextImpl implements RoutingContext {

	private final Map<String, TravelDisutility> travelDisutilities;
	private final Map<String, TravelTime> travelTimes;

	public RoutingContextImpl(Map<String,TravelDisutility> travelDisutilities, Map<String,TravelTime> travelTimes) {
		this.travelDisutilities = travelDisutilities;
		this.travelTimes = travelTimes;
	}

	public RoutingContextImpl(TravelDisutility travelDisutility, TravelTime travelTime) {
		this.travelDisutilities = new HashMap<>();
		this.travelTimes = new HashMap<>();
		this.travelDisutilities.put(TransportMode.car, travelDisutility);
		this.travelTimes.put(TransportMode.car, travelTime);
	}

	@Override
	public TravelDisutility getTravelDisutility() {
		return this.travelDisutilities.get(TransportMode.car);
	}

	@Override
	public TravelTime getTravelTime() {
		return this.travelTimes.get(TransportMode.car);
	}

	@Override
	public TravelDisutility getTravelDisutility(String mode) {
		return travelDisutilities.get(mode);
	}

	@Override
	public TravelTime getTravelTime(String mode) {
		return travelTimes.get(mode);
	}
}