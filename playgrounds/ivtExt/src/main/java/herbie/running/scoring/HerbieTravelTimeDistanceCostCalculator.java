/* *********************************************************************** *
 * project: org.matsim.*
 * KtiTravelTimeDistanceCostCalculator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package herbie.running.scoring;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.TravelMinCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scoring.CharyparNagelScoringParameters;

/*
 * 
 * WARNING: Do not use this class without adaptation to HERBIE!!!!!!
 * 
 */

public class HerbieTravelTimeDistanceCostCalculator implements TravelMinCost, PersonalizableTravelCost {

	private final static Logger log = Logger.getLogger(HerbieTravelTimeDistanceCostCalculator.class);
	protected final TravelTime timeCalculator;
//	private final double travelCostFactor;
	private final double marginalUtlOfDistance;
	private TravelScoringFunction travelScoring;

	public HerbieTravelTimeDistanceCostCalculator(
			TravelTime timeCalculator,
			PlanCalcScoreConfigGroup cnScoringGroup, // NOT delete, will be used for transit parameter...
			CharyparNagelScoringParameters params) {
		super();
		this.timeCalculator = timeCalculator;
		this.travelScoring = new TravelScoringFunction(params);
		
		// ?????????? Why getPerforming_utils_hr() ??
//		this.travelCostFactor = (- cnScoringGroup.getTraveling_utils_hr() / 3600.0) + (cnScoringGroup.getPerforming_utils_hr() / 3600.0);
		
		this.marginalUtlOfDistance = 0.0; // ????????? ktiConfigGroup.getDistanceCostCar()/1000.0 * cnScoringGroup.getMonetaryDistanceCostRateCar()
		  // * cnScoringGroup.getMarginalUtilityOfMoney() ; ???????????
		log.warn("this is the exact translation but I don't know what it means maybe check.  kai, dec'10") ;
		/*
		 * TODO: Adapt distance costs ...
		 *  
		 */
	}

	@Override
	public double getLinkMinimumTravelCost(Link link) {
		
		double travelTime = link.getLength() / link.getFreespeed();
		double distance = link.getLength();
		return Math.abs(travelScoring.getCarScore(distance, travelTime));
		
//		return
//		(link.getLength() / link.getFreespeed()) * this.travelCostFactor - this.marginalUtlOfDistance * link.getLength();
	}

	@Override
	public double getLinkGeneralizedTravelCost(Link link, double time) {
		
		double travelTime = this.timeCalculator.getLinkTravelTime(link, time);
		double distance = link.getLength();
		return Math.abs(travelScoring.getCarScore(distance, travelTime));
		
//		return travelTime * this.travelCostFactor - this.marginalUtlOfDistance * link.getLength();
	}

//	protected double getTravelCostFactor() {
//		double travelCostFactor = 0.0;
//		return travelCostFactor;
//	}
//
//	protected double getMarginalUtlOfDistance() {
//		double marginalUtlOfDistance = 0.0;
//		return marginalUtlOfDistance;
//	}

	@Override
	public void setPerson(Person person) {
	}
}
