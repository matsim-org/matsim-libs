/* *********************************************************************** *
 * project: org.matsim.*
 * AbstractPrepareForSimListener.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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
package playground.thibautd.socnetsim.controller;

import java.util.Collection;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scoring.ScoringFunctionFactory;

import playground.thibautd.router.PlanRoutingAlgorithmFactory;
import playground.thibautd.socnetsim.replanning.GenericPlanAlgorithm;
import playground.thibautd.socnetsim.replanning.grouping.GroupIdentifier;
import playground.thibautd.socnetsim.replanning.grouping.ReplanningGroup;
import playground.thibautd.socnetsim.replanning.modules.AbstractMultithreadedGenericStrategyModule;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * @author thibautd
 */
public abstract class AbstractPrepareForSimListener extends AbstractMultithreadedGenericStrategyModule<ReplanningGroup> implements StartupListener {
	@Inject GroupIdentifier links;
	@Inject Scenario sc;
	@Inject PlanRoutingAlgorithmFactory routingAlgoFactory;
	@Inject ScoringFunctionFactory scoringFunctionFactory;
	@Inject Provider<TripRouter> tripRouter;
	@Inject TravelDisutilityFactory travelDisutility;
	@Inject Provider<TravelTime> travelTime;

	public AbstractPrepareForSimListener( ) {
		super( 1 );
	}

	public AbstractPrepareForSimListener( final GlobalConfigGroup globalConfigGroup ) {
		super( globalConfigGroup );
	}

	@Override
	public void notifyStartup( StartupEvent event ) {

		final Collection<ReplanningGroup> groups = links.identifyGroups( sc.getPopulation() );

		// not nice, but replanningcontextimp wants iteration number...
		final ReplanningContext context = new ReplanningContext() {

			@Override
			public TravelDisutility getTravelDisutility() {
				return travelDisutility.createTravelDisutility( getTravelTime() , sc.getConfig().planCalcScore() );
			}

			@Override
			public TravelTime getTravelTime() {
				return travelTime.get();
			}

			@Override
			public TripRouter getTripRouter() {
				return tripRouter.get();
			}

			@Override
			public ScoringFunctionFactory getScoringFunctionFactory() {
				return scoringFunctionFactory;
			}

			@Override
			public int getIteration() {
				return 0;
			}
		};

		this.handlePlans( context , groups );
	}

	@Override
	public abstract GenericPlanAlgorithm<ReplanningGroup> createAlgorithm( ReplanningContext replanningContext );
}

