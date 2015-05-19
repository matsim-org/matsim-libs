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
package optdyts.algorithms;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import optdyts.DecisionVariable;
import optdyts.ObjectiveFunction;
import optdyts.SimulatorState;
import optdyts.surrogatesolutions.SurrogateSolution;
import optdyts.surrogatesolutions.TransitionSequence;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 */
public class DecisionVariableSetEvaluator<X extends SimulatorState<X>, U extends DecisionVariable> {

	// -------------------- MEMBERS --------------------

	// CONSTANTS

	private final Set<U> decisionVariablesToBeTriedOut;

	private final ObjectiveFunction<X> objectiveFunction;

	private final double maxGap2;

	// TODO new
	private final boolean requireSubsetsToBeConverged = true;

	// MEMBERS

	private SurrogateSolution<X, U> surrogateSolution;

	private U currentDecisionVariable = null;

	// TODO new
	// private X initialState = null;

	private X currentState = null;

	// TODO only for testing
	private StringBuffer msg = new StringBuffer();

	// -------------------- CONSTRUCTION --------------------

	/**
	 * @param decisionVariables
	 *            the decision variables to be tried out
	 * @param objectiveFunction
	 *            represents the objective function that is to be minimized
	 * @param simulatorNoiseVariance
	 *            the variance of the noise of the simulator transition function
	 *            (more specifically, the trace of its covariance matrix), a
	 *            nonnegative number
	 * @param maxGap2
	 *            largest tolerated equilibrium gap, must be strictly larger
	 *            than simulatorNoiseVariance
	 */
	public DecisionVariableSetEvaluator(final Set<U> decisionVariables,
			final ObjectiveFunction<X> objectiveFunction,
			final double simulationNoiseVariance, final double maxGap2) {
		this.decisionVariablesToBeTriedOut = new LinkedHashSet<U>(
				decisionVariables);
		this.objectiveFunction = objectiveFunction;
		this.surrogateSolution = new SurrogateSolution<X, U>(
				simulationNoiseVariance);
		this.maxGap2 = maxGap2;
	}

	// -------------------- IMPLEMENTATION --------------------

	// >>>>> Keeping track of the right call order. >>>>>

	private enum NextExpectedCall {
		implementNextDecisionVariable, registerState
	};

	private NextExpectedCall nextExpectedCall = NextExpectedCall.implementNextDecisionVariable;

	private void checkNextExpectedCall(final NextExpectedCall correct,
			final NextExpectedCall switchTo) {
		if (!this.nextExpectedCall.equals(correct)) {
			throw new RuntimeException("next call should be " + correct
					+ " but is " + this.nextExpectedCall);
		} else {
			this.nextExpectedCall = switchTo;
		}
	}

	// <<<<< Keeping track of the right call order. <<<<<

	/**
	 * To be called once before each simulation iteration.
	 * 
	 * @return the currently implemented decision variable; the calling function
	 *         is not expected to perform any operations with this variable
	 */
	public U implementNextDecisionVariable() {

		this.checkNextExpectedCall(
				NextExpectedCall.implementNextDecisionVariable,
				NextExpectedCall.registerState);

		if (this.decisionVariablesToBeTriedOut.size() > 0) {
			// there still are untried decision variables, pick one
			this.currentDecisionVariable = (this.decisionVariablesToBeTriedOut
					.iterator()).next();
			if (this.currentState != null) {
				// this is not the very first iteration
				this.decisionVariablesToBeTriedOut
						.remove(this.currentDecisionVariable);
				// TODO >>>>> NEW >>>>>
				// this.initialState.implementInSimulation();
				// TODO <<<<< NEW <<<<<
			}
		} else {
			// no more untried decision variables, repeat the least used one
			this.currentDecisionVariable = (this.surrogateSolution
					.getLeastEvaluatedDecisionVariables().iterator().next());
			// TODO >>>>> NEW >>>>>
			this.stateToBeImplemented = this.surrogateSolution
					.getDecisionVariable2TransitionSequence()
					.get(this.currentDecisionVariable).getLastState();
			// this.surrogateSolution.getDecisionVariable2TransitionSequence()
			// .get(this.currentDecisionVariable).getLastState()
			// .implementInSimulation();
			// TODO <<<<< NEW <<<<<
		}

		this.currentDecisionVariable.implementInSimulation();

		// TODO >>> NEW >>>
		if (this.stateToBeImplemented != null) {
			this.stateToBeImplemented.implementInSimulation();
			this.stateToBeImplemented = null; // TODO
		}
		// TODO <<< NEW <<<

		return this.currentDecisionVariable;
	}

