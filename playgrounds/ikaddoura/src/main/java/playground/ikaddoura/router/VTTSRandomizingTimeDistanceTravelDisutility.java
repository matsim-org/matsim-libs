/* *********************************************************************** *
 * project: org.matsim.*
 * TravelTimeDistanceCostCalculator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.ikaddoura.router;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutility;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

import playground.ikaddoura.analysis.vtts.VTTSHandler;

/**
 * A simple cost calculator which only respects time and distance to calculate generalized costs.
 * 
 * Time is converted into costs taking into account the person-specific VTTS. 
 *
 * @author ikaddoura
 */
public final class VTTSRandomizingTimeDistanceTravelDisutility implements TravelDisutility {
	
	private final RandomizingTimeDistanceTravelDisutility delegate;
	private final TravelTime timeCalculator;
	private final PlanCalcScoreConfigGroup cnScoringGroup;
	private final VTTSHandler vttsHandler;
	private final double sigma ;

	VTTSRandomizingTimeDistanceTravelDisutility(final TravelTime timeCalculator, PlanCalcScoreConfigGroup cnScoringGroup, double sigma, VTTSHandler vttsHandler) {
		this.timeCalculator = timeCalculator;
		this.vttsHandler = vttsHandler;
		this.cnScoringGroup = cnScoringGroup;
		this.sigma = sigma;
		
		this.delegate = new RandomizingTimeDistanceTravelDisutility.Builder().createTravelDisutility(timeCalculator, cnScoringGroup);

	}

	@Override
	public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle vehicle) {
		
		this.delegate.getLinkTravelDisutility(link, time, person, vehicle);
				
		// do not use the link travel disutility from the delegate
		
		double travelTime_sec = this.timeCalculator.getLinkTravelTime(link, time, person, vehicle);
		
		double marginalCostOfDistance = - cnScoringGroup.getModes().get( TransportMode.car ).getMonetaryDistanceRate() * cnScoringGroup.getMarginalUtilityOfMoney() ;
		
		double vtts_hour = this.vttsHandler.getVTTS(person.getId(), time);
		
		double logNormalRnd = 1. ;
		if ( sigma != 0. ) {
			logNormalRnd = (double) person.getCustomAttributes().get("logNormalRnd") ;
		}
		
		double linkTravelDisutility = vtts_hour * cnScoringGroup.getMarginalUtilityOfMoney() * travelTime_sec / 3600. + logNormalRnd * marginalCostOfDistance * link.getLength();
		
		return linkTravelDisutility;
	}

	@Override
	public double getLinkMinimumTravelDisutility(final Link link) {
		return this.delegate.getLinkMinimumTravelDisutility(link);
	}

}
