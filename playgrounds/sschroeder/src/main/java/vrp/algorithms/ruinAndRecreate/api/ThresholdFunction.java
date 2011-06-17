package vrp.algorithms.ruinAndRecreate.api;

/**
 * 
 * @author stefan schroeder
 *
 */

public interface ThresholdFunction {
	
	public double getThreshold(int iteration);
	
	public void setInitialThreshold(double initialThreshold);
	
	public void setNofIterations(int nOfIterations);

}