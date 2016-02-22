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

import eu.eunoiaproject.bikesharing.framework.BikeSharingConstants;
import eu.eunoiaproject.bikesharing.framework.scenario.BikeSharingConfigGroup;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.MainModeIdentifierImpl;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;
import playground.thibautd.router.multimodal.LinkSlopeScorer;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Builds a standard trip router factory for bike sharing simulations.
 * @author thibautd
 */
public class BikeSharingTripRouterModule extends AbstractModule {
	
	private boolean routePtUsingSchedule = false;

	private final Scenario scenario;
	private final LinkSlopeScorer slopeScorer;

	private final TransitMultiModalAccessRoutingModule.RoutingData data;

	public BikeSharingTripRouterModule(
			final TransitMultiModalAccessRoutingModule.RoutingData routingData,
			final Scenario scenario,
			final LinkSlopeScorer slopeScorer) {
		this.scenario = scenario;
		this.data = routingData;
		this.slopeScorer = slopeScorer;
	}

	public BikeSharingTripRouterModule(
			final Scenario scenario,
			final LinkSlopeScorer slopeScorer) {
		this( scenario.getConfig().transit().isUseTransit() ?
					new TransitMultiModalAccessRoutingModule.RoutingData( scenario ) :
					null,
			scenario,
			slopeScorer );
	}

	@Override
	public void install() {
		addRoutingModuleBinding( BikeSharingConstants.MODE ).to(BikeSharingRoutingModule.class);

		if ( routePtUsingSchedule || scenario.getConfig().transit().isUseTransit() ) {
			addRoutingModuleBinding( TransportMode.pt ).toProvider( InitialNodePtProvider.class );
		}

		bind( MainModeIdentifier.class ).toInstance(
				new MainModeIdentifierForMultiModalAccessPt(
					new BikeSharingModeIdentifier(
						 new MainModeIdentifierImpl() ) ) );
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

	private class InitialNodePtProvider implements Provider<RoutingModule> {
		private final RoutingModule bsRouting;

		@Inject
		private InitialNodePtProvider(RoutingModule bsRouting) {
			this.bsRouting = bsRouting;
		}

		@Override
		public RoutingModule get() {
			// XXX should be person-dependent
			final CharyparNagelScoringParameters scoringParams =
					new CharyparNagelScoringParameters.Builder(scenario.getConfig().planCalcScore(), scenario.getConfig().planCalcScore().getScoringParameters(null), scenario.getConfig().scenario()).build();
			final Collection<InitialNodeRouter> initialNodeRouters = new ArrayList<InitialNodeRouter>( 2 );
			initialNodeRouters.add(
					new InitialNodeRouter(
							bsRouting,
							scenario.getConfig().transitRouter().getSearchRadius(),
							1,
							scoringParams ) );
			if ( contains( scenario.getConfig().subtourModeChoice().getModes() , BikeSharingConstants.MODE ) ) {
				initialNodeRouters.add(
						new InitialNodeRouter(
								bsRouting,
								( (BikeSharingConfigGroup) scenario.getConfig().getModule( BikeSharingConfigGroup.GROUP_NAME ) ).getPtSearchRadius(),
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

			return	new TransitMultiModalAccessRoutingModule(
							0.75,
							data,
							initialNodeRouters );
		}
	}
}
