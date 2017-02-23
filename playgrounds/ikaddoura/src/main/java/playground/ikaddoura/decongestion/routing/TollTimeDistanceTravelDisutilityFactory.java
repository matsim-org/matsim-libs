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
package playground.ikaddoura.decongestion.routing;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Inject;

import playground.ikaddoura.decongestion.data.DecongestionInfo;


/**
 * @author ikaddoura
 *
 */
public final class TollTimeDistanceTravelDisutilityFactory implements TravelDisutilityFactory {
	private static final Logger log = Logger.getLogger(TollTimeDistanceTravelDisutilityFactory.class);

	private double sigma = 0. ;
	
	@Inject
	private Scenario scenario;
	
	@Inject
	private DecongestionInfo info;
		
	public TollTimeDistanceTravelDisutilityFactory() {
		log.info("Using the toll-adjusted travel disutility factory in the decongestion package.");
	}

	@Override
	public final TravelDisutility createTravelDisutility(TravelTime timeCalculator) {
		return new TollTimeDistanceTravelDisutility(timeCalculator, scenario.getConfig().planCalcScore(), this.sigma, info);
	}
	
	public void setSigma ( double val ) {
		this.sigma = val;
	}

}
