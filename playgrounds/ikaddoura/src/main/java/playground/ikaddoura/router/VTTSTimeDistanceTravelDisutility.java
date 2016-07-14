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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutilityFactory;
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
public final class VTTSTimeDistanceTravelDisutility implements TravelDisutility {
	private final static Logger log = Logger.getLogger(VTTSTimeDistanceTravelDisutility.class);
	
	private final TravelDisutility delegate;
	private final TravelTime timeCalculator;
	private final PlanCalcScoreConfigGroup cnScoringGroup;
	private final VTTSHandler vttsHandler;
	private final double sigma ;

	private static int toSmallVTTSWarning = 0;
	private static int toLargeVTTSWarning = 0;

	VTTSTimeDistanceTravelDisutility(final TravelTime timeCalculator, PlanCalcScoreConfigGroup cnScoringGroup, double sigma, VTTSHandler vttsHandler) {
		this.timeCalculator = timeCalculator;
		this.vttsHandler = vttsHandler;
		this.cnScoringGroup = cnScoringGroup;
		this.sigma = sigma;
		
		final RandomizingTimeDistanceTravelDisutilityFactory builder = new RandomizingTimeDistanceTravelDisutilityFactory( TransportMode.car, cnScoringGroup );
		builder.setSigma(sigma);
		this.delegate = builder.createTravelDisutility(timeCalculator);
	}

	@Override
	public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle vehicle) {
		
		this.delegate.getLinkTravelDisutility(link, time, person, vehicle);
				
		// do not use the link travel disutility from the delegate
		
		double travelTime_sec = this.timeCalculator.getLinkTravelTime(link, time, person, vehicle);
		
		double marginalCostOfDistance = - cnScoringGroup.getModes().get( TransportMode.car ).getMonetaryDistanceRate() * cnScoringGroup.getMarginalUtilityOfMoney() ;
		
		double vtts_hour = this.vttsHandler.getCarVTTS(person.getId(), time);
		
		if (vtts_hour < 1.0) {
			if (toSmallVTTSWarning  <= 3) {
				log.warn("The VTTS is " + vtts_hour + " and considered as too small. Setting the VTTS to 1.0. Further information: " + link + "/" + time + "/" + person + "/" + vehicle);
			}
			if (toSmallVTTSWarning == 3) {
				log.warn("Additional warnings of this type are suppressed.");
			}
			toSmallVTTSWarning++;			
			vtts_hour = 1.0;
		}
		
		if (vtts_hour > 1000000.) {
			if (toLargeVTTSWarning  <= 3) {
				log.warn("The VTTS is " + vtts_hour + " and considered as too large. Setting the VTTS to 1000000.0. Further information: " + link + "/" + time + "/" + person + "/" + vehicle);
			}
			if (toLargeVTTSWarning == 3) {
				log.warn("Additional warnings of this type are suppressed.");
			}
			toLargeVTTSWarning++;
			vtts_hour = 1000000.0;
		}
		
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
