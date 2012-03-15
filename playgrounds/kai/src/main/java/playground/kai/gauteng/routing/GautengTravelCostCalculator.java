/* *********************************************************************** *
 * project: org.matsim.*
 * BKickIncomeTravelTimeDistanceCostCalculator
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.kai.gauteng.routing;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.households.Household;
import org.matsim.households.Income;
import org.matsim.households.PersonHouseholdMapping;
import org.matsim.households.Income.IncomePeriod;


/**
 * @author bkick after dgrether
 *
 */
public class GautengTravelCostCalculator implements PersonalizableTravelCost {
		
	private double distanceCostFactor;
	private double betaTravelTime;
	
	protected TravelTime timeCalculator;
	
	public GautengTravelCostCalculator(final TravelTime timeCalculator, PlanCalcScoreConfigGroup charyparNagelScoring) {
		this.timeCalculator = timeCalculator;
		
		/* Usually, utility from traveling should be negative (it's a disutility)
		 * but the cost should be positive. Thus negate the utility.
		 * Distance dependent routing is only impelemted for car since pt is only pseudo transit*/

//		this.distanceCostFactor = - charyparNagelScoring.getMarginalUtlOfDistanceCar();
		this.distanceCostFactor = - charyparNagelScoring.getMonetaryDistanceCostRateCar() * charyparNagelScoring.getMarginalUtilityOfMoney() ;

		//also opportunity costs of time have to be considered at this point (second summand)!
		this.betaTravelTime = (- charyparNagelScoring.getTraveling_utils_hr() / 3600.0) + (charyparNagelScoring.getPerforming_utils_hr() / 3600.0);
	}

	//calculate generalized travel costs
	public double getLinkGeneralizedTravelCost(Link link, double time) {
		double betaCost = 1. ;
		double distance   = link.getLength();
		double distanceCost = this.distanceCostFactor * distance;
		
		double travelTime = this.timeCalculator.getLinkTravelTime(link, time);
		
		double generalizedDistanceCost   = betaCost * distanceCost;
		double generalizedTravelTimeCost = this.betaTravelTime * travelTime;
		
			if (this.distanceCostFactor == 0.0) {
				return generalizedTravelTimeCost;
			}
		
		double generalizedTravelCost = generalizedDistanceCost + generalizedTravelTimeCost;
		return generalizedTravelCost;
	}
	
	@Override
	public void setPerson(Person person) {
	}

}
