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

package playground.meisterk.kti.router;

import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import playground.meisterk.kti.config.KtiConfigGroup;

public class KtiTravelCostCalculatorFactory implements TravelDisutilityFactory {

	private KtiConfigGroup ktiConfigGroup = null;
	private final PlanCalcScoreConfigGroup cnScoringGroup;
	
	public KtiTravelCostCalculatorFactory(KtiConfigGroup ktiConfigGroup, PlanCalcScoreConfigGroup cnScoringGroup) {
		super();
		this.ktiConfigGroup = ktiConfigGroup;
		this.cnScoringGroup = cnScoringGroup;
	}

	@Override
	public TravelDisutility createTravelDisutility(TravelTime timeCalculator) {
		return new KtiTravelTimeDistanceCostCalculator(timeCalculator, cnScoringGroup, ktiConfigGroup);
	}

}
