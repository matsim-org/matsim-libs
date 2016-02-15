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
package vind;

import static org.apache.commons.math3.optim.linear.Relationship.EQ;
import static org.apache.commons.math3.optim.linear.Relationship.GEQ;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.linear.LinearConstraint;
import org.apache.commons.math3.optim.linear.LinearConstraintSet;
import org.apache.commons.math3.optim.linear.LinearObjectiveFunction;
import org.apache.commons.math3.optim.linear.NoFeasibleSolutionException;
import org.apache.commons.math3.optim.linear.SimplexSolver;
import org.apache.commons.math3.optim.linear.UnboundedSolutionException;

import floetteroed.opdyts.trajectorysampling.SamplingStage;
import floetteroed.utilities.math.Matrix;
import floetteroed.utilities.math.Vector;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class WeightOptimizer15 {

	// -------------------- MEMBERS --------------------

	private final RealVector dInterpolatedObjectiveFunction_dAlpha;

	private final RealVector dEquilibriumGap_dAlpha;

	private final RealMatrix d2EquilibriumGap_dAlpha2;

	private final RealVector dUniformityGap_dAlpha;

	private final RealMatrix d2UniformityGap_dAlpha2;

	// -------------------- CONSTRUCTION --------------------

//	WeightOptimizer15(
//			final SurrogateObjectiveFunction<?> lastSurrogateObjectiveFunction,
//			final Vector trialAlphas) {
//		this(lastSurrogateObjectiveFunction
//				.dInterpolObjFctVal_dAlpha(trialAlphas),
//				lastSurrogateObjectiveFunction
//						.dEquilibriumGap_dAlpha(trialAlphas),
//				lastSurrogateObjectiveFunction
//						.d2EquilibriumGapdAlpha2(trialAlphas),
//				lastSurrogateObjectiveFunction
//						.dUniformityGap_dAlpha(trialAlphas),
//				lastSurrogateObjectiveFunction
//						.d2UniformityGapdAlpha2(trialAlphas));
//	}

	private WeightOptimizer15(final Vector dInterpolatedObjectiveFunctiondAlpha,
			final Vector dEquilibriumGapdAlpha,
			final Matrix d2EquilibriumGapdAlpha2,
			final Vector dUniformityGapdAlpha,
			final Matrix d2UniformityGapdAlpha2) {
		this.dInterpolatedObjectiveFunction_dAlpha = toRealVector(dInterpolatedObjectiveFunctiondAlpha);
		this.dEquilibriumGap_dAlpha = toRealVector(dEquilibriumGapdAlpha);
		this.d2EquilibriumGap_dAlpha2 = toRealMatrix(d2EquilibriumGapdAlpha2);
		this.dUniformityGap_dAlpha = toRealVector(dUniformityGapdAlpha);
		this.d2UniformityGap_dAlpha2 = toRealMatrix(d2UniformityGapdAlpha2);
	}

	// -------------------- INTERNALS --------------------

	// TODO move elsewhere
	private RealVector toRealVector(final Vector vector) {
		final RealVector result = new ArrayRealVector(vector.size());
		for (int i = 0; i < vector.size(); i++) {
			result.setEntry(i, vector.get(i));
		}
		return result;
	}

	// TODO move elsewhere
	private RealMatrix toRealMatrix(final Matrix matrix) {
		final RealMatrix result = new Array2DRowRealMatrix(matrix.rowSize(),
				matrix.columnSize());
		for (int i = 0; i < matrix.rowSize(); i++) {
			result.setRowVector(i, this.toRealVector(matrix.getRow(i)));
		}
		return result;
	}

	private int alphaSize() {
		return this.dEquilibriumGap_dAlpha.getDimension();
	}

	// -------------------- IMPLEMENTATION --------------------

	private RealVector dSurrObjFct_dWeights(final double equilGapWeight,
			final double unifGapWeight) {
		final RealVector result = this.dInterpolatedObjectiveFunction_dAlpha
				.copy();
		result.combineToSelf(1.0, equilGapWeight, this.dEquilibriumGap_dAlpha);
		result.combineToSelf(1.0, unifGapWeight, this.dUniformityGap_dAlpha);
		return result;
	}

	private RealMatrix d2SurrObjFct_dWeights2(final double equilGapWeight,
			final double unifGapWeight) {
		final RealMatrix addend1 = this.d2EquilibriumGap_dAlpha2.copy();
		addend1.scalarMultiply(equilGapWeight);
		final RealMatrix addend2 = this.d2UniformityGap_dAlpha2.copy();
		addend2.scalarMultiply(unifGapWeight);
		return addend1.add(addend2);
	}

	double[] updateWeights(final double equilGapWeight,
			final double unifGapWeight,
			final SamplingStage<?> lastSamplingStage,
			final double finalObjFctValue, final double finalEquilGap,
			final double finalUnifGap) {
		return this.updateWeights(equilGapWeight, unifGapWeight,
				lastSamplingStage.getEquilibriumGap(),
				lastSamplingStage.getUniformityGap(), finalObjFctValue,
				lastSamplingStage.getSurrogateObjectiveFunctionValue(),
				finalEquilGap, finalUnifGap);
	}

	private double[] updateWeights(final double equilGapWeight,
			final double unifGapWeight, final double equilGap,
			final double unifGap, final double finalObjFctValue,
			final double finalSurrogateObjectiveFunctionValue,
			final double finalEquilGap, final double finalUnifGap) {

		/*
		 * Compute gradients etc.
		 */
		final RealVector dSurrObjFct_dAlpha = this.dSurrObjFct_dWeights(
				equilGapWeight, unifGapWeight);
		final RealMatrix d2SurrObjFct_dWeights2 = this.d2SurrObjFct_dWeights2(
				equilGapWeight, unifGapWeight);
		final RealMatrix inverse_d2SurrObjFct_dAlpha2 = new LUDecomposition(
				d2SurrObjFct_dWeights2).getSolver().getInverse();

		final RealMatrix dInverseHessian_dEquilGapWeight = inverse_d2SurrObjFct_dAlpha2
				.multiply(this.d2EquilibriumGap_dAlpha2).multiply(
						inverse_d2SurrObjFct_dAlpha2);
		dInverseHessian_dEquilGapWeight.scalarMultiply(-1.0);
		final RealMatrix dInverseHessian_dUnifGapWeight = inverse_d2SurrObjFct_dAlpha2
				.multiply(this.d2UniformityGap_dAlpha2).multiply(
						inverse_d2SurrObjFct_dAlpha2);
		dInverseHessian_dUnifGapWeight.scalarMultiply(-1.0);

		double dSurrObjFct_dEquilGapWeight = 0;
		double dSurrObjFct_dUnifGapWeight = 0;
		for (int i = 0; i < this.alphaSize(); i++) {
			for (int j = 0; j < this.alphaSize(); j++) {
				dSurrObjFct_dEquilGapWeight += this.dEquilibriumGap_dAlpha
						.getEntry(i)
						* dSurrObjFct_dAlpha.getEntry(j)
						* inverse_d2SurrObjFct_dAlpha2.getEntry(i, j)
						+ dSurrObjFct_dAlpha.getEntry(i)
						* this.dEquilibriumGap_dAlpha.getEntry(j)
						* inverse_d2SurrObjFct_dAlpha2.getEntry(i, j)
						+ dSurrObjFct_dAlpha.getEntry(i)
						* dSurrObjFct_dAlpha.getEntry(j)
						* dInverseHessian_dEquilGapWeight.getEntry(i, j);
				dSurrObjFct_dUnifGapWeight += this.dUniformityGap_dAlpha
						.getEntry(i)
						* dSurrObjFct_dAlpha.getEntry(j)
						* inverse_d2SurrObjFct_dAlpha2.getEntry(i, j)
						+ dSurrObjFct_dAlpha.getEntry(i)
						* this.dUniformityGap_dAlpha.getEntry(j)
						* inverse_d2SurrObjFct_dAlpha2.getEntry(i, j)
						+ dSurrObjFct_dAlpha.getEntry(i)
						* dSurrObjFct_dAlpha.getEntry(j)
						* dInverseHessian_dUnifGapWeight.getEntry(i, j);
			}
		}
		dSurrObjFct_dEquilGapWeight = equilGap - 0.5
				* dSurrObjFct_dEquilGapWeight;
		dSurrObjFct_dUnifGapWeight = unifGap - 0.5 * dSurrObjFct_dUnifGapWeight;

		/*
		 * (Try to) solve the linear problem, otherwise fall back to previous
		 * solution.
		 */
		final LinearObjectiveFunction objectiveFunction = new LinearObjectiveFunction(
				new double[] { dSurrObjFct_dEquilGapWeight,
						dSurrObjFct_dUnifGapWeight }, 0.0);
		final List<LinearConstraint> constraints = new ArrayList<>(4);
		constraints.add(new LinearConstraint(new double[] { 1.0, 0.0 }, GEQ,
				0.0));
		constraints.add(new LinearConstraint(new double[] { 0.0, 1.0 }, GEQ,
				0.0));
		constraints
				.add(new LinearConstraint(
						new double[] { dSurrObjFct_dEquilGapWeight,
								dSurrObjFct_dUnifGapWeight },
						GEQ,
						(finalObjFctValue
								- finalSurrogateObjectiveFunctionValue
								+ dSurrObjFct_dEquilGapWeight * equilGap + dSurrObjFct_dUnifGapWeight
								* unifGap)));
		constraints.add(new LinearConstraint(new double[] { finalEquilGap,
				-finalUnifGap }, EQ, 0.0));
		final LinearConstraintSet allConstraints = new LinearConstraintSet(
				constraints);
		try {
			final PointValuePair result = new SimplexSolver().optimize(
					objectiveFunction, allConstraints);
			return result.getPoint();
		} catch (UnboundedSolutionException e) {
			Logger.getLogger(this.getClass().getName()).warning(e.toString());
			return new double[] { equilGap, unifGap };
		} catch (NoFeasibleSolutionException e) {
			Logger.getLogger(this.getClass().getName()).warning(e.toString());
			return new double[] { equilGap, unifGap };
		}
	}
}
