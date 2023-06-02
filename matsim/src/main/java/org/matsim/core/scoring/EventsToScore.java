/* *********************************************************************** *
 * project: org.matsim.*
 * EventsToScore.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007, 2008 by the members listed in the COPYING,  *
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

package org.matsim.core.scoring;


import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.ControlerListenerManager;
import org.matsim.core.controler.ControlerListenerManagerImpl;
import org.matsim.core.controler.Injector;
import org.matsim.core.events.EventsManagerModule;
import org.matsim.core.scenario.ScenarioByInstanceModule;

import jakarta.inject.Inject;


/**
 * Calculates the score of the selected plans of a given scenario
 * based on events. The final scores are written to the selected plans of each person in the
 * scenario when you call finish on this instance if you created this instance with the corresponding
 * factory method.
 *
 * The Controler does not use this class, but rather uses its delegates directly.
 * Create your own instance if you want to compute scores from an Event file, for example.
 *
 * @author mrieser, michaz
 */
public final class EventsToScore {

	private final NewScoreAssigner newScoreAssigner;
	private final ControlerListenerManagerImpl controlerListenerManager;
	private ScoringFunctionsForPopulation scoringFunctionsForPopulation;
	private final Population population;

	private boolean finished = false;

	private int iteration = -1 ;
	private boolean isLastIteration = false;

	@Inject
	private EventsToScore(ControlerListenerManagerImpl controlerListenerManager, ScoringFunctionsForPopulation scoringFunctionsForPopulation, final Scenario scenario, NewScoreAssigner newScoreAssigner) {
		this.controlerListenerManager = controlerListenerManager;
		this.scoringFunctionsForPopulation = scoringFunctionsForPopulation;
		this.population = scenario.getPopulation();
		this.newScoreAssigner = newScoreAssigner;
	}

	public static EventsToScore createWithScoreUpdating(final Scenario scenario, final ScoringFunctionFactory scoringFunctionFactory, final EventsManager eventsManager) {
		com.google.inject.Injector injector = Injector.createInjector(scenario.getConfig(),
				new ScenarioByInstanceModule(scenario),
				new StandaloneExperiencedPlansModule(false),
				new AbstractModule() {
					@Override
					public void install() {
						bind(ScoringFunctionsForPopulation.class).asEagerSingleton();
						bind(ScoringFunctionFactory.class).toInstance(scoringFunctionFactory);
						bind(NewScoreAssigner.class).to(NewScoreAssignerImpl.class).asEagerSingleton();
						bind(EventsToScore.class).asEagerSingleton();
						bind(ControlerListenerManagerImpl.class).asEagerSingleton();
						bind(ControlerListenerManager.class).to(ControlerListenerManagerImpl.class);
						bind(EventsManager.class).toInstance(eventsManager);
						bind(EventsManagerModule.EventHandlerRegistrator.class).asEagerSingleton();
					}
				});
		return injector.getInstance(EventsToScore.class);
	}

	public static EventsToScore createWithoutScoreUpdating(Scenario scenario, final ScoringFunctionFactory scoringFunctionFactory, final EventsManager eventsManager) {
		com.google.inject.Injector injector = Injector.createInjector(scenario.getConfig(),
				new ScenarioByInstanceModule(scenario),
				new StandaloneExperiencedPlansModule(false),
				new AbstractModule() {
					@Override
					public void install() {
						bind(ScoringFunctionsForPopulation.class).asEagerSingleton();
						bind(ScoringFunctionFactory.class).toInstance(scoringFunctionFactory);
						bind(NewScoreAssigner.class).to(NoopNewScoreAssignerImpl.class).asEagerSingleton();
						bind(EventsToScore.class).asEagerSingleton();
						bind(ControlerListenerManagerImpl.class).asEagerSingleton();
						bind(ControlerListenerManager.class).to(ControlerListenerManagerImpl.class);
						bind(EventsManager.class).toInstance(eventsManager);
						bind(EventsManagerModule.EventHandlerRegistrator.class).asEagerSingleton();
					}
				});
		return injector.getInstance(EventsToScore.class);
	}

	public void beginIteration(int iteration, boolean isLastIteration) {
		this.iteration = iteration;
		this.isLastIteration = isLastIteration;
		this.controlerListenerManager.fireControlerIterationStartsEvent(iteration, isLastIteration);
	}

	/**
	 * Finishes the calculation of the plans' scores and assigns the new scores
	 * to the plans if desired.
	 */
	public void finish() {
		if (iteration == -1) {
			throw new RuntimeException("Please initialize me before the iteration starts.");
		}
		controlerListenerManager.fireControlerAfterMobsimEvent(iteration, isLastIteration);
		scoringFunctionsForPopulation.finishScoringFunctions();
		newScoreAssigner.assignNewScores(this.iteration, scoringFunctionsForPopulation, population);
		finished = true;
	}

	/**
	 * Returns the score of a single agent. This method only returns useful
	 * values if the method {@link #finish() } was called before. description
	 *
	 * @param agentId
	 *            The id of the agent the score is requested for.
	 * @return The score of the specified agent.
	 */
	public Double getAgentScore(final Id<Person> agentId) {
		if (!finished) {
			throw new IllegalStateException("Must call finish first.");
		}
		ScoringFunction scoringFunction = scoringFunctionsForPopulation.getScoringFunctionForAgent(agentId);
		if (scoringFunction == null)
			return null;
		return scoringFunction.getScore();
	}

	private static class NoopNewScoreAssignerImpl implements NewScoreAssigner {
		@Override
		public void assignNewScores(int iteration, ScoringFunctionsForPopulation scoringFunctionsForPopulation, Population population) {

		}
	}
}
