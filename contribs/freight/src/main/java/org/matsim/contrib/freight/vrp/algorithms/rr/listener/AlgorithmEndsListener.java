package org.matsim.contrib.freight.vrp.algorithms.rr.listener;

import org.matsim.contrib.freight.vrp.basics.VehicleRoutingProblemSolution;

public interface AlgorithmEndsListener extends RuinAndRecreateListener {

	public void informAlgorithmEnds(VehicleRoutingProblemSolution currentSolution);

}
