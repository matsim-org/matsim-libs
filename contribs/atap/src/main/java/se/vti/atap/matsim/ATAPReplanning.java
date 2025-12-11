/**
 * se.vti.atap
 * 
 * Copyright (C) 2025 by Gunnar Flötteröd (VTI, LiU).
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
package se.vti.atap.matsim;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.corelisteners.PlansReplanning;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.ReplanningEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.ReplanningListener;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;

import se.vti.emulation.EmulationEngine;
import se.vti.utils.misc.statisticslogging.Statistic;
import se.vti.utils.misc.statisticslogging.StatisticsWriter;
import se.vti.utils.misc.statisticslogging.TimeStampStatistic;

/**
 * @author Gunnar Flötteröd
 */
@Singleton
public final class ATAPReplanning implements PlansReplanning, ReplanningListener, AfterMobsimListener {

	// -------------------- CONSTANTS --------------------

	@SuppressWarnings("unused")
	private final static Logger log = LogManager.getLogger(ATAPReplanning.class);

	private final ATAPConfigGroup atapConfig;

	private final Provider<EmulationEngine> emulationEngineProvider;

	private final MatsimServices services;

	private final StatisticsWriter<ATAPReplanning> statsWriter;

	private final AbstractReplannerSelector replannerSelector;

	private final Set<Id<Person>> personIds;

	private final Collection<? extends Person> persons;

	private final EmulationErrorAnalyzer emulationErrorAnalyzer;

	private final GapAnalyzer gapAnalyzer;

	// -------------------- MEMBERS --------------------

	private Integer replanIteration = null;

	private Double gap = null;

	private final LinkedList<Map<String, LinkTravelTimeCopy>> listOfMode2travelTimes = new LinkedList<>();

	// -------------------- CONSTRUCTION --------------------

	@Inject
	ATAPReplanning(final Provider<EmulationEngine> emulationEngineProvider, final MatsimServices services) {

		this.atapConfig = ConfigUtils.addOrGetModule(services.getConfig(), ATAPConfigGroup.class);
		this.emulationEngineProvider = emulationEngineProvider;
		this.services = services;

		this.personIds = services.getScenario().getPopulation().getPersons().keySet();
		this.persons = services.getScenario().getPopulation().getPersons().values();

		this.replannerSelector = AbstractReplannerSelector.newReplannerSelector(this.atapConfig);

		this.emulationErrorAnalyzer = new EmulationErrorAnalyzer();

		final int percentileStep = 10;
		this.gapAnalyzer = new GapAnalyzer(percentileStep);

		this.statsWriter = new StatisticsWriter<>(
				new File(services.getConfig().controller().getOutputDirectory(), "ATAP.log").toString(),
				false);

		this.statsWriter.addSearchStatistic(new TimeStampStatistic<>());
		this.statsWriter.addSearchStatistic(new Statistic<>() {
			@Override
			public String label() {
				return "ReplanIteration";
			}

			@Override
			public String value(ATAPReplanning data) {
				return data.replanIteration.toString();
			}
		});
		this.statsWriter.addSearchStatistic(new Statistic<>() {
			@Override
			public String label() {
				return "MemorizedTravelTimes";
			}

			@Override
			public String value(ATAPReplanning data) {
				return Integer.toString(data.listOfMode2travelTimes.size());
			}
		});
		this.statsWriter.addSearchStatistic(new Statistic<>() {
			@Override
			public String label() {
				return "TargetReplanningRate";
			}

			@Override
			public String value(ATAPReplanning data) {
				return Statistic.toString(data.replannerSelector.getTargetReplanningRate());
			}
		});
		this.statsWriter.addSearchStatistic(new Statistic<>() {
			@Override
			public String label() {
				return "RealizedReplanningRate";
			}

			@Override
			public String value(ATAPReplanning data) {
				return Statistic.toString(data.replannerSelector.getRealizedReplanningRate());
			}
		});
		this.statsWriter.addSearchStatistic(new Statistic<>() {
			@Override
			public String label() {
				return "Gap";
			}

			@Override
			public String value(ATAPReplanning data) {
				return data.gap.toString();
			}
		});
		
		if (!this.atapConfig.getReduceLogging()) {
			this.statsWriter.addSearchStatistic(new Statistic<>() {
				@Override
				public String label() {
					return "MeanFilteredGap";
				}

				@Override
				public String value(ATAPReplanning data) {
					return Statistic.toString(data.replannerSelector.getMeanFilteredGap());
				}
			});
			this.statsWriter.addSearchStatistic(new Statistic<>() {
				@Override
				public String label() {
					return "MeanReplannerFilteredGap";
				}

				@Override
				public String value(ATAPReplanning data) {
					return Statistic.toString(data.replannerSelector.getMeanReplannerFilteredGap());
				}
			});
			this.statsWriter.addSearchStatistic(new Statistic<>() {
				@Override
				public String label() {
					return "PopMeanEmulationError";
				}

				@Override
				public String value(ATAPReplanning data) {
					return Statistic.toString(data.emulationErrorAnalyzer.getMeanError());
				}
			});
			this.statsWriter.addSearchStatistic(new Statistic<>() {
				@Override
				public String label() {
					return "PopMeanAbsEmulationError";
				}

				@Override
				public String value(ATAPReplanning data) {
					return Statistic.toString(data.emulationErrorAnalyzer.getMeanAbsError());
				}
			});

			this.statsWriter.addSearchStatistic(new Statistic<>() {
				@Override
				public String label() {
					return "MinScore";
				}

				@Override
				public String value(ATAPReplanning data) {
					return Statistic.toString(data.gapAnalyzer.getMinScore());
				}
			});
			this.statsWriter.addSearchStatistic(new Statistic<>() {
				@Override
				public String label() {
					return "MeanScore";
				}

				@Override
				public String value(ATAPReplanning data) {
					return Statistic.toString(data.gapAnalyzer.getMeanScore());
				}
			});
			this.statsWriter.addSearchStatistic(new Statistic<>() {
				@Override
				public String label() {
					return "MaxScore";
				}

				@Override
				public String value(ATAPReplanning data) {
					return Statistic.toString(data.gapAnalyzer.getMaxScore());
				}
			});
			this.statsWriter.addSearchStatistic(new Statistic<>() {
				@Override
				public String label() {
					return GapAnalyzer.createPercentileHeader(percentileStep, p -> "abs" + p + "%");
				}

				@Override
				public String value(ATAPReplanning data) {
					return data.gapAnalyzer.getAbsolutePercentiles();
				}
			});
		}		
	}

