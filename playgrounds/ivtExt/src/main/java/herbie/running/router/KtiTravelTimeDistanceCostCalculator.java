/* *********************************************************************** *
 * project: org.matsim.*
 * KtiTravelTimeDistanceCostCalculator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package herbie.running.router;

import herbie.running.config.KtiConfigGroup;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.TravelMinCost;
import org.matsim.core.router.util.TravelTime;

public class KtiTravelTimeDistanceCostCalculator implements TravelMinCost, PersonalizableTravelCost {

	private final static Logger log = Logger.getLogger(KtiTravelTimeDistanceCostCalculator.class);
	protected final TravelTime timeCalculator;
	private final double travelCostFactor;
	private final double marginalUtlOfDistance;

	public KtiTravelTimeDistanceCostCalculator(
			TravelTime timeCalculator,
			PlanCalcScoreConfigGroup cnScoringGroup,
			KtiConfigGroup ktiConfigGroup) {
		super();
		this.timeCalculator = timeCalculator;
		this.travelCostFactor = (- cnScoringGroup.getTraveling_utils_hr() / 3600.0) + (cnScoringGroup.getPerforming_utils_hr() / 3600.0);

//		this.marginalUtlOfDistance = ktiConfigGroup.getDistanceCostCar()/1000.0 * cnScoringGroup.getMarginalUtlOfDistanceCar();
		this.marginalUtlOfDistance = ktiConfigGroup.getDistanceCostCar()/1000.0 * cnScoringGroup.getMonetaryDistanceCostRateCar()
		   * cnScoringGroup.getMarginalUtilityOfMoney() ;
		log.warn("this is the exact translation but I don't know what it means maybe check.  kai, dec'10") ;
	}

	@Override
	public double getLinkMinimumTravelCost(Link link) {
		return
		(link.getLength() / link.getFreespeed()) * this.travelCostFactor
		- this.marginalUtlOfDistance * link.getLength();
	}

	@Override
	public double getLinkGeneralizedTravelCost(Link link, double time) {
		double travelTime = this.timeCalculator.getLinkTravelTime(link, time);
		return travelTime * this.travelCostFactor - this.marginalUtlOfDistance * link.getLength();
	}

	protected double getTravelCostFactor() {
		return travelCostFactor;
	}

	protected double getMarginalUtlOfDistance() {
		return marginalUtlOfDistance;
	}

	@Override
	public void setPerson(Person person) {
		// TODO Auto-generated method stub

	}



}
