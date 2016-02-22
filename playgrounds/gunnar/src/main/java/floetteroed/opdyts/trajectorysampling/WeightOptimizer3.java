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

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.linear.LinearConstraint;
import org.apache.commons.math3.optim.linear.LinearConstraintSet;
import org.apache.commons.math3.optim.linear.LinearObjectiveFunction;
import org.apache.commons.math3.optim.linear.Relationship;
import org.apache.commons.math3.optim.linear.SimplexSolver;

import floetteroed.opdyts.DecisionVariable;
import floetteroed.utilities.math.Vector;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class WeightOptimizer3 {

	// -------------------- MEMBERS --------------------

	private double equilibriumGapWeight;

	private double uniformityGapWeight;

	private double tolerance;

	// -------------------- CONSTRUCTION --------------------

	public WeightOptimizer3(final double initialEquilibriumGapWeight,
			final double initialUniformityGapWeight, final double tolerance) {
		this.equilibriumGapWeight = initialEquilibriumGapWeight;
		this.uniformityGapWeight = initialUniformityGapWeight;
		this.tolerance = tolerance;
	}

	// -------------------- IMPLEMENTATION --------------------

	public <U extends DecisionVariable> double[] updateWeights(
			final List<Transition<U>> transitions) {

		double newEquilibriumGapWeight = this.equilibriumGapWeight;
		double newUniformityGapWeight = this.uniformityGapWeight;

		for (int rep = 1; rep < 10; rep++) {

			/*
			 * Pretend that only the optimal trajectory has been evaluated.
			 */
			final LinkedList<Double> upperBoundEquilibriumGaps = new LinkedList<>();
			final LinkedList<Double> upperBoundUniformityGaps = new LinkedList<>();
			final LinkedList<Double> upperBoundAverageObjectiveFunctions = new LinkedList<>();
			final LinkedList<Double> upperBounds = new LinkedList<>();
			final LinkedList<Double> lowerBoundEquilibriumGaps = new LinkedList<>();
			final LinkedList<Double> lowerBoundUniformityGaps = new LinkedList<>();
			final LinkedList<Double> lowerBoundAverageObjectiveFunctions = new LinkedList<>();
			final LinkedList<Double> lowerBounds = new LinkedList<>();
			for (int k = 0; k < transitions.size(); k++) {
				{
					// minimize upper bound
					final TransitionSequencesAnalyzer<U> analyzer = new TransitionSequencesAnalyzer<>(
							transitions.subList(0, k + 1),
							newEquilibriumGapWeight, newUniformityGapWeight);
					analyzer.setBound(SurrogateObjectiveFunction.Bound.UPPER);
					final Vector alphas = analyzer.optimalAlphas();
					upperBoundEquilibriumGaps.add(analyzer
							.equilibriumGap(alphas));
					upperBoundUniformityGaps.add(alphas.innerProd(alphas));
					upperBoundAverageObjectiveFunctions.add(analyzer
							.originalObjectiveFunctionValue(alphas));
					upperBounds.add(analyzer
							.surrogateObjectiveFunctionValue(alphas));
				}
				{
					// maximize lower bound
					final TransitionSequencesAnalyzer<U> analyzer = new TransitionSequencesAnalyzer<>(
							transitions.subList(0, k + 1),
							newEquilibriumGapWeight, newUniformityGapWeight);
					analyzer.setBound(SurrogateObjectiveFunction.Bound.LOWER);
					final Vector alphas = analyzer.optimalAlphas();
					lowerBoundEquilibriumGaps.add(analyzer
							.equilibriumGap(alphas));
					lowerBoundUniformityGaps.add(alphas.innerProd(alphas));
					lowerBoundAverageObjectiveFunctions.add(analyzer
							.originalObjectiveFunctionValue(alphas));
					lowerBounds.add(analyzer
							.surrogateObjectiveFunctionValue(alphas));
				}
			}

			/*
			 * Objective function.
			 * 
			 * Q([v w) = <[1 1], [v w]>
			 */
			double vCoeff = 0;
			double wCoeff = 0;
			for (int k = 0; k < transitions.size(); k++) {
				vCoeff += lowerBoundEquilibriumGaps.get(k)
						+ upperBoundEquilibriumGaps.get(k);
				wCoeff += lowerBoundUniformityGaps.get(k)
						+ upperBoundUniformityGaps.get(k);
			}
			vCoeff /= transitions.size();
			wCoeff /= transitions.size();
			final LinearObjectiveFunction objFct = new LinearObjectiveFunction(
					new double[] { vCoeff, wCoeff }, 0);
			final List<LinearConstraint> constraints = new ArrayList<>();

			/*
			 * Constraints.
			 * 
			 * <[1 0], [v w]> >= 0
			 * 
			 * <[0 1], [v w]> >= 0
			 */
			constraints.add(new LinearConstraint(new double[] { 1, 0 },
					Relationship.GEQ, 0.0));
			constraints.add(new LinearConstraint(new double[] { 0, 1 },
					Relationship.GEQ, 0.0));

			/*
			 * Constraints. for all k = 1 ... K :
			 * 
			 * <[eg_upper(k)+eg_lower(k) ug_upper(k)+ug_lower(k)], [v w]>
			 * 
			 * >= avgQ_lower(k)-avQ_upper(k)
			 */
			for (int k = 0; k < transitions.size(); k++) {
				constraints
						.add(new LinearConstraint(
								new double[] {
										(lowerBoundEquilibriumGaps.get(k) + upperBoundEquilibriumGaps
												.get(k)),
										(lowerBoundUniformityGaps.get(k) + upperBoundUniformityGaps
												.get(k)) },
								Relationship.GEQ,
								(lowerBoundAverageObjectiveFunctions.get(k) - upperBoundAverageObjectiveFunctions
										.get(k))));
			}

			// /*
			// * Objective function.
			// *
			// * Q([v w r(1) ... r(K)]) = <[0 0 1 ... 1], [v w r(1) ... r(K)]>
			// *
			// * where r(k) = | Q_inst(k) - Q_avg(k)| as below.
			// */
			// final double[] objFctCoeffs = new double[2 + transitions.size()];
			// for (int i = 2; i < objFctCoeffs.length; i++) {
			// objFctCoeffs[i] = 1.0;
			// }
			// final LinearObjectiveFunction objFct = new
			// LinearObjectiveFunction(
			// objFctCoeffs, 0);
			// final List<LinearConstraint> constraints = new ArrayList<>();
			//
			// /*
			// * Constraints mimicking the absolute deviation constraints.
			// *
			// * r(k) = | Q_inst(k) - Q_avg(k)|
			// *
			// * where Q_avg(k) = Q_surr(k) - v * eg(k) - w * ug(k)
			// *
			// * such that
			// *
			// * r(k) = | Q_inst(k) - (Q_surr(k) - v * eg(k) - w * ug(k))|
			// *
			// * = | Q_inst(k) - Q_surr(k) + v * eg(k) + w *ug(k))|
			// *
			// * Meaning that
			// *
			// * r(k) >= Q_inst(k) - Q_surr(k) + v * eg(k) + w * ug(k)
			// *
			// * => -v * eg(k) - w * ug(k) + r(k) >= Q_inst(k) - Q_surr(k)
			// *
			// * and
			// *
			// * r(k) >= -Q_inst(k) + Q_surr(k) - v * eg(k) - w * ug(k)
			// *
			// * => v * eg(k) + w * ug(k) + r(k) >= - Q_inst(k) + Q_surr(k)
			// */
			// for (int k = 0; k < transitions.size(); k++) {
			// {
			// final double[] coeffs = new double[objFctCoeffs.length];
			// coeffs[0] = -upperBoundEquilibriumGaps.get(k);
			// coeffs[1] = -upperBoundUniformityGaps.get(k);
			// coeffs[2 + k] = +1.0;
			// constraints.add(new LinearConstraint(coeffs,
			// Relationship.GEQ, transitions.get(k)
			// .getToStateObjectiveFunctionValue()
			// - upperBounds.get(k)));
			// }
			// {
			// final double[] coeffs = new double[objFctCoeffs.length];
			// coeffs[0] = +upperBoundEquilibriumGaps.get(k);
			// coeffs[1] = +upperBoundUniformityGaps.get(k);
			// coeffs[2 + k] = +1.0;
			// constraints.add(new LinearConstraint(coeffs,
			// Relationship.GEQ, -transitions.get(k)
			// .getToStateObjectiveFunctionValue()
			// + upperBounds.get(k)));
			// }
			// }
			//
			// /*
			// * Constraints.
			// *
			// * <[1 0], [v w]> >= 0
			// *
			// * <[0 1], [v w]> >= 0
			// *
			// * for all k = 1 ... K :
			// *
			// * <[eg(k) ug(k)], [v w]> >= max{-(avgQ(k)- avgQ(K)),+(avgQ(k)-
			// * avgQ(K))}
			// */
			// {
			// final double[] lowerBoundOnVCoeffs = new
			// double[objFctCoeffs.length];
			// lowerBoundOnVCoeffs[0] = 1.0;
			// constraints.add(new LinearConstraint(lowerBoundOnVCoeffs,
			// Relationship.GEQ, 0.0));
			// }
			// {
			// final double[] lowerBoundOnWCoeffs = new
			// double[objFctCoeffs.length];
			// lowerBoundOnWCoeffs[1] = 1.0;
			// constraints.add(new LinearConstraint(lowerBoundOnWCoeffs,
			// Relationship.GEQ, 0.0));
			// }
			// for (int k = 0; k < transitions.size(); k++) {
			// final double[] coeffs = new double[objFctCoeffs.length];
			// coeffs[0] = upperBoundEquilibriumGaps.get(k);
			// coeffs[1] = upperBoundUniformityGaps.get(k);
			// final double dev = upperBoundAverageObjectiveFunctions.get(k)
			// - upperBoundAverageObjectiveFunctions.getLast();
			// constraints.add(new LinearConstraint(coeffs, Relationship.GEQ,
			// this.tolerance * Math.abs(dev)));
			// }

			/*
			 * And, finally ...
			 */
			try {
				final PointValuePair result = (new SimplexSolver()).optimize(
						objFct, new LinearConstraintSet(constraints));
				final double innoWeight = 1.0 / rep;
				newEquilibriumGapWeight = innoWeight * result.getPoint()[0]
						+ (1.0 - innoWeight) * newEquilibriumGapWeight;
				newUniformityGapWeight = innoWeight * result.getPoint()[1]
						+ (1.0 - innoWeight) * newUniformityGapWeight;
			} catch (Exception e) {
				Logger.getLogger(this.getClass().getName())
						.info(e.getMessage());
				try {
					System.in.read();
				} catch (IOException e2) {
					e2.printStackTrace();
				}
			}

			// >>>>> TESTING >>>>>

			System.out.println();

			System.out.println("real\tlower\tupper");
			for (int k = 0; k < transitions.size(); k++) {
				System.out.println(transitions.get(k)
						.getToStateObjectiveFunctionValue()
						+ "\t"
						+ upperBounds.get(k) + "\t" + upperBounds.get(k));
			}

			System.out.println();

			// <<<<< TESTING <<<<<

		}

		this.equilibriumGapWeight = newEquilibriumGapWeight;
		this.uniformityGapWeight = newUniformityGapWeight;

		return new double[] { this.equilibriumGapWeight,
				this.uniformityGapWeight };
	}
}
