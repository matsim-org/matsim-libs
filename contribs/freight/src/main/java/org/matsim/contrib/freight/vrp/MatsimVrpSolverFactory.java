package org.matsim.contrib.freight.vrp;


import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.vrp.algorithms.rr.costCalculators.TourCost;
import org.matsim.contrib.freight.vrp.basics.VehicleRoutingCosts;

public interface MatsimVrpSolverFactory {

	public abstract MatsimVrpSolver createSolver(Carrier carrier, Network network, TourCost tourCost, VehicleRoutingCosts costs);


}
