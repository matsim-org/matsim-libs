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
import java.util.List;
import java.util.Map;

import optdyts.DecisionVariable;
import optdyts.SimulatorState;
import floetteroed.utilities.math.Vector;

/**
 * A data container representing the properties of a surrogate solution.
 * 
 * @author Gunnar Flötteröd
 * 
 * @param <X>
 *            the simulator state type
 * @param <U>
 *            the decision variable type
 */
class SurrogateSolutionProperties<X extends SimulatorState<X>, U extends DecisionVariable> {

	// -------------------- (CONSTANT) MEMBERS --------------------

	private final X state;

	private final Map<U, Double> decisionVariable2alphaSum;

	private final Double estimatedExpectedGap2;

	// -------------------- CONSTRUCTION --------------------

	SurrogateSolutionProperties(final List<Transition<X, U>> transitions,
			final Vector alphas, final Double estimatedExpectedGap2) {

		/*
		 * (1) Compute surrogate solution state.
		 */

		final List<X> states = new ArrayList<X>(transitions.size());
		final List<Double> weights = new ArrayList<Double>(transitions.size());
		for (int i = 0; i < transitions.size(); i++) {
			// combining toStates because only those contain deep state copies
			states.add(transitions.get(i).getToState());
			weights.add(alphas.get(i));
		}
		this.state = transitions.get(0).getToState().deepCopy();
		this.state.takeOverConvexCombination(states, weights);

		/*
		 * (2) Compute alpha summaries per decision variable.
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
		 * (3) Take over further parameters.
		 */
		this.estimatedExpectedGap2 = estimatedExpectedGap2;
	}

	// -------------------- GETTERS --------------------

	X getState() {
		return this.state;
	}

	Map<U, Double> getDecisionVariable2alphaSum() {
		return this.decisionVariable2alphaSum;
	}

	Double getEstimatedExpectedGap2() {
		return this.estimatedExpectedGap2;
	}

}
