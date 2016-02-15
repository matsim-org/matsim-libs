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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

import floetteroed.utilities.math.Matrix;
import floetteroed.utilities.math.Vector;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class WeightOptimizer2 {

	private final double initialEquilGapWeight;

	private final double initialUnifGapWeight;

	private final List<Double> dSurrObjFct_dEquilGapWeights = new ArrayList<>();

	private final List<Double> dSurrObjFct_dUnifGapWeights = new ArrayList<>();

	private final List<Double> finalSurrogateObjectiveFunctionValues = new ArrayList<>();

	private final List<Double> finalObjFctValues = new ArrayList<>();

	private final List<Double> equilGapWeights = new ArrayList<>();

	private final List<Double> unifGapWeights = new ArrayList<>();

	// -------------------- MEMBERS --------------------

	// private final RealVector dInterpolatedObjectiveFunction_dAlpha;
	//
	// private final RealVector dEquilibriumGap_dAlpha;
	//
	// private final RealMatrix d2EquilibriumGap_dAlpha2;
	//
	// private final RealVector dUniformityGap_dAlpha;
	//
	// private final RealMatrix d2UniformityGap_dAlpha2;

	// -------------------- CONSTRUCTION --------------------

	public WeightOptimizer2(final double initialEquilGapWeight,
			final double initialUnifGapWeight) {
		this.initialEquilGapWeight = initialEquilGapWeight;
		this.initialUnifGapWeight = initialUnifGapWeight;
	}

	// WeightOptimizer2(
	// final SurrogateObjectiveFunction<?> lastSurrogateObjectiveFunction,
	// final Vector trialAlphas) {
	// this(lastSurrogateObjectiveFunction
	// .dInterpolObjFctVal_dAlpha(trialAlphas),
	// lastSurrogateObjectiveFunction
	// .dEquilibriumGap_dAlpha(trialAlphas),
	// lastSurrogateObjectiveFunction
	// .d2EquilibriumGapdAlpha2(trialAlphas),
	// lastSurrogateObjectiveFunction
	// .dUniformityGap_dAlpha(trialAlphas),
	// lastSurrogateObjectiveFunction
	// .d2UniformityGapdAlpha2(trialAlphas));
	// }

	// private WeightOptimizer2(final Vector
	// dInterpolatedObjectiveFunctiondAlpha,
	// final Vector dEquilibriumGapdAlpha,
	// final Matrix d2EquilibriumGapdAlpha2,
	// final Vector dUniformityGapdAlpha,
	// final Matrix d2UniformityGapdAlpha2) {
	// this.dInterpolatedObjectiveFunction_dAlpha =
	// toRealVector(dInterpolatedObjectiveFunctiondAlpha);
	// this.dEquilibriumGap_dAlpha = toRealVector(dEquilibriumGapdAlpha);
	// this.d2EquilibriumGap_dAlpha2 = toRealMatrix(d2EquilibriumGapdAlpha2);
	// this.dUniformityGap_dAlpha = toRealVector(dUniformityGapdAlpha);
	// this.d2UniformityGap_dAlpha2 = toRealMatrix(d2UniformityGapdAlpha2);
	// }

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

	// private int alphaSize(final RealVector dEquilibriumGap_dAlpha) {
	// return dEquilibriumGap_dAlpha.getDimension();
	// }

	// -------------------- IMPLEMENTATION --------------------

	private RealVector dSurrObjFct_dWeights(final double equilGapWeight,
			final double unifGapWeight,
			final RealVector dInterpolatedObjectiveFunction_dAlpha,
			final RealVector dEquilibriumGap_dAlpha,
			final RealVector dUniformityGap_dAlpha) {
		final RealVector result = dInterpolatedObjectiveFunction_dAlpha.copy();
		result.combineToSelf(1.0, equilGapWeight, dEquilibriumGap_dAlpha);
		result.combineToSelf(1.0, unifGapWeight, dUniformityGap_dAlpha);
		return result;
	}

	private RealMatrix d2SurrObjFct_dWeights2(final double equilGapWeight,
			final double unifGapWeight,
			final RealMatrix d2EquilibriumGap_dAlpha2,
			final RealMatrix d2UniformityGap_dAlpha2) {
		final RealMatrix addend1 = d2EquilibriumGap_dAlpha2.copy();
		addend1.scalarMultiply(equilGapWeight);
		final RealMatrix addend2 = d2UniformityGap_dAlpha2.copy();
		addend2.scalarMultiply(unifGapWeight);
		return addend1.add(addend2);
	}

//	public double[] updateWeights(
//			final double equilGapWeight,
//			final double unifGapWeight,
//			final SamplingStage<?> lastSamplingStage,
//			final double finalObjFctValue,
//			final double finalEquilGap,
//			final double finalUnifGap,
//			final SurrogateObjectiveFunction<?> finalSurrogateObjectiveFunction,
//			final Vector finalAlphas) {
//		return this.updateWeights(equilGapWeight, unifGapWeight,
//				lastSamplingStage.getEquilibriumGap(),
//				lastSamplingStage.getUniformityGap(), finalObjFctValue,
//				lastSamplingStage.getSurrogateObjectiveFunctionValue(),
//				finalEquilGap, finalUnifGap, finalSurrogateObjectiveFunction,
//				finalAlphas);
//	}

//	private double[] updateWeights(
//			final double equilGapWeight,
//			final double unifGapWeight,
//			final double equilGap,
//			final double unifGap,
//			final double finalObjFctValue,
//			final double finalSurrogateObjectiveFunctionValue,
//			final double finalEquilGap,
//			final double finalUnifGap,
//			final SurrogateObjectiveFunction<?> finalSurrogateObjectiveFunction,
//			final Vector finalAlphas) {
//
//		/*
//		 * Extract gradients and Hessians.
//		 */
//		final RealVector dInterpolatedObjectiveFunction_dAlpha = toRealVector(finalSurrogateObjectiveFunction
//				.dInterpolObjFctVal_dAlpha(finalAlphas));
//		final RealVector dEquilibriumGap_dAlpha = toRealVector(finalSurrogateObjectiveFunction
//				.dEquilibriumGap_dAlpha(finalAlphas));
//		final RealMatrix d2EquilibriumGap_dAlpha2 = toRealMatrix(finalSurrogateObjectiveFunction
//				.d2EquilibriumGapdAlpha2(finalAlphas));
//		final RealVector dUniformityGap_dAlpha = toRealVector(finalSurrogateObjectiveFunction
//				.dUniformityGap_dAlpha(finalAlphas));
//		final RealMatrix d2UniformityGap_dAlpha2 = toRealMatrix(finalSurrogateObjectiveFunction
//				.d2UniformityGapdAlpha2(finalAlphas));
//
//		/*
//		 * Compute gradients etc.
//		 */
//		final RealVector dSurrObjFct_dAlpha = this.dSurrObjFct_dWeights(
//				equilGapWeight, unifGapWeight,
//				dInterpolatedObjectiveFunction_dAlpha, dEquilibriumGap_dAlpha,
//				dUniformityGap_dAlpha);
//		final RealMatrix d2SurrObjFct_dWeights2 = this.d2SurrObjFct_dWeights2(
//				equilGapWeight, unifGapWeight, d2EquilibriumGap_dAlpha2,
//				d2UniformityGap_dAlpha2);
//		final RealMatrix inverse_d2SurrObjFct_dAlpha2 = new LUDecomposition(
//				d2SurrObjFct_dWeights2).getSolver().getInverse();
//
//		final RealMatrix dInverseHessian_dEquilGapWeight = inverse_d2SurrObjFct_dAlpha2
//				.multiply(d2EquilibriumGap_dAlpha2).multiply(
//						inverse_d2SurrObjFct_dAlpha2);
//		dInverseHessian_dEquilGapWeight.scalarMultiply(-1.0);
//		final RealMatrix dInverseHessian_dUnifGapWeight = inverse_d2SurrObjFct_dAlpha2
//				.multiply(d2UniformityGap_dAlpha2).multiply(
//						inverse_d2SurrObjFct_dAlpha2);
//		dInverseHessian_dUnifGapWeight.scalarMultiply(-1.0);
//
//		double dSurrObjFct_dEquilGapWeight = 0;
//		double dSurrObjFct_dUnifGapWeight = 0;
//		for (int i = 0; i < dEquilibriumGap_dAlpha.getDimension(); i++) {
//			for (int j = 0; j < dEquilibriumGap_dAlpha.getDimension(); j++) {
//				dSurrObjFct_dEquilGapWeight += dEquilibriumGap_dAlpha
//						.getEntry(i)
//						* dSurrObjFct_dAlpha.getEntry(j)
//						* inverse_d2SurrObjFct_dAlpha2.getEntry(i, j)
//						+ dSurrObjFct_dAlpha.getEntry(i)
//						* dEquilibriumGap_dAlpha.getEntry(j)
//						* inverse_d2SurrObjFct_dAlpha2.getEntry(i, j)
//						+ dSurrObjFct_dAlpha.getEntry(i)
//						* dSurrObjFct_dAlpha.getEntry(j)
//						* dInverseHessian_dEquilGapWeight.getEntry(i, j);
//				dSurrObjFct_dUnifGapWeight += dUniformityGap_dAlpha.getEntry(i)
//						* dSurrObjFct_dAlpha.getEntry(j)
//						* inverse_d2SurrObjFct_dAlpha2.getEntry(i, j)
//						+ dSurrObjFct_dAlpha.getEntry(i)
//						* dUniformityGap_dAlpha.getEntry(j)
//						* inverse_d2SurrObjFct_dAlpha2.getEntry(i, j)
//						+ dSurrObjFct_dAlpha.getEntry(i)
//						* dSurrObjFct_dAlpha.getEntry(j)
//						* dInverseHessian_dUnifGapWeight.getEntry(i, j);
//			}
//		}
//		dSurrObjFct_dEquilGapWeight = equilGap - 0.5
//				* dSurrObjFct_dEquilGapWeight;
//		dSurrObjFct_dUnifGapWeight = unifGap - 0.5 * dSurrObjFct_dUnifGapWeight;
//
//		/*
//		 * Add one data point.
//		 */
//		this.equilGapWeights.add(equilGapWeight);
//		this.unifGapWeights.add(unifGap);
//		this.finalObjFctValues.add(finalObjFctValue);
//		this.finalSurrogateObjectiveFunctionValues
//				.add(finalSurrogateObjectiveFunctionValue);
//		this.dSurrObjFct_dEquilGapWeights.add(dSurrObjFct_dEquilGapWeight);
//		this.dSurrObjFct_dUnifGapWeights.add(dSurrObjFct_dUnifGapWeight);
//
//		/*
//		 * Solve the linear problem; in case of problems fall back to the
//		 * previous solution.
//		 */
//		final LeastAbsoluteDeviations lad = new LeastAbsoluteDeviations();
//		lad.setLowerBounds(0.0, 0.0);
//		lad.add(new Vector(initialEquilGapWeight, initialUnifGapWeight), 0.0);
//		for (int i = 0; i < this.finalObjFctValues.size(); i++) {
//			lad.add(new Vector(this.dSurrObjFct_dEquilGapWeights.get(i),
//					this.dSurrObjFct_dUnifGapWeights.get(i)),
//					this.finalObjFctValues.get(i)
//							- this.finalSurrogateObjectiveFunctionValues.get(i)
//							+ this.dSurrObjFct_dEquilGapWeights.get(i)
//							* this.equilGapWeights.get(i)
//							+ this.dSurrObjFct_dUnifGapWeights.get(i)
//							* this.unifGapWeights.get(i));
//		}
//		try {
//			lad.solve();
//			return new double[] { lad.getCoefficients().get(0),
//					lad.getCoefficients().get(1) };
//		} catch (Exception e) {
//			Logger.getLogger(this.getClass().getName()).error(
//					"failed to update search parameters: " + e.getMessage());
//			return new double[] { equilGapWeight, unifGapWeight };
//		}
//	}
}
