/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.juliakern.distribution.withScoring;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.emissions.*;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutility.Builder;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import playground.benjamin.internalization.EmissionCostModule;


public class ResDisFactory implements TravelDisutilityFactory {
	
	private TravelDisutilityFactory tdf;
	private EmissionControlerListener ecl;
	private EmissionModule emissionModule;
	private EmissionCostModule emissionCostModule;
	private final PlanCalcScoreConfigGroup cnScoringGroup;
	
	public ResDisFactory(EmissionControlerListener ecl, EmissionModule emissionModule, 
			EmissionCostModule emissionCostModule, PlanCalcScoreConfigGroup cnScoringGroup){
		this.tdf  = new Builder( TransportMode.car, cnScoringGroup );
		this.ecl = ecl;
		this.emissionModule = emissionModule;
		this.emissionCostModule = emissionCostModule;
		this.cnScoringGroup = cnScoringGroup;
	}
	
	@Override
	public TravelDisutility createTravelDisutility( TravelTime timeCalculator) {
		double marginalutilityOfMoney = cnScoringGroup.getMarginalUtilityOfMoney();
		final ResDisCalculator resdiscal = new ResDisCalculator(tdf.createTravelDisutility(timeCalculator), ecl, marginalutilityOfMoney, this.emissionModule, this.emissionCostModule);
		
		return resdiscal;

	}

}
