package org.matsim.contrib.freight.vrp.algorithms;

import org.matsim.contrib.freight.vrp.basics.VehicleRoutingProblem;
import org.matsim.contrib.freight.vrp.basics.VehicleRoutingProblemSolver;
import org.matsim.contrib.freight.vrp.basics.VehicleRoutingProblemSolverFactory;

public class CVRPTWSolverFactory implements VehicleRoutingProblemSolverFactory{

	private int iterations;
	
	private int warmup;
	
	private boolean iterSet = false;
	
	public CVRPTWSolverFactory() {
		
	}
	
	public CVRPTWSolverFactory(int iterations, int warmupIteration) {
		this.iterations = iterations;
		this.warmup = warmupIteration;
		iterSet = true;
	}

	@Override
	public VehicleRoutingProblemSolver createSolver(VehicleRoutingProblem vrp) {
		CVRPTWSolver solver = new CVRPTWSolver(vrp);
		if(iterSet){
			solver.setIterations(iterations);
			solver.setWarmupIterations(warmup);
		}
		return solver;
	}

}
