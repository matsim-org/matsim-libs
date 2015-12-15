/* *********************************************************************** *
 * project: org.matsim.*
 * ParkAndRideChangeLegModeStrategy.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.thibautd.parknride.replanning;

import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.modules.ChangeLegMode;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.modules.TripsToLegsModule;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.TripRouter;
import playground.thibautd.parknride.ParkAndRideConstants;

import javax.inject.Provider;


/**
 * Change trip modes for the whole plan. This has the side effect of removing
 * all park and ride trips.
 * @author thibautd
 */
public class ParkAndRideChangeLegModeStrategy implements PlanStrategy {
	private final PlanStrategyImpl strategy = new PlanStrategyImpl( new RandomPlanSelector() );

	public ParkAndRideChangeLegModeStrategy(final Controler controler, Provider<TripRouter> tripRouterProvider) {
		StageActivityTypes pnrList = ParkAndRideConstants.PARKING_ACT_TYPE;

		addStrategyModule( new TripsToLegsModule(pnrList, tripRouterProvider, controler.getConfig().global()) );
		addStrategyModule( new ChangeLegMode( controler.getConfig().global(), controler.getConfig().changeLegMode() ) );
		addStrategyModule( new ReRoute(controler.getScenario(), tripRouterProvider) );
		addStrategyModule( new ParkAndRideInvalidateStartTimes( controler ) );
	}

	public void addStrategyModule(final PlanStrategyModule module) {
		strategy.addStrategyModule(module);
	}

	public int getNumberOfStrategyModules() {
		return strategy.getNumberOfStrategyModules();
	}

	@Override
	public void run(final HasPlansAndId<Plan, Person> person) {
		strategy.run(person);
	}

	@Override
	public void init(ReplanningContext replanningContext) {
		strategy.init(replanningContext);
	}

	@Override
	public void finish() {
		strategy.finish();
	}

	@Override
	public String toString() {
		return strategy.toString();
	}

}

