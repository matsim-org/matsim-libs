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

package playground.juliakern.distribution;

import com.google.inject.Inject;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutilityFactory;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import playground.juliakern.distribution.withScoring.EmissionControlerListener;
import playground.juliakern.distribution.withScoring.ResDisCalculator;
import playground.vsp.airPollution.flatEmissions.EmissionCostModule;



public class ResDisFactory implements TravelDisutilityFactory {
	
	private EmissionControlerListener ecl;

	@Inject private EmissionModule emissionModule;
	@Inject private  EmissionCostModule emissionCostModule;
	@Inject private PlanCalcScoreConfigGroup cnScoringGroup;

	
	public ResDisFactory(EmissionControlerListener ecl){
		this.ecl = ecl;
	}
	
	@Override
	public TravelDisutility createTravelDisutility( TravelTime timeCalculator) {
		double marginalutilityOfMoney = cnScoringGroup.getMarginalUtilityOfMoney();
		final ResDisCalculator resdiscal = new ResDisCalculator(new RandomizingTimeDistanceTravelDisutilityFactory( TransportMode.car, cnScoringGroup ).createTravelDisutility(timeCalculator), ecl, marginalutilityOfMoney, this.emissionModule, this.emissionCostModule);
		
		return resdiscal;

	}

}
