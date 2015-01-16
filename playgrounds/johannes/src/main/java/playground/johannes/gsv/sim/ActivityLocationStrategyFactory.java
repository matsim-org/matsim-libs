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
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
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

	private final Controler controler;

	private final double mutationError;

	public ActivityLocationStrategyFactory(Random random, int numThreads, String blacklist, Controler controler, double mutationError) {
		this.random = random;
		this.numThreads = numThreads;
		this.blacklist = blacklist;
		this.controler = controler;
		this.mutationError = mutationError;
	}

	@Override
	public PlanStrategy createPlanStrategy(Scenario scenario, EventsManager eventsManager) {
		if (strategy == null) {
			strategy = new Strategy(new ActivityLocationStrategy(scenario.getActivityFacilities(), random, numThreads, blacklist, mutationError));
			controler.addControlerListener(strategy);
		}
		return strategy;
	}

	private class Strategy implements PlanStrategy, IterationStartsListener {

		private GenericPlanStrategy<Plan, Person> delegate;

		private int iteration;

		public Strategy(GenericPlanStrategy<Plan, Person> delegate) {
			this.delegate = delegate;
		}

		@Override
		public void run(HasPlansAndId<Plan, Person> person) {
			if (iteration >= 5) { // because of cadyts
				delegate.run(person);
			}

		}

		@Override
		public void init(ReplanningContext replanningContext) {
			delegate.init(replanningContext);
		}

		@Override
		public void finish() {
			delegate.finish();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.matsim.core.controler.listener.IterationStartsListener#
		 * notifyIterationStarts
		 * (org.matsim.core.controler.events.IterationStartsEvent)
		 */
		@Override
		public void notifyIterationStarts(IterationStartsEvent event) {
			iteration = event.getIteration();

		}

	}
}
