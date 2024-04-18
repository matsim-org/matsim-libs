/* *********************************************************************** *
 * project: org.matsim.*
 * OnlyTimeDependentTravelCostCalculator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.core.router.costcalculators;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.vehicles.Vehicle;

/**
 *  A Travel Cost Calculator that uses the travel times as travel disutility.
 *
 * @author cdobler
 */
public class OnlyTimeDependentTravelDisutility implements TravelDisutility {

	private static final Logger log = LogManager.getLogger(OnlyTimeDependentTravelDisutility.class);

	protected final TravelTime travelTime;

	public OnlyTimeDependentTravelDisutility(final TravelTime travelTime) {
		if (travelTime == null) {
			log.warn("TimeCalculator is null so FreeSpeedTravelTimes will be calculated!");
			this.travelTime = new FreeSpeedTravelTime();
		} else this.travelTime = travelTime;
	}

	@Override
	public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle vehicle) {
		return this.travelTime.getLinkTravelTime(link, time, person, vehicle);
	}

	@Override
	public double getLinkMinimumTravelDisutility(final Link link) {
		return link.getLength() / link.getFreespeed();
	}
}
