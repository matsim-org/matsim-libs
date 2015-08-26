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

/**
 * 
 * Represents a matrix.
 * 
 * @author Gunnar Flötteröd
 * 
 */
public class Matrix implements Serializable {

	// -------------------- CONSTANTS --------------------

	private static final long serialVersionUID = 1L;

	// -------------------- MEMBERS --------------------

	private final Vector[] rows;

	// -------------------- CONSTRUCTION --------------------

	private Matrix(final Vector[] rows) {
		this.rows = rows;
	}

	// TODO NEW
	public Matrix(final Vector row) {
		this(new Vector[] { row.copy() });
	}

	/**
	 * Creates a matrix with rowCnt rows and colCnt columns.
	 * 
	 * @param rowCnt
	 *            the number of rows in this matrix; must be strictly positive
	 * @param colCnt
	 *            the number of columns in this matrix; must be strictly
	 *            positive
	 */
	public Matrix(final int rowCnt, final int colCnt) {
		if (rowCnt < 1) {
			throw new IllegalArgumentException(
					"matrix must have at least one row");
		}
		this.rows = new Vector[rowCnt];
		for (int i = 0; i < rowCnt; i++)
			this.rows[i] = new Vector(colCnt);
	}

	/**
	 * Returns a new diagonal Matrix with dim rows and dim columns with the val
	 * value at its diagonal
	 * 
	 * @param dim
	 *            the dimension of the new matrix, must be strictly positive
	 * @param val
	 *            the diagonal value of the new matrix
	 * 
	 * @return a new diagonal matrix that is filled with val
	 */
	public static Matrix newDiagonal(final int dim, final double val) {
		final Matrix result = new Matrix(dim, dim);
		for (int i = 0; i < dim; i++)
			result.getRow(i).set(i, val);
		return result;
	}

	public static Matrix newDiagonal(final Vector diagonal) {
		final Matrix result = new Matrix(diagonal.size(), diagonal.size());
		for (int i = 0; i < diagonal.size(); i++) {
			result.getRow(i).set(i, diagonal.get(i));
		}
		return result;
	}

	/**
	 * Returns a "deep" copy of this matrix the row size of which is increased
	 * by rowEnlargement and the column size of which is increased by
	 * columnEnlargement. All additional entries are zeros.
	 * 
	 * @param rowEnlargement
	 *            increase in row dimension; must not be negative
	 * @param columnEnlargement
	 *            increase in column dimension; must not be negative
	 * 
	 * @return a (possibly enlarged) "deep" copy of this matrix
	 */
	public Matrix copyEnlarged(final int rowEnlargement,
			final int columnEnlargement) {

		if (rowEnlargement < 0) {
			throw new IllegalArgumentException(
					"row enlargement must not be negative");
		}
		if (columnEnlargement < 0) {
			throw new IllegalArgumentException(
					"column enlargement must not be negative");
		}

		final Vector[] newRows = new Vector[this.rows.length + rowEnlargement];
		for (int row = 0; row < this.rows.length; row++) {
			newRows[row] = this.rows[row].copyEnlarged(columnEnlargement);
		}
		for (int row = this.rows.length; row < newRows.length; row++) {
			newRows[row] = new Vector(newRows[0].size());
		}
		return new Matrix(newRows);
	}

	/**
	 * Returns a "deep" copy of this matrix.
	 * 
	 * @return a "deep" copy of this matrix
	 */
	public Matrix copy() {
		return this.copyEnlarged(0, 0);
	}

	public Matrix newImmutableView() {
		final Matrix result = new Matrix(new Vector[this.rowSize()]);
		for (int i = 0; i < this.rowSize(); i++) {
			result.rows[i] = this.rows[i].newImmutableView();
		}
		return result;
	}

	// -------------------- IMPLEMENTATION --------------------

	/**
	 * Returns the row size of this matrix.
	 * 
	 * @return the row size of this matrix
	 */
	public int rowSize() {
		return rows.length;
	}

	/**
	 * Returns the column size of this matrix.
	 * 
	 * @return the column size of this matrix
	 */
	public int columnSize() {
		return rows[0].size();
	}

	/**
	 * Returns a Vector instance that represents the ith row of this matrix.
	 * Changes to this vector punch through to the matrix.
	 * 
	 * @param i
	 *            the desired row
	 * 
	 * @return a Vector instance that represents the ith row of this matrix
	 */
	public Vector getRow(final int i) {
		return this.rows[i];
	}

