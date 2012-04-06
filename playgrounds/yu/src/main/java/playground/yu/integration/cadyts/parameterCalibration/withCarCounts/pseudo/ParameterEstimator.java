/* *********************************************************************** *
 * project: org.matsim.*
 * ParameterEstimator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.yu.integration.cadyts.parameterCalibration.withCarCounts.pseudo;

import org.ejml.alg.dense.linsol.SolvePseudoInverse;
import org.ejml.data.DenseMatrix64F;

/***
 * tries to solve the overdetermined problem of:
 * <p>
 * [Attr]<sub>m x n</sub> x [&Delta;&beta;]<sub>n x 1</sub> = [utility
 * correction (from cadyts)]<sub>m x 1</sub>,
 * <p>
 * [Attr]<sub>m x n</sub> and [utility correction]<sub>m x 1</sub> are gegeben.
 * <p>
 * In order to realize min||[utility correction]<sub>m x 1</sub>- [Attr]<sub>m x
 * n</sub> x [&Delta;&beta;]<sub>n x 1</sub>||<sup>2</sup> (The method of least
 * squares can be used to find an approximate solution),
 * <p>
 * [&Delta;&beta;]<sub>n x 1</sub> (vector) is estimated in this class with
 * Moore-Penrose pseudoinverse ( (Attr<sup>T</sup>Attr)&Delta;&beta; =
 * Attr<sup>T</sup>UC &rarr; &Delta;&beta; = Attr<sup>+</sup>UC )
 * <p>
 * (http://en.wikipedia.org/wiki/Moore%E2%80%93Penrose_pseudoinverse),
 * <p>
 * and the code from EJML
 * (http://code.google.com/p/efficient-java-matrix-library)
 * 
 * @author yu
 * 
 */
public class ParameterEstimator {
	private DenseMatrix64F attrM = null, utilCorrV = null;

	/**
	 * A small [Attr]<sub>m x n</sub> x [&Delta;&beta;]<sub>n x 1</sub> =
	 * [utility correction]<sub>m x 1</sub>.
	 * <P>
	 * [Attr]<sub>3 x 2</sub> = (1, 0) (1, 1) (1, 3),
	 * <p>
	 * [utility correction]<sub>3 x 1</sub> = (4, 3, 2)<sup>T</sup>.
	 * <p>
	 * The result should be (27/7, -9/14)<sup>T</sup>
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		ParameterEstimator pe = new ParameterEstimator();

		pe.setAttrM(new DenseMatrix64F(new double[][] { { 1d, 0d }, { 1d, 1d },
				{ 1d, 3d } }));
		System.out.println("AttrM:\n|1, 0|\n|1, 1|\n|1, 3|");

		pe.setUtilCorrV(new DenseMatrix64F(
				new double[][] { { 4 }, { 3 }, { 2 } }));
		System.out.println("UtilCorrM:\n|4|\n|3|\n|2|");

		DenseMatrix64F betaM = pe.getDeltaParameters();
		System.out.println("betaM:\n|" + betaM.get(0, 0) + "|\n|"
				+ betaM.get(1, 0) + "|");
	}

	/**
	 * @return a vector of &Delta;&beta; (delta parameters) e.g. [0.1,0.2] (a
	 *         one row matrix)
	 */
	public DenseMatrix64F getDeltaParameters() {
		if (attrM == null || utilCorrV == null) {
			throw new RuntimeException(
					"Attributes Matrix or the uility correction vector is null yet!");
		}
		DenseMatrix64F deltaBeta = new DenseMatrix64F(attrM.getNumCols(), 1);
		SolvePseudoInverse spi = new SolvePseudoInverse(attrM.getNumRows(),
				attrM.getNumCols());
		spi.setA(attrM);
		spi.solve(utilCorrV, deltaBeta);
		return deltaBeta;
	}

	public void setAttrM(DenseMatrix64F attrM) {
		this.attrM = attrM;
	}

	public void setUtilCorrV(DenseMatrix64F utilCorrV) {
		this.utilCorrV = utilCorrV;
	}

	/**
	 * reinitialize the attrM and utilCorrV (A and b, or X and y)
	 */
	public void clear() {
		if (attrM != null) {
			attrM.zero();
		}
		if (utilCorrV != null) {
			utilCorrV.zero();
		}
	}
}
