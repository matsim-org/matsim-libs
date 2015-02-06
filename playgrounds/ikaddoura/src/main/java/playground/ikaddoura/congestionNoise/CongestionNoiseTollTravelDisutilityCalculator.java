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
package playground.ikaddoura.congestionNoise;

//import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
//import org.matsim.core.utils.misc.Time;
import org.matsim.vehicles.Vehicle;

import playground.ikaddoura.noise.NoiseTollHandler;
import playground.vsp.congestion.handlers.TollHandler;

/**
 * @author ikaddoura
 *
 */
public class CongestionNoiseTollTravelDisutilityCalculator implements TravelDisutility{

//	private static final Logger log = Logger.getLogger(CongestionNoiseTollTravelDisutilityCalculator.class);
	
	private TravelTime timeCalculator;
	private double marginalUtlOfMoney;
	private double distanceCostRateCar;
	private double marginalUtlOfTravelTime;
	private TollHandler tollHandler;
	private NoiseTollHandler noiseTollHandler;
	
	public CongestionNoiseTollTravelDisutilityCalculator(TravelTime timeCalculator, PlanCalcScoreConfigGroup cnScoringGroup, TollHandler tollHandler, NoiseTollHandler noiseTollHandler) {
		this.timeCalculator = timeCalculator;
		this.marginalUtlOfMoney = cnScoringGroup.getMarginalUtilityOfMoney();
		this.distanceCostRateCar = cnScoringGroup.getMonetaryDistanceCostRateCar();
		this.marginalUtlOfTravelTime = (- cnScoringGroup.getTraveling_utils_hr() / 3600.0) + (cnScoringGroup.getPerforming_utils_hr() / 3600.0);
		this.tollHandler = tollHandler;
		this.noiseTollHandler = noiseTollHandler;
		
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
		
		double linkExpectedTollNewValue = this.tollHandler.getAvgToll(link.getId(), time) + ( this.noiseTollHandler.getAvgToll(link.getId(), time) * (-1) );
//		log.info("congestion: " + this.tollHandler.getAvgToll(link.getId(), time));
//		log.info("noise: " + this.noiseTollHandler.getAvgToll(link.getId(), time));
		
//		if (linkExpectedTollNewValue != 0 || linkExpectedTollOldValue != 0) {
//			log.info("-----------> Person " + person.getId() + ": Expected toll (new value) on link " + link.getId() + " at " + Time.writeTime(time, Time.TIMEFORMAT_HHMMSS) + ": " + linkExpectedTollNewValue);
//		
//		}
				
		double linkExpectedTollDisutility = -1 * this.marginalUtlOfMoney * linkExpectedTollNewValue;			
		return linkExpectedTollDisutility;
	}

	@Override
	public double getLinkMinimumTravelDisutility(Link link) {
		throw new UnsupportedOperationException();
	}

}
