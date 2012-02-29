/* *********************************************************************** *
 * project: org.matsim.*
 * ParkAndRideTravelTimeCost.java
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
package playground.thibautd.parknride;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.PersonalizableTravelTime;

/**
 * @author thibautd
 */
public class ParkAndRideTravelTimeCost implements PersonalizableTravelCost, PersonalizableTravelTime {

	@Override
	public double getLinkGeneralizedTravelCost(
			final Link link,
			final double time) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getLinkTravelTime(
			final Link link,
			final double time) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setPerson(final Person person) {
		// TODO Auto-generated method stub
		
	}
}

