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

/**
 * 
 */
package playground.ikaddoura.noise2.routing;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

import playground.ikaddoura.noise2.data.NoiseContext;

/**
 * @author ikaddoura
 *
 */
public class TollTravelDisutilityCalculator implements TravelDisutility{

	private static final Logger log = Logger.getLogger(TollTravelDisutilityCalculator.class);

	private TravelTime timeCalculator;
	private double marginalUtlOfMoney;
	private double distanceCostRateCar;
	private double marginalUtlOfTravelTime;
	private NoiseContext noiseContext;
	
	public TollTravelDisutilityCalculator(TravelTime timeCalculator, PlanCalcScoreConfigGroup cnScoringGroup, NoiseContext noiseContext) {
		this.timeCalculator = timeCalculator;
		this.marginalUtlOfMoney = cnScoringGroup.getMarginalUtilityOfMoney();
		this.distanceCostRateCar = cnScoringGroup.getMonetaryDistanceCostRateCar();
		this.marginalUtlOfTravelTime = (- cnScoringGroup.getTraveling_utils_hr() / 3600.0) + (cnScoringGroup.getPerforming_utils_hr() / 3600.0);
		this.noiseContext = noiseContext;		
	}

	@Override
	public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle v) {
		
		double linkTravelTime = this.timeCalculator.getLinkTravelTime(link, time, person, v);
		double linkTravelTimeDisutility = this.marginalUtlOfTravelTime * linkTravelTime ;

		double distance = link.getLength();
		double distanceCost = - this.distanceCostRateCar * distance;
		double linkDistanceDisutility = this.marginalUtlOfMoney * distanceCost;

		double linkExpectedTollDisutility = calculateExpectedTollDisutility(link, time, person.getId());
		
		double linkTravelDisutility = linkTravelTimeDisutility + linkDistanceDisutility + linkExpectedTollDisutility;

		return linkTravelDisutility;
	}

	private double calculateExpectedTollDisutility(Link link, double time, Id<Person> personId) {
	
		/* The following is an estimate of the tolls that an agent would have to pay if choosing that link in the next
		iteration i based on the tolls in iteration i-1 */
		
		double linkExpectedToll = 0.;
		double timeIntervalEndTime = ((int) (time / this.noiseContext.getNoiseParams().getTimeBinSizeNoiseComputation()) + 1) * this.noiseContext.getNoiseParams().getTimeBinSizeNoiseComputation();
		
		if (this.noiseContext.getTimeInterval2linkId2noiseLinks().get(timeIntervalEndTime) == null ||
				this.noiseContext.getTimeInterval2linkId2noiseLinks().get(timeIntervalEndTime).get(link.getId()) == null) {
	
		} else {

			if (personId.toString().startsWith(this.noiseContext.getNoiseParams().getHgvIdPrefix())) {
				linkExpectedToll = this.noiseContext.getTimeInterval2linkId2noiseLinks().get(timeIntervalEndTime).get(link.getId()).getDamageCostPerHgv();
			} else {
				linkExpectedToll = this.noiseContext.getTimeInterval2linkId2noiseLinks().get(timeIntervalEndTime).get(link.getId()).getDamageCostPerCar();
			}

		}

		log.warn("Expected toll on link " + link.getId() + " at time " + time + " in time interval " + timeIntervalEndTime + ": " + linkExpectedToll);
				
		double linkExpectedTollDisutility = -1 * this.marginalUtlOfMoney * linkExpectedToll;			
		return linkExpectedTollDisutility;
	}

	@Override
	public double getLinkMinimumTravelDisutility(Link link) {
		throw new UnsupportedOperationException();
	}

}
