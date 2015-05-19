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

import optdyts.DecisionVariable;
import optdyts.SimulatorState;
import floetteroed.utilities.math.Matrix;
import floetteroed.utilities.math.Vector;

/**
 * Estimates a surrogate solution.
 * 
 * @author Gunnar Flötteröd
 * 
 * @param <X>
 *            the simulator state type
 * @param <U>
 *            the decision variable type
 */
class SurrogateSolutionEstimator<X extends SimulatorState<X>, U extends DecisionVariable> {

	// -------------------- CONSTANTS --------------------

	// TODO Replace this by something more intelligent.
	private final double largestSimulatorEigenvalue = 0.95;

	private final double simulationNoiseVariance;

	// -------------------- CONSTRUCTION --------------------

	SurrogateSolutionEstimator(final double simulationNoiseVariance) {
		this.simulationNoiseVariance = simulationNoiseVariance;
	}

	// -------------------- IMPLEMENTATION --------------------

	private double estimatedExpectedGap2(final List<Vector> deltaList,
			final Vector alphas) {
		final Vector residual = deltaList.get(0).copy();
		residual.mult(alphas.get(0));
		for (int i = 1; i < alphas.size(); i++) {
			residual.add(deltaList.get(i), alphas.get(i));
		}
		return residual.innerProd(residual) + this.simulationNoiseVariance
				* alphas.innerProd(alphas);
	}

	/**
	 * @param transitions
	 *            the most recently simulated transitions, sorted such that
	 *            transitions.get(i) yields the transition from (k - i) to (k +
	 *            1 - i)
	 */
	SurrogateSolutionProperties<X, U> computeProperties(
			final List<Transition<X, U>> transitions) {
		final List<Double> initialAlphas = new ArrayList<Double>(
				transitions.size());
		final double weight = 1.0 / transitions.size();
		for (int i = 0; i < transitions.size(); i++) {
			initialAlphas.add(weight);
		}
		return this.computeProperties(transitions, initialAlphas);
	}

	/**
	 * @param transitions
	 *            the most recently simulated transitions, sorted such that
	 *            transitions.get(i) yields the transition from (k - i) to (k +
	 *            1 - i)
	 * @param initialAlphas
	 *            a vector of initial coefficients for the state differences in
	 *            the fixed point prediction, sorted such that
	 *            initialAlphas.get(i) returns the coefficient for the
	 *            transition from (k - i) to (k + 1 - i)
	 */
	SurrogateSolutionProperties<X, U> computeProperties(
			final List<Transition<X, U>> transitions,
			final List<Double> initialAlphas) {

		/*
		 * (0) Check parameters. No default fixes applied here!
		 */

		if (transitions.size() < 1) {
			throw new RuntimeException("transitions.size() is "
					+ transitions.size() + " < 1\n");
		}
		if (transitions.size() != initialAlphas.size()) {
			throw new RuntimeException(
					"transitions.size() - initialAlphas.size() is "
							+ transitions.size() + " - " + initialAlphas.size()
							+ " != 0\n");
		}
		double initialAlphaSum = 0.0;
		for (Double initialAlphaValue : initialAlphas) {
			if (initialAlphaValue < 0.0) {
				throw new RuntimeException(
						"an element in initialAlphas has value "
								+ initialAlphaValue + " < 0.0");
			}
			if (initialAlphaValue > 1.0) {
				throw new RuntimeException(
						"an element in initialAlphas has value "
								+ initialAlphaValue + " > 1.0");
			}
			initialAlphaSum += initialAlphaValue;
		}
		if (Math.abs(initialAlphaSum - 1.0) > 1e-6) {
			throw new RuntimeException(
					"the sum of elements in inititalAlphas is "
							+ initialAlphaSum + " != 1.0 [+/- 1e-6]");
		}

		/*
		 * (1) Extract first differences of the state sequence.
		 * 
		 * TODO Try to get rid of deltaList, just use Transition.getDelta().
		 */

		final List<Vector> deltaList = new ArrayList<Vector>(transitions.size());
		for (int i = 0; i < transitions.size(); i++) {
			deltaList.add(transitions.get(i).getDelta());
		}

		/*
		 * (2) Pre-compute inner products of all state differences.
		 */

		final Matrix innerProds = new Matrix(deltaList.size(), deltaList.size());
		for (int i = 0; i < deltaList.size(); i++) {
			for (int j = 0; j <= i; j++) {
				final double val = deltaList.get(i).innerProd(deltaList.get(j));
				innerProds.getRow(i).set(j, val);
				innerProds.getRow(j).set(i, val);
			}
		}

		/*
		 * (3) Find alphas that minimize estimated expected square gap.
		 */

		final Vector alphas = new Vector(initialAlphas);
		alphas.mult(1.0 / alphas.absValueSum());

		boolean noMoreImprovement = false;
		double estimatedExpectedGap2 = this.estimatedExpectedGap2(deltaList,
				alphas);

		while (!noMoreImprovement) {

			System.out.println("ITERATION WITH estimatedExpectedG2 == "
					+ estimatedExpectedGap2);

			// (3.1) Make one round of improvement steps.

			for (int l = 1; l < alphas.size(); l++) {
				double newAlpha = innerProds.get(0, 0) - innerProds.get(l, 0);
				for (int i = 1; i < alphas.size(); i++) {
					if (i != l) {
						newAlpha -= alphas.get(i)
								* (innerProds.get(i, l) - innerProds.get(i, 0)
										- innerProds.get(0, l) + innerProds
											.get(0, 0));
					}
				}
				newAlpha /= (innerProds.get(l, l) - 2.0 * innerProds.get(l, 0)
						+ innerProds.get(0, 0) + Math.pow(
						1.0 - this.largestSimulatorEigenvalue, 2.0)
						* this.simulationNoiseVariance);

				newAlpha = Math.max(newAlpha, 0.0);
				newAlpha = Math.min(newAlpha, alphas.get(0) + alphas.get(l));

				alphas.set(l, newAlpha);
				alphas.set(0, 0.0);
				alphas.set(0, Math.max(0.0, 1.0 - alphas.sum()));
			}

			// (3.2) Compute remaining error and decide if to continue.

			final double oldEstimatedExpectedGap2 = estimatedExpectedGap2;
			estimatedExpectedGap2 = this.estimatedExpectedGap2(deltaList,
					alphas);
			if (Math.abs(oldEstimatedExpectedGap2 - estimatedExpectedGap2)
					/ oldEstimatedExpectedGap2 <= 1e-6) {
				noMoreImprovement = true;
			}
		}

		/*
		 * (4) Compute and return resulting surrogate solution properties.
		 */

		return new SurrogateSolutionProperties<X, U>(transitions, alphas,
				estimatedExpectedGap2);
	}
}
