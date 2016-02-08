/* *********************************************************************** *
 * project: org.matsim.*
 * MSATollTravelDisutilityCalculator.java
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

package matsimConnector.congestionpricing;

import matsimConnector.utility.Constants;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutility;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

public class MSATollTravelDisutilityCalculator implements TravelDisutility {

	private static final Logger log = Logger.getLogger(MSATollTravelDisutilityCalculator.class);

	private final RandomizingTimeDistanceTravelDisutility delegate;
	private final TravelTime timeCalculator;
	private final double marginalUtlOfMoney;
	private final double distanceCostRateCar;
	private final double marginalUtlOfTravelTime;
	private final MSATollHandler tollHandler;
	
	public MSATollTravelDisutilityCalculator(TravelTime timeCalculator, PlanCalcScoreConfigGroup cnScoringGroup, MSATollHandler tollHandler) {
		this.timeCalculator = timeCalculator;
		this.marginalUtlOfMoney = cnScoringGroup.getMarginalUtilityOfMoney();
		this.distanceCostRateCar = cnScoringGroup.getOrCreateModeParams(Constants.CA_LINK_MODE).getMonetaryDistanceRate();
		this.marginalUtlOfTravelTime = (-cnScoringGroup.getModes().get(TransportMode.car).getMarginalUtilityOfTraveling() / 3600.0) + (cnScoringGroup.getPerforming_utils_hr() / 3600.0);
		this.tollHandler = tollHandler;
		final RandomizingTimeDistanceTravelDisutility.Builder builder = new RandomizingTimeDistanceTravelDisutility.Builder( TransportMode.car );
		this.delegate = builder.createTravelDisutility(timeCalculator, cnScoringGroup);
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

//		this.tollHandler.get
		
//		double blendedOldValue = (1 - this.blendFactor) * linkExpectedTollOldValue;
//		double blendedNewValue = this.blendFactor * linkExpectedTollNewValue;	
		
//		if (linkExpectedTollNewValue != 0 || linkExpectedTollOldValue != 0) {
//			log.info("-----------> Person " + person.getId() + ": Expected toll (new value) on link " + link.getId() + " at " + Time.writeTime(time, Time.TIMEFORMAT_HHMMSS) + ": " + linkExpectedTollNewValue);
//			log.info("-----------> Person " + person.getId() + ": Expected toll (old value) on link " + link.getId() + " at " + Time.writeTime(time, Time.TIMEFORMAT_HHMMSS) + ": " + linkExpectedTollOldValue);
//		
//			log.info("ExpectedToll: " + (blendedNewValue + blendedOldValue) );
//		}
				
		double linkExpectedTollDisutility = -1 * this.marginalUtlOfMoney * linkExpectedTollNewValue;			
		return linkExpectedTollDisutility;
	}

	@Override
	public double getLinkMinimumTravelDisutility(Link link) {
		return this.delegate.getLinkMinimumTravelDisutility(link);
	}

}
