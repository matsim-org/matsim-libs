package org.matsim.contrib.freight.vrp.algorithms;

import java.util.Collection;
import java.util.Collections;

import org.matsim.contrib.freight.vrp.algorithms.rr.listener.RuinAndRecreateListener;
import org.matsim.contrib.freight.vrp.basics.VehicleRoutingProblem;
import org.matsim.contrib.freight.vrp.basics.VehicleRoutingProblemSolution;
import org.matsim.contrib.freight.vrp.basics.VehicleRoutingProblemSolver;
import org.matsim.contrib.freight.vrp.basics.VehicleRoutingProblemSolverFactory;

public class CVRPTWSolverFactory implements VehicleRoutingProblemSolverFactory {

	private int iterations;

	private int warmup;

	private boolean iterSet = false;

	private Collection<RuinAndRecreateListener> listener;

	public CVRPTWSolverFactory() {
		listener = Collections.EMPTY_LIST;
	}

	public CVRPTWSolverFactory(int iterations, int warmupIteration) {
		this.iterations = iterations;
		this.warmup = warmupIteration;
		iterSet = true;
		listener = Collections.EMPTY_LIST;
	}

	public CVRPTWSolverFactory(int iterations, int warmupIteration,
			Collection<RuinAndRecreateListener> listener) {
		this.iterations = iterations;
		this.warmup = warmupIteration;
		iterSet = true;
		this.listener = listener;
	}

	@Override
	public VehicleRoutingProblemSolver createSolver(VehicleRoutingProblem vrp) {
		CVRPTWSolver solver = new CVRPTWSolver(vrp);
		solver.setListener(listener);
		if (iterSet) {
			solver.setIterations(iterations);
			solver.setWarmupIterations(warmup);
		}
		return solver;
	}

	@Override
	public VehicleRoutingProblemSolver createSolver(VehicleRoutingProblem vrp, VehicleRoutingProblemSolution initialSolution) {
		if(initialSolution == null) return createSolver(vrp);
		CVRPTWSolver solver = new CVRPTWSolver(vrp,initialSolution);
		solver.setListener(listener);
		if (iterSet) {
			solver.setIterations(iterations);
			solver.setWarmupIterations(warmup);
		}
		return solver;
	}

}
