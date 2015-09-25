/* *********************************************************************** *
 * project: org.matsim.*
 * TripRouterFactoryImpl.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package org.matsim.core.router;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.pt.router.TransitRouter;

import javax.inject.Provider;

/**
 * Default factory, which sets the routing modules according to the
 * config file.
 * @author thibautd
 */
public class TripRouterProviderImpl implements Provider<TripRouter> {
	private final TripRouterFactory delegate;
	private final RoutingContext context;


	public TripRouterProviderImpl(
            final Scenario scenario,
            final TravelDisutilityFactory disutilityFactory,
            final TravelTime travelTime,
            final LeastCostPathCalculatorFactory leastCostAlgoFactory,
            final Provider<TransitRouter> transitRouterFactory) {
		this.delegate = new DefaultTripRouterFactoryImpl(
				scenario,
				leastCostAlgoFactory,
				transitRouterFactory );
		
		this.context = new RoutingContextImpl(disutilityFactory.createTravelDisutility(travelTime, scenario.getConfig().planCalcScore()), travelTime);
	}


	@Override
	public TripRouter get() {
		return delegate.instantiateAndConfigureTripRouter( context );
	}
}

