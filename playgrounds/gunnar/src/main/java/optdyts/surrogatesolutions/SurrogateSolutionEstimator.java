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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import optdyts.DecisionVariable;
import floetteroed.utilities.math.Matrix;
import floetteroed.utilities.math.Vector;

/**
 * Functionality to estimate a surrogate solution.
 * 
 * TODO Simplify structure of this class. Its current form is aligned with the
 * need to perform cross-validations, which is no longer necessary (and unlikely
 * to again become necessary in the future).
 * 
 * @author Gunnar Flötteröd
 * 
 */
class SurrogateSolutionEstimator {

	// -------------------- CONSTANTS --------------------

	// TODO current numerical precision checks are ad hoc
	private static final double EPS = 1e-8;

	// TODO make this configurable
	private static final int maxIterations = 1000;
	
	// -------------------- NO CONSTRUCTION --------------------

	private SurrogateSolutionEstimator() {
	}

	// -------------------- IMPLEMENTATION --------------------

	// private static Matrix newStateCovariances(
	// final List<Transition<?>> transitions) {
	// final Matrix result = new Matrix(transitions.size(), transitions.size());
	// for (int i = 0; i < transitions.size(); i++) {
	// for (int j = 0; j <= i; j++) {
	// final double val = transitions.get(i).getDelta()
	// .innerProd(transitions.get(j).getDelta());
	// result.getRow(i).set(j, val);
	// result.getRow(j).set(i, val);
	// }
	// }
	// return result;
	// }

	// TODO only used as absolute gap
	private static double squareGap(final Matrix stateCovariances,
			final Vector alphas, final double regularizationScale) {
		double result = 0;
		for (int i = 0; i < alphas.size(); i++) {
			result += alphas.get(i) * alphas.get(i)
					* stateCovariances.get(i, i);
			for (int j = 0; j < i; j++) {
				result += 2.0 * alphas.get(i) * alphas.get(j)
						* stateCovariances.get(i, j);
			}
		}
		result += regularizationScale * alphas.innerProd(alphas);
		return result;
	}

	// private static double constrainToQuadraticSolutionInterval(final double
	// x,
	// final double p, final double q) {
	// final double sqrtArg = p * p / 4.0 - q;
	// // TODO a tighter check would have to be thought through better
	// // if (sqrtArg < -EPS) {
	// // throw new RuntimeException(
	// // "infeasible quadratic solution interval with square root argument "
	// // + sqrtArg + " < " + (-EPS));
	// // } else
	// if (sqrtArg < 0.0) {
	// return x;
	// } else {
	// final double sqrt = Math.sqrt(sqrtArg);
	// final double xMin = -p / 2.0 - sqrt;
	// final double xMax = -p / 2.0 + sqrt;
	// return Math.max(xMin, Math.min(x, xMax));
	// }
	// }

