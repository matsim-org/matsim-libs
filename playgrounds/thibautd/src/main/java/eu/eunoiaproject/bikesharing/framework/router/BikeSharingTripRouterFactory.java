/* *********************************************************************** *
 * project: org.matsim.*
 * BikeSharingTripRouterFactory.java
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
package eu.eunoiaproject.bikesharing.framework.router;

import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.router.DefaultTripRouterFactoryImpl;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.RoutingContext;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterFactory;

import eu.eunoiaproject.bikesharing.framework.BikeSharingConstants;
import eu.eunoiaproject.bikesharing.framework.scenario.BikeSharingConfigGroup;
import eu.eunoiaproject.bikesharing.framework.scenario.BikeSharingFacilities;

/**
 * Builds a standard trip router factory for bike sharing simulations.
 * @author thibautd
 */
public class BikeSharingTripRouterFactory implements TripRouterFactory {

	private final TripRouterFactory delegate;
	private final Scenario scenario;

	public BikeSharingTripRouterFactory(
			final TripRouterFactory delegate,
			final Scenario scenario ) {
		this.delegate = delegate;
		this.scenario = scenario;
	}

	public BikeSharingTripRouterFactory(
			final Scenario scenario ) {
		this( DefaultTripRouterFactoryImpl.createRichTripRouterFactoryImpl(scenario) ,
				scenario );
	}

	@Override
	public TripRouter instantiateAndConfigureTripRouter(final RoutingContext iterationContext) {
		final TripRouter router = delegate.instantiateAndConfigureTripRouter(iterationContext);

		final BikeSharingConfigGroup configGroup = (BikeSharingConfigGroup)
			scenario.getConfig().getModule( BikeSharingConfigGroup.GROUP_NAME );
		router.setRoutingModule(
				BikeSharingConstants.MODE,
				new BikeSharingRoutingModule(
					MatsimRandom.getLocalInstance(),
					(BikeSharingFacilities) scenario.getScenarioElement( BikeSharingFacilities.ELEMENT_NAME ),
					configGroup.getSearchRadius(),
					scenario.getConfig().plansCalcRoute()) );

		final MainModeIdentifier defaultModeIdentifier = router.getMainModeIdentifier();
		router.setMainModeIdentifier(
				new MainModeIdentifier() {
					@Override
					public String identifyMainMode(
							final List<PlanElement> tripElements) {
						for ( PlanElement pe : tripElements ) {
							if ( pe instanceof Leg &&
									((Leg) pe).getMode().equals( BikeSharingConstants.MODE ) ) {
								return BikeSharingConstants.MODE;
							}
						}
						return defaultModeIdentifier.identifyMainMode( tripElements );
					}
				});

		return router;
	}
}
