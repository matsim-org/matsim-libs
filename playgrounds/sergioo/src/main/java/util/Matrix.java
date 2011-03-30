/* *********************************************************************** *
 * project: org.matsim.*
 * Controler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package util;

/**
 * @author Sergio Ordóñez
 */
public class Matrix {

	//Attributes
	/**
	 * The double values
	 */
	private final double[][] values;

	//Methods
	/**
	 * Creates a zero matrix of the specified size
	 * @param m Number of rows
	 * @param n Number of columns
	 */
	public Matrix(int m, int n) {
		values = new double[m][n];
	}
	/**
	 * Creates matrix based on 2d array
	 * @param values Numeric values
	 */
	public Matrix(double[][] values) {
		this.values = values;
	}
	/**
	 * Copy constructor
	 * @param a Copied matrix
	 */
	private Matrix(Matrix a) {
		this(a.values);
	}
	/**
	 * Creates and return a randovalues.length M-by-N matrix with values between 0 and 1
	 * @param m Number of rows
	 * @param n Number of columns
	 */
	public static Matrix random(int m, int n) {
		Matrix a = new Matrix(m, n);
		for (int i = 0; i < m; i++)
			for (int j = 0; j < n; j++)
				a.values[i][j] = Math.random();
		return a;
	}
	/**
	 * Create and return the N-by-N identity matrix
	 * @param n Number of rows and columns
	 */
	public static Matrix identity(int n) {
		Matrix id = new Matrix(n, n);
		for (int i = 0; i < n; i++)
			id.values[i][i] = 1;
		return id;
	}
	/**
	 * @param m The row
	 * @param n The column
	 * @return the required value according to the given position
	 */
	public double getValue(int m, int n) {
		return values[m][n];
	}
	/**
	 * Swap the specified columns 
	 * @param i First row position
	 * @param j Second row position
	 */
	private void swap(int i, int j) {
		double[] temp = values[i];
		values[i] = values[j];
		values[j] = temp;
	}
	/**
	 * @return the transpose of the invoking matrix
	 */
	public Matrix transpose() {
		Matrix a = new Matrix(values[0].length, values.length);
		for (int i = 0; i < values.length; i++)
			for (int j = 0; j < values[0].length; j++)
				a.values[j][i] = this.values[i][j];
		return a;
	}
	/**
	 * @return C = A + B
	 */
	public Matrix plus(Matrix b) {
		if (b.values.length != this.values.length || b.values[0].length != this.values[0].length)
			throw new RuntimeException("Illegal values.lengthatrix divalues.lengthensions.");
		Matrix c = new Matrix(values.length, values[0].length);
		for (int i = 0; i < values.length; i++)
			for (int j = 0; j < values[0].length; j++)
				c.values[i][j] = this.values[i][j] + b.values[i][j];
		return c;
	}
	/**
	 * @return C = A - B
	 */
	public Matrix minus(Matrix b) {
		if (b.values.length != this.values.length || b.values[0].length != this.values[0].length)
			throw new RuntimeException("Illegal values.lengthatrix divalues.lengthensions.");
		Matrix c = new Matrix(values.length, values[0].length);
		for (int i = 0; i < values.length; i++)
			for (int j = 0; j < values[0].length; j++)
				c.values[i][j] = this.values[i][j] - b.values[i][j];
		return c;
	}
	/**
	 * @return A = B exactly?
	 */
	public boolean equals(Object other) {
		Matrix b=(Matrix) other;
		if (b.values.length != this.values.length || b.values[0].length != this.values[0].length)
			throw new RuntimeException("Illegal values.lengthatrix divalues.lengthensions.");
		for (int i = 0; i < values.length; i++)
			for (int j = 0; j < values[0].length; j++)
				if (this.values[i][j] != b.values[i][j]) return false;
		return true;
	}
	/**
	 * @return C = A * B
	 */
	public Matrix times(Matrix b) {
		if (this.values[0].length != b.values.length) throw new RuntimeException("Illegal values.lengthatrix divalues.lengthensions.");
		Matrix c = new Matrix(this.values.length, b.values[0].length);
		for (int i = 0; i < c.values.length; i++)
			for (int j = 0; j < c.values[0].length; j++)
				for (int k = 0; k < this.values[0].length; k++)
					c.values[i][j] += (this.values[i][k] * b.values[k][j]);
		return c;
	}
	/**
	 * @return x = A^-1 b
	 */
	public Matrix solve(Matrix rhs) {
		if (values.length != values[0].length || rhs.values.length != values[0].length || rhs.values[0].length != 1)
			throw new RuntimeException("Illegal values.lengthatrix divalues.lengthensions.");

		// create copies of the data
		Matrix a = new Matrix(this);
		Matrix b = new Matrix(rhs);

		// Gaussian elivalues.lengthination with partial pivoting
		for (int i = 0; i < values[0].length; i++) {

			// find pivot row and swap
			int max = i;
			for (int j = i + 1; j < values[0].length; j++)
				if (Math.abs(a.values[j][i]) > Math.abs(a.values[max][i]))
					max = j;
			a.swap(i, max);
			b.swap(i, max);

			// singular
			if (a.values[i][i] == 0.0) throw new RuntimeException("Matrix is singular.");

			// pivot within b
			for (int j = i + 1; j < values[0].length; j++)
				b.values[j][0] -= b.values[i][0] * a.values[j][i] / a.values[i][i];

			// pivot within A
			for (int j = i + 1; j < values[0].length; j++) {
				double s = a.values[j][i] / a.values[i][i];
				for (int k = i+1; k < values[0].length; k++) {
					a.values[j][k] -= a.values[i][k] * s;
				}
				a.values[j][i] = 0.0;
			}
		}

		// back substitution
		Matrix x = new Matrix(values[0].length, 1);
		for (int j = values[0].length - 1; j >= 0; j--) {
			double t = 0.0;
			for (int k = j + 1; k < values[0].length; k++)
				t += a.values[j][k] * x.values[k][0];
			x.values[j][0] = (b.values[j][0] - t) / a.values[j][j];
		}
		return x;

	}
	/**
	 * Prints matrix to standard output
	 */
	public void show() {
		for (int i = 0; i < values.length; i++) {
			for (int j = 0; j < values[0].length; j++) 
				System.out.printf("%9.4f ", values[i][j]);
			System.out.println();
		}
	}
	
}
