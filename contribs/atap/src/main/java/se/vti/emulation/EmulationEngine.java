/**
 * se.vti.emulation
 * 
 * Copyright (C) 2023, 2024, 2025 by Gunnar Flötteröd (VTI, LiU).
 * Partially based on Sebastian Hörl's IER.
 * 
 * VTI = Swedish National Road and Transport Institute
 * LiU = Linköping University, Sweden
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation, either 
 * version 3 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>. See also COPYING and WARRANTY file.
 */
package se.vti.emulation;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.replanning.selectors.BestPlanSelector;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scoring.EventsToScore;
import org.matsim.core.scoring.ScoringFunctionFactory;

import com.google.inject.Inject;
import com.google.inject.Provider;

import se.vti.emulation.emulators.PlanEmulator;
import se.vti.emulation.handlers.EmulationHandler;

/**
 * Carved out of IERReplanning.
 * 
 * @author shoerl
 * @author Gunnar Flötteröd
 * 
 */
public class EmulationEngine {

	// -------------------- CONSTANTS --------------------

	private final static Logger logger = LogManager.getLogger(EmulationEngine.class);

	private final StrategyManager strategyManager;
	private final Scenario scenario;
	private final Provider<ReplanningContext> replanningContextProvider;
	private final Provider<PlanEmulator> planEmulatorProvider;
	private final EmulationConfigGroup ierConfig;
	private final Provider<Set<EmulationHandler>> emulationHandlerProvider;
	private final Provider<ScoringFunctionFactory> scoringFunctionFactoryProvider;

	// -------------------- CONSTRUCTION --------------------

	@Inject
	public EmulationEngine(StrategyManager strategyManager, Scenario scenario,
			Provider<ReplanningContext> replanningContextProvider, Provider<PlanEmulator> planEmulatorProvider,
			Config config, Provider<Set<EmulationHandler>> emulationHandlerProvider,
			Provider<ScoringFunctionFactory> scoringFunctionFactoryProvider) {
		this.strategyManager = strategyManager;
		this.scenario = scenario;
		this.replanningContextProvider = replanningContextProvider;
		this.planEmulatorProvider = planEmulatorProvider;
		this.ierConfig = ConfigUtils.addOrGetModule(config, EmulationConfigGroup.class);
		this.emulationHandlerProvider = emulationHandlerProvider;
		this.scoringFunctionFactoryProvider = scoringFunctionFactoryProvider;
	}

	// -------------------- STATIC UTILITIES --------------------

	public static void selectBestPlans(final Population population) {
		final BestPlanSelector<Plan, Person> bestPlanSelector = new BestPlanSelector<>();
		for (Person person : population.getPersons().values()) {
			person.setSelectedPlan(bestPlanSelector.selectPlan(person));
		}
	}

	public static void removeUnselectedPlans(final Population population) {
		for (Person person : population.getPersons().values()) {
			PersonUtils.removeUnselectedPlans(person);
		}
	}

	public static void ensureOnePlanPerPersonInScenario(final Scenario scenario, final boolean selectBestPlan) {
		if (selectBestPlan) {
			selectBestPlans(scenario.getPopulation());
		}
		removeUnselectedPlans(scenario.getPopulation());
	}

	// -------------------- IMPLEMENTATION --------------------

	public void replan(final int matsimIteration, final List<Map<String, ? extends TravelTime>> listOfMode2travelTime,
			final boolean overrideTravelTimesFromFirstListEntry) {
		final ReplanningContext replanningContext = this.replanningContextProvider.get();

		removeUnselectedPlans(this.scenario.getPopulation());
		this.emulate(matsimIteration, listOfMode2travelTime, overrideTravelTimesFromFirstListEntry);

		for (int i = 0; i < this.ierConfig.getIterationsPerCycle(); i++) {

			logger.info(
					String.format("Started replanning iteration %d/%d", i + 1, this.ierConfig.getIterationsPerCycle()));

			logger.info("[[Suppressing logging while running StrategyManager.]]");
			final Level originalLogLevel = LogManager.getRootLogger().getLevel();
			Configurator.setRootLevel(Level.ERROR);
			this.strategyManager.run(this.scenario.getPopulation(), matsimIteration, replanningContext);
			Configurator.setRootLevel(originalLogLevel);

			this.emulate(matsimIteration, listOfMode2travelTime, overrideTravelTimesFromFirstListEntry);
			selectBestPlans(this.scenario.getPopulation());
			removeUnselectedPlans(this.scenario.getPopulation());

			logger.info(String.format("Finished replanning iteration %d/%d", i + 1,
					this.ierConfig.getIterationsPerCycle()));
		}
	}

