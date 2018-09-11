/*
 * Copyright 2018 Gunnar Flötteröd
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * contact: gunnar.flotterod@gmail.com
 *
 */
package org.matsim.contrib.pseudosimulation.searchacceleration;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Singleton;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.eventsBasedPTRouter.stopStopTimes.StopStopTime;
import org.matsim.contrib.eventsBasedPTRouter.waitTimes.WaitTime;
import org.matsim.contrib.pseudosimulation.MobSimSwitcher;
import org.matsim.contrib.pseudosimulation.PSimConfigGroup;
import org.matsim.contrib.pseudosimulation.mobsim.PSim;
import org.matsim.contrib.pseudosimulation.searchacceleration.datastructures.SpaceTimeIndicators;
import org.matsim.contrib.pseudosimulation.searchacceleration.datastructures.Utilities;
import org.matsim.contrib.pseudosimulation.searchacceleration.logging.AverageDeltaForUniformReplanning;
import org.matsim.contrib.pseudosimulation.searchacceleration.logging.AverageReplanningEfficiency;
import org.matsim.contrib.pseudosimulation.searchacceleration.logging.AverageUtility;
import org.matsim.contrib.pseudosimulation.searchacceleration.logging.DeltaForUniformReplanning;
import org.matsim.contrib.pseudosimulation.searchacceleration.logging.DeltaForUniformReplanningExact;
import org.matsim.contrib.pseudosimulation.searchacceleration.logging.DeltaPercentile;
import org.matsim.contrib.pseudosimulation.searchacceleration.logging.DriversInPhysicalSim;
import org.matsim.contrib.pseudosimulation.searchacceleration.logging.DriversInPseudoSim;
import org.matsim.contrib.pseudosimulation.searchacceleration.logging.EffectiveReplanningRate;
import org.matsim.contrib.pseudosimulation.searchacceleration.logging.ExpectedDeltaUtilityAccelerated;
import org.matsim.contrib.pseudosimulation.searchacceleration.logging.ExpectedDeltaUtilityUniform;
import org.matsim.contrib.pseudosimulation.searchacceleration.logging.FinalObjectiveFunctionValue;
import org.matsim.contrib.pseudosimulation.searchacceleration.logging.LogDataWrapper;
import org.matsim.contrib.pseudosimulation.searchacceleration.logging.MeanReplanningRate;
import org.matsim.contrib.pseudosimulation.searchacceleration.logging.RealizedDeltaUtility;
import org.matsim.contrib.pseudosimulation.searchacceleration.logging.RealizedGreedyScoreChange;
import org.matsim.contrib.pseudosimulation.searchacceleration.logging.RegularizationWeight;
import org.matsim.contrib.pseudosimulation.searchacceleration.logging.ReplanningEfficiency;
import org.matsim.contrib.pseudosimulation.searchacceleration.logging.ShareNeverReplanned;
import org.matsim.contrib.pseudosimulation.searchacceleration.logging.ShareScoreImprovingReplanners;
import org.matsim.contrib.pseudosimulation.searchacceleration.logging.TTSum;
import org.matsim.contrib.pseudosimulation.searchacceleration.logging.TargetPercentile;
import org.matsim.contrib.pseudosimulation.searchacceleration.logging.UniformGreedyScoreChange;
import org.matsim.contrib.pseudosimulation.searchacceleration.logging.UniformReplannerShare;
import org.matsim.contrib.pseudosimulation.searchacceleration.logging.UniformReplanningObjectiveFunctionValue;
import org.matsim.contrib.pseudosimulation.searchacceleration.logging.WeightedCountDifferences2;
import org.matsim.contrib.pseudosimulation.searchacceleration.utils.RecursiveMovingAverage;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.replanning.GenericPlanStrategy;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Inject;

