/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package org.matsim.contrib.noise;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.vehicles.Vehicle;

/**
* @author ikaddoura
*/
class NoiseTollTimeDistanceTravelDisutility implements TravelDisutility {

	private final TravelDisutility travelDisutilityDelegate;
	private final TravelDisutility tollDisutilityDelegate;
	private final double marginalUtilityOfMoney;
	private final boolean usingRandomization;

	NoiseTollTimeDistanceTravelDisutility( TravelDisutility travelDisutilityDelegate, TravelDisutility tollDisutilityDelegate, double marginalUtilityOfMoney,
					       boolean usingRandomization ) {

		this.travelDisutilityDelegate = travelDisutilityDelegate;
		this.tollDisutilityDelegate = tollDisutilityDelegate;
		this.marginalUtilityOfMoney = marginalUtilityOfMoney;
		this.usingRandomization = usingRandomization;
	}

	@Override
	public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {

		double randomizedTimeDistanceDisutilityForLink = this.travelDisutilityDelegate.getLinkTravelDisutility(link, time, person, vehicle );

		double logNormalRnd = 1. ;
		if ( usingRandomization) {
			logNormalRnd = (double) person.getCustomAttributes().get("logNormalRnd") ;
		}

		double linkExpectedTollDisutility = this.marginalUtilityOfMoney * tollDisutilityDelegate.getLinkTravelDisutility(link, time, person, vehicle );
		double randomizedTollDisutility = linkExpectedTollDisutility * logNormalRnd;

		return randomizedTimeDistanceDisutilityForLink + randomizedTollDisutility;
	}

	@Override
	public double getLinkMinimumTravelDisutility(Link link) {
		return travelDisutilityDelegate.getLinkMinimumTravelDisutility( link ) + tollDisutilityDelegate.getLinkMinimumTravelDisutility( link );
	}
}

