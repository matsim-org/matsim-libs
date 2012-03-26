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

package playground.yu.integration.cadyts.parameterCalibration.withCarCounts.testLls;

import org.ejml.alg.dense.linsol.SolvePseudoInverse;
import org.ejml.data.DenseMatrix64F;

/**
 *
 * @author yu
 *
 */
public class ParameterEstimator {
	private DenseMatrix64F attrM = null, utilCorrV = null;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

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
