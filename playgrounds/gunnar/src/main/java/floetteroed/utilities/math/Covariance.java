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


/**
 * 
 * @author Gunnar Flötteröd
 * 
 */
public class Covariance {

	// -------------------- MEMBERS --------------------

	private final Matrix xySum;

	private final Vector xSum;

	private final Vector ySum;

	private int n;

	// -------------------- CONSTRUCTION --------------------

	public Covariance(final int xDim, final int yDim) {
		if (xDim < 1) {
			throw new IllegalArgumentException("xDim is smaller than one");
		}
		if (yDim < 1) {
			throw new IllegalArgumentException("yDim is smaller than one");
		}
		this.xySum = new Matrix(xDim, yDim);
		this.xSum = new Vector(xDim);
		this.ySum = new Vector(yDim);
		this.n = 0;
	}

	// -------------------- IMPLEMENTATION --------------------

	public void add(final Vector x, final Vector y) {
		this.xySum.addOuterProduct(x, y, 1.0);
		this.xSum.add(x, 1.0);
		this.ySum.add(y, 1.0);
		this.n++;
	}

	// TODO NEW
	public Vector getMeanX() {
		final Vector result = this.xSum.copy();
		result.mult(1.0 / this.n);
		return result;
	}

	// TODO NEW
	public Vector getMeanY() {
		final Vector result = this.ySum.copy();
		result.mult(1.0 / this.n);
		return result;
	}

	public Matrix getCovariance() {
		final Matrix result = new Matrix(this.xSum.size(), this.ySum.size());
		result.add(this.xySum, 1.0 / (this.n - 1.0));
		result.addOuterProduct(this.xSum, this.ySum, (-1.0) / this.n
				/ (this.n - 1.0));
		return result;
	}

	public static Matrix turnCovarianceIntoCorrelation(final Matrix covariance) {
		final Matrix result = covariance;
		/*
		 * (1) compute off-diagonal correlation values
		 */
		for (int i = 0; i < result.rowSize(); i++) {
			for (int j = 0; j < i; j++) {
				final double cov = result.getRow(i).get(j);
				final double var1 = result.getRow(i).get(i);
				final double var2 = result.getRow(j).get(j);
				final double corr = cov / Math.sqrt(var1) / Math.sqrt(var2);
				result.getRow(i).set(j, corr);
				result.getRow(j).set(i, corr);
			}
		}
		/*
		 * (2) normalize main diagonal values to one
		 */
		for (int i = 0; i < result.rowSize(); i++) {
			result.getRow(i).set(i, 1.0);
		}
		return result;
	}

	// TODO NEW
	public Matrix getCorrelation() {
		return turnCovarianceIntoCorrelation(this.getCovariance());
	}
}
