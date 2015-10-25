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

import floetteroed.opdyts.DecisionVariable;
import floetteroed.opdyts.DecisionVariableRandomizer;
import floetteroed.opdyts.ObjectBasedObjectiveFunction;
import floetteroed.opdyts.SimulatorState;
import floetteroed.opdyts.VectorBasedObjectiveFunction;
import floetteroed.opdyts.convergencecriteria.ConvergenceCriterion;
import floetteroed.opdyts.trajectorysampling.ParallelTrajectorySampler;
import floetteroed.opdyts.trajectorysampling.SingleTrajectorySampler;
import floetteroed.utilities.statisticslogging.Statistic;
import floetteroed.utilities.statisticslogging.StatisticsMultiWriter;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class RandomSearch<U extends DecisionVariable> {

	// -------------------- CONSTANTS --------------------

	private final Simulator simulator;

	private final DecisionVariableRandomizer<U> randomizer;

	private final ConvergenceCriterion convergenceCriterion;

	private final TrajectorySamplingSelfTuner selfTuner;

	private final int maxIterations;

	private final int maxTransitions;

	private final int populationSize;

	private final Random rnd;

	private final boolean interpolate;

	private final boolean keepBestSolution;

	private final ObjectBasedObjectiveFunction objectBasedObjectiveFunction;

	private final VectorBasedObjectiveFunction vectorBasedObjectiveFunction;

	private final int maxMemoryLength;

	// -------------------- MEMBERS --------------------

	private List<DecisionVariable> bestDecisionVariables = new ArrayList<DecisionVariable>();

	private List<Double> bestObjectiveFunctionValues = new ArrayList<Double>();

	private List<Integer> transitionEvaluations = new ArrayList<Integer>();

	private List<Double> interpolatedObjectiveFunctionValueWeights = new ArrayList<Double>();

	private List<Double> equilibriumGapWeights = new ArrayList<Double>();

	private List<Double> uniformityGapWeights = new ArrayList<Double>();

	private List<Double> offsets = new ArrayList<Double>();

	private StatisticsMultiWriter<RandomSearch<U>> statisticsWriter = null;

	// -------------------- CONSTRUCTION --------------------

	public RandomSearch(final Simulator system,
			final DecisionVariableRandomizer<U> randomizer,
			final ConvergenceCriterion convergenceCriterion,
			final TrajectorySamplingSelfTuner selfTuner,
			final int maxIterations, final int maxTransitions,
			final int populationSize, final Random rnd,
			final boolean interpolate, final boolean keepBestSolution,
			final VectorBasedObjectiveFunction vectorBasedObjectiveFunction,
			final int maxMemoryLength) {
		this.simulator = system;
		this.randomizer = randomizer;
		this.convergenceCriterion = convergenceCriterion;
		this.selfTuner = selfTuner;
		this.maxIterations = maxIterations;
		this.maxTransitions = maxTransitions;
		this.populationSize = populationSize;
		this.rnd = rnd;
		this.interpolate = interpolate;
		this.keepBestSolution = keepBestSolution;
		this.objectBasedObjectiveFunction = null;
		this.vectorBasedObjectiveFunction = vectorBasedObjectiveFunction;
		this.maxMemoryLength = maxMemoryLength;
	}

	public RandomSearch(final Simulator system,
			final DecisionVariableRandomizer<U> randomizer,
			final ConvergenceCriterion convergenceCriterion,
			final TrajectorySamplingSelfTuner selfTuner,
			final int maxIterations, final int maxTransitions,
			final int populationSize, final Random rnd,
			final boolean interpolate, final boolean keepBestSolution,
			final ObjectBasedObjectiveFunction objectBasedObjectiveFunction,
			final int maxMemoryLength) {
		this.simulator = system;
		this.randomizer = randomizer;
		this.convergenceCriterion = convergenceCriterion;
		this.selfTuner = selfTuner;
		this.maxIterations = maxIterations;
		this.maxTransitions = maxTransitions;
		this.populationSize = populationSize;
		this.rnd = rnd;
		this.interpolate = interpolate;
		this.keepBestSolution = keepBestSolution;
		this.objectBasedObjectiveFunction = objectBasedObjectiveFunction;
		this.vectorBasedObjectiveFunction = null;
		this.maxMemoryLength = maxMemoryLength;
	}

	// -------------------- SETTERS AND GETTERS --------------------

	public void setLogFileName(final String logFileName) {
		this.statisticsWriter = new StatisticsMultiWriter<>();
		this.statisticsWriter.addStatistic(logFileName,
				new Statistic<RandomSearch<U>>() {
					@Override
					public String label() {
						return "transition_evaluations";
					}

					@Override
					public String value(final RandomSearch<U> data) {
						return Double.toString(data.transitionEvaluations
								.get(data.transitionEvaluations.size() - 1));
					}
				});
		this.statisticsWriter.addStatistic(logFileName,
				new Statistic<RandomSearch<U>>() {
					@Override
					public String label() {
						return "best_objective_function_value";
					}

					@Override
					public String value(final RandomSearch<U> data) {
						return Double.toString(data.bestObjectiveFunctionValues
								.get(data.bestObjectiveFunctionValues.size() - 1));
					}
				});
		this.statisticsWriter.addStatistic(logFileName,
				new Statistic<RandomSearch<U>>() {
					@Override
					public String label() {
						return "equilibrium_gap_weight";
					}

					@Override
					public String value(final RandomSearch<U> data) {
						return Double.toString(data.equilibriumGapWeights
								.get(data.equilibriumGapWeights.size() - 1));
					}
				});
		this.statisticsWriter.addStatistic(logFileName,
				new Statistic<RandomSearch<U>>() {
					@Override
					public String label() {
						return "uniformity_gap_weight";
					}

					@Override
					public String value(final RandomSearch<U> data) {
						return Double.toString(data.uniformityGapWeights
								.get(data.uniformityGapWeights.size() - 1));
					}
				});
		this.statisticsWriter.addStatistic(logFileName,
				new Statistic<RandomSearch<U>>() {
					@Override
					public String label() {
						return "objective_function_offsets";
					}

					@Override
					public String value(final RandomSearch<U> data) {
						return Double.toString(data.offsets.get(data.offsets
								.size() - 1));
					}
				});

	}

	// -------------------- IMPLEMENTATION --------------------

	private int transitions = 0;

	public void run() {

		double equilibriumGapWeight = 0.0;
		double uniformityGapWeight = 0.0;
		double inertia = 0.95;

		U bestDecisionVariable = this.randomizer.newRandomDecisionVariable();
		Double bestObjectiveFunctionValue = null;
		SimulatorState newInitialState = null;

		for (int it = 0; it < this.maxIterations
				&& this.transitions < this.maxTransitions; it++) {

			System.out.println("Iteration " + (it + 1) + " of "
					+ this.maxIterations + ", transitions " + transitions
					+ " of " + this.maxTransitions);

			this.interpolatedObjectiveFunctionValueWeights.add(1.0);
			this.equilibriumGapWeights.add(equilibriumGapWeight);
			this.uniformityGapWeights.add(uniformityGapWeight);
			// this.equilibriumGapWeights.add(this.selfTuner
			// .getEquilibriumGapWeight());
			// this.uniformityGapWeights.add(this.selfTuner.getUniformityWeight());
			this.offsets.add(this.selfTuner.getOffset());

			final Set<U> candidates = new LinkedHashSet<U>();
			if (this.keepBestSolution) {
				candidates.add(bestDecisionVariable);
			}
			while (candidates.size() < this.populationSize) {
				candidates.add(this.randomizer
						.newRandomVariation(bestDecisionVariable));
			}

			int transitionsPerIteration = 0;
			U newBestDecisionVariable;
			double newBestObjectiveFunctionValue;
			if (this.interpolate) {

				final ParallelTrajectorySampler<U> sampler;
				if (this.objectBasedObjectiveFunction != null) {
					sampler = new ParallelTrajectorySampler<>(candidates,
							this.objectBasedObjectiveFunction,
							this.convergenceCriterion, this.rnd,
							equilibriumGapWeight, uniformityGapWeight);
					// this.selfTuner.getEquilibriumGapWeight(),
					// this.selfTuner.getUniformityWeight());
				} else {
					sampler = new ParallelTrajectorySampler<>(candidates,
							this.vectorBasedObjectiveFunction,
							this.convergenceCriterion, this.rnd,
							equilibriumGapWeight, uniformityGapWeight);
					// this.selfTuner.getEquilibriumGapWeight(),
					// this.selfTuner.getUniformityWeight());
				}
				sampler.setMaxMemoryLength(this.maxMemoryLength);

				newInitialState = this.simulator.run(sampler, newInitialState);
				newBestDecisionVariable = sampler
						.getConvergedDecisionVariables().iterator().next();
				newBestObjectiveFunctionValue = sampler
						.getFinalObjectiveFunctionValue(newBestDecisionVariable);
				transitionsPerIteration = sampler.getTotalTransitionCnt();

				// this.selfTuner.registerSamplingStageSequence(
				// sampler.getInitialObjectiveFunctionValue(),
				// sampler.getInitialEquilibriumGap(),
				// sampler.getInitialUniformityGap(),
				// newBestObjectiveFunctionValue, newBestDecisionVariable);

				// this.selfTuner.registerSamplingStageSequence(
				// sampler.getSamplingStages(),
				// newBestObjectiveFunctionValue,
				// sampler.getInitialGradientNorm(),
				// newBestDecisionVariable);

				final UpperBoundTuner newTuner = new UpperBoundTuner();
				newTuner.registerSamplingStageSequence(
						sampler.getSamplingStages(),
						newBestObjectiveFunctionValue,
						sampler.getInitialGradientNorm(),
						newBestDecisionVariable);

				equilibriumGapWeight = inertia * equilibriumGapWeight
						+ (1.0 - inertia) * newTuner.equilGapWeight;
				uniformityGapWeight *= inertia * uniformityGapWeight
						+ (1.0 - inertia) * newTuner.unifGapWeight;

			} else {

				final SimulatorState thisRoundsInitialState = newInitialState;

				newBestDecisionVariable = null;
				newBestObjectiveFunctionValue = Double.POSITIVE_INFINITY;

				for (U candidate : candidates) {
					this.convergenceCriterion.reset();
					final SingleTrajectorySampler<U> singleSampler;
					if (this.objectBasedObjectiveFunction != null) {
						singleSampler = new SingleTrajectorySampler<>(
								candidate, this.objectBasedObjectiveFunction,
								this.convergenceCriterion);
					} else {
						singleSampler = new SingleTrajectorySampler<>(
								candidate, this.vectorBasedObjectiveFunction,
								this.convergenceCriterion);
					}
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

				// for (DecisionVariable candidate : candidates) {
				//
				// final Set<DecisionVariable> singletonCandidates = new
				// LinkedHashSet<DecisionVariable>();
				// singletonCandidates.add(candidate);
				//
				// final ParallelTrajectorySampler sampler;
				// if (this.objectBasedObjectiveFunction != null) {
				// sampler = new ParallelTrajectorySampler(
				// singletonCandidates,
				// this.objectBasedObjectiveFunction,
				// this.convergenceCriterion, this.rnd,
				// this.selfTuner.getEquilibriumGapWeight(),
				// this.selfTuner.getUniformityWeight());
				// } else {
				// sampler = new ParallelTrajectorySampler(
				// singletonCandidates,
				// this.vectorBasedObjectiveFunction,
				// this.convergenceCriterion, this.rnd,
				// this.selfTuner.getEquilibriumGapWeight(),
				// this.selfTuner.getUniformityWeight());
				// }
				//
				// final SimulatorState candidateInitialState = this.simulator
				// .run(sampler, thisRoundsInitialState);
				// final DecisionVariable candidateDecisionVariable = sampler
				// .getConvergedDecisionVariables().iterator().next();
				// final double candidateObjectiveFunctionValue = sampler
				// .getFinalObjectiveFunctionValue(candidateDecisionVariable);
				// if (candidateObjectiveFunctionValue <
				// bestObjectiveFunctionValue) {
				// bestDecisionVariable = candidateDecisionVariable;
				// bestObjectiveFunctionValue = candidateObjectiveFunctionValue;
				// newInitialState = candidateInitialState;
				// }
				// transitionsPerIteration += sampler.getTotalTransitionCnt();
				// }
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

			if (this.statisticsWriter != null) {
				this.statisticsWriter.writeToFile(this);
			}
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