	public void emulate(int iteration, final List<Map<String, ? extends TravelTime>> listOfMode2travelTime,
			final boolean overrideTravelTimesFromFirstListEntry) {
		this.emulate(this.scenario.getPopulation().getPersons().values(), iteration, listOfMode2travelTime, null,
				overrideTravelTimesFromFirstListEntry);
	}

	public void emulate(Collection<? extends Person> persons, int iteration,
			final List<Map<String, ? extends TravelTime>> listOfMode2travelTime, final EventHandler eventsHandler,
			final boolean overrideTravelTimesFromFirstListEntry) {

		Iterator<? extends Person> personIterator = persons.iterator();
		List<Thread> threads = new LinkedList<>();

		long totalNumberOfPersons = persons.size();
		AtomicLong processedNumberOfPersons = new AtomicLong(0);
		AtomicBoolean finished = new AtomicBoolean(false);

		logger.info("[[Suppressing logging while emulating.]]");
		final Level originalLogLevel = LogManager.getRootLogger().getLevel();
		Configurator.setRootLevel(Level.ERROR);

		for (int i = 0; i < this.scenario.getConfig().global().getNumberOfThreads(); i++) {
			Thread thread = new Thread(() -> {

				final PlanEmulator planEmulator;
				synchronized (this.planEmulatorProvider) {
					planEmulator = this.planEmulatorProvider.get();
				}

				final Set<Person> personsToScore = new LinkedHashSet<>();
				Map<Id<Person>, Person> batch = new LinkedHashMap<>();

				do {
					batch.clear();

					synchronized (personIterator) {
						while (personIterator.hasNext() && batch.size() < this.ierConfig.getBatchSize()) {
							final Person person = personIterator.next();
							batch.put(person.getId(), person);
							personsToScore.add(person);
						}
					}

					Map<Person, Double> person2scoreSum = new LinkedHashMap<>(personsToScore.size());

					for (int travelTimeIndex = 0; travelTimeIndex < listOfMode2travelTime.size(); travelTimeIndex++) {
						Map<String, ? extends TravelTime> mode2travelTime = listOfMode2travelTime.get(travelTimeIndex);

						final EventsManager eventsManager = EventsUtils.createEventsManager();
						if (eventsHandler != null) {
							eventsManager.addHandler(eventsHandler);
						}
						eventsManager.initProcessing();

						final EventsToScore events2score;
						synchronized (this.scoringFunctionFactoryProvider) {
							events2score = EventsToScore.createWithoutScoreUpdating(this.scenario,
									this.scoringFunctionFactoryProvider.get(), eventsManager);
						}
						events2score.beginIteration(iteration,
								this.scenario.getConfig().controller().getLastIteration() == iteration);

						for (Person person : batch.values()) {
							planEmulator.emulate(person, person.getSelectedPlan(), // eventsManager,
									mode2travelTime, eventsHandler, this.emulationHandlerProvider, iteration,
									eventsManager, events2score,
									overrideTravelTimesFromFirstListEntry && (travelTimeIndex == 0));
						}

						events2score.finish();
						for (Person person : batch.values()) {
							final double newScore = events2score.getAgentScore(person.getId());
							person2scoreSum.compute(person, (p, s) -> s == null ? newScore : s + newScore);
						}
					}

					for (Person person : batch.values()) {
						person.getSelectedPlan().setScore(person2scoreSum.get(person) / listOfMode2travelTime.size());
					}

					processedNumberOfPersons.addAndGet(batch.size());
				} while (batch.size() > 0);
			});

			threads.add(thread);
			thread.start();
		}

		Thread progressThread = new Thread(() -> {
			long currentProcessedNumberOfPersons = 0;
			long lastProcessedNumberOfPersons = -1;

			while (!finished.get()) {
				try {
					currentProcessedNumberOfPersons = processedNumberOfPersons.get();
					// not useful with logger turned off
					if (currentProcessedNumberOfPersons > lastProcessedNumberOfPersons) {
						logger.info(String.format("Emulating... %d / %d (%.2f%%)", currentProcessedNumberOfPersons,
								totalNumberOfPersons, 100.0 * currentProcessedNumberOfPersons / totalNumberOfPersons));
					}
					lastProcessedNumberOfPersons = currentProcessedNumberOfPersons;
					Thread.sleep(10);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
		});
		progressThread.start();

		try {
			for (Thread thread : threads) {
				thread.join();
			}
			finished.set(true);
			progressThread.join();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}

		Configurator.setRootLevel(originalLogLevel);

		logger.info("Emulation finished.");

	}
}
