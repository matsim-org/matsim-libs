package org.matsim.contrib.freight.vrp.basics;

public interface VehicleRoutingProblemSolverFactory {

	public VehicleRoutingProblemSolver createSolver(VehicleRoutingProblem vrp);
	
	public VehicleRoutingProblemSolver createSolver(VehicleRoutingProblem vrp, VehicleRoutingProblemSolution initialSolution);

}
