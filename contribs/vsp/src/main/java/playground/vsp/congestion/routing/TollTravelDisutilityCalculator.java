/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.vsp.congestion.routing;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

import playground.vsp.congestion.handlers.TollHandler;

/**
 * @author ikaddoura
 *
 */
public class TollTravelDisutilityCalculator implements TravelDisutility{

	private static final Logger log = Logger.getLogger(TollTravelDisutilityCalculator.class);

	/*
	 * Blur the Social Cost to speed up the relaxation process. Values between
	 * 0.0 and 1.0 are valid. 0.0 means the old value will be kept, 1.0 means
	 * the old value will be totally overwritten.
	 */
	private final double blendFactor = 0.1;
	
	private TravelTime timeCalculator;
	private double marginalUtlOfMoney;
	private double distanceCostRateCar;
	private double marginalUtlOfTravelTime;
	private TollHandler tollHandler;
	
	@Deprecated
	public TollTravelDisutilityCalculator(TravelTime timeCalculator, PlanCalcScoreConfigGroup cnScoringGroup, TollHandler tollHandler) {
		this.timeCalculator = timeCalculator;
		this.marginalUtlOfMoney = cnScoringGroup.getMarginalUtilityOfMoney();
		this.distanceCostRateCar = cnScoringGroup.getModes().get(TransportMode.car).getMonetaryDistanceRate();
		// ignores cnScoringGroup.getModes().get(TransportMode.car).getMarginalUtilityOfDistance();
		this.marginalUtlOfTravelTime = (-cnScoringGroup.getModes().get(TransportMode.car).getMarginalUtilityOfTraveling() / 3600.0) + (cnScoringGroup.getPerforming_utils_hr() / 3600.0);
		this.tollHandler = tollHandler;
		
		log.info("The 'blend factor' which is used for the calculation of the expected tolls in the next iteration is set to " + this.blendFactor);
	}

	@Override
	public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle v) {
		
		double linkTravelTime = this.timeCalculator.getLinkTravelTime(link, time, person, v);
		double linkTravelTimeDisutility = this.marginalUtlOfTravelTime * linkTravelTime ;

		double distance = link.getLength();
		double distanceCost = - this.distanceCostRateCar * distance;
		double linkDistanceDisutility = this.marginalUtlOfMoney * distanceCost;

		double linkExpectedTollDisutility = calculateExpectedTollDisutility(link, time, person);
		
		double linkTravelDisutility = linkTravelTimeDisutility + linkDistanceDisutility + linkExpectedTollDisutility;

		return linkTravelDisutility;
	}

	private double calculateExpectedTollDisutility(Link link, double time, Person person) {

		/* The following is an estimate of the tolls that an agent would have to pay if choosing that link in the next
		iteration i based on the tolls in iteration i-1 and i-2 */
		
		double linkExpectedTollNewValue = this.tollHandler.getAvgToll(link.getId(), time);
		double linkExpectedTollOldValue = this.tollHandler.getAvgTollOldValue(link.getId(), time);

		double blendedOldValue = (1 - blendFactor) * linkExpectedTollOldValue;
		double blendedNewValue = blendFactor * linkExpectedTollNewValue;	
		
//		if (linkExpectedTollNewValue != 0 || linkExpectedTollOldValue != 0) {
//			log.info("-----------> Person " + person.getId() + ": Expected toll (new value) on link " + link.getId() + " at " + Time.writeTime(time, Time.TIMEFORMAT_HHMMSS) + ": " + linkExpectedTollNewValue);
//			log.info("-----------> Person " + person.getId() + ": Expected toll (old value) on link " + link.getId() + " at " + Time.writeTime(time, Time.TIMEFORMAT_HHMMSS) + ": " + linkExpectedTollOldValue);
//		
//			log.info("ExpectedToll: " + (blendedNewValue + blendedOldValue) );
//		}
				
		double linkExpectedTollDisutility = -1 * this.marginalUtlOfMoney * (blendedOldValue + blendedNewValue);			
		return linkExpectedTollDisutility;
	}

	@Override
	public double getLinkMinimumTravelDisutility(Link link) {
		throw new UnsupportedOperationException();
	}

}
