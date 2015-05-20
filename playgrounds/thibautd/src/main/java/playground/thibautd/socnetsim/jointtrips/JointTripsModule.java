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
package playground.thibautd.socnetsim.jointtrips;

import com.google.inject.Scopes;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.router.TripRouter;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.population.algorithms.XY2Links;
import playground.thibautd.socnetsim.framework.PlanRoutingAlgorithmFactory;
import playground.thibautd.socnetsim.framework.controller.AbstractPrepareForSimListener;
import playground.thibautd.socnetsim.framework.replanning.GenericPlanAlgorithm;
import playground.thibautd.socnetsim.framework.replanning.grouping.ReplanningGroup;
import playground.thibautd.socnetsim.jointtrips.qsim.JointQSimFactory;
import playground.thibautd.socnetsim.jointtrips.router.JointPlanRouterFactory;
import playground.thibautd.socnetsim.jointtrips.router.JointTripRouterFactory;
import playground.thibautd.socnetsim.jointtrips.scoring.CharyparNagelWithJointModesScoringFunctionFactory;

/**
 * @author thibautd
 */
public class JointTripsModule extends AbstractModule {
	// TODO: move joint trip specific strategies in there?

	@Override
	public void install() {
		bind( PlanRoutingAlgorithmFactory.class ).to( JointPlanRouterFactory.class ).in( Scopes.SINGLETON );
		bind(Mobsim.class).toProvider(JointQSimFactory.class);

		bind( ScoringFunctionFactory.class ).to(CharyparNagelWithJointModesScoringFunctionFactory.class);
		bind( TripRouter.class ).toProvider( JointTripRouterFactory.class );

		// TODO: extract in files (messy and not modular)
		addControlerListenerBinding().toInstance(
				new AbstractPrepareForSimListener() {
					@Override
					public GenericPlanAlgorithm<ReplanningGroup> createAlgorithm(final ReplanningContext replanningContext) {
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

					@Override
					protected String getName() {
						return "PrepareJointRoutesForSim";
					}
				});
	}
}

