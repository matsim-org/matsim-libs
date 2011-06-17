package vrp.algorithms.ruinAndRecreate.thresholdFunctions;

import vrp.algorithms.ruinAndRecreate.api.ThresholdFunction;

/**
 * See 
 * Schrimpf G., J. Schneider, Hermann Stamm-Wilbrandt and Gunter Dueck (2000): Record Breaking Optimization Results Using 
 * the Ruin and Recreate Principle, Journal of Computational Physics 159, 139Ð171 (2000).
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

	public double getThreshold(int iteration){
		double scheduleVariable = (double)iteration/(double)nOfIterations;
		double currentThreshold = initialThreshold * Math.exp(-Math.log(2)*scheduleVariable/alpha);
		return currentThreshold;
	}

	public void setInitialThreshold(double initialThreshold) {
		this.initialThreshold = initialThreshold;
	}
	
	public void setNofIterations(int nOfIterations){
		this.nOfIterations = nOfIterations;
	}
	

}
