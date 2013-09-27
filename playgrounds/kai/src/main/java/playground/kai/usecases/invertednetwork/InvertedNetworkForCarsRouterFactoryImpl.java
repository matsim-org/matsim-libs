/* *********************************************************************** *
 * project: org.matsim.													   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,     *
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

/**
 * 
 */
package playground.kai.usecases.invertednetwork;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.router.DefaultTripRouterFactoryImpl;
import org.matsim.core.router.RoutingContext;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterFactory;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;

/**
 * @author thomas
 *
 */
public class InvertedNetworkForCarsRouterFactoryImpl implements TripRouterFactory {


	private TripRouterFactory delegate;

	private Scenario scenario;



	private TravelDisutilityFactory tdf;

//	private boolean firstCall = true;
	
	public InvertedNetworkForCarsRouterFactoryImpl(final Scenario scenario, TravelDisutilityFactory tdf ) {
		this.delegate = DefaultTripRouterFactoryImpl.createRichTripRouterFactoryImpl(scenario);
		this.scenario = scenario;
		this.tdf = tdf ;
	}
	
	@Override
	public TripRouter instantiateAndConfigureTripRouter(RoutingContext iterationContext) {
	
		TripRouter tripRouter = this.delegate.instantiateAndConfigureTripRouter(iterationContext);

		// add alternative module for car routing
		tripRouter.setRoutingModule(TransportMode.car, new InvertedRoutingModule(this.scenario,this.tdf) );
		
		return tripRouter;
	}
}
