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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.analysis.MultivariateFunction;
import org.apache.commons.math3.analysis.MultivariateVectorFunction;
import org.apache.commons.math3.exception.TooManyEvaluationsException;
import org.apache.commons.math3.optim.InitialGuess;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.MaxIter;
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.SimpleValueChecker;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunctionGradient;
import org.apache.commons.math3.optim.nonlinear.scalar.gradient.NonLinearConjugateGradientOptimizer;
import org.apache.commons.math3.util.FastMath;
import org.apache.log4j.Logger;

import floetteroed.opdyts.DecisionVariable;
import floetteroed.utilities.math.Matrix;
import floetteroed.utilities.math.Vector;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class TransitionSequencesAnalyzer<U extends DecisionVariable> {

	// -------------------- CONSTANTS --------------------

	private final List<Transition<U>> transitions;

	private final SurrogateObjectiveFunction<U> surrogateObjectiveFunction;

	private final int maxIterations = 10 * 1000; // TODO make configurable

	// -------------------- CONSTRUCTION --------------------

	TransitionSequencesAnalyzer(final List<Transition<U>> transitions,
			final double equilibriumGapWeight, final double uniformityWeight) {
		if ((transitions == null) || (transitions.size() == 0)) {
			throw new IllegalArgumentException(
					"there must be at least one transition");
		}
		this.transitions = transitions;
		this.surrogateObjectiveFunction = new SurrogateObjectiveFunction<>(
				transitions, equilibriumGapWeight, uniformityWeight);
	}

	TransitionSequencesAnalyzer(
			final Map<U, TransitionSequence<U>> decisionVariable2transitionSequence,
			final double equilibriumWeight, final double uniformityWeight) {
		this(map2list(decisionVariable2transitionSequence), equilibriumWeight,
				uniformityWeight);
	}

	static <V extends DecisionVariable> List<Transition<V>> map2list(
			final Map<V, TransitionSequence<V>> decisionVariable2transitionSequence) {
		final List<Transition<V>> result = new ArrayList<Transition<V>>();
		for (TransitionSequence<V> transitionSequence : decisionVariable2transitionSequence
				.values()) {
			result.addAll(transitionSequence.getTransitions());
		}
		return result;
	}

	// -------------------- GETTERS --------------------

	public double getEquilibriumGapWeight() {
		return this.surrogateObjectiveFunction.getEquilibriumGapWeight();
	}

	public double getUniformityWeight() {
		return this.surrogateObjectiveFunction.getUniformityWeight();
	}

	// -------------------- GENERAL STATISTICS --------------------

	public double originalObjectiveFunctionValue(final Vector alphas) {
		return this.surrogateObjectiveFunction
				.interpolatedObjectiveFunctionValue(alphas);
	}

	public double equilibriumGap(final Vector alphas) {
		return this.surrogateObjectiveFunction.equilibriumGap(alphas);
	}

	public double surrogateObjectiveFunctionValue(final Vector alphas) {
		return this.surrogateObjectiveFunction
				.surrogateObjectiveFunctionValue(alphas);
	}

	public Map<U, Double> decisionVariable2alphaSum(final Vector alphas) {
		final Map<U, Double> result = new LinkedHashMap<U, Double>();
		for (int i = 0; i < alphas.size(); i++) {
			final U decisionVariable = this.transitions.get(i)
					.getDecisionVariable();
			final Double sum = result.get(decisionVariable);
			result.put(decisionVariable, sum == null ? alphas.get(i) : sum
					+ alphas.get(i));
		}
		return result;
	}

	// -------------------- OPTIMIZATION --------------------

	public SamplingStage<U> newOptimalSamplingStage(
			final Transition<U> lastTransition,
			final Double convergedObjectiveFunctionValue) {
		final Vector alphas = this.optimalAlphas();
		return new SamplingStage<>(alphas, this, lastTransition,
				convergedObjectiveFunctionValue);
	}

	public Vector optimalAlphas() {
		if (this.transitions.size() == 1) {
			return new Vector(1.0);
		} else {
			final Vector initialAlphas = new Vector(this.transitions.size());
			initialAlphas.fill(1.0 / initialAlphas.size());

			// final NonLinearConjugateGradientOptimizer solver = new
			// NonLinearConjugateGradientOptimizer(
			// NonLinearConjugateGradientOptimizer.Formula.POLAK_RIBIERE, //
			// FLETCHER_REEVES,
			// new SimpleValueChecker(1e-8, 1e-8));

			PointValuePair result = null;
			double initialBracketingRange = 1e-8;
			do {
				try {
					final NonLinearConjugateGradientOptimizer solver = new NonLinearConjugateGradientOptimizer(
							NonLinearConjugateGradientOptimizer.Formula.POLAK_RIBIERE, // FLETCHER_REEVES,
							new SimpleValueChecker(1e-8, 1e-8), 1e-8, 1e-8,
							initialBracketingRange);
					result = solver.optimize(new ObjectiveFunction(
							new MyObjectiveFunction()),
							new ObjectiveFunctionGradient(new MyGradient()),
							GoalType.MINIMIZE, new InitialGuess(
									new double[this.transitions.size() - 1]),
							new MaxEval(Integer.MAX_VALUE), new MaxIter(
									this.maxIterations));
				} catch (TooManyEvaluationsException e) {
					initialBracketingRange *= 10.0;
					Logger.getLogger(this.getClass().getName()).info(
							"Extending initial bracketing range to "
									+ initialBracketingRange);
					result = null;
				}
			} while (result == null);

			return proba(newVectPlusOneZero(result.getPoint()));
		}
	}

	/*
	 * An unconstrained nonlinear optimization routine is used to compute
	 * optimal alpha. To ensure that alpha represents a proper probability
	 * distribution, the problem is reformulated with the utilities of a
	 * multinomial logit model as decision variables.
	 */

	private static final double V_MIN = -10.0;

	private static final double V_MAX = +10.0;

	private double boundPenaltyValue(final double[] point) {
		double result = 0;
		for (int i = 0; i < point.length; i++) {
			if (point[i] < V_MIN) {
				result += (point[i] - V_MIN) * (point[i] - V_MIN);
			} else if (point[i] > V_MAX) {
				result += (point[i] - V_MAX) * (point[i] - V_MAX);
			}
		}
		return result;
	}

	private double[] boundPenaltyGradient(final double[] point) {
		final double[] result = new double[point.length];
		for (int i = 0; i < point.length; i++) {
			if (point[i] < V_MIN) {
				result[i] = 2.0 * (point[i] - V_MIN);
			} else if (point[i] > V_MAX) {
				result[i] = 2.0 * (point[i] - V_MAX);
			}
		}
		return result;
	}

	private static Vector newVectPlusOneZero(final double[] array) {
		final double[] data = new double[array.length + 1];
		System.arraycopy(array, 0, data, 0, array.length);
		return new Vector(data);
	}

	private static Vector proba(final Vector v) {
		final double vMax = v.max();
		final Vector proba = new Vector(v.size());
		for (int i = 0; i < v.size(); i++) {
			proba.set(i, FastMath.exp(v.get(i) - vMax));
		}
		proba.mult(1.0 / proba.sum());
		return proba;
	}

	private static Matrix dProba_dV(final Vector v, final Vector proba) {
		final Matrix dP_dV = new Matrix(proba.size(), v.size());
		for (int i = 0; i < proba.size(); i++) {
			final Vector row = dP_dV.getRow(i);
			final double pi = proba.get(i);
			for (int j = 0; j < row.size(); j++) {
				row.set(j, -pi * proba.get(j));
			}
			row.add(i, pi);
		}
		return dP_dV;
	}

	private class MyObjectiveFunction implements MultivariateFunction {
		@Override
		public double value(double[] point) {
			final Vector alphas = proba(newVectPlusOneZero(point));
			return surrogateObjectiveFunctionValue(alphas)
					+ boundPenaltyValue(point);
		}
	}

	private class MyGradient implements MultivariateVectorFunction {
		@Override
		public double[] value(double[] point) {

			final Vector v = newVectPlusOneZero(point);
			final Vector alphas = proba(v);
			final Matrix dAlpha_dV = dProba_dV(v, alphas);
			final Vector dQ_dAlpha = surrogateObjectiveFunction
					.dSurrObjFctVal_dAlpha(alphas);

			final double[] boundPenaltyGradient = boundPenaltyGradient(point);
			// dQ/dV(i) = sum_j dQ/dAlpha(j) * dAlpha(j)/dV(i)
			final double[] dQ_dV = new double[v.size() - 1];
			for (int i = 0; i < dQ_dV.length; i++) {
				dQ_dV[i] = boundPenaltyGradient[i];
				for (int j = 0; j < alphas.size(); j++) {
					dQ_dV[i] += dQ_dAlpha.get(j) * dAlpha_dV.getRow(j).get(i);
				}
			}

			return dQ_dV;
		}
	}
}