import floetteroed.utilities.statisticslogging.StatisticsWriter;
import floetteroed.utilities.statisticslogging.TimeStampStatistic;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
@Singleton
public class SearchAccelerator implements StartupListener, IterationEndsListener, LinkEnterEventHandler,
		VehicleEntersTrafficEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler {

	// -------------------- CONSTANTS --------------------

	private static final Logger log = Logger.getLogger(Controler.class);

	// -------------------- INJECTED MEMBERS --------------------

	@Inject
	private MatsimServices services;

	/*
	 * We know if we are in a pSim iteration or in a "real" iteration. The
	 * MobsimSwitcher is updated at iterationStarts, i.e. always *before* the mobsim
	 * (or psim) is executed. The SearchAccelerator, on the other hand, is invoked
	 * at iterationEnds, i.e. *after* the corresponding mobsim (or psim) run.
	 * 
	 */
	@Inject
	private MobSimSwitcher mobsimSwitcher;

	@Inject
	private TravelTime linkTravelTimes;

	@Inject
	private WaitTime waitTime;

	@Inject
	private StopStopTime stopStopTime;

	// -------------------- NON-INJECTED MEMBERS --------------------

	private Set<Id<Person>> replanners = null;

	private final Set<Id<Person>> everReplanners = new LinkedHashSet<>();

	private PopulationState hypotheticalPopulationState = null;

	private double currentDelta = 0.0;

	// >>> created upon startup >>>

	private SlotUsageListener slotUsageListener;
	// private LinkUsageListener matsimIVMobsimUsageListener;
	// private TransitVehicleUsageListener matsimOVMobsimUsageListener;

	// >>> created upon startup >>>
	private StatisticsWriter<LogDataWrapper> statsWriter;
	private Utilities utilities;
	private RecursiveMovingAverage expectedUtilityChangeSumAccelerated;
	private RecursiveMovingAverage expectedUtilityChangeSumUniform;
	private RecursiveMovingAverage realizedUtilityChangeSum;

	// <<< created upon startup <<<

	private List<IndividualReplanningResult> individualReplanningResultsList = null;
	// private List<Byte> lastActualReplanIndicatorList = null;
	// private List<Byte> lastUniformReplanIndicatorList = null;
	// private List<Double> lastCriticalDeltaList = null;
	// private List<Double> lastExpectedScoreChangeList = null;

	private Double previouslyRealizedDeltaPercentile = null;
	private Double targetDeltaPercentile = null;

	public Double lastAverageUtility = null;

	// -------------------- CONSTRUCTION --------------------

	@Inject
	public SearchAccelerator() {
	}

	// -------------------- HELPERS --------------------

	private AccelerationConfigGroup replanningParameters() {
		return ConfigUtils.addOrGetModule(this.services.getConfig(), AccelerationConfigGroup.class);
	}

	private void setWeightOfHypotheticalReplanning(final double weight) {
		final StrategyManager strategyManager = this.services.getStrategyManager();
		for (GenericPlanStrategy<Plan, Person> strategy : strategyManager.getStrategies(null)) {
			if (strategy instanceof AcceptIntendedReplanningStrategy) {
				strategyManager.changeWeightOfStrategy(strategy, null, weight);
			}
		}
	}

	public Integer getDriversInPhysicalSim() {
		if (this.lastPhysicalSlotUsages != null) {
			return this.lastPhysicalSlotUsages.size();
		} else {
			return null;
		}
	}

	// public Double getReplanningEfficiency() {
	// if ((this.expectedUtilityImprovementSum.size() == 0) ||
	// (this.realizedUtilityImprovementSum.size() == 0)) {
	// return null;
	// } else {
	// final double expected = Math.max(1e-8,
	// this.expectedUtilityImprovementSum.mostRecentValue());
	// final double realized = Math.max(1e-8,
	// this.realizedUtilityImprovementSum.mostRecentValue());
	// return realized / expected;
	// }
	// }

	// public Double getAverageReplanningEfficiency() {
	// if ((this.expectedUtilityImprovementSum.size() == 0) ||
	// (this.realizedUtilityImprovementSum.size() == 0)) {
	// return null;
	// } else {
	// final double expected = Math.max(1e-8,
	// this.expectedUtilityImprovementSum.sum());
	// final double realized = Math.max(1e-8,
	// this.realizedUtilityImprovementSum.sum());
	// return realized / expected;
	// }
	// }

	public Double getPhysicalTravelTimeSum_h() {
		double ttSum_s = 0;
		for (Link link : this.services.getScenario().getNetwork().getLinks().values()) {
			for (int time_s = 0; time_s < 24 * 3600; time_s += 15 * 60) {
				ttSum_s += this.linkTravelTimes.getLinkTravelTime(link, time_s, null, null);
			}
		}
		return (ttSum_s / 3600.0);
	}

	public Double getEffectiveReplanningRate() {
		if (this.replanners == null) {
			return null;
		} else {
			return ((double) this.replanners.size()) / this.services.getScenario().getPopulation().getPersons().size();
		}
	}

	public Double getShareNeverReplanned() {
		return 1.0 - ((double) this.everReplanners.size())
				/ this.services.getScenario().getPopulation().getPersons().size();
	}

	// public Double getDeltaForUniformReplanning() {
	// if (this.averageDeltaForUniformReplanning == null ||
	// this.averageDeltaForUniformReplanning.size() == 0) {
	// return null;
	// } else {
	// return this.averageDeltaForUniformReplanning.mostRecentValue();
	// }
	// }

	// public Double getDeltaForUniformReplanningExact() {
	// if (this.averageDeltaForUniformReplanningExact == null
	// || this.averageDeltaForUniformReplanningExact.size() == 0) {
	// return null;
	// } else {
	// return this.averageDeltaForUniformReplanningExact.mostRecentValue();
	// }
	// }

	// public Double getAverageDeltaForUniformReplanning() {
	// if (this.averageDeltaForUniformReplanning == null) {
	// return null;
	// } else {
	// return this.averageDeltaForUniformReplanning.average();
	// }
	// }

	public Double getRegularizationWeight() {
		return this.currentDelta;
	}

	public Double getPercentile() {
		return this.previouslyRealizedDeltaPercentile;
	}

	public Double getLastExpectedUtilityChangeSumAccelerated() {
		return this.expectedUtilityChangeSumAccelerated.average();
	}

	public Double getLastExpectedUtilityChangeSumUniform() {
		return this.expectedUtilityChangeSumUniform.average();
	}

	public Double getLastRealizedUtilityChangeSum() {
		return this.realizedUtilityChangeSum.average();
	}

	public Double getTargetPercentile() {
		return this.targetDeltaPercentile;
	}

	public Double getLastAverageUtility() {
		return this.lastAverageUtility;
	}

	// --------------- IMPLEMENTATION OF StartupListener ---------------

	private AccelerationConfigGroup accelerationConfig = null;

	@Override
	public void notifyStartup(final StartupEvent event) {

		// TODO What would be the typed access structures this deprecation refers to?
		this.accelerationConfig = (AccelerationConfigGroup) this.services.getConfig()
				.getModule(AccelerationConfigGroup.GROUP_NAME);

		this.slotUsageListener = new SlotUsageListener(this.services.getScenario().getPopulation(),
				this.services.getScenario().getTransitVehicles(), this.accelerationConfig);
		// this.matsimIVMobsimUsageListener = new
		// LinkUsageListener(this.replanningParameters().getTimeDiscretization());
		// this.matsimOVMobsimUsageListener = new TransitVehicleUsageListener(
		// this.replanningParameters().getTimeDiscretization(),
		// this.services.getScenario().getPopulation());

		this.expectedUtilityChangeSumAccelerated = new RecursiveMovingAverage(
				// this.replanningParameters().getAverageIterations()
				1);
		this.expectedUtilityChangeSumUniform = new RecursiveMovingAverage(
				// this.replanningParameters().getAverageIterations()
				1);

		// TODO filtering only this because its expectation is a convergence measure
		// TODO this could as well be consequence of 0/0 numerics in the critical delta
		// computations
		this.realizedUtilityChangeSum = new RecursiveMovingAverage(this.replanningParameters().getAverageIterations());

		this.utilities = new Utilities(
				// this.replanningParameters().getAverageIterations()
				1);

		this.statsWriter = new StatisticsWriter<>(
				new File(this.services.getConfig().controler().getOutputDirectory(), "acceleration.log").toString(),
				false);
		this.statsWriter.addSearchStatistic(new TimeStampStatistic<>());
		this.statsWriter.addSearchStatistic(new DriversInPhysicalSim());
		this.statsWriter.addSearchStatistic(new DriversInPseudoSim());
		this.statsWriter.addSearchStatistic(new DeltaPercentile());
		this.statsWriter.addSearchStatistic(new TargetPercentile());
		this.statsWriter.addSearchStatistic(new RegularizationWeight());
		this.statsWriter.addSearchStatistic(new MeanReplanningRate());
		this.statsWriter.addSearchStatistic(new EffectiveReplanningRate());
		this.statsWriter.addSearchStatistic(new RealizedGreedyScoreChange());
		this.statsWriter.addSearchStatistic(new UniformGreedyScoreChange());
		this.statsWriter.addSearchStatistic(new ShareNeverReplanned());
		this.statsWriter.addSearchStatistic(new UniformReplanningObjectiveFunctionValue());
		this.statsWriter.addSearchStatistic(new FinalObjectiveFunctionValue());
		this.statsWriter.addSearchStatistic(new ShareScoreImprovingReplanners());
		this.statsWriter.addSearchStatistic(new WeightedCountDifferences2());
		this.statsWriter.addSearchStatistic(new TTSum());
		this.statsWriter.addSearchStatistic(new ReplanningEfficiency());
		this.statsWriter.addSearchStatistic(new AverageReplanningEfficiency());
		this.statsWriter.addSearchStatistic(new DeltaForUniformReplanning());
		this.statsWriter.addSearchStatistic(new DeltaForUniformReplanningExact());
		this.statsWriter.addSearchStatistic(new AverageDeltaForUniformReplanning());
		this.statsWriter.addSearchStatistic(new UniformReplannerShare());
		// this.statsWriter.addSearchStatistic(new ReplanningSignalAKF());
		this.statsWriter.addSearchStatistic(new AverageUtility());
		this.statsWriter.addSearchStatistic(new RealizedDeltaUtility());
		this.statsWriter.addSearchStatistic(new ExpectedDeltaUtilityUniform());
		this.statsWriter.addSearchStatistic(new ExpectedDeltaUtilityAccelerated());
	}

	// -------------------- IMPLEMENTATION OF EventHandlers --------------------

	@Override
	public void reset(final int iteration) {
		this.slotUsageListener.reset(iteration);
		// this.matsimIVMobsimUsageListener.reset(iteration);
		// this.matsimOVMobsimUsageListener.reset(iteration);
		// this.stopInteractionListener.reset(iteration);
	}

	@Override
	public void handleEvent(final VehicleEntersTrafficEvent event) {
		if (this.mobsimSwitcher.isQSimIteration()) {
			this.slotUsageListener.handleEvent(event);
			// this.matsimIVMobsimUsageListener.handleEvent(event);
		}
	}

	@Override
	public void handleEvent(final LinkEnterEvent event) {
		if (this.mobsimSwitcher.isQSimIteration()) {
			this.slotUsageListener.handleEvent(event);
			// this.matsimIVMobsimUsageListener.handleEvent(event);
		}
	}

	// @Override
	// public void handleEvent(final TransitDriverStartsEvent event) {
	// if (this.mobsimSwitcher.isQSimIteration()) {
	// this.stopInteractionListener.handleEvent(event);
	// }
	// }

	// @Override
	// public void handleEvent(final VehicleDepartsAtFacilityEvent event) {
	// if (this.mobsimSwitcher.isQSimIteration()) {
	// this.stopInteractionListener.handleEvent(event);
	// }
	// }

	@Override
	public void handleEvent(final PersonEntersVehicleEvent event) {
		if (this.mobsimSwitcher.isQSimIteration()) {
			this.slotUsageListener.handleEvent(event);
			// this.matsimOVMobsimUsageListener.handleEvent(event);
			// this.stopInteractionListener.handleEvent(event);
		}
	}

	@Override
	public void handleEvent(final PersonLeavesVehicleEvent event) {
		if (this.mobsimSwitcher.isQSimIteration()) {
			this.slotUsageListener.handleEvent(event);
			// this.matsimOVMobsimUsageListener.handleEvent(event);
			// this.stopInteractionListener.handleEvent(event);
		}
	}

	// --------------- IMPLEMENTATION OF IterationEndsListener ---------------

	private PopulationState lastPhysicalPopulationState = null;
	private Map<Id<Person>, SpaceTimeIndicators<Id<?>>> lastPhysicalSlotUsages = null;

	private int pseudoSimIterationCnt = 0;

	// A safeguard.
	private boolean nextMobsimIsExpectedToBePhysical = true;

	@Override
	public void notifyIterationEnds(final IterationEndsEvent event) {

		if (this.mobsimSwitcher.isQSimIteration()) {
			log.info("physical mobsim run in iteration " + event.getIteration() + " ends");
			if (!this.nextMobsimIsExpectedToBePhysical) {
				throw new RuntimeException("Did not expect a physical mobsim run!");
			}
			this.lastPhysicalPopulationState = new PopulationState(this.services.getScenario().getPopulation());
			this.lastPhysicalSlotUsages = this.slotUsageListener.getNewIndicatorView();
			log.info("number of physical slot usage indicators " + this.lastPhysicalSlotUsages.size());
			// this.lastPhysicalLinkUsages =
			// this.matsimIVMobsimUsageListener.getAndClearIndicators();

			this.pseudoSimIterationCnt = 0;

			this.lastAverageUtility = 0.0;
			for (Person person : this.services.getScenario().getPopulation().getPersons().values()) {
				this.lastAverageUtility += person.getSelectedPlan().getScore();
			}
			this.lastAverageUtility /= this.services.getScenario().getPopulation().getPersons().size();

			// this.stopInteractionListener.analyzeResult(this.services.getScenario().getTransitSchedule());
			// this.stopInteractionListener.setEnabled(false);

		} else {
			log.info("pseudoSim run in iteration " + event.getIteration() + " ends");
			if (this.nextMobsimIsExpectedToBePhysical) {
				throw new RuntimeException("Expected a physical mobsim run!");
			}
			this.pseudoSimIterationCnt++;
		}

		if (this.pseudoSimIterationCnt == (ConfigUtils.addOrGetModule(this.services.getConfig(), PSimConfigGroup.class)
				.getIterationsPerCycle() - 1)) {

			// this.stopInteractionListener.setEnabled(true);

			/*
			 * Extract, for each agent, the expected (hypothetical) score change and do some
			 * book-keeping.
			 */

			for (Person person : this.services.getScenario().getPopulation().getPersons().values()) {
				final double realizedUtility = this.lastPhysicalPopulationState.getSelectedPlan(person.getId())
						.getScore();
				final double expectedUtility = person.getSelectedPlan().getScore();
				this.utilities.update(person.getId(), realizedUtility, expectedUtility);
			}

			final Utilities.SummaryStatistics utilityStatsBeforeReplanning = this.utilities.newSummaryStatistics();

			/*
			 * Book-keeping and program control parameter update.
			 */

			if (utilityStatsBeforeReplanning.previousDataValid) {

				// Merely a safeguard.

				if ((this.replanners == null) || (this.individualReplanningResultsList == null)) {
					throw new RuntimeException(
							"Previous data is valid but previous replanners have not been registered.");
				}

				// Update aggregate utility statistics.

				this.realizedUtilityChangeSum.add(utilityStatsBeforeReplanning.currentRealizedUtilitySum
						- utilityStatsBeforeReplanning.previousRealizedUtilitySum);
				this.expectedUtilityChangeSumUniform
						.add(this.replanningParameters().getMeanReplanningRate(event.getIteration())
								* (utilityStatsBeforeReplanning.previousExpectedUtilitySum
										- utilityStatsBeforeReplanning.previousRealizedUtilitySum));
				double previousExpectedUtilityChangeSumAcceleratedTmp = 0.0;
				for (Id<Person> replannerId : this.replanners) {
					previousExpectedUtilityChangeSumAcceleratedTmp += this.utilities.getUtilities(replannerId)
							.getPreviousExpectedUtilityChange();
				}
				this.expectedUtilityChangeSumAccelerated.add(previousExpectedUtilityChangeSumAcceleratedTmp);

				// Compute target percentile and corresponding delta for the next iteration.

				// final double upperBound =
				// this.expectedUtilityChangeSumUniform.mostRecentValue()
				// + Math.max(0, this.realizedUtilityChangeSum.mostRecentValue());
				// double currentVal = this.expectedUtilityChangeSumUniform.mostRecentValue();

				final double upperBound = Math.max(0.0, this.realizedUtilityChangeSum.average());
				double utilityPredictionError = 0.0; // between accelerated and greedy score, starting at delta=inf

				int percentileIndex = this.individualReplanningResultsList.size() - 1;

				while ((Math.abs(utilityPredictionError) < upperBound) && (percentileIndex > 0)) {
					percentileIndex--;
					final IndividualReplanningResult individualResult = this.individualReplanningResultsList
							.get(percentileIndex);
					if (individualResult.wouldBeGreedyReplanner) {
						if (!individualResult.wouldBeUniformReplanner) {
							utilityPredictionError += individualResult.expectedScoreChange;
						}
					} else { // would not be greedy
						if (individualResult.wouldBeUniformReplanner) {
							utilityPredictionError -= individualResult.expectedScoreChange;
						}
					}
				}
				if (Math.abs(utilityPredictionError) < upperBound) {
					percentileIndex = Math.min(this.individualReplanningResultsList.size() - 1, percentileIndex + 1);
				}

				// if (currentVal > upperBound) { // TODO Use relative threshold?
				//
				// while ((currentVal > upperBound)
				// && (percentileIndex + 1 < this.individualReplanningResultsList.size())) {
				// percentileIndex++;
				// final IndividualReplanningResult individualResult =
				// this.individualReplanningResultsList
				// .get(percentileIndex);
				// if (individualResult.isActualReplanner &&
				// !individualResult.wouldBeUniformReplanner) {
				// currentVal -= individualResult.expectedScoreChange;
				// } else if (!individualResult.isActualReplanner &&
				// individualResult.wouldBeUniformReplanner) {
				// currentVal += individualResult.expectedScoreChange;
				// }
				// }
				//
				// } else { // TODO Use relative threshold?
				//
				// while ((currentVal < upperBound) && (percentileIndex - 1 >= 0)) {
				// percentileIndex--;
				// final IndividualReplanningResult individualResult =
				// this.individualReplanningResultsList
				// .get(percentileIndex);
				// if (individualResult.isActualReplanner &&
				// !individualResult.wouldBeUniformReplanner) {
				// currentVal += individualResult.expectedScoreChange;
				// } else if (!individualResult.isActualReplanner &&
				// individualResult.wouldBeUniformReplanner) {
				// currentVal -= individualResult.expectedScoreChange;
				// }
				// }
				// if (currentVal > upperBound) {
				// percentileIndex = Math.min(this.individualReplanningResultsList.size() - 1,
				// percentileIndex + 1);
				// }
				// }

				this.targetDeltaPercentile = Math.max(0, Math.min(100,
						(percentileIndex * 100.0) / this.services.getScenario().getPopulation().getPersons().size()));
				this.currentDelta = Math.max(0.0,
						this.individualReplanningResultsList.get(percentileIndex).criticalDelta);
			}

			/*
			 * Extract hypothetical selected plans.
			 */

			final Collection<Plan> selectedHypotheticalPlans = new ArrayList<>(
					this.services.getScenario().getPopulation().getPersons().size());
			for (Person person : this.services.getScenario().getPopulation().getPersons().values()) {
				selectedHypotheticalPlans.add(person.getSelectedPlan());
			}

			/*
			 * Execute one pSim with the full population.
			 */

			final Map<Id<Person>, SpaceTimeIndicators<Id<?>>> lastPseudoSimSlotUsages;
			{
				final SlotUsageListener pSimSlotUsageListener = new SlotUsageListener(
						this.services.getScenario().getPopulation(), this.services.getScenario().getTransitVehicles(),
						this.accelerationConfig);
				// final LinkUsageListener pSimLinkUsageListener = new LinkUsageListener(
				// this.replanningParameters().getTimeDiscretization());
				final EventsManager eventsManager = EventsUtils.createEventsManager();
				eventsManager.addHandler(pSimSlotUsageListener);
				final PSim pSim = new PSim(this.services.getScenario(), eventsManager, selectedHypotheticalPlans,
						this.linkTravelTimes, waitTime, stopStopTime, null);
				// final PSim pSim = new PSim(this.services.getScenario(), eventsManager,
				// selectedHypotheticalPlans,
				// this.linkTravelTimes);
				pSim.run();
				lastPseudoSimSlotUsages = pSimSlotUsageListener.getNewIndicatorView();
				log.info("number of pSim slot usage indicators " + lastPseudoSimSlotUsages.size());
			}

			/*
			 * Memorize the most recent hypothetical population state and re-set the
			 * population to its most recent physical state.
			 */

			this.hypotheticalPopulationState = new PopulationState(this.services.getScenario().getPopulation());
			this.lastPhysicalPopulationState.set(this.services.getScenario().getPopulation());

			/*
			 * DECIDE WHO GETS TO RE-PLAN.
			 * 
			 * At this point, one has (i) the link usage statistics from the last physical
			 * MATSim network loading (lastPhysicalLinkUsages), and (ii) the hypothetical
			 * link usage statistics that would result from a 100% re-planning rate if
			 * network congestion did not change (lastPseudoSimLinkUsages).
			 * 
			 * Now solve an optimization problem that aims at balancing simulation
			 * advancement (changing link usage patterns) and simulation stabilization
			 * (keeping link usage patterns as they are). Most of the code below prepares
			 * the (heuristic) solution of this problem.
			 * 
			 */

			final ReplannerIdentifier replannerIdentifier = new ReplannerIdentifier(this.replanningParameters(),
					event.getIteration(), this.lastPhysicalSlotUsages, lastPseudoSimSlotUsages,
					this.slotUsageListener.getWeightView(), this.services.getScenario().getPopulation(),
					utilityStatsBeforeReplanning.personId2currentDeltaUtility,
					utilityStatsBeforeReplanning.currentDeltaUtilitySum, this.currentDelta);
			this.replanners = replannerIdentifier.drawReplanners();
			this.everReplanners.addAll(this.replanners);
			this.individualReplanningResultsList = replannerIdentifier.getIndividualReplanningResultListView();

			final LogDataWrapper data = new LogDataWrapper(this, replannerIdentifier, lastPseudoSimSlotUsages.size());
			this.statsWriter.writeToFile(data);

			// this.lastActualReplanIndicatorList =
			// replannerIdentifier.getActualReplanIndicatorListView();
			// this.lastUniformReplanIndicatorList =
			// replannerIdentifier.getUniformReplanIndicatorListView();
			// this.lastCriticalDeltaList =
			// replannerIdentifier.getDeltaForUniformReplanningListView();
			// this.lastExpectedScoreChangeList =
			// replannerIdentifier.getExpectedScoreChangeListView();

			this.nextMobsimIsExpectedToBePhysical = true;
			this.setWeightOfHypotheticalReplanning(1e9);

		} else {

			this.nextMobsimIsExpectedToBePhysical = false;
			this.setWeightOfHypotheticalReplanning(0);

		}
	}

	// -------------------- REPLANNING FUNCTIONALITY --------------------

	public void replan(final HasPlansAndId<Plan, Person> person) {
		if ((this.replanners != null) && this.replanners.contains(person.getId())) {
			// This replaces the entire choice set and not just the selected plan. Why not.
			this.hypotheticalPopulationState.set(person);
		}
	}
}