	private static Vector computeAlphas(final Matrix stateCovariances,
			final Vector initialAlphas, final double regularizationScale,
			final double minimumAverageIterations) {

		// Check parameters.

		if (initialAlphas.min() < 0) {
			throw new RuntimeException("an element in initialAlphas has value "
					+ initialAlphas.min() + " < 0.0");
		}
		if (initialAlphas.max() > 1.0) {
			throw new RuntimeException("an element in initialAlphas has value "
					+ initialAlphas.max() + " > 0.0");
		}
		if (Math.abs(initialAlphas.sum() - 1.0) > EPS) {
			throw new RuntimeException(
					"the sum of elements in inititalAlphas is "
							+ initialAlphas.sum() + " != 1.0 [+/- " + EPS + "]");
		}
		if (stateCovariances.rowSize() != initialAlphas.size()
				|| stateCovariances.columnSize() != initialAlphas.size()) {
			throw new RuntimeException("dimension of stateCovariances is ("
					+ stateCovariances.rowSize() + " x "
					+ stateCovariances.columnSize() + ") != ("
					+ initialAlphas.size() + " x " + initialAlphas.size() + ")");
		}
		if ((1.0 / initialAlphas.innerProd(initialAlphas)) + EPS < minimumAverageIterations) {
			throw new RuntimeException(
					"equivalent averaging iterations of initial solution are "
							+ (1.0 / initialAlphas.innerProd(initialAlphas))
							+ " < minimumAverageIterations of "
							+ minimumAverageIterations);
		}

		final Vector alphas = initialAlphas.copy();
		// alphas.mult(1.0 / alphas.absValueSum());
		boolean noMoreImprovement = false;
		// double gap2 = squareGap(stateCovariances, alphas,
		// regularizationScale);

		System.out.print("solving for alpha ");
		int iteration = 0;		
		while (!noMoreImprovement && iteration < maxIterations) {

			// >>>>>>>>>> TODO NEW >>>>>>>>>>
			noMoreImprovement = true;
			System.out.print(".");
			// <<<<<<<<<< TODO NEW <<<<<<<<<<

			for (int l = 1; l < alphas.size(); l++) {
				double newAlpha = stateCovariances.get(0, 0)
						- stateCovariances.get(l, 0);
				for (int i = 1; i < alphas.size(); i++) {
					if (i != l) {
						newAlpha -= alphas.get(i)
								* (stateCovariances.get(i, l)
										- stateCovariances.get(i, 0)
										- stateCovariances.get(0, l) + stateCovariances
											.get(0, 0));
					}
				}
				newAlpha /= (stateCovariances.get(l, l) - 2.0
						* stateCovariances.get(l, 0)
						+ stateCovariances.get(0, 0) + regularizationScale);

				// bound constraints
				double lower = 0;
				double upper = alphas.get(0) + alphas.get(l);

				// newAlpha = Math.max(newAlpha, 0.0);
				// newAlpha = Math.min(newAlpha, alphas.get(0) + alphas.get(l));

				// sum of squares constraint
				final double p = -(alphas.get(0) + alphas.get(l));
				final double q = 0.5 * ((p * p)
						+ (alphas.innerProd(alphas) - alphas.get(0)
								* alphas.get(0) - alphas.get(l) * alphas.get(l)) - 1.0 / minimumAverageIterations);
				final double sqrtArg = p * p / 4.0 - q;
				if (sqrtArg >= 0.0) {
					final double sqrt = Math.sqrt(sqrtArg);
					// final double xMin = -p / 2.0 - sqrt;
					// final double xMax = -p / 2.0 + sqrt;
					lower = Math.max(lower, -p / 2.0 - sqrt);
					upper = Math.min(upper, -p / 2.0 + sqrt);
				}
				// newAlpha = constrainToQuadraticSolutionInterval(newAlpha, p,
				// q);
				// <<<<<<<<<< TODO NEW <<<<<<<<<<

				// establish constraints
				newAlpha = Math.max(newAlpha, lower);
				newAlpha = Math.min(newAlpha, upper);

				// check convergence
				if (Math.abs(newAlpha - alphas.get(l)) > 1e-8) {
					noMoreImprovement = false;
				}

				// implement update
				alphas.set(l, newAlpha);
				alphas.set(0, 0.0);
				alphas.set(0, Math.max(0.0, 1.0 - alphas.sum())); // TODO
			}

			// final double oldGap2 = gap2;
			// gap2 = squareGap(stateCovariances, alphas, regularizationScale);
			// if (Math.abs(oldGap2 - gap2) <= 1e-8 * oldGap2) {
			// // TODO this is ad hoc
			// noMoreImprovement = true;
			// }
		}
		System.out.println();
		
		return alphas;
	}

