/* *********************************************************************** *
 * project: org.matsim.*
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
package playground.ivt.replanning;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.modules.TimeAllocationMutator;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.router.CompositeStageActivityTypes;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.router.TripRouter;

/**
 * @author thibautd
 */
public class BlackListedTimeAllocationMutatorStrategyModule extends AbstractModule {
	@Override
	public void install() {
		addPlanStrategyBinding( "BlackListedTimeAllocationMutator" ).toProvider(
				new Provider<PlanStrategy>() {
					@Inject
					private TripRouter tripRouter;

					@Override
					public PlanStrategy get() {
						BlackListedTimeAllocationMutatorConfigGroup configGroup =
								ConfigUtils.addOrGetModule(
									getConfig(),
									BlackListedTimeAllocationMutatorConfigGroup.GROUP_NAME,
									BlackListedTimeAllocationMutatorConfigGroup.class );
						return new PlanStrategyImpl.Builder(
										new RandomPlanSelector<Plan, Person>() )
								.addStrategyModule(
										new BlackListedTimeAllocationMutatorModule(
												getConfig(),
												new CompositeStageActivityTypes(
														new StageActivityTypesImpl(
																configGroup.getBlackList() ),
														tripRouter.getStageActivityTypes() )) )
								.build();
					}
				} );
	}
}
