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

	// MEMBERS

	private SurrogateSolution<X, U> surrogateSolution;

	private U currentDecisionVariable = null;

	private X fromState = null;

	private StringBuffer msg = new StringBuffer(); // TODO only for testing

	// -------------------- CONSTRUCTION --------------------

	/**
	 * @param decisionVariables
	 *            the decision variables to be tried out
	 * @param objectiveFunction
	 *            represents the objective function that is to be minimized
	 * @param transitionNoiseVarianceScale
	 *            defines an estimate of the simulation transition noise as
	 *            (transitionNoiseVarianceScale * the square norm of the current
	 *            simulator state)
	 * @param convergenceNoiseVarianceScale
	 *            defines the convergence threshold
	 *            (convergenceNoiseVarianceScale * the square norm of the
	 *            current simulator state)
	 */
	public DecisionVariableSetEvaluator(final Set<U> decisionVariables,
			final ObjectiveFunction<X> objectiveFunction,
			final double transitionNoiseVarianceScale,
			final double convergenceNoiseVarianceScale) {
		this.decisionVariablesToBeTriedOut = new LinkedHashSet<U>(
				decisionVariables);
		this.objectiveFunction = objectiveFunction;
		this.surrogateSolution = new SurrogateSolution<X, U>(
				transitionNoiseVarianceScale, convergenceNoiseVarianceScale);
	}

	// -------------------- GETTERS --------------------

	public U getCurrentDecisionVariable() {
		return this.currentDecisionVariable;
	}

	// TODO experimental, remove again
	public Map<U, TransitionSequence<X, U>> getDecisionVariable2TransitionSequence() {
		return this.surrogateSolution.getDecisionVariable2TransitionSequence();
	}

	// -------------------- IMPLEMENTATION --------------------

	/**
	 * Call once before the simulation is started. This only implements a
	 * randomly selected decision variable in the simulation, merely with the
	 * objective to enable a first simulation transition.
	 */
	public void initialize() {
		this.currentDecisionVariable = (this.decisionVariablesToBeTriedOut
				.iterator()).next();
		this.currentDecisionVariable.implementInSimulation();
	}

	/**
	 * To be called once after each simulation iteration. Registers the
	 * simulation state reached after that iteration and implements a new trial
	 * decision variable in the simulation.
	 * 
	 * @param newState
	 *            the newly reached simulator state
	 */
	public void afterIteration(final X newState) {

		/*
		 * (1) If fromState is not null, then a full transition has been
		 * observed that can now be processed.
		 */

		if (this.fromState != null) {

			// Evaluate the new transition.
			this.surrogateSolution.addTransition(this.fromState,
					this.currentDecisionVariable, newState);
			this.surrogateSolution.evaluate();

			// logging, replace by something more meaningful
			this.msg.append("G2 = "
					+ this.surrogateSolution.getEstimatedExpectedGap2()
					+ " vs "
					+ this.surrogateSolution.getConvergenceNoiseVariance()
					+ "; estimated simulation noise variance = "
					+ this.surrogateSolution.getTransitionNoiseVariance()
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
					&& (this.surrogateSolution.isConverged())) {

				SurrogateSolution<X, U> best = this.surrogateSolution;
				double bestObjectiveFunctionValue = this.objectiveFunction
						.evaluateState(this.surrogateSolution
								.getEquilibriumState());

				for (SurrogateSolution<X, U> candidate : this.surrogateSolution
						.newEvaluatedSubsets()) {
					if (candidate.isConverged()) {
						final double candidateObjectiveFunctionValue = this.objectiveFunction
								.evaluateState(candidate.getEquilibriumState());
						if (candidateObjectiveFunctionValue < bestObjectiveFunctionValue) {
							best = candidate;
							bestObjectiveFunctionValue = candidateObjectiveFunctionValue;
						}
					}
				}

				this.surrogateSolution = best;
			}
		}

		/*
		 * (2) Prepare the next iteration.
		 */

		if (this.decisionVariablesToBeTriedOut.size() > 0) {
			// there still are untried decision variables, pick one
			this.currentDecisionVariable = (this.decisionVariablesToBeTriedOut
					.iterator()).next();
			this.decisionVariablesToBeTriedOut
					.remove(this.currentDecisionVariable);
			if (this.fromState == null) {
				this.fromState = newState.deepCopy();
			} else {
				this.fromState.implementInSimulation();
			}
		} else {
			// no more untried decision variables, repeat the least used one
			this.currentDecisionVariable = (this.surrogateSolution
					.getLeastEvaluatedDecisionVariables().iterator().next());
			// set simulation to last state visited by that decision variable
			this.fromState = this.surrogateSolution
					.getDecisionVariable2TransitionSequence()
					.get(this.currentDecisionVariable).getLastState();
			this.fromState.implementInSimulation();
		}

		this.currentDecisionVariable.implementInSimulation();
	}
}