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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.vehicles.Vehicle;

import playground.vsp.congestion.handlers.TollHandler;

/**
* @author ikaddoura
*/

public class CongestionTollTimeDistanceTravelDisutility implements TravelDisutility {
	
	private final TravelDisutility randomizedTimeDistanceTravelDisutility;
	private final TollHandler tollHandler;
	private final double marginalUtilityOfMoney;
	private final double sigma;
	
	/*
	 * Blur the Social Cost to speed up the relaxation process. Values between
	 * 0.0 and 1.0 are valid. 0.0 means the old value will be kept, 1.0 means
	 * the old value will be totally overwritten.
	 */
	private final double blendFactor;
	
	public CongestionTollTimeDistanceTravelDisutility(TravelDisutility randomizedTimeDistanceTravelDisutility,
			TollHandler tollHandler,
			double marginalUtilityOfMoney,
			double sigma,
			double blendFactor) {

		this.blendFactor = blendFactor;
		this.randomizedTimeDistanceTravelDisutility = randomizedTimeDistanceTravelDisutility;
		this.tollHandler = tollHandler;
		this.marginalUtilityOfMoney = marginalUtilityOfMoney;
		this.sigma = sigma;
	}

	@Override
	public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
				
		double randomizedTimeDistanceDisutilityForLink = this.randomizedTimeDistanceTravelDisutility.getLinkTravelDisutility(link, time, person, vehicle);
		
		double logNormalRnd = 1. ;
		if ( sigma != 0. ) {
			logNormalRnd = (double) person.getCustomAttributes().get("logNormalRnd") ;
		}
		
		double linkExpectedTollDisutility = calculateExpectedTollDisutility(link.getId(), time, person.getId());
		double randomizedTollDisutility = linkExpectedTollDisutility * logNormalRnd;
		
		return randomizedTimeDistanceDisutilityForLink + randomizedTollDisutility;				
	}

	@Override
	public double getLinkMinimumTravelDisutility(Link link) {
		throw new UnsupportedOperationException();
	}

	private double calculateExpectedTollDisutility(Id<Link> linkId, double time, Id<Person> personId) {
		
		/* The following is an estimate of the tolls that an agent would have to pay if choosing that link in the next
		iteration based on the tolls in the previous iteration(s) */
		
		// congestion toll disutility
		
		double linkExpectedTollNewValue = this.tollHandler.getAvgToll(linkId, time);
		double linkExpectedTollOldValue = this.tollHandler.getAvgTollOldValue(linkId, time);

		double blendedOldValue = (1 - blendFactor) * linkExpectedTollOldValue;
		double blendedNewValue = blendFactor * linkExpectedTollNewValue;	

		double expectedLinkCongestionTollDisutility = -1 * this.marginalUtilityOfMoney * (blendedOldValue + blendedNewValue);						
		return expectedLinkCongestionTollDisutility;
	}
}

