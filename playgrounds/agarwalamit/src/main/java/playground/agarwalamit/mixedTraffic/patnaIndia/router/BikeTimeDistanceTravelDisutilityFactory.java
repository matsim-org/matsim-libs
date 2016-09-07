/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.agarwalamit.mixedTraffic.patnaIndia.router;

import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

/**
 * @author amit
 */

public class BikeTimeDistanceTravelDisutilityFactory  implements TravelDisutilityFactory{

	private final String mode;
	private final PlanCalcScoreConfigGroup cnScoringGroup;
	
	public BikeTimeDistanceTravelDisutilityFactory (final String mode, PlanCalcScoreConfigGroup cnScoringGroup){
		this.mode = mode;
		this.cnScoringGroup = cnScoringGroup;
	}
	
	@Override
	public TravelDisutility createTravelDisutility(TravelTime timeCalculator) {
		
		/* Usually, the travel-utility should be negative (it's a disutility) but the cost should be positive. Thus negate the utility.*/
		final PlanCalcScoreConfigGroup.ModeParams params = cnScoringGroup.getModes().get( mode ) ;
		if ( params == null ) {
			throw new NullPointerException( mode+" is not part of the valid mode parameters "+cnScoringGroup.getModes().keySet() );
		}
		final double marginalCostOfTime_s = (-params.getMarginalUtilityOfTraveling() / 3600.0) + (cnScoringGroup.getPerforming_utils_hr() / 3600.0);

		final double marginalCostOfDistance_m = -params.getMonetaryDistanceRate() * cnScoringGroup.getMarginalUtilityOfMoney() + (- params.getMarginalUtilityOfDistance()) ;

		return new BikeTimeDistanceTravelDisUtility(timeCalculator, marginalCostOfTime_s, marginalCostOfDistance_m);
	}
}
