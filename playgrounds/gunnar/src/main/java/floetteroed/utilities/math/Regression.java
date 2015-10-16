/*
 * Copyright 2015 Gunnar Flötteröd
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * contact: gunnar.floetteroed@abe.kth.se
 *
 */ 
package floetteroed.utilities.math;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;



/**
 * A recursive regression. Builds a "full" ordinary regression by incrementally
 * accounting for the measurements. Has tracking capabilities.
 * 
 * @author Gunnar Flötteröd
 * 
 * @see http://en.wikipedia.org/wiki/Recursive_least_squares
 * 
 */
public class Regression implements Serializable {

	// -------------------- CONSTANTS --------------------

	private static final long serialVersionUID = 1L;

	// -------------------- MEMBERS --------------------

	private final List<SignalSmoother> avgInputs;

	private double offset = 0;

	private Vector coefficients;

	private Matrix precisionMatrix;

	private double inertia;

	// -------------------- CONSTRUCTION --------------------

	/**
	 * Creates a Regression instance that operates on the passed data
	 * structures.
	 * 
	 * @param inertia
	 *            the inertia of this regression; must be in (0,1]; a value of
	 *            1.0 corresponds to an ordinary regression and lower values
	 *            cause an exponential forgetting of earlier measurements
	 * @param coefficients
	 *            a vector of initial parameters for this regression; must have
	 *            at least one entry
	 * @param precisionMatrix
	 *            the precision matrix of this calibration; must have the same
	 *            row and column dimension as the parameters vector
	 */
	public Regression(final double inertia, final Vector coefficients,
			final Matrix precisionMatrix) {

		if (coefficients == null) {
			throw new IllegalArgumentException(
					"initial coefficent vector is null");
		}
		if (precisionMatrix == null) {
			throw new IllegalArgumentException("precisionMatrix is null");
		}
		if (precisionMatrix.rowSize() != coefficients.size()
				|| precisionMatrix.columnSize() != coefficients.size()) {
			throw new IllegalArgumentException(
					"dimension of precision matrix is inconsistent with "
							+ "size of coefficient vector");
		}

		this.avgInputs = new ArrayList<SignalSmoother>(coefficients.size());
		for (int i = 0; i < coefficients.size(); i++) {
			this.avgInputs.add(null);
		}

		this.setInertia(inertia);
		this.coefficients = coefficients;
		this.precisionMatrix = precisionMatrix;
	}

	/**
	 * Creates a dim-dimensional regression with the given intertia and a
	 * precision matrix only the value of 1e6 only on the main diagonal
	 * 
	 * @param inertia
	 *            the inertia of this regression; must be in (0,1]; a value of
	 *            1.0 corresponds to an ordinary regression and lower values
	 *            cause an exponential forgetting of earlier measurements
	 * @param dim
	 *            the dimension of this regression; must be strictly positive
	 */
	public Regression(final double inertia, final int dim) {
		this(inertia, new Vector(dim), Matrix.newDiagonal(dim, 1e6));
	}

	/**
	 * Adds enlargement parameters to this regression. The parameters vector is
	 * initialized with an appropriate number of additional zeros, and the
	 * precision matrix is initialized with an appropriate number of zero rows
	 * and columns, where all new main diagonal entries are initialized with the
	 * value 1e6.
	 * 
	 * @param enlargement
	 *            by how many parameters this regression is to be enlarged; must
	 *            not be negative
	 */
	public void appendParameters(final int enlargement) {
		this.coefficients = this.coefficients.copyEnlarged(enlargement);
		this.precisionMatrix = this.precisionMatrix.copyEnlarged(enlargement,
				enlargement);
		for (int i = this.coefficients.size() - enlargement; i < this.coefficients
				.size(); i++) {
			this.precisionMatrix.getRow(i).set(i, 1e6);
		}
	}

	// -------------------- IMPLEMENTATION --------------------

	public void enableInputCentering(final int i) {
		final SignalSmoother avgInput = new SignalSmoother(1.0 - this.inertia);
		if (this.inertia == 1.0) {
			avgInput.freeze();
		}
		this.avgInputs.set(i, avgInput);
	}

	/**
	 * Returns a reference to the coefficient vector of this regression.
	 * 
	 * @return a reference to the coefficient vector of this regression
	 */
	public Vector getCoefficients() {
		return this.coefficients;
	}

	/**
	 * Returns a reference to the precision matrix of this regression. This
	 * matrix is only proportional to the covariance matrix of the parameter
	 * estimates. The actual parameter covariance matrix can be obtained by
	 * multiplying the precision matrix with the (estimated) variance of the
	 * residual error.
	 * 
	 * @return a reference to the precision matrix of this regression
	 */
	public Matrix getPrecisionMatrix() {
		return this.precisionMatrix;
	}

	/**
	 * Sets the inertia of this regression to the given value.
	 * 
	 * @param inertia
	 *            the new intertia; must be in (0, 1]
	 */
	public void setInertia(final double inertia) {
		if (inertia <= 0 || inertia > 1) {
			throw new IllegalArgumentException("lambda must be in (0,1]");
		}
		this.inertia = inertia;

		for (int i = 0; i < this.avgInputs.size(); i++) {
			final SignalSmoother avgInput = this.avgInputs.get(i);
			if (avgInput != null) {
				if (inertia < 1.0) {
					avgInput.setInnovationWeight(1.0 - inertia);
				} else {
					avgInput.freeze();
				}
			}
		}
	}

	/**
	 * Returns the current intertia of this regression.
	 * 
	 * @return the current intertia of this regression
	 */
	public double getInertia() {
		return this.inertia;
	}

	/**
	 * Returns the size of this regression's parameter vector.
	 * 
	 * @return the size of this regression's parameter vector
	 */
	public int getDimension() {
		return getCoefficients().size();
	}

	private Vector input(final Vector x, final boolean update) {
		final Vector result = x.copy();
		for (int i = 0; i < this.avgInputs.size(); i++) {
			final SignalSmoother avgInput = this.avgInputs.get(i);
			if (avgInput != null) {
				if (update) {
					final double oldAvgInput = avgInput.getSmoothedValue();
					avgInput.addValue(x.get(i));
					this.offset += this.coefficients.get(i)
							* (avgInput.getSmoothedValue() - oldAvgInput);
				}
				result.add(i, -avgInput.getSmoothedValue());
			}
		}
		return result;
	}

	/**
	 * Applies the model estimated by this regression to the input vector x.
	 * 
	 * @param x
	 *            input vector for the model; size must be consistent with the
	 *            dimension of this regression
	 * @return the output of the estimated model given the input x
	 */
	public double predict(final Vector x) {
		return input(x, false).innerProd(this.getCoefficients()) + this.offset;
	}

	/**
	 * Updates the coefficients of this regression with the (input, output) pair
	 * (x, y).
	 * 
	 * @param x
	 *            the new input vector; size must be consistent with the
	 *            dimension of this regression
	 * @param y
	 *            the new output value
	 */
	public void update(final Vector x, double y) {

		final Vector xCentered = this.input(x, true);
		y -= this.offset;

		final Vector xP = this.precisionMatrix.timesVectorFromLeft(xCentered);
		final double gScale = 1.0 / (this.inertia + xP.innerProd(xCentered));

		this.getCoefficients().add(xP,
				(y - xCentered.innerProd(this.getCoefficients())) * gScale);

		this.precisionMatrix.addOuterProduct(xP, xP, -gScale);
		this.precisionMatrix.mult(1.0 / this.inertia);

		// current Matrix implementation does not exploit symmetry
		this.precisionMatrix.symmetrize();
	}
}
