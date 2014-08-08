/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.sim;

import java.util.Random;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.replanning.GenericPlanStrategy;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyFactory;
import org.matsim.core.replanning.ReplanningContext;

/**
 * @author johannes
 *
 */
public class ActivityLocationStrategyFactory implements PlanStrategyFactory {

	private Strategy strategy;
	
	private Random random;
	
	private String blacklist;
	
	private final int numThreads;
	
	public ActivityLocationStrategyFactory(Random random, int numThreads, String blacklist) {
		this.random = random;
		this.numThreads = numThreads;
		this.blacklist = blacklist;
	}

	@Override
	public PlanStrategy createPlanStrategy(Scenario scenario,	EventsManager eventsManager) {
		if(strategy == null) {
			strategy = new Strategy(new ActivityLocationStrategy(scenario.getActivityFacilities(), random, numThreads, blacklist));
		}
		return strategy;
	}

	private class Strategy implements PlanStrategy {

		private GenericPlanStrategy<Plan> delegate;
		
		public Strategy(GenericPlanStrategy<Plan> delegate) {
			this.delegate = delegate;
		}
		
		@Override
		public void run(HasPlansAndId<Plan> person) {
			delegate.run(person);
			
		}

		@Override
		public void init(ReplanningContext replanningContext) {
			delegate.init(replanningContext);
		}

		@Override
		public void finish() {
			delegate.finish();
		}
		
	}
}
