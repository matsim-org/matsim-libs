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
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterFactoryBuilderWithDefaults;
import playground.ivt.kticompatibility.KtiPtRoutingModule.KtiPtRoutingModuleInfo;

import javax.inject.Provider;

/**
 * @author thibautd
 */
public class KtiTripRouterFactory implements Provider<TripRouter> {
	final KtiPtRoutingModuleInfo ptInfo;
	private Provider<TripRouter> delegate;
	private Scenario scenario;

	public KtiTripRouterFactory(final Scenario scenario) {
		this.ptInfo = new KtiPtRoutingModuleInfo(
				(KtiPtConfigGroup) scenario.getConfig().getModule( KtiPtConfigGroup.GROUP_NAME ),
				scenario.getNetwork() );
		this.delegate =
				TripRouterFactoryBuilderWithDefaults.createDefaultTripRouterFactoryImpl(scenario);
		this.scenario = scenario;
	}

	@Override
	public TripRouter get() {

		final TripRouter router = delegate.get();

		router.setRoutingModule(
				TransportMode.pt,
				new KtiPtRoutingModule(
					scenario.getConfig().plansCalcRoute(),
					ptInfo,
					scenario.getNetwork()) );

		return router;
	}

}

