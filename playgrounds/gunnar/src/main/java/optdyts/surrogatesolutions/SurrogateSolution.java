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
import optdyts.SimulatorState;
import floetteroed.utilities.math.Vector;

/**
 * A surrogate solution represents a convex combination of stationary simulator
 * states that are compatible with a set of Transition instances. In a way, an
 * approximate equilibrium state predictor.
 * 
 * @author Gunnar Flötteröd
 * 
 */
public class SurrogateSolution<X extends SimulatorState<X>, U extends DecisionVariable> {

	// -------------------- MEMBERS --------------------

	// PARAMETERS

	private final double convergenceNoiseVarianceScale;

	private final double transitionNoiseVarianceScale;

	// DATA TO BE PROCESSED

	private Map<U, TransitionSequence<X, U>> decisionVariable2transitionSequence = new LinkedHashMap<U, TransitionSequence<X, U>>();

	// RESULTS

	private SurrogateSolutionProperties<X, U> properties = null;

	// -------------------- CONSTRUCTION --------------------

	public SurrogateSolution(final double convergenceNoiseVarianceScale,
			final double transitionNoiseVarianceScale) {
		this.convergenceNoiseVarianceScale = convergenceNoiseVarianceScale;
		this.transitionNoiseVarianceScale = transitionNoiseVarianceScale;
	}

	// -------------------- GETTERS --------------------

	public boolean hasProperties() {
		return (this.properties != null);
	}

	public int size() {
		return this.decisionVariable2transitionSequence.size();
	}

	public Double getEstimatedExpectedGap2() {
		return this.properties.getEstimatedExpectedGap2();
	}

	public X getEquilibriumState() {
		return this.properties.getState();
	}

	public Map<U, Double> getDecisionVariable2alphaSum() {
		return this.properties.getDecisionVariable2alphaSum();
	}

	// TODO experimental, should be removed.
	public Map<U, TransitionSequence<X, U>> getDecisionVariable2TransitionSequence() {
		return this.decisionVariable2transitionSequence;
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

	double getAverageLastStateNorm2() {
		double result = 0;
		for (Map.Entry<U, TransitionSequence<X, U>> entry : this.decisionVariable2transitionSequence
				.entrySet()) {
			final double alpha = (this.hasProperties() ? this
					.getDecisionVariable2alphaSum().get(entry.getKey())
					: (1.0 / this.size()));
			final Vector lastStateVector = entry.getValue().getLastState()
					.getReferenceToVectorRepresentation();
			result += alpha * lastStateVector.innerProd(lastStateVector);
		}
		return result;
	}

	public double getConvergenceNoiseVariance() {
		return this.convergenceNoiseVarianceScale
				* this.getAverageLastStateNorm2();
	}

	public double getTransitionNoiseVariance() {
		return this.transitionNoiseVarianceScale
				* this.getAverageLastStateNorm2();
	}

	// TODO new
	public boolean isConverged() {
		return (this.properties.getEstimatedExpectedGap2() <= this
				.getConvergenceNoiseVariance());
	}

	// -------------------- IMPLEMENTATION --------------------

	public void addTransition(final X fromState, final U decisionVariable,
			final X toState) {
		final Transition<X, U> transition = new Transition<X, U>(fromState,
				decisionVariable, toState);
		TransitionSequence<X, U> implementationPath = this.decisionVariable2transitionSequence
				.get(transition.getDecisionVariable());
		if (implementationPath == null) {
			implementationPath = new TransitionSequence<X, U>(transition);
			this.decisionVariable2transitionSequence.put(
					transition.getDecisionVariable(), implementationPath);
		} else {
			implementationPath.addTransition(transition);
		}
		this.properties = null;
	}

	public void removeDecisionVariable(final U decisionVariable) {
		this.decisionVariable2transitionSequence.remove(decisionVariable);
		this.properties = null;
	}

	public void evaluate(final Map<U, Double> decisionVariable2alphaSum) {

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

		final List<Transition<X, U>> transitionList = new ArrayList<Transition<X, U>>();
		for (TransitionSequence<X, U> implementationPath : this.decisionVariable2transitionSequence
				.values()) {
			transitionList.addAll(implementationPath.getTransitions());
		}

		/*
		 * (2) Estimate the surrogate solution.
		 */

		// TODO: Parameter shall be TRANSITIONNOISEVARIANCE
		final SurrogateSolutionEstimator<X, U> ssEstimator = new SurrogateSolutionEstimator<X, U>(
				this.getTransitionNoiseVariance());

		if (decisionVariable2alphaSum == null) {
			this.properties = ssEstimator.computeProperties(transitionList);
		} else {
			final List<Double> initialAlphas = new ArrayList<Double>();
			for (Transition<X, U> transition : transitionList) {
				final U decisionVariable = transition.getDecisionVariable();
				initialAlphas.add(decisionVariable2alphaSum
						.get(decisionVariable)
						/ this.decisionVariable2transitionSequence.get(
								decisionVariable).size());
			}
			this.properties = ssEstimator.computeProperties(transitionList,
					initialAlphas);
		}
	}

	public void evaluate() {
		this.evaluate(null);
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
		 * (1) Create all "take-one-decision-variable-out"-subsets.
		 */

		final List<SurrogateSolution<X, U>> result = new ArrayList<SurrogateSolution<X, U>>();

		for (U takeOutDecisionVariable : this.decisionVariable2transitionSequence
				.keySet()) {

			// 1.1. Add all transition sequences but one to the new solution.

			final SurrogateSolution<X, U> newSurrogateSolution = new SurrogateSolution<X, U>(
					this.convergenceNoiseVarianceScale,
					this.transitionNoiseVarianceScale);
			newSurrogateSolution.decisionVariable2transitionSequence
					.putAll(this.decisionVariable2transitionSequence);
			newSurrogateSolution
					.removeDecisionVariable(takeOutDecisionVariable);

			// 1.2. Evaluate the new sub-surrogate solution.

			final double takeOutAlpha = this.getDecisionVariable2alphaSum()
					.get(takeOutDecisionVariable);
			if (takeOutAlpha > 0.99) {
				newSurrogateSolution.evaluate();
			} else {
				final Map<U, Double> newInitialAlphas = new LinkedHashMap<U, Double>();
				for (U decisionVariable : this.decisionVariable2transitionSequence
						.keySet()) {
					if (!decisionVariable.equals(takeOutDecisionVariable)) {
						newInitialAlphas.put(
								decisionVariable,
								this.getDecisionVariable2alphaSum().get(
										decisionVariable)
										/ (1.0 - takeOutAlpha));
					}
				}
				newSurrogateSolution.evaluate(newInitialAlphas);
			}

			result.add(newSurrogateSolution);
		}

		return result;
	}
}