	/**
	 * Returns the Frobenius norm of this matrix.
	 * 
	 * @return the Frobenius norm of this matrix
	 */
	public double frobeniusNorm() {
		double result = 0;
		for (Vector row : this.rows)
			result += row.innerProd(row);
		return Math.sqrt(result);
	}

	/**
	 * Multiplies every entry of this matrix by value.
	 * 
	 * @param value
	 *            the value with which this matrix is to be multiplied
	 */
	public void mult(final double value) {
		for (Vector row : this.rows)
			row.mult(value);
	}

	public void add(final Matrix other, final double weight) {
		for (int i = 0; i < this.rowSize(); i++) {
			this.rows[i].add(other.rows[i], weight);
		}
	}

	public void clear() {
		for (Vector row : this.rows) {
			row.clear();
		}
	}

	/**
	 * Multiplies the the outer product of the vectors other1 and other2 by
	 * weight and adds it to this matrix.
	 * 
	 * @param other1
	 *            the column vector of the outer product; its size must equal
	 *            the row dimension of this matrix
	 * @param other2
	 *            the row vector of the outer product; its size must equal the
	 *            column dimension of this matrix
	 * @param weight
	 *            the value by which the outer product is to be multiplied
	 *            before it is added to this matrix
	 */
	public void addOuterProduct(final Vector other1, final Vector other2,
			final double weight) {

		if (other1.size() != this.rowSize()) {
			throw new IllegalArgumentException(
					"size of first vector must equal matrix row dimension");
		}
		if (other2.size() != this.columnSize()) {
			throw new IllegalArgumentException(
					"size of second vector must equal matrix column dimension");
		}

		for (int i = 0; i < other1.size(); i++) {
			this.getRow(i).add(other2, other1.get(i) * weight);
		}
	}

	/**
	 * Multiplies this matrix by vector other from the left and writes the
	 * result into the result vector.
	 * 
	 * @param other
	 *            the vector by which this matrix is to be multiplied; its size
	 *            must equal the row dimension of this vector
	 * @param result
	 *            the vector in which the result is to be written; must be of
	 *            same size as the result vector
	 * 
	 * @return a reference to the result vector
	 */
	public Vector timesVectorFromLeft(final Vector other, final Vector result) {

		if (other.size() != this.rowSize()) {
			throw new IllegalArgumentException(
					"other vector must have same size as row dimension "
							+ "of this matrix");
		}
		if (result.size() != this.columnSize()) {
			throw new IllegalArgumentException("result vector must be of same "
					+ "dimension as column dimension of this matrix");
		}

		for (int l = 0; l < other.size(); l++)
			result.add(this.getRow(l), other.get(l));
		return result;
	}

	/**
	 * Returns a new vector that contains the result of multiplying this matrix
	 * from the left with the other vector.
	 * 
	 * @param other
	 *            the vector with which this matrix is to be multiplied
	 * 
	 * @return a new vector that contains the result of multiplying this matrix
	 *         from the left with the other vector
	 */
	public Vector timesVectorFromLeft(final Vector other) {
		return timesVectorFromLeft(other, new Vector(this.columnSize()));
	}

	/**
	 * Rounds the entries of this matrix to decimals positions after the comma.
	 * If decimals is negative, the rounding is carried over to positions before
	 * the comma.
	 * 
	 * @param decimals
	 *            positions after the comma to which to round
	 */
	public void round(final int decimals) {
		for (Vector row : this.rows) {
			row.round(decimals);
		}
	}

	/**
	 * Returns a textual representation of this matrix.
	 */
	public String toString() {
		final StringBuffer result = new StringBuffer();
		for (int i = 0; i < this.rowSize() - 1; i++) {
			result.append(this.rows[i].toString());
			result.append("\n");
		} // no newline after last row!
		result.append(this.rows[this.rowSize() - 1].toString());
		return result.toString();
	}

	/**
	 * Makes this matrix exactly symmetrical.
	 */
	public void symmetrize() {
		for (int i = 0; i < this.rowSize(); i++) {
			final Vector iRow = this.getRow(i);
			for (int j = 0; j < i; j++) {
				final Vector jRow = this.getRow(j);
				final double cov = 0.5 * (iRow.get(j) + jRow.get(i));
				iRow.set(j, cov);
				jRow.set(i, cov);
			}
		}
	}

