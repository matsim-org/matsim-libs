/* *********************************************************************** *
 * project: org.matsim.*
 * FreeSpeedTravelTimeCalculator.java
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

package playground.christoph.withinday.trafficmonitoring;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.PersonalizableTravelTime;

/**
 * Calculates and returns the FreeSpeedTravelTime on a link at the given time.
 * @author cdobler
 */
public class FreeSpeedTravelTimeCalculator implements PersonalizableTravelTime {

	@Override
	public double getLinkTravelTime(Link link, double time) {
		return link.getLength() / link.getFreespeed(time);
	}

	@Override
	public void setPerson(Person person) {
		// nothing to do here
	}
}
