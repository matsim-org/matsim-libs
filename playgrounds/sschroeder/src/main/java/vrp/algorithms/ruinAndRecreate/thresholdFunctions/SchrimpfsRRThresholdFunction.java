/*******************************************************************************
 * Copyright (C) 2011 Stefan Schršder.
 * eMail: stefan.schroeder@kit.edu
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package vrp.algorithms.ruinAndRecreate.thresholdFunctions;

import vrp.algorithms.ruinAndRecreate.api.ThresholdFunction;

/**
 * See 
 * Schrimpf G., J. Schneider, Hermann Stamm-Wilbrandt and Gunter Dueck (2000): Record Breaking Optimization Results Using 
 * the Ruin and Recreate Principle, Journal of Computational Physics 159, 139-171 (2000).
 * 
 * @author stefan schroeder
 *
 */

public class SchrimpfsRRThresholdFunction implements ThresholdFunction {
	
	private int nOfIterations;
	
	private double alpha;
	
	private double initialThreshold;
	
	public SchrimpfsRRThresholdFunction(double alpha) {
		super();
		this.alpha = alpha;
	}

	@Override
	public double getThreshold(int iteration){
		double scheduleVariable = (double)iteration/(double)nOfIterations;
		double currentThreshold = initialThreshold * Math.exp(-Math.log(2)*scheduleVariable/alpha);
		return currentThreshold;
	}

	@Override
	public void setInitialThreshold(double initialThreshold) {
		this.initialThreshold = initialThreshold;
	}
	
	@Override
	public void setNofIterations(int nOfIterations){
		this.nOfIterations = nOfIterations;
	}
	

}
