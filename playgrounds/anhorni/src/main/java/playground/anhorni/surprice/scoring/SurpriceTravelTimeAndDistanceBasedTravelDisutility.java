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

package playground.anhorni.surprice.scoring;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;


/**
 * A simple cost calculator which only respects time and distance to calculate generalized costs
 *
 * @author mrieser
 */
public class SurpriceTravelTimeAndDistanceBasedTravelDisutility implements TravelDisutility {

	protected final TravelTime timeCalculator;
	private final double marginalCostOfTime;
	private final double marginalCostOfDistance;
	
	private static int wrnCnt = 0 ;
	private String day;
	
	private final static Logger log = Logger.getLogger(SurpriceTravelTimeAndDistanceBasedTravelDisutility.class);

	public SurpriceTravelTimeAndDistanceBasedTravelDisutility(final TravelTime timeCalculator, PlanCalcScoreConfigGroup cnScoringGroup, String day) {
		this.day = day;
		this.timeCalculator = timeCalculator;
		/* Usually, the travel-utility should be negative (it's a disutility)
		 * but the cost should be positive. Thus negate the utility.
		 */
		this.marginalCostOfTime = (- cnScoringGroup.getTraveling_utils_hr() / 3600.0) + (cnScoringGroup.getPerforming_utils_hr() / 3600.0);

//		this.marginalUtlOfDistance = cnScoringGroup.getMarginalUtlOfDistanceCar();
		this.marginalCostOfDistance = - cnScoringGroup.getMonetaryDistanceCostRateCar() * cnScoringGroup.getMarginalUtilityOfMoney() ;
		if ( wrnCnt < 1 ) {
			wrnCnt++ ;
			if ( cnScoringGroup.getMonetaryDistanceCostRateCar() > 0. ) {
				log.warn("Monetary distance cost rate needs to be NEGATIVE to produce the normal" +
				"behavior; just found positive.  Continuing anyway.  This behavior may be changed in the future.") ;
			}
		}
		
	}

	@Override
	public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle vehicle) {
		double travelTime = this.timeCalculator.getLinkTravelTime(link, time, person, vehicle);		
		return this.marginalCostOfTime * travelTime + this.marginalCostOfDistance * link.getLength();
	}

	@Override
	public double getLinkMinimumTravelDisutility(final Link link) {		
		log.error("this one should not be used :( ");
		System.exit(99);

		return (link.getLength() / link.getFreespeed()) * this.marginalCostOfTime
		+ this.marginalCostOfDistance * link.getLength();
	}

}
