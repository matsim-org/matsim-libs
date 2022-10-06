/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.contrib.accessibility;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.vehicles.Vehicle;

/**
 * cost calculator for travel distances
 *
 * @author thomas
 */
class TravelDistanceCalculator implements TravelDisutility{

	private static final Logger log = LogManager.getLogger(TravelDistanceCalculator.class);

	@Override
	public double getLinkTravelDisutility(final Link link, final double time, final Person person,
			final Vehicle vehicle) {
		return getLinkTravelDisutilityImpl(link);
	}

	@Override
	public double getLinkMinimumTravelDisutility(Link link) {
		return getLinkTravelDisutilityImpl(link);
	}

	private double getLinkTravelDisutilityImpl(Link link) {
		if (link != null) {
			return link.getLength();    // travel distance in meter
		}
		log.warn("Link is null. Returned 0 as distance.");
		return 0.;
	}
}
