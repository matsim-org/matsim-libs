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

	// CONSTANTS

	private final double regularizationScale = 1e-3;

	// PARAMETERS

	// private final double convergenceNoiseVarianceScale;

	private final ObjectiveFunction<X> objectiveFunction;

	private final int minimumAverageIterations;

	private final double maximumRelativeGap;

	// DATA TO BE PROCESSED

	private Map<U, TransitionSequence<X, U>> decisionVariable2transitionSequence = new LinkedHashMap<U, TransitionSequence<X, U>>();

	// PROPERTIES

	private SurrogateSolutionProperties properties = null;

	// -------------------- CONSTRUCTION --------------------

	public SurrogateSolution(
			// final double convergenceNoiseVarianceScale,
			final ObjectiveFunction<X> objectiveFunction,
			final int minimumAverageIterations, final double maximumRelativeGap) {
		// this.convergenceNoiseVarianceScale = convergenceNoiseVarianceScale;
		this.objectiveFunction = objectiveFunction;
		this.minimumAverageIterations = minimumAverageIterations;
		this.maximumRelativeGap = maximumRelativeGap;
	}

	// -------------------- GETTERS --------------------

	public boolean hasProperties() {
		return (this.properties != null);
	}

	public int size() {
		return this.decisionVariable2transitionSequence.size();
	}

	// public Double getGap2() {
	// return this.properties.getGap2();
	// }

	public Double getAbsoluteConvergenceGap() {
		return this.properties.getAbsoluteConvergenceGap();
	}

	public Double getInterpolatedFromStateEuclideanNorm() {
		return this.properties.getInterpolatedFromStateEuclideanNorm();
	}

	public Double getAlphaSum(final U decisionVariable) {
		return this.properties.getAlphaSum(decisionVariable);
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

	// double getAverageLastStateNorm2() {
	// double result = 0;
	// for (Map.Entry<U, TransitionSequence<X, U>> entry :
	// this.decisionVariable2transitionSequence
	// .entrySet()) {
	// final double alpha = (this.hasProperties() ? this.getAlphaSum(entry
	// .getKey()) : (1.0 / this.size()));
	// final Vector lastStateVector = entry.getValue().getLastState()
	// .getReferenceToVectorRepresentation();
	// result += alpha * lastStateVector.innerProd(lastStateVector);
	// }
	// return result;
	// }

	// public double getConvergenceNoiseVariance() {
	// return this.convergenceNoiseVarianceScale
	// * this.getAverageLastStateNorm2();
	// }

	// public double getRegularizationWeight() {
	// return this.properties.getRegularizationScale();
	// }

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
		this.properties = null;
	}

	public void removeDecisionVariable(final U decisionVariable) {
		this.decisionVariable2transitionSequence.remove(decisionVariable);
		this.properties = null;
	}

	public void evaluate() {
		// final Map<U, Double> decisionVariable2alphaSum,
		// final Double regularizationWeight) {

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

		// TODO init ArrayList size
		final List<Transition<U>> transitionList = new ArrayList<Transition<U>>();
		for (TransitionSequence<X, U> implementationPath : this.decisionVariable2transitionSequence
				.values()) {
			transitionList.addAll(implementationPath.getTransitions());
		}

		// final double regularizationScale;
		// if (transitionList.size() < 2) {
		// regularizationScale = 0.0;
		// } else {
		// final CrossValidationError<U> cve = new CrossValidationError<U>(
		// transitionList, alphas.copy());
		// final InitialBracket ib = new InitialBracket();
		// ib.run(0, 1, cve);
		// System.out.println("Bracket: [" + ib.ax + ", " + ib.bx + ", " + ib.cx
		// + "] with cross-validation residuals [" + ib.fa + ", " + ib.fb
		// + ", " + ib.fc + "]");
		// final Brent brent = new Brent();
		// brent.run(ib.ax, ib.bx, ib.cx, cve);
		// System.out.println("Minimum at " + brent.xmin
		// + " with cross-validation residual " + brent.fmin);
		// regularizationScale = brent.xmin;
		// }

		/*
		 * (2) Compute surrogate solution.
		 */

		if (transitionList.size() < this.minimumAverageIterations) {

			this.properties = null;

			// this.properties = SurrogateSolutionEstimator.computeProperties(
			// transitionList, this.zeroRegularizationScale, 0.0);

		} else {

			// final Vector initialAlphas;
			// if (decisionVariable2alphaSum != null) {
			// final List<Double> initialAlphaList = new ArrayList<Double>();
			// for (Transition<U> transition : transitionList) {
			// final U decisionVariable = transition.getDecisionVariable();
			// initialAlphaList.add(decisionVariable2alphaSum
			// .get(decisionVariable)
			// / this.decisionVariable2transitionSequence.get(
			// decisionVariable).size());
			// }
			// initialAlphas = new Vector(initialAlphaList);
			// } else {
			// initialAlphas = new Vector(transitionList.size());
			// initialAlphas.fill(1.0 / transitionList.size());
			// }

			// final double useThisRegularizationWeight;
			// if (regularizationWeight != null) {
			// useThisRegularizationWeight = regularizationWeight;
			// } else {
			// final CrossValidationError<U> cve = new CrossValidationError<U>(
			// transitionList, initialAlphas);
			// final InitialBracket ib = new InitialBracket();
			// final double xa = 0.00;
			// final double xb = 1.00;
			// final double fa = cve.evaluate(xa);
			// final double fb = cve.evaluate(xb);
			// if (fa > fb) {
			// ib.run(xa, xb, fa, fb, cve);
			// System.out.println("Bracket: [" + ib.ax + ", " + ib.bx
			// + ", " + ib.cx
			// + "] with cross-validation residuals [" + ib.fa
			// + ", " + ib.fb + ", " + ib.fc + "]");
			// final Brent brent = new Brent();
			// brent.run(ib.ax, ib.bx, ib.cx, cve);
			// System.out.println("Minimum at " + brent.xmin
			// + " with cross-validation residual " + brent.fmin);
			// useThisRegularizationWeight = brent.xmin;
			// } else {
			// useThisRegularizationWeight = 0.0;
			// }
			// }

			this.properties = SurrogateSolutionEstimator.computeProperties(
					transitionList, this.regularizationScale,
					this.minimumAverageIterations);
		}

		// if (decisionVariable2alphaSum == null) {
		// this.properties = SurrogateSolutionEstimator.computeProperties(
		// transitionList, this.getTransitionNoiseVariance());
		// } else {
		// final List<Double> initialAlphas = new ArrayList<Double>();
		// for (Transition<U> transition : transitionList) {
		// final U decisionVariable = transition.getDecisionVariable();
		// initialAlphas.add(decisionVariable2alphaSum
		// .get(decisionVariable)
		// / this.decisionVariable2transitionSequence.get(
		// decisionVariable).size());
		// }
		// this.properties = SurrogateSolutionEstimator.computeProperties(
		// transitionList, new Vector(initialAlphas),
		// this.getTransitionNoiseVariance());
		// }

	}

	// TODO largely copy and paste and not at all efficient
	// public void testCrossValidation() {
	//
	// /*
	// * (1) Create input parameters for SurrogateSolutionEstimator.
	// */
	//
	// final List<Transition<U>> transitionList = new
	// ArrayList<Transition<U>>();
	// for (TransitionSequence<X, U> implementationPath :
	// this.decisionVariable2transitionSequence
	// .values()) {
	// transitionList.addAll(implementationPath.getTransitions());
	// }
	//
	// final List<Double> initialAlphas = new ArrayList<Double>(
	// transitionList.size());
	// for (int i = 0; i < transitionList.size(); i++) {
	// initialAlphas.add(1.0 / transitionList.size());
	// }
	//
	// /*
	// * (2) Estimate the surrogate solution.
	// */
	//
	// // final SurrogateSolutionEstimator<X, U> ssEstimator = new
	// // SurrogateSolutionEstimator<X, U>(
	// // this.getTransitionNoiseVariance());
	// SurrogateSolutionEstimator.testOptimalRegularization(transitionList,
	// initialAlphas, this.getAverageLastStateNorm2());
	// }

	// public void evaluate() {
	// this.evaluate(null, null);
	// }

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
					this.maximumRelativeGap);
			newSurrogateSolution.decisionVariable2transitionSequence
					.putAll(this.decisionVariable2transitionSequence);
			newSurrogateSolution
					.removeDecisionVariable(takeOutDecisionVariable);

			// 1.2. Evaluate the new subset surrogate solution.

			newSurrogateSolution.evaluate();

			// final double takeOutAlpha = this
			// .getAlphaSum(takeOutDecisionVariable);
			// if (takeOutAlpha > 0.99) {
			// newSurrogateSolution.evaluate();
			// } else {
			// final Map<U, Double> newInitialAlphas = new LinkedHashMap<U,
			// Double>();
			// for (U decisionVariable :
			// this.decisionVariable2transitionSequence
			// .keySet()) {
			// if (!decisionVariable.equals(takeOutDecisionVariable)) {
			// newInitialAlphas.put(decisionVariable,
			// this.getAlphaSum(decisionVariable)
			// / (1.0 - takeOutAlpha));
			// }
			// }
			// newSurrogateSolution.evaluate(); // newInitialAlphas, 0.0);
			// }

			result.add(newSurrogateSolution);
		}

		return result;
	}
}
