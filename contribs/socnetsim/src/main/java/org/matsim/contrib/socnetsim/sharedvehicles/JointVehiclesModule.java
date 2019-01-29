/* *********************************************************************** *
 * project: org.matsim.*
 * JointVehiclesModule.java
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
package org.matsim.contrib.socnetsim.sharedvehicles;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.ReplanningContext;

import org.matsim.contrib.socnetsim.framework.PlanRoutingAlgorithmFactory;
import org.matsim.contrib.socnetsim.framework.controller.AbstractPrepareForSimListener;
import org.matsim.contrib.socnetsim.framework.population.JointPlans;
import org.matsim.contrib.socnetsim.framework.replanning.GenericPlanAlgorithm;
import org.matsim.contrib.socnetsim.framework.replanning.grouping.ReplanningGroup;
import org.matsim.contrib.socnetsim.framework.replanning.modules.PlanLinkIdentifier;
import org.matsim.contrib.socnetsim.framework.replanning.modules.PlanLinkIdentifier.Strong;
import org.matsim.contrib.socnetsim.jointtrips.router.JointPlanRouterFactory;
import org.matsim.contrib.socnetsim.framework.controller.ScenarioElementProvider;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * @author thibautd
 */
public class JointVehiclesModule extends AbstractModule {

	@Override
	public void install() {
//		if ( !scenario.getConfig().qsim().getVehicleBehavior().equals( "wait" ) ) {
//			throw new RuntimeException( "agents should wait for vehicles when vehicle ressources are used! Setting is "+
//					scenario.getConfig().qsim().getVehicleBehavior() );
//		}
		// For convenience
		bind( VehicleRessources.class ).toProvider( new ScenarioElementProvider<VehicleRessources>( VehicleRessources.ELEMENT_NAME ) );

		addControlerListenerBinding().toInstance(
			new AbstractPrepareForSimListener() {
				@Inject JointPlans jointPlans;
				@Inject VehicleRessources vehicles;
				@Inject @Strong PlanLinkIdentifier planlinks;

				@Override
				public GenericPlanAlgorithm<ReplanningGroup> createAlgorithm(ReplanningContext replanningContext) {
					return new PrepareVehicleAllocationForSimAlgorithm(
								MatsimRandom.getLocalInstance(),
								jointPlans,
								vehicles,
								planlinks );
				}

				@Override
				protected String getName() {
					return "PrepareVehiclesForSim";
				}

			} );

		addControlerListenerBinding().to( VehicleAllocationConsistencyChecker.class );

		bind( PlanRoutingAlgorithmFactory.class ).toProvider( 
				new Provider<PlanRoutingAlgorithmFactory>() {
					@Inject Scenario sc;

					@Override
					public PlanRoutingAlgorithmFactory get() {
						final PlanRoutingAlgorithmFactory jointRouterFactory =
									new JointPlanRouterFactory(
											sc.getActivityFacilities() );
						return new PlanRouterWithVehicleRessourcesFactory(
									jointRouterFactory );
					}
				} );

	}
}

