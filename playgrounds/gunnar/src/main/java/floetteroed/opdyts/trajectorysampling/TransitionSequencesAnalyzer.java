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

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.analysis.MultivariateFunction;
import org.apache.commons.math3.analysis.MultivariateVectorFunction;

import floetteroed.opdyts.DecisionVariable;
import floetteroed.opdyts.trajectorysampling.FrankWolfe.LineSearch;
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

	// -------------------- IMPLEMENTATION --------------------

	public SamplingStage<U> newOptimalSamplingStage(
			final Transition<U> lastTransition,
			final Double convergedObjectiveFunctionValue,
			final Map<Transition<?>, Double> transition2initialSolution) {
		final double[] result = this
				.newOptimalPoint(transition2initialSolution);
		final Vector alphas = new Vector(result);
		return new SamplingStage<>(alphas, this, lastTransition,
				convergedObjectiveFunctionValue,
				this.newTransition2solution(result));
	}

	public double[] newOptimalPoint(
			final Map<Transition<?>, Double> transition2initialSolution) {
		final FrankWolfe fw = new FrankWolfe(new MyObjectiveFunction(),
				new MyGradient(), new MyLineSearch(), 1e-6);
		fw.run(this.newInitialPoint(this.transitions.size(),
				transition2initialSolution));
		return fw.getPoint();
	}

	// -------------------- SOLUTION BOOKKEEPING --------------------

	private Map<Transition<?>, Double> newTransition2solution(double[] point) {
		final Map<Transition<?>, Double> result = new LinkedHashMap<Transition<?>, Double>();
		for (int i = 0; i < this.transitions.size(); i++) {
			result.put(this.transitions.get(i), point[i]);
		}
		return result;
	}

	private double[] newInitialPoint(final int targetDim,
			final Map<Transition<?>, Double> transition2initialSolution) {
		final double[] result = new double[targetDim];
		if (transition2initialSolution != null) {
			double recycledSum = 0;
			for (int i = 0; i < this.transitions.size(); i++) {
				final Double val = transition2initialSolution
						.get(this.transitions.get(i));
				if (val != null) {
					result[i] = val;
					recycledSum += val;
				}
			}
			if (recycledSum > 1e-3) {
				for (int i = 0; i < result.length; i++) {
					result[i] = result[i] / recycledSum;
				}
			} else {
				Arrays.fill(result, 1.0 / result.length);
			}
		} else {
			Arrays.fill(result, 1.0 / result.length);
		}
		return result;
	}

	// -------------------- OPTIMIZATION INTERNALS --------------------

	private class MyObjectiveFunction implements MultivariateFunction {
		@Override
		public double value(double[] point) {
			final Vector alphas = new Vector(point);
			return surrogateObjectiveFunctionValue(alphas);
		}
	}

	private class MyGradient implements MultivariateVectorFunction {
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

			if (Math.abs(num) > 0) {
				return (num / denom);
			} else {
				return 0.0;
			}
		}
	}
}
