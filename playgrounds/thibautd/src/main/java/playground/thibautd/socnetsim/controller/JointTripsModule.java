/* *********************************************************************** *
 * project: org.matsim.*
 * JointTripsAndJointVehiclesModule.java
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
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.population.algorithms.XY2Links;

import playground.thibautd.router.PlanRoutingAlgorithmFactory;
import playground.thibautd.socnetsim.replanning.GenericPlanAlgorithm;
import playground.thibautd.socnetsim.replanning.grouping.GroupIdentifier;
import playground.thibautd.socnetsim.replanning.grouping.ReplanningGroup;
import playground.thibautd.socnetsim.replanning.modules.AbstractMultithreadedGenericStrategyModule;
import playground.thibautd.socnetsim.router.JointPlanRouterFactory;
import playground.thibautd.socnetsim.utils.ImportedJointRoutesChecker;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Scopes;

/**
 * @author thibautd
 */
public class JointTripsModule extends AbstractModule {
	// TODO: move joint trip specific strategies in there?

	@Override
	public void install() {
		bind( PlanRoutingAlgorithmFactory.class ).to( JointPlanRouterFactory.class ).in( Scopes.SINGLETON );

		// TODO: extract in files (messy and not modular)
		addControlerListenerBinding().toInstance(
			new StartupListener() {
				@Inject GroupIdentifier links;
				@Inject Scenario sc;
				@Inject PlanRoutingAlgorithmFactory routingAlgoFactory;
				@Inject ScoringFunctionFactory scoringFunctionFactory;
				@Inject Provider<TripRouter> tripRouter;
				@Inject TravelDisutilityFactory travelDisutility;
				@Inject Provider<TravelTime> travelTime;

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

					new AbstractMultithreadedGenericStrategyModule<ReplanningGroup>( sc.getConfig().global()) {
						@Override
						public GenericPlanAlgorithm<ReplanningGroup> createAlgorithm(ReplanningContext replanningContext) {
							final PlanAlgorithm routingAlgorithm =
										routingAlgoFactory.createPlanRoutingAlgorithm(
											replanningContext.getTripRouter() );
							final PlanAlgorithm checkJointRoutes =
								new ImportedJointRoutesChecker( replanningContext.getTripRouter() );
							final PlanAlgorithm xy2link = new XY2Links( sc.getNetwork() );
							return new GenericPlanAlgorithm<ReplanningGroup>() {
								@Override
								public void run(final ReplanningGroup group) {
									for ( Person person : group.getPersons() ) {
										for ( Plan plan : person.getPlans() ) {
											xy2link.run( plan );
											checkJointRoutes.run( plan );
											if ( hasLegsWithoutRoutes( plan ) ) {
												routingAlgorithm.run( plan );
											}
										}
									}
								}

								private boolean hasLegsWithoutRoutes(final Plan plan) {
									for ( PlanElement pe : plan.getPlanElements() ) {
										if ( pe instanceof Leg && ((Leg) pe).getRoute() == null ) {
											return true;
										}
									}
									return false;
								}
							};
						}
					}.handlePlans( context , groups );
				}
			} );
	}
}

