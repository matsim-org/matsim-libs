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

package playground.julia.distribution;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;
import playground.vsp.emissions.EmissionModule;


public class ResponsibilityTravelDisutilityCalculator implements TravelDisutility{
	private static final Logger logger = Logger.getLogger(ResponsibilityTravelDisutilityCalculator.class);
	
	TravelTime timeCalculator;
	double marginalUtlOfMoney;
	double distanceCostRateCar;
	double marginalUtlOfTravelTime;
	EmissionModule emissionModule;
	ResponsibilityCostModule responsibilityCostModule;
	
	public ResponsibilityTravelDisutilityCalculator(TravelTime timeCalculator, PlanCalcScoreConfigGroup cnScoringGroup, EmissionModule emissionModule, ResponsibilityCostModule responsibilityCostModule){
		this.timeCalculator = timeCalculator;
		this.marginalUtlOfMoney = cnScoringGroup.getMarginalUtilityOfMoney();
		this.distanceCostRateCar = cnScoringGroup.getMonetaryDistanceCostRateCar();
		this.marginalUtlOfTravelTime = (- cnScoringGroup.getTraveling_utils_hr() / 3600.0) + (cnScoringGroup.getPerforming_utils_hr() / 3600.0);
		this.emissionModule = emissionModule;
		this.responsibilityCostModule = responsibilityCostModule;
	}


	@Override
	public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle v) {
		double linkTravelDisutility;

		double linkTravelTime = this.timeCalculator.getLinkTravelTime(link, time, person, v);
		double linkTravelTimeDisutility = this.marginalUtlOfTravelTime * linkTravelTime ;

		double distance = link.getLength();
		double distanceCost = - this.distanceCostRateCar * distance;
		double linkDistanceDisutility = this.marginalUtlOfMoney * distanceCost;

		Double responsibilityDisutility = responsibilityCostModule.getDisutilityValue(person, v, link, time);
		linkTravelDisutility = linkTravelTimeDisutility + linkDistanceDisutility + responsibilityDisutility;

		return linkTravelDisutility;
	}


	@Override
	public double getLinkMinimumTravelDisutility(Link link) {
		throw new UnsupportedOperationException();
	}

	public double getMinimumTravelDisutility(Link link) {
		// TODO Auto-generated method stub
		return 0;
	}
}
