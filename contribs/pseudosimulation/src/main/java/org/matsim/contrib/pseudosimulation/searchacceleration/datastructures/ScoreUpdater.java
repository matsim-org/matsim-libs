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
package org.matsim.contrib.pseudosimulation.searchacceleration.datastructures;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.pseudosimulation.searchacceleration.AccelerationConfigGroup;

import floetteroed.utilities.DynamicData;
import floetteroed.utilities.Tuple;

/**
 * The "score" this class refers to is the anticipated change of the search
 * acceleration objective function resulting from setting a single agent's
 * (possibly space-weighted) 0/1 re-planning indicator.
 * 
 * Implements the score used in the greedy heuristic of Merz, P. and Freisleben,
 * B. (2002). "Greedy and local search heuristics for unconstrained binary
 * quadratic programming." Journal of Heuristics 8:197–213.
 * 
 * @author Gunnar Flötteröd
 * 
 * @param L
 *            the space coordinate type
 *
 */
public class ScoreUpdater<L> {

	// -------------------- MEMBERS --------------------

	private final DynamicData<L> interactionResiduals;

	private double inertiaResidual;

	private double regularizationResidual;

	private final double individualUtilityChange;

	private final SpaceTimeCounts<L> individualWeightedChanges;

	private final double scoreChangeIfZero;

	private final double scoreChangeIfOne;

	private final double greedyScoreChangeIfOne;

	private final double greedyScoreChangeIfZero;

	private boolean residualsUpdated = false;

	private final double deltaForUniformReplanning;

	private double sumOfInteractionResiduals2;

	public final boolean wouldBeUniformReplanner;

	public final boolean wouldBeGreedyReplanner;

	// -------------------- CONSTRUCTION --------------------

	public ScoreUpdater(final SpaceTimeIndicators<L> currentIndicators, final SpaceTimeIndicators<L> upcomingIndicators,
			final Map<Id<?>, Double> weights,
			final double meanLambda, final double beta, final double delta, final DynamicData<L> interactionResiduals,
			final double inertiaResidual, final double regularizationResidual, final AccelerationConfigGroup replParams,
			final double individualUtilityChange, final double totalUtilityChange, Double sumOfInteractionResiduals2) {

		this.interactionResiduals = interactionResiduals;
		this.inertiaResidual = inertiaResidual;
		this.regularizationResidual = regularizationResidual;

		this.individualUtilityChange = individualUtilityChange;

		/*
		 * One has to go beyond 0/1 indicator arithmetics in the following because the
		 * same vehicle may enter the same link multiple times during one time bin.
		 */

		this.individualWeightedChanges = new SpaceTimeCounts<L>(upcomingIndicators, weights); // replParams.getLinkWeightView());
		this.individualWeightedChanges.subtract(new SpaceTimeCounts<>(currentIndicators, weights)); // replParams.getLinkWeightView()));

		// Update the residuals.

		this.sumOfInteractionResiduals2 = sumOfInteractionResiduals2;
		sumOfInteractionResiduals2 = null; // only use the (updated) member variable!

		for (Map.Entry<Tuple<L, Integer>, Double> entry : this.individualWeightedChanges.entriesView()) {
			final L spaceObj = entry.getKey().getA();
			final int timeBin = entry.getKey().getB();
			final double weightedIndividualChange = entry.getValue();
			double oldResidual = this.interactionResiduals.getBinValue(spaceObj, timeBin);
			double newResidual = oldResidual - meanLambda * weightedIndividualChange;
			this.interactionResiduals.put(spaceObj, timeBin, newResidual);
			// this.interactionResiduals.add(spaceObj, timeBin, -meanLambda *
			// weightedIndividualChange);
			this.sumOfInteractionResiduals2 += newResidual * newResidual - oldResidual * oldResidual;
		}

		this.inertiaResidual -= (1.0 - meanLambda) * this.individualUtilityChange;

		this.regularizationResidual -= meanLambda;

		// Compute individual score terms.

		double sumOfWeightedIndividualChanges2 = 0.0;
		double sumOfWeightedIndividualChangesTimesInteractionResiduals = 0.0;

		for (Map.Entry<Tuple<L, Integer>, Double> entry : this.individualWeightedChanges.entriesView()) {
			final L spaceObj = entry.getKey().getA();
			final int timeBin = entry.getKey().getB();
			final double weightedIndividualChange = entry.getValue();
			sumOfWeightedIndividualChanges2 += weightedIndividualChange * weightedIndividualChange;
			sumOfWeightedIndividualChangesTimesInteractionResiduals += weightedIndividualChange
					* this.interactionResiduals.getBinValue(spaceObj, timeBin);
		}

		// Compute score components and score changes.

		final double interactionIfOne = this.expectedInteraction(1.0, sumOfWeightedIndividualChanges2,
				sumOfWeightedIndividualChangesTimesInteractionResiduals, this.sumOfInteractionResiduals2);
		final double interactionIfMean = this.expectedInteraction(meanLambda, sumOfWeightedIndividualChanges2,
				sumOfWeightedIndividualChangesTimesInteractionResiduals, this.sumOfInteractionResiduals2);
		final double interactionIfZero = this.expectedInteraction(0.0, sumOfWeightedIndividualChanges2,
				sumOfWeightedIndividualChangesTimesInteractionResiduals, this.sumOfInteractionResiduals2);
		final double inertiaIfOne = this.expectedInertia(1.0, individualUtilityChange, inertiaResidual);
		final double inertiaIfMean = this.expectedInertia(meanLambda, individualUtilityChange, inertiaResidual);
		final double inertiaIfZero = this.expectedInertia(0.0, individualUtilityChange, inertiaResidual);
		final double regularizationIfOne = this.expectedRegularization(1.0, regularizationResidual);
		final double regularizationIfMean = this.expectedRegularization(meanLambda, regularizationResidual);
		final double regularizationIfZero = this.expectedRegularization(0.0, regularizationResidual);

		this.greedyScoreChangeIfOne = (interactionIfOne - interactionIfMean) + beta * (inertiaIfOne - inertiaIfMean);
		this.greedyScoreChangeIfZero = (interactionIfZero - interactionIfMean) + beta * (inertiaIfZero - inertiaIfMean);

		this.scoreChangeIfOne = this.greedyScoreChangeIfOne + delta * (regularizationIfOne - regularizationIfMean);
		this.scoreChangeIfZero = this.greedyScoreChangeIfZero + delta * (regularizationIfZero - regularizationIfMean);

		final double deltaInteraction = interactionIfOne - interactionIfZero;
		final double deltaInertia = inertiaIfOne - inertiaIfZero;
		final double deltaRegularization = regularizationIfOne - regularizationIfZero;

		final double deltaRegularizationWellBehaved = Math.signum(deltaRegularization)
				* Math.max(1.0, Math.abs(deltaRegularization));
		this.deltaForUniformReplanning = -(deltaInteraction + beta * deltaInertia) / deltaRegularizationWellBehaved;

		this.wouldBeUniformReplanner = (this.regularizationResidual <= -0.5);
		this.wouldBeGreedyReplanner = (this.greedyScoreChangeIfOne <= this.greedyScoreChangeIfZero);
	}

