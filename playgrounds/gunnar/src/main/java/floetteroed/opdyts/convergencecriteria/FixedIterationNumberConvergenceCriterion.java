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
package floetteroed.opdyts.convergencecriteria;

import java.util.ArrayList;
import java.util.List;

import floetteroed.opdyts.DecisionVariable;
import floetteroed.opdyts.trajectorysampling.Transition;
import floetteroed.utilities.math.BasicStatistics;
import floetteroed.utilities.math.Vector;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class FixedIterationNumberConvergenceCriterion implements
		ConvergenceCriterion {

	// -------------------- CONSTANTS --------------------

	private final int iterationsToConvergence;

	private final int averagingIterations;

	// -------------------- CONSTRUCTION --------------------

	public FixedIterationNumberConvergenceCriterion(
			final int iterationsToConvergence, final int averagingIterations) {
		this.iterationsToConvergence = iterationsToConvergence;
		this.averagingIterations = averagingIterations;
	}

	// -------------------- INTERNALS --------------------

	private <U extends DecisionVariable> List<Double> objectiveFunctionValues(
			final List<Transition<U>> transitions) {
		final List<Double> result = new ArrayList<Double>(transitions.size());
		for (Transition<?> transition : transitions) {
			result.add(transition.getToStateObjectiveFunctionValue());
		}
		return result;
	}

	// --------------- IMPLEMENTATION OF ConvergenceCriterion ---------------

	@Override
	public double effectiveAveragingIterations() {
		return this.averagingIterations;
	}
	
	@Override
	public <U extends DecisionVariable> ConvergenceCriterionResult evaluate(
			final List<Transition<U>> transitionSequence) {

		if (transitionSequence.size() >= this.averagingIterations) {

			// objective function statistics
			final List<Double> objectiveFunctionValues = this
					.objectiveFunctionValues(transitionSequence);
			final BasicStatistics objectiveFunctionValueStats = new BasicStatistics();
			for (int i = transitionSequence.size() - this.averagingIterations; i < transitionSequence
					.size(); i++) {
				objectiveFunctionValueStats.add(objectiveFunctionValues.get(i));
			}

			// gap statistics
			final Vector totalDelta = transitionSequence
					.get(transitionSequence.size() - this.averagingIterations)
					.getDelta().copy();
			for (int i = transitionSequence.size() - this.averagingIterations
					+ 1; i < transitionSequence.size(); i++) {
				totalDelta.add(transitionSequence.get(i).getDelta());
			}

			// package the results
			final boolean converged = (transitionSequence.size() >= this.iterationsToConvergence);
			return new ConvergenceCriterionResult(converged,
					objectiveFunctionValueStats.getAvg(),
					objectiveFunctionValueStats.getStddev()
							/ Math.sqrt(this.averagingIterations),
					totalDelta.euclNorm() / this.averagingIterations,
					1.0 / this.averagingIterations, transitionSequence.get(0)
							.getDecisionVariable(), transitionSequence.size());

		} else {

			return new ConvergenceCriterionResult(false, null, null, null,
					null, null, null);

		}

		// if ((transitionSequence.iterations() < this.iterationsToConvergence))
		// {
		//
		// return null;
		//
		// } else {
		//
		// // objective function statistics
		// final List<Double> objectiveFunctionValues = transitionSequence
		// .getObjectiveFunctionValues();
		// final BasicStatistics objectiveFunctionValueStats = new
		// BasicStatistics();
		// for (int i = transitionSequence.size() - this.averagingIterations; i
		// < transitionSequence
		// .size(); i++) {
		// objectiveFunctionValueStats.add(objectiveFunctionValues.get(i));
		// }
		//
		// // gap statistics
		// final Vector totalDelta = transitionSequence.getTransitions()
		// .get(transitionSequence.size() - this.averagingIterations)
		// .getDelta().copy();
		// for (int i = transitionSequence.size() - this.averagingIterations
		// + 1; i < transitionSequence.size(); i++) {
		// totalDelta.add(transitionSequence.getTransitions().get(i)
		// .getDelta());
		// }
		//
		// // package the results
		// return new ConvergenceCriterionResult(
		// objectiveFunctionValueStats.getAvg(),
		// objectiveFunctionValueStats.getStddev(),
		// totalDelta.euclNorm() / this.averagingIterations,
		// 1.0 / this.averagingIterations,
		// transitionSequence.getDecisionVariable(),
		// transitionSequence.size());
		// }
	}
}
