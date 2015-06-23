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

import optdyts.DecisionVariable;
import floetteroed.utilities.math.Vector;

/**
 * Represents a simulator transition.
 * 
 * @author Gunnar Flötteröd
 * 
 * @param <U>
 *            the decision variable type
 */
class Transition<U extends DecisionVariable> {

	// -------------------- CONSTANTS --------------------

	private final U decisionVariable;

	private final Vector delta;

	private final double objectiveFunctionValue;

	private final double fromStateEuclideanNorm;

	private final double toStateEuclideanNorm;

	// -------------------- CONSTRUCTION --------------------

	/**
	 * Memorizes *references* to its parameters.
	 * 
	 * @param decisionVariable
	 *            the decision variable used in this transition
	 * @param delta
	 *            the move in vector-valued state space representing this
	 *            transition
	 * @param objectiveFunctionValue
	 *            the objective function value of the to-state of this
	 *            transition
	 */
	Transition(final U decisionVariable, final Vector delta,
			final double objectiveFunctionValue,
			final double fromStateEuclideanNorm,
			final double toStateEuclideanNorm) {

		if (decisionVariable == null) {
			throw new IllegalArgumentException("decisionVariable is null");
		}
		if (delta == null) {
			throw new IllegalArgumentException("delta is null");
		}
		if (fromStateEuclideanNorm < 0) {
			throw new IllegalArgumentException("fromStateEuclideanNorm is "
					+ fromStateEuclideanNorm + " < 0");
		}
		if (toStateEuclideanNorm < 0) {
			throw new IllegalArgumentException("toStateEuclideanNorm is "
					+ toStateEuclideanNorm + " < 0");
		}

		this.decisionVariable = decisionVariable;
		this.delta = delta;
		this.objectiveFunctionValue = objectiveFunctionValue;
		this.fromStateEuclideanNorm = fromStateEuclideanNorm;
		this.toStateEuclideanNorm = toStateEuclideanNorm;
	}

	// -------------------- GETTERS --------------------

	U getDecisionVariable() {
		return this.decisionVariable;
	}

	Vector getDelta() {
		return this.delta;
	}

	double getObjectiveFunctionValue() {
		return this.objectiveFunctionValue;
	}

	double getFromStateEuclideanNorm() {
		return this.fromStateEuclideanNorm;
	}
	
	double getToStateEuclideanNorm() {
		return this.toStateEuclideanNorm;
	}
	
}
