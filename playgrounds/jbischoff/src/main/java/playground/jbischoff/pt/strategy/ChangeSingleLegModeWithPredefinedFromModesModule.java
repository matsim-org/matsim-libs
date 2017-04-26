/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.jbischoff.pt.strategy;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl.Builder;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.modules.TripsToLegsModule;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.router.TripRouter;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class ChangeSingleLegModeWithPredefinedFromModesModule extends AbstractModule {

	/* (non-Javadoc)
	 * @see org.matsim.core.controler.AbstractModule#install()
	 */
	@Override
	public void install() {
		{
			addPlanStrategyBinding(DefaultPlanStrategiesModule.DefaultStrategy.ChangeSingleTripMode.toString())
					.toProvider(new Provider<PlanStrategy>() {
						@Inject
						Provider<TripRouter> tripRouterProvider;
						@Inject
						Scenario scenario;

						@Override
						public PlanStrategy get() {
							final Builder builder = new Builder(new RandomPlanSelector<Plan, Person>());
							builder.addStrategyModule(
									new TripsToLegsModule(tripRouterProvider, getConfig().global()));
							builder.addStrategyModule(new ChangeSingleLegModeWithPredefinedFromModes(
									getConfig().global(), getConfig().changeMode()));
							builder.addStrategyModule(new ReRoute(scenario, tripRouterProvider));
							return builder.build();
						}
					});

		}		
	}

}