	// -------------------- INTERNALS --------------------

	private Map<String, LinkTravelTimeCopy> newFilteredTravelTimes() {
		final Map<String, LinkTravelTimeCopy> result = new LinkedHashMap<>();

		for (String mode : this.listOfMode2travelTimes.getFirst().keySet()) {
			final List<LinkTravelTimeCopy> travelTimes = new ArrayList<>(this.listOfMode2travelTimes.size());
			final List<Double> weights = new ArrayList<>(this.listOfMode2travelTimes.size());
			for (Map<String, LinkTravelTimeCopy> mode2travelTime : this.listOfMode2travelTimes) {
				travelTimes.add(mode2travelTime.get(mode));
				weights.add(1.0 / this.listOfMode2travelTimes.size());
			}
			result.put(mode, LinkTravelTimeCopy.newWeightedSum(travelTimes, weights));
		}

		return result;
	}

	// -------------------- AFTER MOBSIM LISTENER --------------------

	@Override
	public void notifyAfterMobsim(final AfterMobsimEvent event) {

		while (this.listOfMode2travelTimes.size() >= this.atapConfig.getMaxMemory()) {
			this.listOfMode2travelTimes.removeLast();
		}

		final Map<String, TravelTime> realizedMode2travelTime = event.getServices().getInjector()
				.getInstance(Key.get(new TypeLiteral<Map<String, TravelTime>>() {
				}));
		final Map<String, LinkTravelTimeCopy> newMode2travelTime = new ConcurrentHashMap<>(
				realizedMode2travelTime.size());
		for (Map.Entry<String, TravelTime> realizedEntry : realizedMode2travelTime.entrySet()) {
			newMode2travelTime.put(realizedEntry.getKey(), new LinkTravelTimeCopy(realizedEntry.getValue(),
					event.getServices().getConfig(), event.getServices().getScenario().getNetwork()));
		}
		this.listOfMode2travelTimes.addFirst(newMode2travelTime);
	}

	// -------------------- REPLANNING LISTENER --------------------

