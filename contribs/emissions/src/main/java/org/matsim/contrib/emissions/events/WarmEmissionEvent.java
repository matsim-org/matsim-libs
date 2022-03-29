/* *********************************************************************** *
 * project: org.matsim.*
 * WarmEmissionEventImpl.java
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
package org.matsim.contrib.emissions.events;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.emissions.Pollutant;
import org.matsim.vehicles.Vehicle;

import java.util.Map;
import java.util.Map.Entry;


/**
 * @author benjamin
 */
public final class WarmEmissionEvent extends EmissionEvent {
	// leave this public so that external code can generate "standard" emission events. MATSIM-893

	public final static String EVENT_TYPE = "warmEmissionEvent";

	public WarmEmissionEvent(double time, Id<Link> linkId, Id<Vehicle> vehicleId, Map<Pollutant, Double> warmEmissions) {
		// this is a WARM emission event, and so can accept the typed map. kai, jan'20
		super(time, linkId, vehicleId, warmEmissions);
	}

	public Map<Pollutant, Double> getWarmEmissions() {
		return getEmissions();
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}
}
