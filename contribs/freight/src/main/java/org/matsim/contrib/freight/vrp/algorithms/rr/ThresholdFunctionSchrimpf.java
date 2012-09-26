/*******************************************************************************
 * Copyright (c) 2011 Stefan Schroeder.
 * eMail: stefan.schroeder@kit.edu
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Stefan Schroeder - initial API and implementation
 ******************************************************************************/
package org.matsim.contrib.freight.vrp.algorithms.rr;

/**
 * See Schrimpf G., J. Schneider, Hermann Stamm-Wilbrandt and Gunter Dueck
 * (2000): Record Breaking Optimization Results Using the Ruin and Recreate
 * Principle, Journal of Computational Physics 159, 139-171 (2000).
 * 
 * @author stefan schroeder
 * 
 */

public class ThresholdFunctionSchrimpf implements ThresholdFunction {

	private int nOfIterations;

	private double alpha;

	private double initialThreshold;

	public ThresholdFunctionSchrimpf(double alpha) {
		super();
		this.alpha = alpha;
	}

	@Override
	public double getThreshold(int iteration) {
		double scheduleVariable = (double) iteration / (double) nOfIterations;
		double currentThreshold = initialThreshold
				* Math.exp(-Math.log(2) * scheduleVariable / alpha);
		return currentThreshold;
	}

	@Override
	public void setInitialThreshold(double initialThreshold) {
		this.initialThreshold = initialThreshold;
	}

	@Override
	public void setNofIterations(int nOfIterations) {
		this.nOfIterations = nOfIterations;
	}

}
