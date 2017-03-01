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
package playground.ikaddoura.integrationCNE;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Inject;

import playground.ikaddoura.moneyTravelDisutility.MoneyEventAnalysis;
import playground.ikaddoura.moneyTravelDisutility.MoneyTimeDistanceTravelDisutility;
import playground.ikaddoura.moneyTravelDisutility.data.AgentFilter;
import playground.ikaddoura.router.VTTSTimeDistanceTravelDisutilityFactory;


/**
 * 
 * A travel disutility which accounts for link-, time- and vehicle-specific monetary payments and additionally accounts for person- and trip-specific VTTS.
 * 
 * @author ikaddoura
 *
 */
public final class VTTSMoneyTimeDistanceTravelDisutilityFactory implements TravelDisutilityFactory {

	@Inject
	private MoneyEventAnalysis moneyAnalysis;
	
	@Inject
	private Scenario scenario;

	@Inject(optional = true)
	private AgentFilter vehicleFilter;
	
	private double sigma = 0. ;
	private VTTSTimeDistanceTravelDisutilityFactory vttsTimeDistanceTravelDisutilityFactory;

	public VTTSMoneyTimeDistanceTravelDisutilityFactory(VTTSTimeDistanceTravelDisutilityFactory vttsTimeDistanceTravelDisutilityFactory) {
		this.vttsTimeDistanceTravelDisutilityFactory = vttsTimeDistanceTravelDisutilityFactory;
	}

	@Override
	public final TravelDisutility createTravelDisutility(TravelTime timeCalculator) {
		
		vttsTimeDistanceTravelDisutilityFactory.setSigma(sigma);
		
		return new MoneyTimeDistanceTravelDisutility(
				vttsTimeDistanceTravelDisutilityFactory.createTravelDisutility(timeCalculator),
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
