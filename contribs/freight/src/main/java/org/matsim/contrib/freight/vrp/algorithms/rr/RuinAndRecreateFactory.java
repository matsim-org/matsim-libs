package org.matsim.contrib.freight.vrp.algorithms.rr;

import org.matsim.contrib.freight.vrp.basics.VehicleRoutingProblem;


public interface RuinAndRecreateFactory {

	/**
	 * Standard ruin and recreate without time windows. This algo is configured according to Schrimpf et. al (2000).
	 * @param vrp
	 * @param initialSolution
	 * @return
	 */
	public abstract RuinAndRecreate createAlgorithm(VehicleRoutingProblem vrp, RRSolution initialSolution);

	public abstract void addRuinAndRecreateListener(RuinAndRecreateListener l);
	
	public abstract void setIterations(int iterations);
	
	public abstract void setWarmUp(int nOfWarmUpIterations);
}