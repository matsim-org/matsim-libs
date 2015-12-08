/* *********************************************************************** *
 * project: org.matsim.*
 * ParkAndRideTimeAllocationMutator.java
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
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.router.StageActivityTypes;
import playground.thibautd.parknride.ParkAndRideConstants;
import playground.ivt.replanning.BlackListedTimeAllocationMutatorModule;

/**
 * @author thibautd
 */
public class ParkAndRideTimeAllocationMutator implements PlanStrategy {
	private final PlanStrategyImpl strategy;

	public ParkAndRideTimeAllocationMutator(final Controler controler) {
		strategy = new PlanStrategyImpl( new RandomPlanSelector() );

		addStrategyModule(
				new BlackListedTimeAllocationMutatorModule(
					controler.getConfig(),
					new BlackList( controler ) ) );

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

	private static class BlackList implements StageActivityTypes {
		private Controler controler = null;
		private StageActivityTypes blackList = null;

		public BlackList(final Controler controler) {
			this.controler = controler;
		}

		@Override
		public boolean isStageActivity(final String activityType) {
			if (blackList == null) {
				// we need this hack, because when initialising the strategy the router factory
				// may not be set correctly yet...
				blackList = controler.getTripRouterProvider().get().getStageActivityTypes();
				controler = null;
			}

			return blackList.isStageActivity( activityType ) || ParkAndRideConstants.PARKING_ACT.equals( activityType );
		}
	}
}

