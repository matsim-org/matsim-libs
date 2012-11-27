package org.matsim.contrib.freight.vrp.algorithms.rr.listener;

import org.matsim.contrib.freight.vrp.basics.VehicleRoutingProblemSolution;

public interface IterationEndsListener extends RuinAndRecreateListener {

	void informIterationEnds(int currentIteration,
			VehicleRoutingProblemSolution currentSolution,
			VehicleRoutingProblemSolution rejectedSolution);

}
