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
package playground.ikaddoura.moneyTravelDisutility;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutilityFactory;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Inject;

import playground.ikaddoura.moneyTravelDisutility.data.AgentFilter;


/**
 * 
 * A travel disutility which accounts for link-, time- and vehicle-specific monetary payments.
 * 
 * @author ikaddoura
 *
 */
public final class MoneyTimeDistanceTravelDisutilityFactory implements TravelDisutilityFactory {

	@Inject
	private MoneyEventAnalysis moneyAnalysis;
	
	@Inject
	private Scenario scenario;

	@Inject
	private AgentFilter vehicleFilter;
	
	private double sigma = 0. ;
	private RandomizingTimeDistanceTravelDisutilityFactory randomizedTimeDistanceTravelDisutilityFactory;

	public MoneyTimeDistanceTravelDisutilityFactory(RandomizingTimeDistanceTravelDisutilityFactory randomizedTimeDistanceTravelDisutilityFactory) {
		this.randomizedTimeDistanceTravelDisutilityFactory = randomizedTimeDistanceTravelDisutilityFactory;
	}

	@Override
	public final TravelDisutility createTravelDisutility(TravelTime timeCalculator) {
		
		randomizedTimeDistanceTravelDisutilityFactory.setSigma(sigma);
		
		return new MoneyTimeDistanceTravelDisutility(
				randomizedTimeDistanceTravelDisutilityFactory.createTravelDisutility(timeCalculator),
				this.sigma,
				this.scenario,
				this.moneyAnalysis,
				this.vehicleFilter
			);
	}
	
	public void setSigma ( double val ) {
		this.sigma = val;
	}
	
}
