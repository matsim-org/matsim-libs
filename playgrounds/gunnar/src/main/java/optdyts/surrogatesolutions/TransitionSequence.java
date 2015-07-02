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

import java.util.LinkedList;
import java.util.List;

import optdyts.DecisionVariable;
import optdyts.SimulatorState;
import floetteroed.utilities.math.Vector;

/**
 * Represents a (possibly non consecutive) sequence of simulator transitions.
 * 
 * @author Gunnar Flötteröd
 * 
 * @param <X>
 *            the simulator state type
 * @param <U>
 *            the decision variable type
 */
class TransitionSequence<X extends SimulatorState, U extends DecisionVariable> {

	// -------------------- MEMBERS --------------------

	private final LinkedList<Transition<U>> transitions = new LinkedList<Transition<U>>();

	private X lastState = null;

	// -------------------- CONSTRUCTION --------------------

	TransitionSequence(final X fromState, final U decisionVariable,
			final X toState, final double objectiveFunctionValue) {
		this.addTransition(fromState, decisionVariable, toState,
				objectiveFunctionValue);
	}

	// -------------------- SETTERS --------------------

	void addTransition(final X fromState, final U decisionVariable,
			final X toState, final double objectiveFunctionValue) {

		if (fromState == null) {
			throw new IllegalArgumentException("fromState is null");
		}
		if (decisionVariable == null) {
			throw new IllegalArgumentException("decisionVariable is null");
		}
		if (toState == null) {
			throw new IllegalArgumentException("toState is null");
		}
		if (Double.isInfinite(objectiveFunctionValue)
				|| Double.isNaN(objectiveFunctionValue)) {
			throw new IllegalArgumentException("objectiveFunctionValue is "
					+ objectiveFunctionValue);
		}
		if ((this.size() > 0)
				&& (!this.getDecisionVariable().equals(decisionVariable))) {
			throw new IllegalArgumentException("Cannot add "
					+ " transition with decision variable "
					+ this.getDecisionVariable() + " to "
					+ " transition sequence with decision variable "
					+ this.getDecisionVariable() + ".");
		}

		final Vector delta = toState.getReferenceToVectorRepresentation()
				.copy();
		delta.add(fromState.getReferenceToVectorRepresentation(), -1.0);

		// important: new transitions are added at the end
		this.transitions.add(new Transition<U>(decisionVariable, delta,
				objectiveFunctionValue, fromState
						.getReferenceToVectorRepresentation().euclNorm(),
				toState.getReferenceToVectorRepresentation().euclNorm()));
		this.lastState = toState;
	}

	void shrinkToMaximumLength(final int maximumLength) {
		while (this.transitions.size() > maximumLength) {
			// important: old transitions are removed from the front
			this.transitions.removeFirst();
		}
	}

	// -------------------- GETTERS --------------------

	int size() {
		return this.transitions.size();
	}

	U getDecisionVariable() {
		return this.transitions.getFirst().getDecisionVariable();
	}

	List<Transition<U>> getTransitions() {
		return this.transitions;
	}

	X getLastState() {
		return this.lastState;
	}

}
