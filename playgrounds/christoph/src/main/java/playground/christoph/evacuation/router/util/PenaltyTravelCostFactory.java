/* *********************************************************************** *
 * project: org.matsim.*
 * PenaltyTravelCostFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.christoph.evacuation.router.util;

import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.router.costcalculators.TravelCostCalculatorFactory;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.PersonalizableTravelTime;

public class PenaltyTravelCostFactory implements TravelCostCalculatorFactory {

	private final TravelCostCalculatorFactory costFactory;
	private final AffectedAreaPenaltyCalculator penaltyCalculator;
	
	public PenaltyTravelCostFactory(TravelCostCalculatorFactory costFactory, AffectedAreaPenaltyCalculator penaltyCalculator) {
		this.costFactory = costFactory;
		this.penaltyCalculator = penaltyCalculator;
	}

	@Override
	public PersonalizableTravelCost createTravelCostCalculator(PersonalizableTravelTime travelTime, PlanCalcScoreConfigGroup cnScoringGroup) {
		PersonalizableTravelCost travelCost = this.costFactory.createTravelCostCalculator(travelTime, cnScoringGroup);
		return new PenaltyTravelCost(travelCost, penaltyCalculator.getPenaltyCalculatorInstance());
	}
	
}