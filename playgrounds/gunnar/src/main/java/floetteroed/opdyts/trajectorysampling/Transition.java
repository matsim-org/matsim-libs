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
package floetteroed.opdyts.trajectorysampling;

import floetteroed.opdyts.DecisionVariable;
import floetteroed.utilities.math.Vector;

/**
 * Represents a simulator transition.
 * 
 * @author Gunnar Flötteröd
 * 
 */
public class Transition<U extends DecisionVariable> {

	// -------------------- MEMBERS --------------------

	private final U decisionVariable;

	private final Vector delta;

	private final Vector toState;

	private final double toStateObjectiveFunctionValue;

	// -------------------- CONSTRUCTION --------------------

	/**
	 * Memorizes *references* to its parameters.
	 * 
	 * @param decisionVariable
	 *            the decision variable used in this transition
	 * @param delta
	 *            the move in vector-valued state space representing this
	 *            transition
	 * @param toStateObjectiveFunctionValue
	 *            the objective function value of the to-state of this
	 *            transition
	 */
	Transition(final U decisionVariable, final Vector delta,
			final Vector toState, final double toStateObjectiveFunctionValue) {
		if (decisionVariable == null) {
			throw new IllegalArgumentException("decisionVariable is null");
		}
		if (delta == null) {
			throw new IllegalArgumentException("delta is null");
		}
		this.decisionVariable = decisionVariable;
		this.delta = delta;
		this.toState = toState;
		this.toStateObjectiveFunctionValue = toStateObjectiveFunctionValue;
	}

	// -------------------- GETTERS --------------------

	public U getDecisionVariable() {
		return this.decisionVariable;
	}

	public Vector getDelta() {
		return this.delta;
	}

	public Vector getToState() {
		return this.toState;
	}

	public double getToStateObjectiveFunctionValue() {
		return this.toStateObjectiveFunctionValue;
	}
}
