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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import optdyts.DecisionVariable;
import floetteroed.utilities.math.Vector;

/**
 * A data container representing the properties of a surrogate solution.
 * 
 * @author Gunnar Flötteröd
 * 
 * @param <U>
 *            the decision variable type
 */
class SurrogateSolutionProperties<U extends DecisionVariable> {

	// -------------------- (CONSTANT) MEMBERS --------------------

	private final Map<U, Double> decisionVariable2alphaSum;

	private final double estimatedExpectedGap2;

	private final double interpolatedObjectiveFunctionValue;

	// -------------------- CONSTRUCTION --------------------

	SurrogateSolutionProperties(final List<Transition<U>> transitions,
			final Vector alphas, final Double estimatedExpectedGap2) {

		/*
		 * (1) Compute alpha summaries per decision variable.
		 */

		this.decisionVariable2alphaSum = new LinkedHashMap<U, Double>();
		for (int i = 0; i < transitions.size(); i++) {
			final U decisionVariable = transitions.get(i).getDecisionVariable();
			final Double alphaSumSoFar = this.decisionVariable2alphaSum
					.get(decisionVariable);
			this.decisionVariable2alphaSum.put(
					decisionVariable,
					(alphaSumSoFar == null ? 0.0 : alphaSumSoFar)
							+ alphas.get(i));
		}

		/*
		 * (2) Interpolate objective function value.
		 */
		double tmpInterpolObjFctVal = 0;
		for (int i = 0; i < transitions.size(); i++) {
			tmpInterpolObjFctVal += alphas.get(i)
					* transitions.get(i).getObjectiveFunctionValue();
		}
		this.interpolatedObjectiveFunctionValue = tmpInterpolObjFctVal;

		/*
		 * (3) Take over further parameters.
		 */
		this.estimatedExpectedGap2 = estimatedExpectedGap2;
	}

	// -------------------- GETTERS --------------------

	Double getAlphaSum(final U decisionVariable) {
		return this.decisionVariable2alphaSum.get(decisionVariable);
	}

	double getEstimatedExpectedGap2() {
		return this.estimatedExpectedGap2;
	}

	double getInterpolatedObjectiveFunctionValue() {
		return this.interpolatedObjectiveFunctionValue;
	}
}