	private X stateToBeImplemented = null;

	// public void implementNextSimulatorState() {
	// if (this.stateToBeImplemented != null) {
	// this.stateToBeImplemented.implementInSimulation();
	// this.stateToBeImplemented = null; // TODO
	// }
	// }

	/**
	 * To be called once after each simulation iteration. Registers the
	 * simulation state after that iteration.
	 * 
	 * @param newState
	 *            the newly reached simulator state
	 */
	public void registerState(final X newState) {

		this.checkNextExpectedCall(NextExpectedCall.registerState,
				NextExpectedCall.implementNextDecisionVariable);

		// TODO >>>>> NEW >>>>>
		// if (this.initialState == null) {
		// this.initialState = newState.deepCopy();
		// }
		// TODO <<<<< NEW <<<<<

		if (this.currentState != null) {

			// Evaluate the new transition.
			this.surrogateSolution.addTransition(this.currentState,
					this.currentDecisionVariable, newState);
			this.surrogateSolution.evaluate();

			// logging, replace by something more meaningful
			this.msg.append("G2 = "
					+ this.surrogateSolution.getEstimatedExpectedGap2()
					+ " vs "
					+ this.maxGap2
					+ "; Q = "
					+ this.objectiveFunction
							.evaluateState(this.surrogateSolution
									.getEquilibriumState()) + "; M = "
					+ this.surrogateSolution.size() + "; alpha = "
					+ this.surrogateSolution.getDecisionVariable2alphaSum()
					+ "\n");
			final PrintWriter writer;
			try {
				writer = new PrintWriter("fps.txt");
				writer.println(this.msg.toString());
				writer.flush();
				writer.close();
			} catch (FileNotFoundException e) {
				throw new RuntimeException(e);
			}

			// Possible to remove a solution?
			if ((this.decisionVariablesToBeTriedOut.size() == 0)
					&& (this.surrogateSolution.size() > 1)
					&& (this.surrogateSolution.getEstimatedExpectedGap2() <= this.maxGap2)) {

				SurrogateSolution<X, U> best = this.surrogateSolution;
				double bestObjectiveFunctionValue = this.objectiveFunction
						.evaluateState(this.surrogateSolution
								.getEquilibriumState());

				for (SurrogateSolution<X, U> candidate : this.surrogateSolution
						.newEvaluatedSubsets()) {
					if (!this.requireSubsetsToBeConverged
							|| (candidate.getEstimatedExpectedGap2() <= this.maxGap2)) {
						final double candidateObjectiveFunctionValue = this.objectiveFunction
								.evaluateState(candidate.getEquilibriumState());
						if (candidateObjectiveFunctionValue < bestObjectiveFunctionValue) {
							best = candidate;
							bestObjectiveFunctionValue = candidateObjectiveFunctionValue;
						}
					}
				}

				this.surrogateSolution = best;

				this.msg.append("NEW SURROGATE SOLUTION GAP = "
						+ this.surrogateSolution.getEstimatedExpectedGap2()
						+ "\n");
			}
		}

		// new code:
		if (this.currentState == null) {
			this.currentState = newState.deepCopy();
			this.stateToBeImplemented = null;
		} else if (this.decisionVariablesToBeTriedOut.size() == 0) {
			this.currentState = newState.deepCopy();
			this.stateToBeImplemented = null;
		} else {
			this.stateToBeImplemented = this.currentState;
			// this.currentState.implementInSimulation();
		}
		// was before:
		// this.currentState = newState.deepCopy();
	}

	public U getCurrentDecisionVariable() {
		return this.currentDecisionVariable;
	}

	// TODO experimental code below
	public Map<U, TransitionSequence<X, U>> getDecisionVariable2TransitionSequence() {
		return this.surrogateSolution.getDecisionVariable2TransitionSequence();
	}
}