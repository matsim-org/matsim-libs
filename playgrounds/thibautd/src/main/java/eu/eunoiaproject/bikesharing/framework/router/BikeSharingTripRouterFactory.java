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
import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.DefaultTripRouterFactoryImpl;
import org.matsim.core.router.RoutingContext;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterFactory;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;
import org.matsim.facilities.Facility;

import playground.thibautd.router.multimodal.LinkSlopeScorer;
import eu.eunoiaproject.bikesharing.framework.BikeSharingConstants;
import eu.eunoiaproject.bikesharing.framework.scenario.BikeSharingConfigGroup;
import eu.eunoiaproject.bikesharing.framework.scenario.BikeSharingFacilities;

/**
 * Builds a standard trip router factory for bike sharing simulations.
 * @author thibautd
 */
public class BikeSharingTripRouterFactory implements TripRouterFactory {
	
	private boolean routePtUsingSchedule = false;

	private final TripRouterFactory delegate;
	private final Scenario scenario;
	private final LinkSlopeScorer slopeScorer;

	private final TransitMultiModalAccessRoutingModule.RoutingData data;

	public BikeSharingTripRouterFactory(
			final TransitMultiModalAccessRoutingModule.RoutingData routingData,
			final TripRouterFactory delegate,
			final Scenario scenario,
			final LinkSlopeScorer slopeScorer) {
		this.delegate = delegate;
		this.scenario = scenario;
		this.data = routingData;
		this.slopeScorer = slopeScorer;
	}

	public BikeSharingTripRouterFactory(
			final TripRouterFactory delegate,
			final Scenario scenario,
			final LinkSlopeScorer slopeScorer) {
		this( scenario.getConfig().transit().isUseTransit() ?
					new TransitMultiModalAccessRoutingModule.RoutingData( scenario ) :
					null,
			delegate,
			scenario,
			slopeScorer );
	}

	public BikeSharingTripRouterFactory(
			final TransitMultiModalAccessRoutingModule.RoutingData routingData,
			final Scenario scenario,
			final LinkSlopeScorer slopeScorer) {
		this( routingData,
				DefaultTripRouterFactoryImpl.createRichTripRouterFactoryImpl(scenario) ,
				scenario,
				slopeScorer );
	}

	public BikeSharingTripRouterFactory(
			final Scenario scenario,
			final LinkSlopeScorer slopeScorer) {
		this( DefaultTripRouterFactoryImpl.createRichTripRouterFactoryImpl(scenario) ,
				scenario,
				slopeScorer );
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

		if ( routePtUsingSchedule || scenario.getConfig().transit().isUseTransit() ) {
			// XXX should be person-dependent
			final CharyparNagelScoringParameters scoringParams =
					CharyparNagelScoringParameters.getBuilder(scenario.getConfig().planCalcScore()).create();
			final Collection<InitialNodeRouter> initialNodeRouters = new ArrayList<InitialNodeRouter>( 2 );
			initialNodeRouters.add( 
					new InitialNodeRouter(
							new RoutingModuleProxy(
								TransportMode.walk,
								router ),
							scenario.getConfig().transitRouter().getSearchRadius(),
							1,
							scoringParams ) );
			if ( contains( scenario.getConfig().subtourModeChoice().getModes() , BikeSharingConstants.MODE ) ) {
				initialNodeRouters.add(
						new InitialNodeRouter(
								new RoutingModuleProxy(
									BikeSharingConstants.MODE,
									router ),
								configGroup.getPtSearchRadius(),
								3, // there is randomness: keep the "best" of a few draws
								scoringParams ) {
							@Override
							protected double calcCost( final List<? extends PlanElement> trip ) {
								double baseCost = super.calcCost( trip );
								if ( slopeScorer == null ) return baseCost;

								for ( PlanElement pe : trip ) {
									if ( pe instanceof Leg && ((Leg) pe).getMode().equals( BikeSharingConstants.MODE ) ) {
										baseCost -= slopeScorer.calcGainUtil( (NetworkRoute) ((Leg) pe).getRoute() );
									}
								}

								return baseCost;
							}
						});
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

	public void setRoutePtUsingSchedule( boolean routePtUsingSchedule ) {
		this.routePtUsingSchedule = routePtUsingSchedule;
	}

	// not sure this really corresponds to the "proxy" pattern...
	// the idea it  to always get the last added routing module,
	// to avoid problems with "piped" factories.
	private static class RoutingModuleProxy implements RoutingModule {
		final String mode;
		final TripRouter router;

		public RoutingModuleProxy(
				final String mode,
				final TripRouter router ) {
			this.mode = mode;
			this.router = router;
		}

		@Override
		public List<? extends PlanElement> calcRoute(
				final Facility fromFacility,
				final Facility toFacility,
				final double departureTime,
				final Person person ) {
			return router.getRoutingModule( mode ).calcRoute(
					fromFacility,
					toFacility,
					departureTime,
					person );
		}

		@Override
		public StageActivityTypes getStageActivityTypes() {
			return router.getRoutingModule( mode ).getStageActivityTypes();
		}
	}
}
