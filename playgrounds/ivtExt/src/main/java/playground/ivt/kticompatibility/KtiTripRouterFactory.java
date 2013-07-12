/* *********************************************************************** *
 * project: org.matsim.*
 * KtiTripRouterFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.ivt.kticompatibility;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.router.DefaultTripRouterFactoryImpl;
import org.matsim.core.router.TripRouterFactory;
import org.matsim.core.router.RoutingContext;
import org.matsim.core.router.TripRouter;

import playground.ivt.kticompatibility.KtiPtRoutingModule.KtiPtRoutingModuleInfo;

/**
 * @author thibautd
 */
public class KtiTripRouterFactory implements TripRouterFactory {
	final KtiPtRoutingModuleInfo ptInfo;
	private TripRouterFactory delegate;
	private Scenario scenario;

	public KtiTripRouterFactory(final Scenario scenario) {
		this.ptInfo = new KtiPtRoutingModuleInfo(
				(KtiPtConfigGroup) scenario.getConfig().getModule( KtiPtConfigGroup.GROUP_NAME ),
				scenario.getNetwork() );
		this.delegate =
				DefaultTripRouterFactoryImpl
						.createRichTripRouterFactoryImpl(scenario);
		this.scenario = scenario;
	}

	@Override
	public TripRouter instantiateAndConfigureTripRouter(RoutingContext iterationContext) {

		final TripRouter router = delegate.instantiateAndConfigureTripRouter(iterationContext);

		router.setRoutingModule(
				TransportMode.pt,
				new KtiPtRoutingModule(
					scenario.getConfig().plansCalcRoute(),
					ptInfo,
					scenario.getNetwork()) );

		return router;
	}

}

