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
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.router.DefaultTripRouterFactoryImpl;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.RoutingContext;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterFactory;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;

import eu.eunoiaproject.bikesharing.framework.BikeSharingConstants;
import eu.eunoiaproject.bikesharing.framework.router.TransitMultiModalAccessRoutingModule.InitialNodeRouter;
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
					router) );

		// XXX should be person-dependent
		final CharyparNagelScoringParameters scoringParams =
			new CharyparNagelScoringParameters(
				scenario.getConfig().planCalcScore() );
		router.setRoutingModule(
				TransportMode.pt,
				new TransitMultiModalAccessRoutingModule(
						scenario,
						new InitialNodeRouter(
							router.getRoutingModule( TransportMode.walk ),
							scenario.getConfig().transitRouter().getSearchRadius(),
							1,
							scoringParams ),
						new InitialNodeRouter(
							router.getRoutingModule( BikeSharingConstants.MODE ),
							configGroup.getPtSearchRadius(),
							3, // there is randomness: keep the "best" of a few draws
							scoringParams )
						) );

		final MainModeIdentifier defaultModeIdentifier = router.getMainModeIdentifier();
		router.setMainModeIdentifier(
				new MainModeIdentifier() {
					@Override
					public String identifyMainMode(
							final List<PlanElement> tripElements) {
						boolean hadBikeSharing = false;
						for ( PlanElement pe : tripElements ) {
							if ( pe instanceof Leg ) {
								final Leg l = (Leg) pe;
								if ( l.getMode().equals( BikeSharingConstants.MODE ) ) {
									hadBikeSharing = true;
								}
								if ( l.getMode().equals( TransportMode.transit_walk ) ) {
									return TransportMode.pt;
								}
							}
						}

						if ( hadBikeSharing ) {
							// there were bike sharing legs but no transit walk
							return BikeSharingConstants.MODE;
						}

						return defaultModeIdentifier.identifyMainMode( tripElements );
					}
				});

		return router;
	}
}
