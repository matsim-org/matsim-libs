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
import java.util.List;

import optdyts.SimulatorState;

/**
 * Represents a (possibly non consecutive) sequence of transitions.
 * 
 * @author Gunnar Flötteröd
 * 
 * @param <X>
 *            the simulator state type
 * @param <U>
 *            the decision variable type
 */
public class TransitionSequence<X extends SimulatorState<X>, U> {

	// -------------------- MEMBERS --------------------

	private final List<Transition<X, U>> transitions = new ArrayList<Transition<X, U>>();

	// -------------------- CONSTRUCTION --------------------

	TransitionSequence(final Transition<X, U> transition) {
		if (transition == null) {
			throw new RuntimeException("Cannot initialize "
					+ this.getClass().getSimpleName()
					+ " with a null transition.");
		}
		this.transitions.add(transition);
	}

	void addTransition(final Transition<X, U> transition) {
		if (!this.getDecisionVariable()
				.equals(transition.getDecisionVariable())) {
			throw new RuntimeException("Cannot add "
					+ transition.getClass().getSimpleName()
					+ " with decision variable "
					+ transition.getDecisionVariable() + " to "
					+ this.getClass().getSimpleName()
					+ " with decision variable " + this.getDecisionVariable()
					+ ".");
		}
		this.transitions.add(transition);
	}

	// -------------------- GETTERS --------------------

	public U getDecisionVariable() {
		return this.transitions.get(0).getDecisionVariable();
	}

	public int size() {
		return this.transitions.size();
	}

	public List<Transition<X, U>> getTransitions() {
		return this.transitions;
	}

	public X getLastState() {
		return this.transitions.get(this.transitions.size() - 1).getToState();
	}

}
