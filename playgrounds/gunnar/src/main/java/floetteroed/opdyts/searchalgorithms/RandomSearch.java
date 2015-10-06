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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import floetteroed.opdyts.DecisionVariable;
import floetteroed.opdyts.DecisionVariableRandomizer;
import floetteroed.opdyts.SimulatorState;
import floetteroed.opdyts.VectorBasedObjectiveFunction;
import floetteroed.opdyts.convergencecriteria.ConvergenceCriterion;
import floetteroed.opdyts.trajectorysampling.TrajectorySampler;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class RandomSearch {

	// -------------------- CONSTANTS --------------------

	private final Simulator simulator;

	private final DecisionVariableRandomizer randomizer;

	private final ConvergenceCriterion convergenceCriterion;

	private final TrajectorySamplingSelfTuner selfTuner;

	private final int maxIterations;

	private final int maxTransitions;

	private final int populationSize;

	private final Random rnd;

	private final boolean interpolate;

	private final boolean keepBestSolution;

	private final VectorBasedObjectiveFunction objectiveFunction;

	private final int maxMemoryLength;

	// -------------------- MEMBERS --------------------

	private List<DecisionVariable> bestDecisionVariables = new ArrayList<DecisionVariable>();

	private List<Double> bestObjectiveFunctionValues = new ArrayList<Double>();

	private List<Integer> transitionEvaluations = new ArrayList<Integer>();

	private List<Double> interpolatedObjectiveFunctionValueWeights = new ArrayList<Double>();

	private List<Double> equilibriumGapWeights = new ArrayList<Double>();

	private List<Double> uniformityWeights = new ArrayList<Double>();

	private List<Double> offsets = new ArrayList<Double>();

	// -------------------- CONSTRUCTION --------------------

	public RandomSearch(
			final Simulator system,
			final DecisionVariableRandomizer randomizer,
			final ConvergenceCriterion convergenceCriterion,
			final TrajectorySamplingSelfTuner selfTuner,
			final int maxIterations, final int maxTransitions,
			final int populationSize, final Random rnd,
			final boolean interpolate, final boolean keepBestSolution,
			final VectorBasedObjectiveFunction objectiveFunction,
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
		this.objectiveFunction = objectiveFunction;
		this.maxMemoryLength = maxMemoryLength;
	}

	// -------------------- IMPLEMENTATION --------------------

	public void run() {

		int transitions = 0;

		DecisionVariable bestDecisionVariable = this.randomizer
				.newRandomDecisionVariable();
		Double bestObjectiveFunctionValue = null;
		SimulatorState newInitialState = null;

		for (int it = 0; it < this.maxIterations
				&& transitions < this.maxTransitions; it++) {

			System.out.println("Iteration " + (it + 1) + " of "
					+ this.maxIterations + ", transitions " + transitions
					+ " of " + this.maxTransitions);

			this.interpolatedObjectiveFunctionValueWeights.add(1.0);
			this.equilibriumGapWeights.add(this.selfTuner
					.getEquilibriumGapWeight());
			this.uniformityWeights.add(this.selfTuner.getUniformityWeight());
			this.offsets.add(this.selfTuner.getOffset());

			final Set<DecisionVariable> candidates = new LinkedHashSet<DecisionVariable>();
			if (this.keepBestSolution) {
				candidates.add(bestDecisionVariable);
			}
			while (candidates.size() < this.populationSize) {
				candidates.add(this.randomizer
						.newRandomVariation(bestDecisionVariable));
			}

			int transitionsPerIteration = 0;
			if (this.interpolate) {
				// final SamplingStrategy samplingStrategy = new
				// FullInterpolationSamplingStrategy(
				// 1.0, this.selfTuner.getEquilibriumGapWeight(),
				// this.selfTuner.getUniformityWeight(),
				// this.objectiveFunction);

				final TrajectorySampler sampler = new TrajectorySampler(
						candidates, this.objectiveFunction,
						this.convergenceCriterion, this.rnd,
						this.selfTuner.getEquilibriumGapWeight(),
						this.selfTuner.getUniformityWeight());
				sampler.setMaxMemoryLength(this.maxMemoryLength);

				newInitialState = this.simulator.run(sampler, newInitialState);
				bestDecisionVariable = sampler.getConvergedDecisionVariables()
						.iterator().next();
				bestObjectiveFunctionValue = sampler
						.getFinalObjectiveFunctionValue(bestDecisionVariable);
				transitionsPerIteration = sampler.getTotalTransitionCnt();

				try {
					this.selfTuner.registerSamplingStageSequence(
							sampler.getSamplingStages(),
							bestObjectiveFunctionValue,
							sampler.getInitialGradientNorm(),
							bestDecisionVariable);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			} else {
				final SimulatorState thisRoundsInitialState = newInitialState;

				bestDecisionVariable = null;
				bestObjectiveFunctionValue = Double.POSITIVE_INFINITY;

				for (DecisionVariable candidate : candidates) {

					// final SamplingStrategy samplingStrategy = new
					// FullInterpolationSamplingStrategy(
					// 1.0, 0.0, 0.0, this.objectiveFunction);

					final Set<DecisionVariable> singletonCandidates = new LinkedHashSet<DecisionVariable>();
					singletonCandidates.add(candidate);
					final TrajectorySampler sampler = new TrajectorySampler(
							singletonCandidates, this.objectiveFunction,
							this.convergenceCriterion, this.rnd,
							this.selfTuner.getEquilibriumGapWeight(),
							this.selfTuner.getUniformityWeight());

					final SimulatorState candidateInitialState = this.simulator
							.run(sampler, thisRoundsInitialState);
					final DecisionVariable candidateDecisionVariable = sampler
							.getConvergedDecisionVariables().iterator().next();
					final double candidateObjectiveFunctionValue = sampler
							.getFinalObjectiveFunctionValue(candidateDecisionVariable);
					if (candidateObjectiveFunctionValue < bestObjectiveFunctionValue) {
						bestDecisionVariable = candidateDecisionVariable;
						bestObjectiveFunctionValue = candidateObjectiveFunctionValue;
						newInitialState = candidateInitialState;
					}
					transitionsPerIteration += sampler.getTotalTransitionCnt();
				}
			}

			this.bestDecisionVariables.add(bestDecisionVariable);
			this.bestObjectiveFunctionValues.add(bestObjectiveFunctionValue);
			this.transitionEvaluations.add(transitionsPerIteration);

			transitions += transitionsPerIteration;
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
		return Collections.unmodifiableList(this.uniformityWeights);
	}

	public List<Double> getOffsetsView() {
		return Collections.unmodifiableList(this.offsets);
	}
}
