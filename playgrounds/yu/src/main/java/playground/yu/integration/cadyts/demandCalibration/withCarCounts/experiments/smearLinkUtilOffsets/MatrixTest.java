/* *********************************************************************** *
 * project: org.matsim.*
 * MatrixTest.java
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

package playground.yu.integration.cadyts.demandCalibration.withCarCounts.experiments.smearLinkUtilOffsets;

import no.uib.cipr.matrix.DenseMatrix;
import no.uib.cipr.matrix.DenseVector;
import no.uib.cipr.matrix.Vector;
import no.uib.cipr.matrix.sparse.BiCGstab;
import no.uib.cipr.matrix.sparse.CompRowMatrix;
import no.uib.cipr.matrix.sparse.IterativeSolver;
import no.uib.cipr.matrix.sparse.IterativeSolverNotConvergedException;
import no.uib.cipr.matrix.sparse.OutputIterationReporter;
import no.uib.cipr.matrix.sparse.SparseVector;
import Jama.Matrix;

public class MatrixTest {
	public static void runJama(String[] args) {
		Matrix A = new Matrix(new double[][] { { 1, 0, 0 }, { 0, 0, 0 },
				{ 0, 0, 1 } });
		Matrix b = new Matrix(new double[] { 10, 0, 30 }, 3);
		Matrix x = A.solve(b);
		System.out.println("Ax = b, x:");
		x.print(10, 8);
	}

	public static void runMTJ(String[] args) {
		// CompRowMatrix A = new CompRowMatrix(3, 3, new int[][] { { 0 }, { 1 },
		// { 2 } });
		no.uib.cipr.matrix.Matrix A = new DenseMatrix(new double[][] {
				{ 1, 4, 0 }, { 1, 0, 0 }, { 0, 0, 1 } });
		Vector b = new DenseVector(new double[] { 10, 20, 300 });
		Vector x = new DenseVector(3);
		x = A.solve(b, x);
		System.out.println("Ax = b, x:");
		System.out.println(x.toString());
	}

	public static void runMTJ2(String[] args) {
		int[][] a = new int[][] { { 1, 2 }, { 1 }, { 2 }, { 2 } };
		CompRowMatrix A = new CompRowMatrix(4, 3, a);
		A.set(0, 2, 3);// fortran [1,3]=3
		A.set(0, 1, 1);// fortran [1,2]=1
		A.set(1, 1, 1);// fortran [2,2]=1
		A.set(2, 2, 4);// fortran [3,3]=4
		A.set(3, 2, 2);// fortran [4,3]=2
		System.out.println("A:\n" + A);
		System.out.println("A num of Columes:\t" + A.numColumns());

		int[][] at = new int[][] { {}, { 0, 1 }, { 0, 2, 3 } };
		CompRowMatrix AT = new CompRowMatrix(3, 4, at);
		AT.set(2, 0, 3);// fortran [1,3]=3
		AT.set(1, 0, 1);// fortran [1,2]=1
		AT.set(1, 1, 1);// fortran [2,2]=1
		AT.set(2, 2, 4);// fortran [3,3]=4
		AT.set(2, 3, 2);// fortran [4,3]=2
		System.out.println("AT:\n" + AT);
		System.out.println("AT num of Columes:\t" + AT.numColumns());

		{
			System.out
					.println("---------------CompRowMatrix??mult------------------");
			int[][] product = new int[][] { {}, { 1, 2 }, { 1, 2 } };
			CompRowMatrix Product = new CompRowMatrix(3, 3, product);
			Product = (CompRowMatrix) AT.mult(A, Product);
			System.out.println("Product:\n" + Product);
			System.out.println("Product num of Rows:\t" + Product.numRows());
			System.out.println("Product num of Columes:\t"
					+ Product.numColumns());
			System.out
					.println("---------------CompRowMatrix??mult------------------");
		}
		{
			System.out.println("---------------Matrix??mult------------------");
			no.uib.cipr.matrix.Matrix Product = new DenseMatrix(3, 3);
			Product = AT.mult(A, Product);
			System.out.println("Product:\n" + Product);
			System.out.println("Product num of Rows:\t" + Product.numRows());
			System.out.println("Product num of Columes:\t"
					+ Product.numColumns());
			System.out.println("---------------Matrix??mult------------------");

		}//

		{
			CompRowMatrix AT2 = new CompRowMatrix(3, 4, at);
			A.transpose(AT2);
			System.out.println("AT2:\n" + AT2);
			MatrixUtils.writeMatrix(AT2, "D:/tmp/AT2.log.gz");
		}
	}

	public static void iterativeSolve() {

		no.uib.cipr.matrix.Matrix A = new CompRowMatrix(3, 4, new int[][] {
				{ 0 }, {}, { 2, 3 } });
		A.set(0, 0, 1);
		A.set(2, 2, 2);
		A.set(2, 3, 1);

		SparseVector x = new SparseVector(4);

		DenseVector b = new DenseVector(3);
		b.set(0, 3);
		b.set(1, 2);
		b.set(2, 1);

		no.uib.cipr.matrix.Matrix AT = new DenseMatrix(4, 3);
		AT = A.transpose(AT);

		no.uib.cipr.matrix.Matrix ATA = new DenseMatrix(4, 4);
		ATA = AT.mult(A, ATA);

		Vector ATb = new DenseVector(4);
		ATb = AT.mult(b, ATb);

		// Allocate storage for Conjugate Gradients
		IterativeSolver solver = new BiCGstab(x);

		// // Create a Cholesky preconditioner
		// Preconditioner M = new AMG();// TODO
		//
		// // Set up the preconditioner, and attach it
		// M.setMatrix(ATA.copy());
		// solver.setPreconditioner(M);

		// Add a convergence monitor
		solver.getIterationMonitor().setIterationReporter(
				new OutputIterationReporter());

		// Start the solver, and check for problems
		try {
			solver.solve(ATA, ATb, x);
		} catch (IterativeSolverNotConvergedException e) {
			System.err.println("Iterative solver failed to converge");
		}

		System.out.println("x:\n" + x);
	}

	public static void runAddElement(String[] args) {
		CompRowMatrix A = new CompRowMatrix(3, 3, new int[][] { { 0 }, { 1 },
				{ 1, 2 } });
		A.add(0, 0, 1);
		A.add(0, 0, 1.5);
		A.add(1, 1, 1);
		A.add(2, 1, 1);
		A.add(2, 2, 1);
		System.out.println("A :\n" + A);
	}

	public static void main(String[] args) {
		// runJama(args);
		// runMTJ(args);
		// runMTJ2(args);
		// runAddElement(args);
		iterativeSolve();
	}
}
