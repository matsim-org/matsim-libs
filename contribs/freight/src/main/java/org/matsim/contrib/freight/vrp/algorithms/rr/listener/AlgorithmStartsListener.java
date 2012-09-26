package org.matsim.contrib.freight.vrp.algorithms.rr.listener;

import org.matsim.contrib.freight.vrp.basics.VehicleRoutingProblemSolver;

public interface AlgorithmStartsListener extends RuinAndRecreateListener {

	public void informAlgorithmStarts(VehicleRoutingProblemSolver solver);

}
