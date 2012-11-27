package org.matsim.contrib.freight.vrp.algorithms.rr.listener;

import org.matsim.contrib.freight.vrp.basics.VehicleRoutingProblemSolution;

public interface IterationStartListener extends RuinAndRecreateListener {

	public void informIterationStarts(int iteration,
			VehicleRoutingProblemSolution currentSolution);

}
