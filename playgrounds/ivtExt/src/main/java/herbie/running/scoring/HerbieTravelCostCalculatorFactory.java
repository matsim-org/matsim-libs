/* *********************************************************************** *
 * project: org.matsim.*
 * KtiTravelCostCalculatorFactory.java
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

package herbie.running.scoring;

import herbie.running.config.HerbieConfigGroup;

import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;

public class HerbieTravelCostCalculatorFactory implements TravelDisutilityFactory {
	
	private CharyparNagelScoringParameters params = null;
	private HerbieConfigGroup herbieConfigGroup = null;
	private final PlanCalcScoreConfigGroup cnScoringGroup;

	public HerbieTravelCostCalculatorFactory(CharyparNagelScoringParameters params, HerbieConfigGroup herbieConfigGroup,
			PlanCalcScoreConfigGroup cnScoringGroup) {
		super();
		this.params  = params;
		this.herbieConfigGroup = herbieConfigGroup;
		this.cnScoringGroup = cnScoringGroup;
	}

	@Override
	public TravelDisutility createTravelDisutility(TravelTime timeCalculator) {
		return new HerbieTravelTimeDistanceCostCalculator(timeCalculator, cnScoringGroup, params, herbieConfigGroup);
	}
}
