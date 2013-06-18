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
package kticompatibility;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterFactory;
import org.matsim.core.router.TripRouterFactoryImpl;

import kticompatibility.KtiPtRoutingModule.KtiPtRoutingModuleInfo;

/**
 * @author thibautd
 */
public class KtiTripRouterFactory implements TripRouterFactory {
	final Controler controler;
	final KtiPtRoutingModuleInfo ptInfo;

	public KtiTripRouterFactory(final Controler controler) {
		this.controler = controler;
		this.ptInfo = new KtiPtRoutingModuleInfo(
				(NewKtiConfigGroup) controler.getScenario().getConfig().getModule( NewKtiConfigGroup.GROUP_NAME ),
				controler.getScenario().getNetwork() );
	}

	@Override
	public TripRouter instantiateAndConfigureTripRouter() {
		final TripRouterFactory delegate =
			new TripRouterFactoryImpl(
				controler.getScenario(),
				controler.getTravelDisutilityFactory(),
				controler.getLinkTravelTimes(),
				controler.getLeastCostPathCalculatorFactory(),
				controler.getTransitRouterFactory() );
		final TripRouter router = delegate.instantiateAndConfigureTripRouter();

		router.setRoutingModule(
				TransportMode.pt,
				new KtiPtRoutingModule(
					controler.getConfig().plansCalcRoute(),
					ptInfo,
					controler.getScenario().getNetwork()) );

		return router;
	}

}

