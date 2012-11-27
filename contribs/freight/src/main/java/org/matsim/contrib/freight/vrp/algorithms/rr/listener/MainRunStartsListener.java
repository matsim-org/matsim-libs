package org.matsim.contrib.freight.vrp.algorithms.rr.listener;

import org.matsim.contrib.freight.vrp.basics.VehicleRoutingProblemSolution;

public interface MainRunStartsListener extends RuinAndRecreateListener {

	public void informMainRunStarts(VehicleRoutingProblemSolution iniSolution);

}
