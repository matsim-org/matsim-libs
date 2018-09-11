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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.pseudosimulation.searchacceleration.datastructures.CountIndicatorUtils;
import org.matsim.contrib.pseudosimulation.searchacceleration.datastructures.ScoreUpdater;
import org.matsim.contrib.pseudosimulation.searchacceleration.datastructures.SpaceTimeIndicators;
import org.matsim.contrib.pseudosimulation.searchacceleration.recipes.AccelerationRecipe;
import org.matsim.contrib.pseudosimulation.searchacceleration.recipes.Mah2007Recipe;
import org.matsim.contrib.pseudosimulation.searchacceleration.recipes.Mah2009Recipe;
import org.matsim.contrib.pseudosimulation.searchacceleration.recipes.ReplannerIdentifierRecipe;
import org.matsim.contrib.pseudosimulation.searchacceleration.recipes.UniformReplanningRecipe;

import floetteroed.utilities.DynamicData;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class ReplannerIdentifier {

	// private static final Logger log = Logger.getLogger(Controler.class);

	// -------------------- MEMBERS --------------------

	private final AccelerationConfigGroup replanningParameters;
	private final double lambda;
	private final double delta;

	private final Population population;

	private final Map<Id<Person>, SpaceTimeIndicators<Id<?>>> personId2physicalSlotUsage;
	private final Map<Id<Person>, SpaceTimeIndicators<Id<?>>> personId2pseudoSimSlotUsage;
	private final Map<Id<?>, Double> slotWeights;
	private final DynamicData<Id<?>> currentWeightedCounts;
	private final DynamicData<Id<?>> upcomingWeightedCounts;
	private final double sumOfWeightedCountDifferences2;

	// private final Map<Id<Person>, SpaceTimeIndicators<Id<?>>>
	// driverId2physicalLinkUsage;
	// private final Map<Id<Person>, SpaceTimeIndicators<Id<?>>>
	// driverId2pseudoSimLinkUsage;
	// private final Map<Id<Person>, SpaceTimeIndicators<Id<?>>>
	// passengerId2physicalTransitVehicleUsage;
	// private final Map<Id<Person>, SpaceTimeIndicators<Id<?>>>
	// passengerId2pseudoSimTransitVehicleUsage;
	//
	// private final DynamicData<Id<?>> currentWeightedLinkCounts;
	// private final DynamicData<Id<?>> upcomingWeightedLinkCounts;
	// private final DynamicData<Id<?>> currentWeightedTransitCounts;
	// private final DynamicData<Id<?>> upcomingWeightedTransitCounts;
	//
	// private final double sumOfWeightedLinkCountDifferences2;
	// private final double sumOfWeightedTransitCountDifferences2;

	private final double beta;

	private final Map<Id<Person>, Double> personId2utilityChange;
	private final double totalUtilityChange;

	private Double shareOfScoreImprovingReplanners = null;
	private Double score = null;

	private SortedSet<IndividualReplanningResult> individualReplanningResults = new TreeSet<>();
	// private List<Double> deltaForUniformReplanningList = new ArrayList<>();
	// private List<Double> expectedScoreChangeList = new ArrayList<>();
	// private List<Byte> actualReplanIndicatorList = new ArrayList<>();
	// private List<Byte> uniformReplanIndicatorList = new ArrayList<>();

	private Double uniformGreedyScoreChange = null;
	private Double realizedGreedyScoreChange = null;

	private Double uniformReplannerShare = null;

	private List<Double> replanningSignalAKF = null;

	// -------------------- GETTERS (FOR LOGGING) --------------------

	public double getUniformReplanningObjectiveFunctionValue() {
		return (2.0 - this.lambda) * this.lambda * (this.sumOfWeightedCountDifferences2 + this.delta);
	}

	public Double getShareOfScoreImprovingReplanners() {
		return this.shareOfScoreImprovingReplanners;
	}

	public Double getFinalObjectiveFunctionValue() {
		return this.score;
	}

	public Double getSumOfWeightedCountDifferences2() {
		return this.sumOfWeightedCountDifferences2;
	}

	public Double getMeanReplanningRate() {
		return this.lambda;
	}

	// public double getDeltaForUniformReplanningExact(final int percentile) {
	// Collections.sort(this.allDeltaForUniformReplanning);
	// final int index = Math.min(this.allDeltaForUniformReplanning.size() - 1,
	// (percentile * this.allDeltaForUniformReplanning.size()) / 100);
	// return this.allDeltaForUniformReplanning.get(index);
	// return
	// this.getDeltaForUniformReplanning(this.getDeltaForUniformReplanningListView(),
	// percentile);
	// }

	// private double getDeltaForUniformReplanning(final List<Double> allDelta,
	// final int percentile) {
	// final int index = Math.min(allDelta.size() - 1, (percentile *
	// allDelta.size()) / 100);
	// return allDelta.get(index);
	// }

	public Double getUniformGreedyScoreChange() {
		return this.uniformGreedyScoreChange;
	}

	public Double getRealizedGreedyScoreChange() {
		return this.realizedGreedyScoreChange;
	}

	public Double getUniformReplannerShare() {
		return this.uniformReplannerShare;
	}

	public List<Double> getReplanningSignalAKF() {
		return this.replanningSignalAKF;
	}

	public List<IndividualReplanningResult> getIndividualReplanningResultListView() {
		return Collections.unmodifiableList(new ArrayList<>(this.individualReplanningResults));
	}

	// public List<Double> getDeltaForUniformReplanningListView() {
	// return Collections.unmodifiableList(this.deltaForUniformReplanningList);
	// }
	//
	// public List<Byte> getActualReplanIndicatorListView() {
	// return Collections.unmodifiableList(this.actualReplanIndicatorList);
	// }
	//
	// public List<Byte> getUniformReplanIndicatorListView() {
	// return Collections.unmodifiableList(this.uniformReplanIndicatorList);
	// }
	//
	// public List<Double> getExpectedScoreChangeListView() {
	// return Collections.unmodifiableList(this.expectedScoreChangeList);
	// }

	// -------------------- CONSTRUCTION --------------------

	ReplannerIdentifier(final AccelerationConfigGroup replanningParameters, final int iteration,
			final Map<Id<Person>, SpaceTimeIndicators<Id<?>>> personId2physicalSlotUsage,
			final Map<Id<Person>, SpaceTimeIndicators<Id<?>>> personId2pseudoSimSlotUsage,
			// final Map<Id<Person>, SpaceTimeIndicators<Id<?>>> driverId2physicalLinkUsage,
			// final Map<Id<Person>, SpaceTimeIndicators<Id<?>>>
			// driverId2pseudoSimLinkUsage,
			// final Map<Id<Person>, SpaceTimeIndicators<Id<?>>>
			// passengerId2physicalTransitVehicleUsage,
			// final Map<Id<Person>, SpaceTimeIndicators<Id<?>>>
			// passengerId2pseudoSimTransitVehicleUsage,
			final Map<Id<?>, Double> slotWeights, final Population population,
			final Map<Id<Person>, Double> personId2UtilityChange, final double totalUtilityChange, final double delta) {

//		System.out.println(slotWeights);
//		System.out.println(slotWeights.size());
//		System.exit(0);

		this.replanningParameters = replanningParameters;
		this.population = population;
		this.personId2utilityChange = personId2UtilityChange;
		this.totalUtilityChange = totalUtilityChange;

		this.personId2physicalSlotUsage = personId2physicalSlotUsage;
		this.personId2pseudoSimSlotUsage = personId2pseudoSimSlotUsage;
		this.slotWeights = slotWeights;
		this.currentWeightedCounts = CountIndicatorUtils.newWeightedCounts(this.personId2physicalSlotUsage.values(),
				slotWeights, this.replanningParameters.getTimeDiscretization());
		this.upcomingWeightedCounts = CountIndicatorUtils.newWeightedCounts(this.personId2pseudoSimSlotUsage.values(),
				slotWeights, this.replanningParameters.getTimeDiscretization());
		this.sumOfWeightedCountDifferences2 = CountIndicatorUtils.sumOfDifferences2(this.currentWeightedCounts,
				this.upcomingWeightedCounts);

		// this.driverId2physicalLinkUsage = driverId2physicalLinkUsage;
		// this.driverId2pseudoSimLinkUsage = driverId2pseudoSimLinkUsage;
		// this.currentWeightedLinkCounts =
		// CountIndicatorUtils.newWeightedCounts(this.driverId2physicalLinkUsage.values(),
		// this.replanningParameters.getLinkWeightView(),
		// this.replanningParameters.getTimeDiscretization());
		// this.upcomingWeightedLinkCounts = CountIndicatorUtils.newWeightedCounts(
		// this.driverId2pseudoSimLinkUsage.values(),
		// this.replanningParameters.getLinkWeightView(),
		// this.replanningParameters.getTimeDiscretization());
		// this.sumOfWeightedLinkCountDifferences2 =
		// CountIndicatorUtils.sumOfDifferences2(this.currentWeightedLinkCounts,
		// this.upcomingWeightedLinkCounts);
		//
		// this.passengerId2physicalTransitVehicleUsage =
		// passengerId2physicalTransitVehicleUsage;
		// this.passengerId2pseudoSimTransitVehicleUsage =
		// passengerId2pseudoSimTransitVehicleUsage;
		// this.currentWeightedTransitCounts = CountIndicatorUtils.newWeightedCounts(
		// this.passengerId2physicalTransitVehicleUsage.values(), transitWeights,
		// this.replanningParameters.getTimeDiscretization());
		// this.upcomingWeightedTransitCounts = CountIndicatorUtils.newWeightedCounts(
		// this.passengerId2pseudoSimTransitVehicleUsage.values(), transitWeights,
		// this.replanningParameters.getTimeDiscretization());
		// this.sumOfWeightedTransitCountDifferences2 = CountIndicatorUtils
		// .sumOfDifferences2(this.currentWeightedTransitCounts,
		// this.upcomingWeightedTransitCounts);

		this.lambda = this.replanningParameters.getMeanReplanningRate(iteration);
		this.beta = 2.0 * this.lambda * this.sumOfWeightedCountDifferences2 / this.totalUtilityChange;
		this.delta = delta;
	}

	// -------------------- IMPLEMENTATION --------------------

	Set<Id<Person>> drawReplanners() {

		// Initialize score residuals.

		final DynamicData<Id<?>> interactionResiduals = CountIndicatorUtils
				.newWeightedDifference(this.upcomingWeightedCounts, this.currentWeightedCounts, this.lambda);
		double inertiaResidual = (1.0 - this.lambda) * this.totalUtilityChange;
		double regularizationResidual = 0;
		double sumOfInteractionResiduals2 = interactionResiduals.sumOfEntries2();

		final DynamicData<Id<?>> uniformInteractionResiduals = CountIndicatorUtils
				.newWeightedDifference(this.upcomingWeightedCounts, this.currentWeightedCounts, this.lambda);
		double uniformInertiaResidual = inertiaResidual;
		double uniformRegularizationResidual = regularizationResidual;
		double sumOfUniformInteractionResiduals2 = sumOfInteractionResiduals2;

		// Select the replanning recipe.

		final ReplannerIdentifierRecipe recipe;
		if (AccelerationConfigGroup.ModeType.off == this.replanningParameters.getModeTypeField()) {
			recipe = new UniformReplanningRecipe(this.lambda);
		} else if (AccelerationConfigGroup.ModeType.accelerate == this.replanningParameters.getModeTypeField()) {
			recipe = new AccelerationRecipe(this.lambda);
		} else if (AccelerationConfigGroup.ModeType.mah2007 == this.replanningParameters.getModeTypeField()) {
			recipe = new Mah2007Recipe(this.personId2utilityChange, this.lambda);
		} else if (AccelerationConfigGroup.ModeType.mah2009 == this.replanningParameters.getModeTypeField()) {
			recipe = new Mah2009Recipe(this.personId2utilityChange, this.lambda);
		} else {
			throw new RuntimeException("Unknown mode: " + this.replanningParameters.getModeTypeField());
		}

		// Go through all vehicles and decide which driver gets to re-plan.

		final Set<Id<Person>> replanners = new LinkedHashSet<>();
		final List<Id<Person>> allPersonIdsShuffled = new ArrayList<>(this.population.getPersons().keySet());
		Collections.shuffle(allPersonIdsShuffled);

		this.score = this.getUniformReplanningObjectiveFunctionValue();

		int scoreImprovingReplanners = 0;

		this.realizedGreedyScoreChange = 0.0;
		this.uniformGreedyScoreChange = 0.0;

		int uniformReplanners = 0;

		for (Id<Person> driverId : allPersonIdsShuffled) {

			final ScoreUpdater<Id<?>> scoreUpdater = new ScoreUpdater<>(this.personId2physicalSlotUsage.get(driverId),
					this.personId2pseudoSimSlotUsage.get(driverId), this.slotWeights, this.lambda, this.beta,
					this.delta, interactionResiduals, inertiaResidual, regularizationResidual,
					this.replanningParameters, this.personId2utilityChange.get(driverId), this.totalUtilityChange,
					sumOfInteractionResiduals2);

			final ScoreUpdater<Id<?>> uniformScoreUpdater = new ScoreUpdater<>(
					this.personId2physicalSlotUsage.get(driverId), this.personId2pseudoSimSlotUsage.get(driverId),
					this.slotWeights, this.lambda, this.beta, this.delta, uniformInteractionResiduals,
					uniformInertiaResidual, uniformRegularizationResidual, this.replanningParameters,
					this.personId2utilityChange.get(driverId), this.totalUtilityChange,
					sumOfUniformInteractionResiduals2);

			final boolean replanner = recipe.isReplanner(driverId, scoreUpdater.getScoreChangeIfOne(),
					scoreUpdater.getScoreChangeIfZero());
			if (replanner) {
				replanners.add(driverId);
				this.score += scoreUpdater.getScoreChangeIfOne();
				realizedGreedyScoreChange += scoreUpdater.getGreedyScoreChangeIfOne();
			} else {
				this.score += scoreUpdater.getScoreChangeIfZero();
				realizedGreedyScoreChange += scoreUpdater.getGreedyScoreChangeIfZero();
			}

			this.uniformGreedyScoreChange += this.lambda * scoreUpdater.getGreedyScoreChangeIfOne()
					+ (1.0 - this.lambda) * scoreUpdater.getGreedyScoreChangeIfZero();

			if (Math.min(scoreUpdater.getScoreChangeIfOne(), scoreUpdater.getScoreChangeIfZero()) < 0) {
				scoreImprovingReplanners++;
			}

			if (replanner == uniformScoreUpdater.wouldBeUniformReplanner) {
				uniformReplanners++;
			}

			scoreUpdater.updateResiduals(replanner ? 1.0 : 0.0); // interaction residual by reference
			inertiaResidual = scoreUpdater.getUpdatedInertiaResidual();
			regularizationResidual = scoreUpdater.getUpdatedRegularizationResidual();
			sumOfInteractionResiduals2 = scoreUpdater.getUpdatedSumOfInteractionResiduals2();

			uniformScoreUpdater.updateResiduals(uniformScoreUpdater.wouldBeUniformReplanner ? 1.0 : 0.0);
			uniformInertiaResidual = uniformScoreUpdater.getUpdatedInertiaResidual();
			uniformRegularizationResidual = uniformScoreUpdater.getUpdatedRegularizationResidual();
			sumOfUniformInteractionResiduals2 = uniformScoreUpdater.getUpdatedSumOfInteractionResiduals2();

			// for debugging
			// {
			// final double exactSum2 = interactionResiduals.sumOfEntries2();
			// log.info("Relative interaction residual error = "
			// + (sumOfInteractionResiduals2 - exactSum2) / exactSum2);
			// }

			// this.allDeltaForUniformReplanning.add(scoreUpdater.getDeltaForUniformReplanning());
			// this.actualReplanIndicatorList.add(replanner ? (byte) 1 : 0);
			// this.uniformReplanIndicatorList.add(scoreUpdater.wouldBeUniformReplanner ?
			// (byte) 1 : 0);
			// this.expectedScoreChangeList.add(this.personId2utilityChange.get(driverId));
			// this.deltaForUniformReplanningList.add(uniformScoreUpdater.getDeltaForUniformReplanning());

			this.individualReplanningResults.add(new IndividualReplanningResult(uniformScoreUpdater.getCriticalDelta(),
					this.personId2utilityChange.get(driverId), replanner, scoreUpdater.wouldBeUniformReplanner,
					scoreUpdater.wouldBeGreedyReplanner));
		}

		this.shareOfScoreImprovingReplanners = ((double) scoreImprovingReplanners) / allPersonIdsShuffled.size();

		this.uniformReplannerShare = ((double) uniformReplanners) / allPersonIdsShuffled.size();

		// this.replanningSignalAKF = new Vector(this.actualReplanIndicator).akf((int)
		// (2.0 / this.lambda)).asList();

		return replanners;
	}
}
