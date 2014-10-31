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

import java.util.ArrayList;
import java.util.Collection;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.router.DefaultTripRouterFactoryImpl;
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

	private final TransitMultiModalAccessRoutingModule.RoutingData data;

	public BikeSharingTripRouterFactory(
			final TripRouterFactory delegate,
			final Scenario scenario ) {
		this.delegate = delegate;
		this.scenario = scenario;
		this.data = scenario.getConfig().scenario().isUseTransit() ?
					new TransitMultiModalAccessRoutingModule.RoutingData( scenario ) :
					null;
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

		if ( scenario.getConfig().scenario().isUseTransit() ) {
			// XXX should be person-dependent
			final CharyparNagelScoringParameters scoringParams =
					new CharyparNagelScoringParameters(
							scenario.getConfig().planCalcScore() );
			final Collection<InitialNodeRouter> initialNodeRouters = new ArrayList<InitialNodeRouter>( 2 );
			initialNodeRouters.add( 
					new InitialNodeRouter(
							router.getRoutingModule( TransportMode.walk ),
							scenario.getConfig().transitRouter().getSearchRadius(),
							1,
							scoringParams ) );
			if ( contains( scenario.getConfig().subtourModeChoice().getModes() , BikeSharingConstants.MODE ) ) {
				initialNodeRouters.add(
						new InitialNodeRouter(
								router.getRoutingModule( BikeSharingConstants.MODE ),
								configGroup.getPtSearchRadius(),
								3, // there is randomness: keep the "best" of a few draws
								scoringParams ) );
			}
			router.setRoutingModule(
					TransportMode.pt,
					new TransitMultiModalAccessRoutingModule(
							0.75,
							data,
							initialNodeRouters ) );
		}

		router.setMainModeIdentifier(
				new MainModeIdentifierForMultiModalAccessPt(
					new BikeSharingModeIdentifier(
						router.getMainModeIdentifier() )) );

		return router;
	}

	private boolean contains(
			final String[] modes,
			final String mode) {
		for ( String m : modes ) if ( mode.equals( m ) ) return true;
		return false;
	}
}
