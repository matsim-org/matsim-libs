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
package optdyts;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import optdyts.logging.SearchStatisticsWriter;
import optdyts.surrogatesolutions.SurrogateSolution;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 */
public class DecisionVariableSetEvaluator<X extends SimulatorState, U extends DecisionVariable> {

	// -------------------- MEMBERS --------------------

	// CONSTANTS

	private final boolean interpolateObjectiveFunctionValues = true;

	private final Set<U> decisionVariablesToBeTriedOut;

	// MEMBERS

	private SurrogateSolution<X, U> surrogateSolution;

	private U currentDecisionVariable = null;

	private X fromState = null;

	private List<SearchStatisticsWriter<X, U>> statisticsWriters = new ArrayList<SearchStatisticsWriter<X, U>>();

	// -------------------- CONSTRUCTION --------------------

	/**
	 * @param decisionVariables
	 *            the decision variables to be tried out
	 * @param objectiveFunction
	 *            represents the objective function that is to be minimized
	 * @param minimumAverageIterations
	 *            the minimum number of iterations over which the simulation
	 *            output should be averaged to be considered
	 *            "sufficiently noise free"
	 * @param maximumRelativeGap
	 *            the relative gap below which two subsequent and averaged
	 *            simulation iterations are considered to be converged
	 */
	public DecisionVariableSetEvaluator(final Set<U> decisionVariables,
			final ObjectiveFunction<X> objectiveFunction,
			final int minimumAverageIterations, final double maximumRelativeGap) {
		this.decisionVariablesToBeTriedOut = new LinkedHashSet<U>(
				decisionVariables);
		this.surrogateSolution = new SurrogateSolution<X, U>(objectiveFunction,
				minimumAverageIterations, Integer.MAX_VALUE, maximumRelativeGap);
	}

	// -------------------- SETTERS AND GETTERS --------------------

	public boolean getInterpolateObjectiveFunctionValues() {
		return this.interpolateObjectiveFunctionValues;
	}

	public void setStandardLogFileName(final String logFileName) {
		final SearchStatisticsWriter<X, U> writer = new SearchStatisticsWriter<X, U>(
				logFileName);
		writer.addStandards();
		this.statisticsWriters.add(writer);
	}

	public void addSearchStatisticsWriter(
			final SearchStatisticsWriter<X, U> writer) {
		this.statisticsWriters.add(writer);
	}

	public U getCurrentDecisionVariable() {
		return this.currentDecisionVariable;
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

			// logging
			for (SearchStatisticsWriter<X, U> writer : this.statisticsWriters) {
				writer.writeToFile(this.surrogateSolution);
			}

			// Possible to remove a solution?
			if ((this.decisionVariablesToBeTriedOut.size() == 0)
					&& (this.surrogateSolution.size() > 1)
					&& (this.surrogateSolution.isConverged())) {
				SurrogateSolution<X, U> best = this.surrogateSolution;
				for (SurrogateSolution<X, U> candidate : this.surrogateSolution
						.newEvaluatedSubsets()) {
					if (candidate.isConverged()
							&& (candidate
									.getInterpolatedObjectiveFunctionValue() < best
									.getInterpolatedObjectiveFunctionValue())) {
						best = candidate;
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
				this.fromState = newState;
			} else {
				this.fromState.implementInSimulation();
			}
		} else {
			// no more untried decision variables, repeat the least used one
			this.currentDecisionVariable = (this.surrogateSolution
					.getLeastEvaluatedDecisionVariables().iterator().next());
			// set simulation to last state visited by that decision variable
			this.fromState = this.surrogateSolution
					.getLastState(this.currentDecisionVariable);
			this.fromState.implementInSimulation();
		}

		this.currentDecisionVariable.implementInSimulation();
	}

	// TODO NEW
	public boolean foundSolution() {
		return ((this.decisionVariablesToBeTriedOut.size() == 0) && (this.surrogateSolution
				.size() == 1));
	}
	
	// TODO NEW
	public SurrogateSolution<X, U> getCurrentSurrogateSolution() {
		return this.surrogateSolution;
	}
	
}
