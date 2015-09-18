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

package playground.vsp.congestion.routing;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.vehicles.Vehicle;

import playground.vsp.congestion.handlers.TollHandler;

/**
* @author ikaddoura
*/

public class RandomizedTollTimeDistanceTravelDisutility implements TravelDisutility {
	
	private final TravelDisutility randomizedTimeDistanceTravelDisutility;
	private final TollHandler tollHandler;
	private final double marginalUtilityOfMoney;
	private final double sigma ;
	
	public RandomizedTollTimeDistanceTravelDisutility(TravelDisutility randomizedTimeDistanceTravelDisutility,
			TollHandler tollHandler,
			double marginalUtilityOfMoney,
			double sigma) {

		this.randomizedTimeDistanceTravelDisutility = randomizedTimeDistanceTravelDisutility;
		this.tollHandler = tollHandler;
		this.marginalUtilityOfMoney = marginalUtilityOfMoney;
		this.sigma = sigma;
	}

	@Override
	public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
				
		double logNormalRnd = 1. ;
		if ( sigma != 0. ) {
			logNormalRnd = (double) person.getCustomAttributes().get("logNormalRnd") ;
		}
		
		double randomizedTimeDistanceDisutilityForLink = this.randomizedTimeDistanceTravelDisutility.getLinkTravelDisutility(link, time, person, vehicle);
		
		double tollCostsForLink = -1. * this.tollHandler.getAvgToll(link.getId(), time);
		double randomizedTollDisutilityForLink = tollCostsForLink * this.marginalUtilityOfMoney * logNormalRnd;
		
		return randomizedTimeDistanceDisutilityForLink + randomizedTollDisutilityForLink;
	}

	@Override
	public double getLinkMinimumTravelDisutility(Link link) {
		throw new UnsupportedOperationException();
	}

}

