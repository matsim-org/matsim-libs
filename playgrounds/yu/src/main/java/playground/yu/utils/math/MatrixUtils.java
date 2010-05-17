/* *********************************************************************** *
 * project: org.matsim.*
 * MatrixUtils.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.yu.utils.math;

import java.text.DecimalFormat;

import Jama.Matrix;

/**
 * @author yu
 * 
 */
public class MatrixUtils {
	/**
	 * @param A
	 * @param y
	 * @return
	 */
	public static boolean isConsistantEquation(Matrix A, Matrix y) {
		return A.rank() == getAugmentMatrix(A, y).rank();
	}

	/**
	 * @param A
	 *            coefficient matrix
	 * @param y
	 * @return augmented matrix
	 */
	public static Matrix getAugmentMatrix(Matrix A, Matrix y) {
		int n_y = y.getColumnDimension();
		if (n_y != 1) {
			System.err.println("y should has only one column!!!");
			System.exit(0);
			return null;
		}
		int m = A.getRowDimension(), m_y = y.getRowDimension();
		if (m != m_y) {
			System.err
					.println("A and y should have the same row dimentions!!!");
			System.exit(0);
			return null;
		}
		int n = A.getColumnDimension();
		Matrix augmented = new Matrix(m, n + 1);
		augmented.setMatrix(0, m - 1, 0, n - 1, A);
		augmented.setMatrix(0, m - 1, n, n, y);
		return augmented;
	}

	/**
	 * it should be used with consistent Linear equation system Ax=y
	 * 
	 * @param A
	 *            Coefficient matrix
	 * @param Y
	 * @return
	 */
	public static Matrix getMinimumNormSolution(Matrix A, Matrix y) {
		if (!isConsistantEquation(A, y)) {
			System.err.println("Ax=y is not a consistant equation!!!");
			System.exit(0);
			return null;
		}
		Matrix GI = MatrixUtils.getGeneralizedInverse(A);
		Matrix A_adjugate = A.transpose();
		Matrix GAA_adjugate = GI.times(A).times(A_adjugate);
		if (MatrixUtils.equals(A_adjugate, GAA_adjugate)) {
			System.out
					.println("this generalized inverse is a appropriate one for the minimum norm solution!!!");
			return GI.times(y);
		} else {
			System.err
					.println("this generalized inverse is NOT for the minimum norm solution!!!");
			return null;
		}

	}

	public static boolean equals(Matrix A, Matrix B) {
		int m = A.getRowDimension(), n = A.getColumnDimension();
		if (m != B.getRowDimension())
			return false;
		if (n != B.getColumnDimension())
			return false;
		for (int i = 0; i < m; i++)
			for (int j = 0; j < n; j++)
				if (A.get(i, j) != B.get(i, j))
					return false;
		return true;
	}

	public static Matrix getGeneralizedInverse(Matrix A) {
		FullRankDecomposition frd = new FullRankDecomposition(A);
		Matrix F_t = frd.getF().transpose(), G_t = frd.getG().transpose();
		return G_t.times(F_t.times(A).times(G_t).inverse()).times(F_t);
	}

	public static void main(String[] args) {
		double[][] a = new double[][] { { 1d, 2d } };
		Matrix A = new Matrix(a);

		double[] b = new double[] { 10d };
		Matrix y = new Matrix(b, 1);

		getMinimumNormSolution(A, y).print(new DecimalFormat(), 10);
	}

	public static class FullRankDecomposition {
		private int m/* row number */, n/* column number */, r/* rank */;
		private Matrix F, G;

		public FullRankDecomposition(Matrix A) {
			this.r = A.rank();
			this.m = A.getRowDimension();
			this.n = A.getColumnDimension();
			PivotGaussianElimination pge = new PivotGaussianElimination(A);
			this.G = pge.getRowEchelonForm().getMatrix(0/* init row Idx */,
					r - 1/* final row idx */, 0/* init column idx */, n - 1/*
																		 * final
																		 * column
																		 * idx
																		 */);// first
			// r
			// rows
			Matrix TE = pge.getTotalElementaryMatrix();
			System.out.println("TE:\t");
			TE.print(new DecimalFormat(), 10);
			this.F = TE.inverse().getMatrix(0/*
											 * init row Idx
											 */, m - 1/* final row idx */, 0/*
																		 * init
																		 * column
																		 * idx
																		 */,
					r - 1/*
						 * final column idx
						 */);// first
			// r
			// columns
		}

