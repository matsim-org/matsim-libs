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

import java.util.Map;

import optdyts.DecisionVariable;

/**
 * A data container representing the properties of a surrogate solution.
 * 
 * @author Gunnar Flötteröd
 */
class SurrogateSolutionProperties {

	// -------------------- (CONSTANT) MEMBERS --------------------

	private final Map<? extends DecisionVariable, Double> decisionVariable2alphaSum;

	// private final double gap2;

	private final double interpolatedObjectiveFunctionValue;

	private final double interpolatedFromStateEuclideanNorm;

	// private final double regularizationScale;

	private final double equivalentAveragingIterations;

	private final double absoluteConvergenceGap;

	// -------------------- CONSTRUCTION --------------------

	SurrogateSolutionProperties(
			final Map<? extends DecisionVariable, Double> decisionVariable2alphaSum,
			final double interpolatedObjectiveFunctionValue,
			final double interpolatedFromStateEuclideanNorm,
			final double equivalentAveragingIterations,
			final double absoluteConvergenceGap) {
		this.decisionVariable2alphaSum = decisionVariable2alphaSum;
		this.interpolatedObjectiveFunctionValue = interpolatedObjectiveFunctionValue;
		this.interpolatedFromStateEuclideanNorm = interpolatedFromStateEuclideanNorm;
		this.equivalentAveragingIterations = equivalentAveragingIterations;
		this.absoluteConvergenceGap = absoluteConvergenceGap;
	}

	// SurrogateSolutionProperties(final List<Transition<U>> transitions,
	// final Vector alphas, final Double gap2,
	// final double regularizationScale,
	// final double equivalentAveragingIterations,
	// final double absoluteConvergenceGap) {
	//
	// /*
	// * (1) Compute alpha summaries per decision variable.
	// */
	// this.decisionVariable2alphaSum = new LinkedHashMap<U, Double>();
	// for (int i = 0; i < transitions.size(); i++) {
	// final U decisionVariable = transitions.get(i).getDecisionVariable();
	// final Double alphaSumSoFar = this.decisionVariable2alphaSum
	// .get(decisionVariable);
	// this.decisionVariable2alphaSum.put(
	// decisionVariable,
	// (alphaSumSoFar == null ? 0.0 : alphaSumSoFar)
	// + alphas.get(i));
	// }
	//
	// /*
	// * (2) Interpolate objective function value.
	// */
	// double tmpInterpolObjFctVal = 0;
	// double tmpInterpolFromStateNorm = 0;
	// for (int i = 0; i < transitions.size(); i++) {
	// tmpInterpolObjFctVal += alphas.get(i)
	// * transitions.get(i).getObjectiveFunctionValue();
	// tmpInterpolFromStateNorm += alphas.get(i)
	// * transitions.get(i).getFromStateEuclideanNorm();
	// }
	// this.interpolatedObjectiveFunctionValue = tmpInterpolObjFctVal;
	// this.interpolatedFromStateEuclideanNorm = tmpInterpolFromStateNorm;
	//
	// /*
	// * (3) Take over further parameters.
	// */
	// // this.gap2 = gap2;
	// // this.regularizationScale = regularizationScale;
	// this.equivalentAveragingIterations = equivalentAveragingIterations;
	// this.absoluteConvergenceGap = absoluteConvergenceGap;
	// }

	// -------------------- GETTERS --------------------

	Double getAlphaSum(final DecisionVariable decisionVariable) {
		return this.decisionVariable2alphaSum.get(decisionVariable);
	}

	// double getGap2() {
	// return this.gap2;
	// }

	double getInterpolatedObjectiveFunctionValue() {
		return this.interpolatedObjectiveFunctionValue;
	}

	double getInterpolatedFromStateEuclideanNorm() {
		return this.interpolatedFromStateEuclideanNorm;
	}

	// double getRegularizationScale() {
	// return this.regularizationScale;
	// }

	double getEquivalentAveragingIterations() {
		return this.equivalentAveragingIterations;
	}

	double getAbsoluteConvergenceGap() {
		return this.absoluteConvergenceGap;
	}

	public boolean isConverged(final double maximumRelativeGap) {
		return (this.absoluteConvergenceGap <= maximumRelativeGap
				* this.interpolatedFromStateEuclideanNorm);
	}

}