	/**
	 * TODO only one function call (but specification of initial alpha vector
	 * may be used up to speed up later implementations)
	 * 
	 * @param transitions
	 *            the most recently simulated transitions, sorted such that
	 *            transitions.get(i) yields the transition from (k - i) to (k +
	 *            1 - i)
	 * @param initialAlphas
	 *            a vector of initial coefficients for the state differences in
	 *            the fixed point prediction, sorted such that
	 *            initialAlphas.get(i) returns the coefficient for the
	 *            transition from (k - i) to (k + 1 - i)
	 * @param regularizationScale
	 */
	private static <U extends DecisionVariable> SurrogateSolutionProperties computeProperties(
			final List<Transition<U>> transitions, final Vector initialAlphas,
			final double regularizationScale, final int minimumAverageIterations) {

		// Check if enough iterations have been performed.
		if (transitions.size() < minimumAverageIterations) {
			return null; // ------------------------------------------------------------
		}

		// Compute state covariance matrix. (TODO incorrect terminology)
		final Matrix stateCovariances = new Matrix(transitions.size(),
				transitions.size());
		for (int i = 0; i < transitions.size(); i++) {
			for (int j = 0; j <= i; j++) {
				final double val = transitions.get(i).getDelta()
						.innerProd(transitions.get(j).getDelta());
				stateCovariances.getRow(i).set(j, val);
				stateCovariances.getRow(j).set(i, val);
			}
		}

		// Compute alpha values. (TODO only one function call)
		final Vector alphas;
		if (transitions.size() == minimumAverageIterations) {
			alphas = initialAlphas.copy();
		} else {
			alphas = computeAlphas(stateCovariances, initialAlphas,
					regularizationScale, minimumAverageIterations);
		}

		// Compute summary statistics.
		final Map<U, Double> decisionVariable2alphaSum = new LinkedHashMap<U, Double>();
		double interpolatedObjectiveFunctionValue = 0;
		double interpolatedFromStateEuclideanNorm = 0;
		for (int i = 0; i < transitions.size(); i++) {
			// Total alpha sum per decision variable.
			final U decisionVariable = transitions.get(i).getDecisionVariable();
			final Double alphaSumSoFar = decisionVariable2alphaSum
					.get(decisionVariable);
			decisionVariable2alphaSum.put(
					decisionVariable,
					(alphaSumSoFar == null ? 0.0 : alphaSumSoFar)
							+ alphas.get(i));
			// Interpolate objective function value.
			interpolatedObjectiveFunctionValue += alphas.get(i)
					* transitions.get(i).getObjectiveFunctionValue();
			interpolatedFromStateEuclideanNorm += alphas.get(i)
					* transitions.get(i).getFromStateEuclideanNorm();
		}
		final double equivalentAveragingIterations = 1.0 / alphas
				.innerProd(alphas);
		// TODO only one function call
		final double absoluteConvergenceGap = Math.sqrt(squareGap(
				stateCovariances, alphas, 0.0));

		// Create and return a properties object representing these results.
		return new SurrogateSolutionProperties(decisionVariable2alphaSum,
				interpolatedObjectiveFunctionValue,
				interpolatedFromStateEuclideanNorm,
				equivalentAveragingIterations, absoluteConvergenceGap);

		// return new SurrogateSolutionProperties<U>(transitions, alphas, gap2(
		// stateCovariances, alphas, 0.0), regularizationScale,
		// 1.0 / alphas.innerProd(alphas), Math.sqrt(gap2(
		// stateCovariances, alphas, 0.0)));
	}

	/**
	 * @param transitions
	 *            the most recently simulated transitions, sorted such that
	 *            transitions.get(i) yields the transition from (k - i) to (k +
	 *            1 - i)
	 * @param regularizationScale
	 */
	static <U extends DecisionVariable> SurrogateSolutionProperties computeProperties(
			final List<Transition<U>> transitions,
			final double regularizationScale, final int minimumAverageIterations) {
		final Vector initialAlphas = new Vector(transitions.size());
		initialAlphas.fill(1.0 / transitions.size());
		return computeProperties(transitions, initialAlphas,
				regularizationScale, minimumAverageIterations);
	}
}