	@Override
	public void notifyReplanning(final ReplanningEvent event) {

		final int memory = this.listOfMode2travelTimes.size();

		if (this.replanIteration == null) {
			this.replanIteration = 0;
		} else {
			this.replanIteration++;
		}

		/*
		 * (0) Extract (filtered) travel times for different purposes: population
		 * distance, replanning, emulation.
		 */

		final Map<String, LinkTravelTimeCopy> mode2filteredTravelTimes;
		if (this.listOfMode2travelTimes.size() == 1) {
			mode2filteredTravelTimes = this.listOfMode2travelTimes.getFirst();
		} else {
			// TODO speed up with a recursive formulation
			mode2filteredTravelTimes = this.newFilteredTravelTimes();
		}

		final List<Map<String, ? extends TravelTime>> listOfMode2travelTimes4emulation = Collections
				.synchronizedList(new ArrayList<>(memory));
		for (Map<String, LinkTravelTimeCopy> mode2travelTimes : this.listOfMode2travelTimes) {
			listOfMode2travelTimes4emulation.add(mode2travelTimes);
		}

		/*
		 * (1) Extract old plans and compute new plans. Evaluate both old and new plans.
		 */

		EmulationEngine.ensureOnePlanPerPersonInScenario(this.services.getScenario(), false);

		this.emulationErrorAnalyzer.setSimulatedScores(this.services.getScenario().getPopulation());
		final EventsChecker emulatedEventsChecker;
		if (this.atapConfig.getCheckEmulatedAgentsCnt() > 0) {
			emulatedEventsChecker = new EventsChecker("observedPersons.txt", false);
		} else {
			emulatedEventsChecker = null;
		}

		EmulationEngine emulationEngine = this.emulationEngineProvider.get();
		emulationEngine.emulate(this.replanIteration.intValue(), listOfMode2travelTimes4emulation, true);
		final PlansContainer oldPlans = new PlansContainer(this.services.getScenario().getPopulation());

		this.emulationErrorAnalyzer.setEmulatedScores(this.services.getScenario().getPopulation());
		if (emulatedEventsChecker != null) {
			emulatedEventsChecker.writeReport("emulatedEventsReport." + (event.getIteration() - 1) + ".txt");
		}

		final EmulationEngine replanningEngine = this.emulationEngineProvider.get();
		replanningEngine.replan(event.getIteration(), listOfMode2travelTimes4emulation, true);
		final PlansContainer newPlans = new PlansContainer(event.getServices().getScenario().getPopulation());

		/*
		 * (2) Compute intermediate statistics.
		 */

		final Map<Id<Person>, Double> personId2FilteredGap = this.personIds.stream().collect(Collectors.toMap(id -> id,
				id -> (newPlans.getSelectedPlan(id).getScore() - oldPlans.getSelectedPlan(id).getScore())));

		{
			// TODO Only for testing

			emulationEngine = this.emulationEngineProvider.get();
			oldPlans.set(this.services.getScenario().getPopulation());
			emulationEngine.emulate(this.replanIteration.intValue(),
					Collections.singletonList(this.listOfMode2travelTimes.getFirst()), false);
			this.gap = (-1.0) * this.services.getScenario().getPopulation().getPersons().values().stream()
					.mapToDouble(p -> p.getSelectedPlan().getScore()).average().getAsDouble();
			this.gapAnalyzer.registerPlansBeforeReplanning(this.services.getScenario().getPopulation());

			emulationEngine = this.emulationEngineProvider.get();
			newPlans.set(this.services.getScenario().getPopulation());
			emulationEngine.emulate(this.replanIteration.intValue(),
					Collections.singletonList(this.listOfMode2travelTimes.getFirst()), false);
			this.gap += this.services.getScenario().getPopulation().getPersons().values().stream()
					.mapToDouble(p -> p.getSelectedPlan().getScore()).average().getAsDouble();
			this.gapAnalyzer.registerPlansAfterReplanning(this.services.getScenario().getPopulation());

			if (this.gapAnalyzer.linkCntAndAbsoluteGap != null && (this.replanIteration % 100 == 0
					|| this.replanIteration == this.services.getConfig().controller().getLastIteration() - 1)) {
				this.gapAnalyzer.writeLinkCntAndAbsoluteGapScatterplot(
						new File(services.getConfig().controller().getOutputDirectory(),
								"linkCnt_vs_absGap." + this.replanIteration + ".txt").toString());
			}
		}

		/*
		 * (3) Identify re-planners.
		 */

		final PopulationDistance popDist = new PopulationDistance(oldPlans, newPlans,
				this.services.getScenario(), mode2filteredTravelTimes);
		this.replannerSelector.setDistanceToReplannedPopulation(popDist);

		final Set<Id<Person>> replannerIds = this.replannerSelector.selectReplanners(personId2FilteredGap,
				this.replanIteration);

		for (Person person : this.persons) {
			if (replannerIds.contains(person.getId())) {
				newPlans.set(person);
			} else {
				oldPlans.set(person);
			}
		}

		/*
		 * (4) Postprocess.
		 */

		this.emulationErrorAnalyzer.update(this.services.getScenario().getPopulation());
		this.statsWriter.writeToFile(this);
	}
}
