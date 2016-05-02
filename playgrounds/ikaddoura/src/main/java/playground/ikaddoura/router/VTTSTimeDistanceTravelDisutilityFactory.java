/* *********************************************************************** *
 * project: org.matsim.*
 * DefaultTravelCostCalculatorFactoryImpl
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
package playground.ikaddoura.router;

import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import playground.ikaddoura.analysis.vtts.VTTSHandler;


/**
 * @author ikaddoura
 *
 */
public final class VTTSTimeDistanceTravelDisutilityFactory implements TravelDisutilityFactory {

	private double sigma = 0. ;
	private VTTSHandler vttsHandler;
	private final PlanCalcScoreConfigGroup cnScoringGroup;
	
	public VTTSTimeDistanceTravelDisutilityFactory(VTTSHandler vttsHandler, PlanCalcScoreConfigGroup cnScoringGroup) {
		this.vttsHandler = vttsHandler ;
		this.cnScoringGroup = cnScoringGroup;
	}

	@Override
	public final TravelDisutility createTravelDisutility(TravelTime timeCalculator) {
		return new VTTSTimeDistanceTravelDisutility(timeCalculator, cnScoringGroup, this.sigma, vttsHandler);
	}
	
	public void setSigma ( double val ) {
		this.sigma = val;
	}

}
