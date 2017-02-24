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

package org.matsim.contrib.accessibility.costcalculators;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.utils.misc.Time;
import org.matsim.vehicles.Vehicle;

/**
 * cost calculator for travel distances
 * @author thomas
 *
 */
public class TravelDistanceCalculator implements TravelDisutility{

	private static final Logger log = Logger.getLogger(TravelDistanceCalculator.class);
	
	@Override
	public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle vehicle) {
		if(link != null)
			return link.getLength();	// travel distance in meter
		log.warn("Link is null. Returned 0 as distance.");
		return 0.;
	}

	@Override
	public double getLinkMinimumTravelDisutility(Link link) {
		return getLinkTravelDisutility(link, Time.UNDEFINED_TIME, null, null);
	}
}
