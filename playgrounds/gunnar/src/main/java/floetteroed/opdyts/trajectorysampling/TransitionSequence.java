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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import floetteroed.opdyts.DecisionVariable;
import floetteroed.opdyts.ObjectBasedObjectiveFunction;
import floetteroed.opdyts.SimulatorState;
import floetteroed.opdyts.VectorBasedObjectiveFunction;
import floetteroed.utilities.math.Vector;

/**
 * Represents a sequence of simulator transitions.
 * 
 * @author Gunnar Flötteröd
 * 
 */
public class TransitionSequence<U extends DecisionVariable> {

	// -------------------- MEMBERS --------------------

	private final LinkedList<Transition<U>> transitions = new LinkedList<Transition<U>>();

	private SimulatorState lastState = null;

	// -------------------- CONSTRUCTION --------------------

	TransitionSequence(final SimulatorState fromState,
			final U decisionVariable, final SimulatorState toState,
			final ObjectBasedObjectiveFunction objectBasedObjectiveFunction,
			final VectorBasedObjectiveFunction vectorBasedObjectiveFunction) {
		this.addTransition(fromState, decisionVariable, toState,
				objectBasedObjectiveFunction, vectorBasedObjectiveFunction);
	}

	// -------------------- SETTERS --------------------

	void addTransition(final SimulatorState fromState,
			final U decisionVariable, final SimulatorState toState,
			final ObjectBasedObjectiveFunction objectBasedObjectiveFunction,
			final VectorBasedObjectiveFunction vectorBasedObjectiveFunction) {

		if (fromState == null) {
			throw new IllegalArgumentException("fromState is null");
		}
		if (decisionVariable == null) {
			throw new IllegalArgumentException("decisionVariable is null");
		}
		if (toState == null) {
			throw new IllegalArgumentException("toState is null");
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

		final Vector toStateVectorRepresentation = toState
				.getReferenceToVectorRepresentation();

		final double toStateObjectiveFunctionValue;
		if (objectBasedObjectiveFunction != null) {
			toStateObjectiveFunctionValue = objectBasedObjectiveFunction
					.value(toState);
		} else {
			toStateObjectiveFunctionValue = vectorBasedObjectiveFunction
					.value(toStateVectorRepresentation);
		}

		// new transitions are added at the end
		this.transitions.add(new Transition<>(decisionVariable, delta,
				toStateVectorRepresentation, toStateObjectiveFunctionValue));
		this.lastState = toState;
	}

	void shrinkToMaximumLength(final int maximumLength) {
		while (this.transitions.size() > maximumLength) {
			// old transitions are removed from the front
			this.transitions.removeFirst();
		}
	}

	// -------------------- GETTERS --------------------

	DecisionVariable getDecisionVariable() {
		return this.transitions.getFirst().getDecisionVariable();
	}

	SimulatorState getLastState() {
		return this.lastState;
	}

	List<Transition<U>> getTransitions() {
		return this.transitions;
	}

	public int size() {
		return this.transitions.size();
	}

	public List<Double> getObjectiveFunctionValues() {
		final List<Double> result = new ArrayList<Double>(this.size());
		for (Transition<U> transition : this.transitions) {
			// result.add(objectiveFunction.value(transition.getToState()));
			result.add(transition.getToStateObjectiveFunctionValue());
		}
		return result;
	}
}
