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
package optdyts.surrogatesolutions;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import optdyts.DecisionVariable;
import optdyts.ObjectiveFunction;
import optdyts.SimulatorState;

/**
 * A surrogate solution represents a convex combination of stationary simulator
 * states that are compatible with a set of Transition instances. In a way, an
 * approximate equilibrium state predictor.
 * 
 * @author Gunnar Flötteröd
 * 
 */
public class SurrogateSolution<X extends SimulatorState, U extends DecisionVariable> {

	// -------------------- MEMBERS --------------------

	// PARAMETERS

	private final ObjectiveFunction<X> objectiveFunction;

	private final int minimumAverageIterations;

	private final int maximumAverageIterations;

	private final double maximumRelativeGap;

	// DATA TO BE PROCESSED

	private Map<U, TransitionSequence<X, U>> decisionVariable2transitionSequence = new LinkedHashMap<U, TransitionSequence<X, U>>();

	// PROPERTIES

	private SurrogateSolutionProperties properties = null;

	// -------------------- CONSTRUCTION --------------------

	public SurrogateSolution(final ObjectiveFunction<X> objectiveFunction,
			final int minimumAverageIterations,
			final int maximumAverageIterations, final double maximumRelativeGap) {
		this.objectiveFunction = objectiveFunction;
		this.minimumAverageIterations = minimumAverageIterations;
		this.maximumAverageIterations = maximumAverageIterations;
		this.maximumRelativeGap = maximumRelativeGap;
	}

	// -------------------- GETTERS --------------------

	public boolean hasProperties() {
		return (this.properties != null);
	}

	public int size() {
		return this.decisionVariable2transitionSequence.size();
	}

	public Double getAbsoluteConvergenceGap() {
		return this.properties.getAbsoluteConvergenceGap();
	}

	public Double getInterpolatedFromStateEuclideanNorm() {
		return this.properties.getInterpolatedFromStateEuclideanNorm();
	}

	public double getAlphaSum(final U decisionVariable) {
		final Double result = this.properties.getAlphaSum(decisionVariable);
		if (result != null) {
			return result;
		} else {
			return 0.0;
		}
	}

	public X getLastState(final U decisionVariable) {
		return this.decisionVariable2transitionSequence.get(decisionVariable)
				.getLastState();
	}

	public double getInterpolatedObjectiveFunctionValue() {
		return this.properties.getInterpolatedObjectiveFunctionValue();
	}

	public double getEquivalentAveragingIterations() {
		return this.properties.getEquivalentAveragingIterations();
	}

	public Set<U> getLeastEvaluatedDecisionVariables() {
		final Set<U> result = new LinkedHashSet<U>();
		int minEvaluationCount = Integer.MAX_VALUE;
		for (Map.Entry<U, TransitionSequence<X, U>> entry : this.decisionVariable2transitionSequence
				.entrySet()) {
			final int candidateEvaluationCount = entry.getValue().size();
			if (candidateEvaluationCount <= minEvaluationCount) {
				if (candidateEvaluationCount < minEvaluationCount) {
					result.clear();
					minEvaluationCount = candidateEvaluationCount;
				}
				final U candidateDecisionVariable = entry.getKey();
				result.add(candidateDecisionVariable);
			}
		}
		return result;
	}

	public double getMaximumRelativeGap() {
		return this.maximumRelativeGap;
	}

	public boolean isConverged() {
		if (this.hasProperties()) {
			return this.properties.isConverged(this.maximumRelativeGap);
		} else {
			return false;
		}
	}

	public Set<U> getDecisionVariables() {
		return this.decisionVariable2transitionSequence.keySet();
	}

	// -------------------- IMPLEMENTATION --------------------

	public void addTransition(final X fromState, final U decisionVariable,
			final X toState) {
		final double objectiveFunctionValue = this.objectiveFunction
				.evaluateState(toState);
		TransitionSequence<X, U> transitionSequence = this.decisionVariable2transitionSequence
				.get(decisionVariable);
		if (transitionSequence == null) {
			transitionSequence = new TransitionSequence<X, U>(fromState,
					decisionVariable, toState, objectiveFunctionValue);
			this.decisionVariable2transitionSequence.put(decisionVariable,
					transitionSequence);
		} else {
			transitionSequence.addTransition(fromState, decisionVariable,
					toState, objectiveFunctionValue);
		}
		transitionSequence.shrinkToMaximumLength(this.maximumAverageIterations);
		this.properties = null;
	}

	public void removeDecisionVariable(final U decisionVariable) {
		this.decisionVariable2transitionSequence.remove(decisionVariable);
		this.properties = null;
	}

	public void evaluate() {

		/*
		 * (0) Ensure that there are enough observed transitions for an
		 * evaluation.
		 */

		if (this.decisionVariable2transitionSequence.size() < 1) {
			throw new IllegalArgumentException("There are only "
					+ this.decisionVariable2transitionSequence.size()
					+ " decision variables registered.");
		}

		/*
		 * (1) Create input parameters for SurrogateSolutionEstimator.
		 */

		final List<Transition<U>> transitionList = new ArrayList<Transition<U>>();
		for (TransitionSequence<X, U> implementationPath : this.decisionVariable2transitionSequence
				.values()) {
			transitionList.addAll(implementationPath.getTransitions());
		}

		/*
		 * (2) Compute surrogate solution.
		 */

		if (transitionList.size() < this.minimumAverageIterations) {

			this.properties = null;

		} else {

			final double absoluteMaximumGap;
			if (this.properties == null) {
				double sumOfFromStateEuclideanNorm = 0;
				for (Transition<U> transition : transitionList) {
					sumOfFromStateEuclideanNorm += transition
							.getFromStateEuclideanNorm();
				}
				absoluteMaximumGap = this.maximumRelativeGap
						* (sumOfFromStateEuclideanNorm / transitionList.size());
			} else {
				absoluteMaximumGap = this.maximumRelativeGap
						* this.properties
								.getInterpolatedFromStateEuclideanNorm();
			}
			final double regularizationWeight = 0.01
					* this.minimumAverageIterations
					* Math.pow(absoluteMaximumGap, 2.0);

			this.properties = SurrogateSolutionEstimator.computeProperties(
					transitionList, this.minimumAverageIterations,
					regularizationWeight);
		}
	}

	public List<SurrogateSolution<X, U>> newEvaluatedSubsets() {

		/*
		 * (0) Check if there is anything to do at all.
		 */

		if (this.decisionVariable2transitionSequence.size() < 2) {
			return new ArrayList<SurrogateSolution<X, U>>(0);
			// -------------------------------------------------------------
		}

		/*
		 * (1) Go through all "take-one-decision-variable-out"-subsets.
		 */

		final List<SurrogateSolution<X, U>> result = new ArrayList<SurrogateSolution<X, U>>();

		for (U takeOutDecisionVariable : this.decisionVariable2transitionSequence
				.keySet()) {

			// 1.1. Add all transition sequences but one to the new solution.

			final SurrogateSolution<X, U> newSurrogateSolution = new SurrogateSolution<X, U>(
					// this.convergenceNoiseVarianceScale,
					this.objectiveFunction, this.minimumAverageIterations,
					this.maximumAverageIterations, this.maximumRelativeGap);
			newSurrogateSolution.decisionVariable2transitionSequence
					.putAll(this.decisionVariable2transitionSequence);
			newSurrogateSolution
					.removeDecisionVariable(takeOutDecisionVariable);

			// 1.2. Evaluate the new subset surrogate solution.

			newSurrogateSolution.evaluate();
			result.add(newSurrogateSolution);
		}

		return result;
	}
}