	public void setColumn(int j, final Vector column) {
		for (int i = 0; i < this.rowSize(); i++) {
			this.getRow(i).set(j, column.get(i));
		}
	}

	public boolean isAllZeros() {
		for (Vector row : this.rows) {
			if (!row.isAllZeros()) {
				return false;
			}
		}
		return true;
	}

	public String toSingleLineString() {
		final StringBuffer result = new StringBuffer();
		result.append("[");
		for (int i = 0; i < this.rowSize(); i++) {
			result.append(this.rows[i].toString());
		}
		result.append("]");
		return result.toString();
	}

	public Vector timesVectorFromRight(final Vector other, final Vector result) {
		for (int i = 0; i < this.rowSize(); i++) {
			result.set(i, this.getRow(i).innerProd(other));
		}
		return result;
	}

	public Vector timesVectorFromRight(final Vector other) {
		return this.timesVectorFromRight(other, new Vector(this.rowSize()));
	}

	public Matrix newTransposed() {
		final Matrix result = new Matrix(this.columnSize(), this.rowSize());
		for (int i = 0; i < this.rowSize(); i++) {
			final Vector fromRow_i = this.getRow(i);
			for (int j = 0; j < this.columnSize(); j++) {
				result.getRow(j).set(i, fromRow_i.get(j));
			}
		}
		return result;
	}

	public static Matrix product(final Matrix _A, final Matrix _B) {
		if (_A.columnSize() != _B.rowSize()) {
			throw new IllegalArgumentException(
					"column size of first argument does not "
							+ "equal row size of second argument");
		}
		final Matrix _C = new Matrix(_A.rowSize(), _B.columnSize());
		for (int i = 0; i < _C.rowSize(); i++) {
			final Vector _A_row_i = _A.getRow(i);
			final Vector _C_row_i = _C.getRow(i);
			for (int k = 0; k < _A.columnSize(); k++) {
				_C_row_i.add(_B.getRow(k), _A_row_i.get(k));
			}
		}
		return _C;
	}

	// TODO NEW
	public boolean isNaN() {
		for (Vector row : this.rows) {
			if (row.isNaN()) {
				return true;
			}
		}
		return false;
	}

	// TODO NEW
	public double absValueSum() {
		double result = 0;
		for (Vector row : this.rows) {
			result += row.absValueSum();
		}
		return result;
	}

	// TODO NEW
	public double max() {
		double result = Double.NEGATIVE_INFINITY;
		for (Vector row : this.rows) {
			result = Math.max(result, row.max());
		}
		return result;
	}

	// TODO NEW
	public void makeProbability() {
		double sum = 0;
		for (Vector row : this.rows) {
			for (int i = 0; i < row.size(); i++) {
				if (row.get(i) < 0.0) {
					row.set(i, 0.0);
				} else if (row.get(i) > 1.0) {
					row.set(i, 1.0);
					sum += 1.0;
				} else {
					sum += row.get(i);
				}
			}
		}
		this.mult(1.0 / sum);
	}

	// TODO NEW
	public static double maxAbsDiff(final Matrix a, final Matrix b) {
		double result = 0;
		for (int i = 0; i < a.rowSize(); i++) {
			result = Math.max(result,
					Vector.maxAbsDiff(a.getRow(i), b.getRow(i)));
		}
		return result;
	}

	// TODO NEW
	public double get(final int row, final int column) {
		return this.getRow(row).get(column);
	}

	// TODO NEW
	public Matrix newLeaveOneRowOut(final int leaveOutRowIndex) {
		final Vector[] newRows = new Vector[this.rowSize() - 1];
		int iTo = 0;
		for (int iFrom = 0; iFrom < this.rowSize(); iFrom++) {
			if (iFrom != leaveOutRowIndex) {
				newRows[iTo++] = this.rows[iFrom].copy();
			}
		}
		return new Matrix(newRows);
	}

	// TODO NEW
	public Matrix newLeaveOneColumnOut(final int leaveOutColumnIndex) {
		final Vector[] newRows = new Vector[this.rowSize()];
		for (int i = 0; i < this.rowSize(); i++) {
			newRows[i] = this.rows[i].newLeaveOneOut(leaveOutColumnIndex);
		}
		return new Matrix(newRows);
	}

}
