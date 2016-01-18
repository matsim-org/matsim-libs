/*
 * Opdyts - Optimization of dynamic traffic simulations
 *
 * Copyright 2015 Gunnar Flötteröd
 * 
 *
 * This file is part of Opdyts.
 *
 * Opdyts is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Opdyts is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Opdyts.  If not, see <http://www.gnu.org/licenses/>.
 *
 * contact: gunnar.floetteroed@abe.kth.se
 *
 */
package floetteroed.opdyts.searchalgorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;

import floetteroed.opdyts.DecisionVariable;
import floetteroed.opdyts.DecisionVariableRandomizer;
import floetteroed.opdyts.ObjectiveFunction;
import floetteroed.opdyts.SimulatorState;
import floetteroed.opdyts.convergencecriteria.ConvergenceCriterion;
import floetteroed.opdyts.trajectorysampling.ParallelTrajectorySampler;
import floetteroed.opdyts.trajectorysampling.SamplingStage;
import floetteroed.opdyts.trajectorysampling.SingleTrajectorySampler;
import floetteroed.utilities.statisticslogging.Statistic;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class RandomSearch<U extends DecisionVariable> {

	// -------------------- CONSTANTS --------------------

	public static final String RANDOM_SEARCH_ITERATION = "Random Search Iteration";

	private final Simulator<U> simulator;

	private final DecisionVariableRandomizer<U> randomizer;

	private final ConvergenceCriterion convergenceCriterion;

	private final int maxIterations;

	private final int maxTransitions;

	private final int populationSize;

	private final Random rnd;

	private final boolean interpolate;

	private final ObjectiveFunction objectBasedObjectiveFunction;

	private final int maxMemoryLength;

	private final Double inertia;

	// -------------------- MEMBERS --------------------

	private List<DecisionVariable> bestDecisionVariables = new ArrayList<DecisionVariable>();

	private List<Double> bestObjectiveFunctionValues = new ArrayList<Double>();

	private List<Integer> transitionEvaluations = new ArrayList<Integer>();

	private List<Double> interpolatedObjectiveFunctionValueWeights = new ArrayList<Double>();

	private List<Double> equilibriumGapWeights = new ArrayList<Double>();

	private List<Double> uniformityGapWeights = new ArrayList<Double>();

	private List<Double> offsets = new ArrayList<Double>();

	// private StatisticsMultiWriter<RandomSearch<U>> statisticsWriter = null;

	private String logFileName = null;

	// -------------------- CONSTRUCTION --------------------

	public RandomSearch(final Simulator<U> simulator,
			final DecisionVariableRandomizer<U> randomizer,
			final ConvergenceCriterion convergenceCriterion,
			final int maxIterations, final int maxTransitions,
			final int populationSize, final Random rnd,
			final boolean interpolate,
			final ObjectiveFunction objectBasedObjectiveFunction,
			final int maxMemoryLength, final Double inertia) {
		this.simulator = simulator;
		this.randomizer = randomizer;
		this.convergenceCriterion = convergenceCriterion;
		this.maxIterations = maxIterations;
		this.maxTransitions = maxTransitions;
		this.populationSize = populationSize;
		this.rnd = rnd;
		this.interpolate = interpolate;
		this.objectBasedObjectiveFunction = objectBasedObjectiveFunction;
		this.maxMemoryLength = maxMemoryLength;
		this.inertia = inertia;
	}

	// -------------------- SETTERS AND GETTERS --------------------

	public void setLogFileName(final String logFileName) {
		this.logFileName = logFileName;
	}

	// -------------------- IMPLEMENTATION --------------------

	private int transitions = 0;

	public void run() {

		double equilibriumGapWeight = 0.0;
		double uniformityGapWeight = 0.0;
		// double inertia = 0.95;

		U bestDecisionVariable = this.randomizer.newRandomDecisionVariable();
		Double bestObjectiveFunctionValue = null;
		SimulatorState newInitialState = null;

		final UpperBoundTuner2 newTuner2 = new UpperBoundTuner2();

		for (int it = 0; it < this.maxIterations
				&& this.transitions < this.maxTransitions; it++) {

			Logger.getLogger(this.getClass().getName()).info(
					"Iteration " + (it + 1) + " of " + this.maxIterations
							+ ", transitions " + this.transitions + " of "
							+ this.maxTransitions + " ====================");

			this.interpolatedObjectiveFunctionValueWeights.add(1.0);
			this.equilibriumGapWeights.add(equilibriumGapWeight);
			this.uniformityGapWeights.add(uniformityGapWeight);
			this.offsets.add(Double.NaN);

			final Set<U> candidates = new LinkedHashSet<U>();
			// if (this.keepBestSolution) {
			// candidates.add(bestDecisionVariable); // TODO experimental
			// }
			while (candidates.size() < this.populationSize) {
				candidates.addAll(this.randomizer
						.newRandomVariations(bestDecisionVariable));
			}

			int transitionsPerIteration = 0;
			U newBestDecisionVariable;
			double newBestObjectiveFunctionValue;
			if (this.interpolate) {

				final ParallelTrajectorySampler<U> sampler;
				sampler = new ParallelTrajectorySampler<>(candidates,
						this.objectBasedObjectiveFunction,
						this.convergenceCriterion, this.rnd,
						equilibriumGapWeight, uniformityGapWeight, (it > 0));
				sampler.setMaxMemoryLength(this.maxMemoryLength);

				// TODO >>>>> NEW >>>>>
				if (this.logFileName != null) {
					final int currentIt = it; // needs to be final
					sampler.addStatistic(this.logFileName,
							new Statistic<SamplingStage<U>>() {
								@Override
								public String label() {
									return RANDOM_SEARCH_ITERATION;
								}

								@Override
								public String value(final SamplingStage<U> data) {
									// TODO Auto-generated method stub
									return Integer.toString(currentIt);
								}
							});
					final Double currentBestObjectiveFunctionValue = bestObjectiveFunctionValue;
					sampler.addStatistic(this.logFileName,
							new Statistic<SamplingStage<U>>() {
								@Override
								public String label() {
									return "Best Overall Solution";
								}

								@Override
								public String value(final SamplingStage<U> data) {
									if (currentBestObjectiveFunctionValue == null) {
										return "";
									} else {
										return Double
												.toString(currentBestObjectiveFunctionValue);
									}
								}
							});
					sampler.setStandardLogFileName(this.logFileName);
				}
				// TODO <<<<< NEW <<<<<

				newInitialState = this.simulator.run(sampler, newInitialState);
				newBestDecisionVariable = sampler
						.getDecisionVariable2finalObjectiveFunctionValue()
						.keySet().iterator().next();
				newBestObjectiveFunctionValue = sampler
						.getDecisionVariable2finalObjectiveFunctionValue().get(
								newBestDecisionVariable);
				transitionsPerIteration = sampler.getTotalTransitionCnt();

				// final UpperBoundTuner newTuner = new UpperBoundTuner();
				// newTuner.registerSamplingStageSequence(
				// sampler.getSamplingStages(),
				// newBestObjectiveFunctionValue);
				newTuner2.registerSamplingStageSequence(
						sampler.getSamplingStages(),
						newBestObjectiveFunctionValue);

				if (this.inertia != null) {
					equilibriumGapWeight = this.inertia * equilibriumGapWeight
							+ (1.0 - this.inertia) * newTuner2.equilGapWeight;
					uniformityGapWeight *= this.inertia * uniformityGapWeight
							+ (1.0 - this.inertia) * newTuner2.unifGapWeight;
				} else {
					// equilibriumGapWeight = Math.max(equilibriumGapWeight,
					// newTuner.equilGapWeight);
					// uniformityGapWeight = Math.max(uniformityGapWeight,
					// newTuner.unifGapWeight);
					final double msaInertia = 1.0 - 1.0 / (1.0 + it);
					equilibriumGapWeight = msaInertia * equilibriumGapWeight
							+ (1.0 - msaInertia) * newTuner2.equilGapWeight;
					uniformityGapWeight *= msaInertia * uniformityGapWeight
							+ (1.0 - msaInertia) * newTuner2.unifGapWeight;
				}

				// TODO >>>>> NEW >>>>>
				final double msaInertia = 1.0 - 1.0 / (1.0 + it);
				equilibriumGapWeight = msaInertia * equilibriumGapWeight
						+ (1.0 - msaInertia) * sampler.v;
				uniformityGapWeight = msaInertia * uniformityGapWeight
						+ (1.0 - msaInertia) * sampler.w;
				// TODO <<<<< NEW <<<<<

			} else {

				final SimulatorState thisRoundsInitialState = newInitialState;

				newBestDecisionVariable = null;
				newBestObjectiveFunctionValue = Double.POSITIVE_INFINITY;

				for (U candidate : candidates) {
					this.convergenceCriterion.reset();
					final SingleTrajectorySampler<U> singleSampler;
					singleSampler = new SingleTrajectorySampler<>(candidate,
							this.objectBasedObjectiveFunction,
							this.convergenceCriterion);
					final SimulatorState candidateInitialState = this.simulator
							.run(singleSampler, thisRoundsInitialState);
					final double candidateObjectiveFunctionValue = singleSampler
							.getDecisionVariable2finalObjectiveFunctionValue()
							.get(candidate);
					if (candidateObjectiveFunctionValue < newBestObjectiveFunctionValue) {
						newBestDecisionVariable = candidate;
						newBestObjectiveFunctionValue = candidateObjectiveFunctionValue;
						newInitialState = candidateInitialState;
					}
					transitionsPerIteration += singleSampler
							.getTotalTransitionCnt();
				}
			}

			if (bestObjectiveFunctionValue == null
					|| newBestObjectiveFunctionValue < bestObjectiveFunctionValue) {
				bestDecisionVariable = newBestDecisionVariable;
				bestObjectiveFunctionValue = newBestObjectiveFunctionValue;
			}

			this.bestDecisionVariables.add(bestDecisionVariable);
			this.bestObjectiveFunctionValues.add(bestObjectiveFunctionValue);
			this.transitionEvaluations.add(transitionsPerIteration);
			this.transitions += transitionsPerIteration;

			// if (this.statisticsWriter != null) {
			// this.statisticsWriter.writeToFile(this);
			// }
		}
	}

	// -------------------- RESULT ACCESS --------------------

	public List<DecisionVariable> getBestDecisionVariablesView() {
		return Collections.unmodifiableList(this.bestDecisionVariables);
	}

	public List<Double> getBestObjectiveFunctionValuesView() {
		return Collections.unmodifiableList(this.bestObjectiveFunctionValues);
	}

	public List<Integer> getTransitionEvalautionsView() {
		return Collections.unmodifiableList(this.transitionEvaluations);
	}

	public List<Double> getInterpolatedObjectiveFunctionValueWeightsView() {
		return Collections
				.unmodifiableList(this.interpolatedObjectiveFunctionValueWeights);
	}

	public List<Double> getEquilibriumGapWeightsView() {
		return Collections.unmodifiableList(this.equilibriumGapWeights);
	}

	public List<Double> getUniformityWeightsView() {
		return Collections.unmodifiableList(this.uniformityGapWeights);
	}

	public List<Double> getOffsetsView() {
		return Collections.unmodifiableList(this.offsets);
	}
}
