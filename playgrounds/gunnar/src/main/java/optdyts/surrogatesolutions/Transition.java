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

import optdyts.SimulatorState;
import floetteroed.utilities.math.Vector;

/**
 * Represents a simulator transition.
 * 
 * @author Gunnar Flötteröd
 * 
 * @param <X>
 *            the simulator state type
 * @param <U>
 *            the decision variable type
 */
public class Transition<X extends SimulatorState<X>, U> {

	// -------------------- CONSTANTS --------------------

	private final X fromState;

	private final U decisionVariable;

	private final X toState;

	private final Vector delta;

	// -------------------- CONSTRUCTION --------------------

	Transition(final X fromState, final U decisionVariable, final X toState) {

		if (fromState == null) {
			throw new IllegalArgumentException("fromState is null");
		}
		if (decisionVariable == null) {
			throw new IllegalArgumentException("decisionVariable is null");
		}
		if (toState == null) {
			throw new IllegalArgumentException("toState is null");
		}

		this.fromState = fromState;
		this.decisionVariable = decisionVariable;
		this.toState = toState;

		this.delta = this.toState.getReferenceToVectorRepresentation().copy();
		this.delta.add(this.fromState.getReferenceToVectorRepresentation(),
				-1.0);
	}

	// -------------------- GETTERS --------------------

	public X getFromState() {
		return this.fromState;
	}

	public U getDecisionVariable() {
		return this.decisionVariable;
	}

	public X getToState() {
		return this.toState;
	}

	public Vector getDelta() {
		return this.delta;
	}

}
