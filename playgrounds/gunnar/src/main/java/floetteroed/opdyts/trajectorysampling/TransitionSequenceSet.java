package floetteroed.opdyts.trajectorysampling;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

import floetteroed.opdyts.DecisionVariable;
import floetteroed.opdyts.SimulatorState;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class TransitionSequenceSet<U extends DecisionVariable> {

	// -------------------- MEMBERS --------------------

	private final Map<U, TransitionSequence<U>> decisionVariable2transitionSequence = new LinkedHashMap<>();

	private final LinkedList<Transition<U>> transitionsInInsertionOrder = new LinkedList<Transition<U>>();

	// -------------------- CONSTRUCTION --------------------

	TransitionSequenceSet() {
	}

	// -------------------- SETTERS --------------------

	void addTransition(final SimulatorState fromState,
			final U decisionVariable, final SimulatorState toState,
			final double objectiveFunctionValue) {
		TransitionSequence<U> transitionSequence = this.decisionVariable2transitionSequence
				.get(decisionVariable);
		if (transitionSequence == null) {
			transitionSequence = new TransitionSequence<U>(fromState,
					decisionVariable, toState, objectiveFunctionValue);
			this.decisionVariable2transitionSequence.put(decisionVariable,
					transitionSequence);
		} else {
			transitionSequence.addTransition(fromState, decisionVariable,
					toState, objectiveFunctionValue);
		}
		// transitionSequence.shrinkToMaximumLength(this.maxSequenceLength);
		this.transitionsInInsertionOrder.add(transitionSequence
				.getLastTransition());
	}

	// -------------------- GETTERS --------------------

	int size() {
		return this.transitionsInInsertionOrder.size();
	}

	LinkedList<Transition<U>> getTransitions(final U decisionVariable) {
		return this.decisionVariable2transitionSequence.get(decisionVariable)
				.getTransitions();
	}

	LinkedList<Transition<U>> getAllTransitionsInInsertionOrder() {
		return this.transitionsInInsertionOrder;
	}

	SimulatorState getLastState(final U decisionVariable) {
		return this.decisionVariable2transitionSequence.get(decisionVariable)
				.getLastState();
	}
}
