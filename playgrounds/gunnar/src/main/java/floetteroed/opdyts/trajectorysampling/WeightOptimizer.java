package floetteroed.opdyts.trajectorysampling;

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
import org.apache.commons.math3.optim.linear.SimplexSolver;
import org.apache.commons.math3.optim.linear.UnboundedSolutionException;

import floetteroed.utilities.math.Matrix;
import floetteroed.utilities.math.Vector;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class WeightOptimizer {

	private final RealVector dInterpolatedObjectiveFunctiondAlpha;

	private final RealVector dEquilibriumGapdAlpha;

	private final RealMatrix d2EquilibriumGapdAlpha2;

	private final RealVector dUniformityGapdAlpha;

	private final RealMatrix d2UniformityGapdAlpha2;

	WeightOptimizer(final Vector dInterpolatedObjectiveFunctiondAlpha,
			final Vector dEquilibriumGapdAlpha,
			final Matrix d2EquilibriumGapdAlpha2,
			final Vector dUniformityGapdAlpha,
			final Matrix d2UniformityGapdAlpha2) {
		this.dInterpolatedObjectiveFunctiondAlpha = toRealVector(dInterpolatedObjectiveFunctiondAlpha);
		this.dEquilibriumGapdAlpha = toRealVector(dEquilibriumGapdAlpha);
		this.d2EquilibriumGapdAlpha2 = toRealMatrix(d2EquilibriumGapdAlpha2);
		this.dUniformityGapdAlpha = toRealVector(dUniformityGapdAlpha);
		this.d2UniformityGapdAlpha2 = toRealMatrix(d2UniformityGapdAlpha2);
	}

	RealVector toRealVector(final Vector vector) {
		final RealVector result = new ArrayRealVector(vector.size());
		for (int i = 0; i < vector.size(); i++) {
			result.setEntry(i, vector.get(i));
		}
		return result;
	}

	RealMatrix toRealMatrix(final Matrix matrix) {
		final RealMatrix result = new Array2DRowRealMatrix(matrix.rowSize(),
				matrix.columnSize());
		for (int i = 0; i < matrix.rowSize(); i++) {
			result.setRowVector(i, this.toRealVector(matrix.getRow(i)));
		}
		return result;
	}

	private int alphaSize() {
		return this.dEquilibriumGapdAlpha.getDimension();
	}

	// ----- serious calculations starting here -----

	private RealVector gradient(final double equilGapWeight,
			final double unifGapWeight) {
		final RealVector result = this.dInterpolatedObjectiveFunctiondAlpha
				.copy();
		result.combineToSelf(1.0, equilGapWeight, this.dEquilibriumGapdAlpha);
		result.combineToSelf(1.0, unifGapWeight, this.dUniformityGapdAlpha);
		return result;
	}

	private RealMatrix hessian(final double equilGapWeight,
			final double unifGapWeight) {
		final RealMatrix addend1 = this.d2EquilibriumGapdAlpha2.copy();
		addend1.scalarMultiply(equilGapWeight);
		final RealMatrix addend2 = this.d2UniformityGapdAlpha2.copy();
		addend2.scalarMultiply(unifGapWeight);
		return addend1.add(addend2);
	}

	private Double last_dValue_dEquilGapWeight = null;

	private Double last_dValue_dUnifGapWeight = null;

	private void update(final double equilGapWeight, final double unifGapWeight) {

		/*
		 * Preparations.
		 */
		final RealVector gradient = this
				.gradient(equilGapWeight, unifGapWeight);
		final RealMatrix hessian = this.hessian(equilGapWeight, unifGapWeight);
		final RealMatrix inverseHessian = new LUDecomposition(hessian)
				.getSolver().getInverse();

		/*
		 * Value computation.
		 */
		// final double lastValue = this.surrogateObjectiveFunctionValue0 - 0.5
		// * inverseHessian.preMultiply(gradient).dotProduct(gradient);

		/*
		 * Gradient computation.
		 */
		final RealMatrix dInverseHessian_dEquilGapWeight = inverseHessian
				.multiply(this.d2EquilibriumGapdAlpha2)
				.multiply(inverseHessian);
		dInverseHessian_dEquilGapWeight.scalarMultiply(-1.0);

		final RealMatrix dInverseHessian_dUnifGapWeight = inverseHessian
				.multiply(this.d2UniformityGapdAlpha2).multiply(inverseHessian);
		dInverseHessian_dUnifGapWeight.scalarMultiply(-1.0);

		double dValue_dEquilGapWeight = 0;
		double dValue_dUnifGapWeight = 0;

		for (int i = 0; i < this.alphaSize(); i++) {
			for (int j = 0; j < this.alphaSize(); j++) {
				dValue_dEquilGapWeight += dEquilibriumGapdAlpha.getEntry(i)
						* gradient.getEntry(j) * inverseHessian.getEntry(i, j)
						+ gradient.getEntry(i)
						* dEquilibriumGapdAlpha.getEntry(j)
						* inverseHessian.getEntry(i, j) + gradient.getEntry(i)
						* gradient.getEntry(j)
						* dInverseHessian_dEquilGapWeight.getEntry(i, j);
				dValue_dUnifGapWeight += dUniformityGapdAlpha.getEntry(i)
						* gradient.getEntry(j) * inverseHessian.getEntry(i, j)
						+ gradient.getEntry(i)
						* dUniformityGapdAlpha.getEntry(j)
						* inverseHessian.getEntry(i, j) + gradient.getEntry(i)
						* gradient.getEntry(j)
						* dInverseHessian_dUnifGapWeight.getEntry(i, j);

			}
		}
		this.last_dValue_dEquilGapWeight = dValue_dEquilGapWeight;
		this.last_dValue_dUnifGapWeight = dValue_dUnifGapWeight;
	}

	public double[] updateWeights(final double equilGapWeight,
			final double unifGapWeight, final double equilGap,
			final double unifGap, final double finalObjFctValue,
			final double interpolObjFctValue) {

		this.update(equilGapWeight, unifGapWeight);

		final LinearObjectiveFunction objectiveFunction = new LinearObjectiveFunction(
				new double[] { this.last_dValue_dEquilGapWeight,
						this.last_dValue_dUnifGapWeight }, 0.0);

		final List<LinearConstraint> constraints = new ArrayList<>(3);
		constraints.add(new LinearConstraint(new double[] { 1.0, 0.0 }, GEQ,
				0.0));
		constraints.add(new LinearConstraint(new double[] { 0.0, 1.0 }, GEQ,
				0.0));
		constraints.add(new LinearConstraint(
				new double[] { equilGap, unifGap }, GEQ, finalObjFctValue
						- interpolObjFctValue));
		final LinearConstraintSet allConstraints = new LinearConstraintSet(
				constraints);

		try {
			final PointValuePair result = new SimplexSolver().optimize(
					objectiveFunction, allConstraints);
			return result.getPoint();
		} catch (UnboundedSolutionException e) {
			Logger.getLogger(this.getClass().getName()).warning(e.toString());
			return new double[] { equilGap, unifGap };
		}
	}
}
