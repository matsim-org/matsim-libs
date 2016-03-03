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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.analysis.MultivariateFunction;
import org.apache.commons.math3.analysis.MultivariateVectorFunction;
import org.apache.commons.math3.util.FastMath;

import floetteroed.opdyts.DecisionVariable;
import floetteroed.opdyts.trajectorysampling.FrankWolfe.LineSearch;
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

	private SurrogateObjectiveFunction<U> surrogateObjectiveFunction;

	private final int maxIterations = Integer.MAX_VALUE;

	// -------------------- CONSTRUCTION --------------------

	public TransitionSequencesAnalyzer(final List<Transition<U>> transitions,
			final double equilibriumGapWeight, final double uniformityGapWeight) {
		if ((transitions == null) || (transitions.size() == 0)) {
			throw new IllegalArgumentException(
					"there must be at least one transition");
		}
		this.transitions = transitions;
		this.surrogateObjectiveFunction = new SurrogateObjectiveFunction<>(
				transitions, equilibriumGapWeight, Math.max(0.0,
						uniformityGapWeight));
	}

	// -------------------- SETTERS AND GETTERS --------------------

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

	// -------------------- INTERACTIONS WITH MATLAB --------------------

	private void writeProblem() {
		try {

			{
				final PrintWriter objFctValsWriter = new PrintWriter(
						"ObjFctVals.csv");
				objFctValsWriter.print(this.transitions.get(0)
						.getToStateObjectiveFunctionValue());
				for (int i = 1; i < this.transitions.size(); i++) {
					objFctValsWriter.print(",");
					objFctValsWriter.print(this.transitions.get(i)
							.getToStateObjectiveFunctionValue());
				}
				objFctValsWriter.flush();
				objFctValsWriter.close();
			}

			{
				final PrintWriter transitionsWriter = new PrintWriter(
						"Transitions.csv");
				transitionsWriter.print(this.transitions.get(0).getDelta()
						.toStringCSV());
				for (int i = 1; i < this.transitions.size(); i++) {
					transitionsWriter.println();
					transitionsWriter.print(this.transitions.get(i).getDelta()
							.toStringCSV());
				}
				transitionsWriter.flush();
				transitionsWriter.close();
			}

			{
				final PrintWriter weightsWriter = new PrintWriter("Weights.csv");
				weightsWriter.print(Double.toString(this
						.getEquilibriumGapWeight()));
				weightsWriter.print(",");
				weightsWriter
						.print(Double.toString(this.getUniformityWeight()));
				weightsWriter.flush();
				weightsWriter.close();
			}

		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	private void runMATLAB() {
		final String cmd = "matlab.exe -nosplash -nodesktop -nojvm -noFigureWindows -wait -r "
				+ "\"run('MinimizeSurrogateObjectiveFunction.m');exit;\"";
		final Process proc;
		try {
			proc = Runtime.getRuntime().exec(cmd, null, new File("."));
			proc.waitFor();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private Vector returnMATLABresult() {
		final String[] line;
		try {
			final BufferedReader reader = new BufferedReader(new FileReader(
					"Alphas.csv"));
			line = reader.readLine().split("\\Q" + "," + "\\E");
			reader.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		final Vector alphas = new Vector(line.length);
		for (int i = 0; i < line.length; i++) {
			alphas.set(i, Double.parseDouble(line[i]));
		}
		return alphas;
	}

	// -------------------- OPTIMIZATION --------------------

	private final boolean useLastPoint = true;

	private double[] newInitialPoint(final int targetDim,
			final double[] lastPoint) {
		final double[] result = new double[targetDim];
		if (this.useLastPoint && (lastPoint != null)) {
			System.arraycopy(lastPoint, 0, result, 0,
					Math.min(lastPoint.length, result.length));
		} else {
			Arrays.fill(result, 1.0 / result.length);
		}
		return result;
	}

	public SamplingStage<U> newOptimalSamplingStage(
			final Transition<U> lastTransition,
			final Double convergedObjectiveFunctionValue,
			final double[] lastInitialPoint) {

		final double[] result = this.newOptimalPoint(lastInitialPoint);
		final Vector alphas = new Vector(result);
		return new SamplingStage<>(alphas, this, lastTransition,
				convergedObjectiveFunctionValue, Double.NaN, result);

	}

	public double[] newOptimalPoint(final double[] lastInitialPoint) {

		final FrankWolfe fw = new FrankWolfe(new MyObjectiveFunction2(),
				new MyGradient2(), new MyLineSearch(), 1e-6);
		fw.run(this.newInitialPoint(this.transitions.size(), lastInitialPoint));
		return fw.getPoint();
		// final Vector alphas = new Vector(result);
		// return new SamplingStage<>(alphas, this, lastTransition,
		// convergedObjectiveFunctionValue, Double.NaN, result);

		// this.writeProblem();
		// this.runMATLAB();
		// final Vector alphas = this.returnMATLABresult();
		// return new SamplingStage<>(alphas, this, lastTransition,
		// convergedObjectiveFunctionValue, Double.NaN, null);

		// final double[] initialPoint = this.newInitialPoint(
		// this.transitions.size() - 1, lastInitialPoint);
		// final double[] optimalPoint = this.optimalPoint(initialPoint);
		// final Vector alphas = this.alphasFromPoint(optimalPoint);
		// final double solutionValue = (new MyObjectiveFunction())
		// .value(optimalPoint);
		// return new SamplingStage<>(alphas, this, lastTransition,
		// convergedObjectiveFunctionValue, solutionValue, optimalPoint);
	}

	// public Vector optimalAlphas(final double[] initialPoint) {
	// return this.alphasFromPoint(this.optimalPoint(initialPoint));
	// }

	// private Vector alphasFromPoint(final double[] point) {
	// return proba(newVectPlusOneZero(point));
	// }

	// private double[] optimalPoint(final double[] initialPoint) {
	// if (this.transitions.size() == 1) {
	// return new double[0];
	// } else {
	// PointValuePair result = null;
	// final double eps = 1e-8;
	// double initialBracketingRange = eps;
	// do {
	// try {
	// final NonLinearConjugateGradientOptimizer solver = new
	// NonLinearConjugateGradientOptimizer(
	// NonLinearConjugateGradientOptimizer.Formula.POLAK_RIBIERE, //
	// FLETCHER_REEVES,
	// new SimpleValueChecker(eps, eps), eps, eps,
	// initialBracketingRange);
	// result = solver.optimize(
	// new ObjectiveFunction(new MyObjectiveFunction()),
	// new ObjectiveFunctionGradient(new MyGradient()),
	// GoalType.MINIMIZE,
	// new InitialGuess(
	// (initialPoint != null) ? initialPoint
	// : new double[this.transitions
	// .size() - 1]), new MaxEval(
	// Integer.MAX_VALUE), new MaxIter(
	// this.maxIterations));
	// } catch (TooManyEvaluationsException e) {
	// initialBracketingRange *= 10.0;
	// Logger.getLogger(this.getClass().getName()).info(
	// "Extending initial bracketing range to "
	// + initialBracketingRange);
	// result = null;
	// }
	// } while (result == null);
	// return result.getPoint();
	// }
	// }

	/*
	 * An unconstrained nonlinear optimization routine is used to compute
	 * optimal alpha. To ensure that alpha represents a proper probability
	 * distribution, the problem is reformulated with the utilities of a
	 * multinomial logit model as decision variables.
	 */

	private static final double V_MIN = -10.0;

	private static final double V_MAX = +10.0;

	private final boolean useBounds = true;

	private double boundPenaltyValue(final double[] point) {
		double result = 0;
		if (this.useBounds) {
			for (int i = 0; i < point.length; i++) {
				if (point[i] < V_MIN) {
					result += (point[i] - V_MIN) * (point[i] - V_MIN);
				} else if (point[i] > V_MAX) {
					result += (point[i] - V_MAX) * (point[i] - V_MAX);
				}
			}
		}
		return result;
	}

	private double[] boundPenaltyGradient(final double[] point) {
		final double[] result = new double[point.length];
		if (this.useBounds) {
			for (int i = 0; i < point.length; i++) {
				if (point[i] < V_MIN) {
					result[i] = 2.0 * (point[i] - V_MIN);
				} else if (point[i] > V_MAX) {
					result[i] = 2.0 * (point[i] - V_MAX);
				}
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

	private class MyObjectiveFunction2 implements MultivariateFunction {
		@Override
		public double value(double[] point) {
			final Vector alphas = new Vector(point);
			return surrogateObjectiveFunctionValue(alphas);
		}
	}

	private class MyGradient2 implements MultivariateVectorFunction {
		@Override
		public double[] value(double[] point) {
			final Vector alphas = new Vector(point);
			return surrogateObjectiveFunction.dSurrObjFctVal_dAlpha(alphas)
					.toArray();
		}
	}

	private class MyLineSearch implements LineSearch {

		@Override
		public double stepLength(final double[] pnt, final double[] dir) {

			final double v = surrogateObjectiveFunction
					.getEquilibriumGapWeight();
			final double w = surrogateObjectiveFunction.getUniformityWeight();
			final double eg = surrogateObjectiveFunction
					.equilibriumGap(new Vector(pnt));

			final Vector deltaSumTimesDir = new Vector(transitions.get(0)
					.getDelta().size());
			final Vector deltaSumTimesPnt = new Vector(transitions.get(0)
					.getDelta().size());
			for (int i = 0; i < transitions.size(); i++) {
				deltaSumTimesDir.add(transitions.get(i).getDelta(), dir[i]);
				deltaSumTimesPnt.add(transitions.get(i).getDelta(), pnt[i]);
			}

			double num = 0;
			for (int i = 0; i < transitions.size(); i++) {
				num -= eg * dir[i]
						* transitions.get(i).getToStateObjectiveFunctionValue();
			}
			num -= v * deltaSumTimesDir.innerProd(deltaSumTimesPnt);
			num -= 2.0 * w * eg * FrankWolfe.innerProd(dir, pnt);

			double denom = v * deltaSumTimesDir.innerProd(deltaSumTimesDir);
			denom += 2.0 * w * eg * FrankWolfe.innerProd(dir, dir);

			// Logger.getLogger(this.getClass().getName()).info(
			// "num=" + num + ", denom=" + denom);

			if (Math.abs(num) > 0) {
				return (num / denom);
			} else {
				return 0.0;
			}
		}
	}
}