	private double expectedInteraction(final double lambda, final double sumOfWeightedIndividualChanges2,
			final double sumOfWeightedIndividualChangesTimesInteractionResiduals,
			final double sumOfInteractionResiduals2) {
		return lambda * lambda * sumOfWeightedIndividualChanges2
				+ 2.0 * lambda * sumOfWeightedIndividualChangesTimesInteractionResiduals + sumOfInteractionResiduals2;
	}

	private double expectedInertia(final double lambda, final double individualUtilityChange,
			final double inertiaResidual) {
		return (1.0 - lambda) * individualUtilityChange + inertiaResidual;
	}

	private double expectedRegularization(final double lambda, final double regularizationResidual) {
		return lambda * lambda + 2.0 * lambda * regularizationResidual
				+ regularizationResidual * regularizationResidual;
	}

	// -------------------- IMPLEMENTATION --------------------

	public void updateResiduals(final double newLambda) {
		if (this.residualsUpdated) {
			throw new RuntimeException("Residuals have already been updated.");
		}
		this.residualsUpdated = true;

		for (Map.Entry<Tuple<L, Integer>, Double> entry : this.individualWeightedChanges.entriesView()) {
			final L spaceObj = entry.getKey().getA();
			final int timeBin = entry.getKey().getB();
			final double oldResidual = this.interactionResiduals.getBinValue(spaceObj, timeBin);
			final double newResidual = oldResidual + newLambda * entry.getValue();
			this.interactionResiduals.put(spaceObj, timeBin, newResidual);
			// this.interactionResiduals.add(spaceObj, timeBin, newLambda *
			// entry.getValue());
			this.sumOfInteractionResiduals2 += newResidual * newResidual - oldResidual * oldResidual;
		}
		this.inertiaResidual += (1.0 - newLambda) * this.individualUtilityChange;
		this.regularizationResidual += newLambda;
	}

	// -------------------- GETTERS --------------------

	public double getUpdatedInertiaResidual() {
		if (!this.residualsUpdated) {
			throw new RuntimeException("Residuals have not yet updated.");
		}
		return this.inertiaResidual;
	}

	public double getUpdatedRegularizationResidual() {
		if (!this.residualsUpdated) {
			throw new RuntimeException("Residuals have not yet updated.");
		}
		return this.regularizationResidual;
	}

	public double getScoreChangeIfOne() {
		return this.scoreChangeIfOne;
	}

	public double getScoreChangeIfZero() {
		return this.scoreChangeIfZero;
	}

	public double getGreedyScoreChangeIfOne() {
		return this.greedyScoreChangeIfOne;
	}

	public double getGreedyScoreChangeIfZero() {
		return this.greedyScoreChangeIfZero;
	}

	public Double getCriticalDelta() {
		return this.deltaForUniformReplanning;
	}

	public double getUpdatedSumOfInteractionResiduals2() {
		if (!this.residualsUpdated) {
			throw new RuntimeException("Residuals have not yet updated.");
		}
		return this.sumOfInteractionResiduals2;
	}

}