		public Matrix getF() {
			return F;
		}

		public Matrix getG() {
			return G;
		}

		public static void main(String[] args) {
			double[][] a = new double[][] { { 0d, 2d, 1d, 4d },
					{ 2d, 3d, 2d, 6d }, {
					// 8d, 4d, 3d, 2d
							4d, 6d, 4d, 12d //
					} };
			Matrix A = new Matrix(a);

			FullRankDecomposition frd = new FullRankDecomposition(A);
			Matrix F = frd.getF(), G = frd.getG();

			System.out.println("A\t:");
			A.print(new DecimalFormat(), 10);
			System.out.println("F\t:");
			F.print(new DecimalFormat(), 10);
			System.out.println("G\t:");
			G.print(new DecimalFormat(), 10);
			System.out.println("F*G\t:");
			F.times(G).print(new DecimalFormat(), 10);
		}
	}

	public static class PivotGaussianElimination {
		private Matrix REF/* row echelon form */, TE/*
												 * product of all Elementary
												 * matrix
												 */;
		private int m/* row number */, n/* column number */;

		public PivotGaussianElimination(Matrix A) {
			REF = A;
			m = this.REF.getRowDimension();
			n = this.REF.getColumnDimension();
			TE = Matrix.identity(m, m);// Identity matrix
			calculate();
		}

		public Matrix getRowEchelonForm() {
			return REF;
		}

		public Matrix getTotalElementaryMatrix() {
			return TE;
		}

		private void calculate() {
			int i = 0, j = 0;

			while (i < m && j < n) {
				// Find pivot in column j, starting in row i:
				int maxi = i;
				for (int k = i + 1; k < m; k++) {
					if (Math.abs(REF.get(k, j)) > Math.abs(REF.get(maxi, j)))
						maxi = k;
				}// end for

				// if A[maxi,j] ≠ 0 then
				// swap rows i and maxi, but do not change the value of i
				// Now A[i,j] will contain the old value of A[maxi,j].
				if (REF.get(maxi, j) != 0d) {
					{// row switching
						Matrix E = new Matrix(m, m, 0d);// Permutation Matrix
						for (int d = 0; d < m; d++) {
							if (d == i)
								E.set(i, maxi, 1d);
							else if (d == maxi)
								E.set(maxi, i, 1d);
							else
								E.set(d, d, 1d);
						}// end for d
						this.REF = E.times(this.REF);
						TE = E.times(TE);
					}// end row switching

					// Row-multiplying and Row-addition
					{
						Matrix E = Matrix.identity(m, m);// Identity matrix
						for (int u = i + 1; u < m; u++)
							E.set(u, j, -this.REF.get(u, j)
									/ this.REF.get(i, j));
						// end for u
						this.REF = E.times(this.REF);
						this.TE = E.times(TE);
					}
					i++;
				}// end if
				j++;
			}// end while
		}

		public static void main(String[] args) {
			double[][] a = new double[][] { { 0d, 2d, 1d, 4d },
					{ 2d, 3d, 2d, 6d }, {
					// 8d, 4d, 3d, 2d
							4d, 6d, 4d, 12d //
					} };
			Matrix A = new Matrix(a);
			PivotGaussianElimination pge = new PivotGaussianElimination(A);
			System.out.println("A\t:");
			A.print(new DecimalFormat(), 10);
			System.out.println("REF\t:");
			pge.getRowEchelonForm().print(new DecimalFormat(), 10);
			System.out.println("TE\t:");
			pge.getTotalElementaryMatrix().print(new DecimalFormat(), 10);
		}
	}
}
