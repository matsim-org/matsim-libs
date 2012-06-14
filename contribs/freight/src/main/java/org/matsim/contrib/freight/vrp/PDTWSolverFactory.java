package org.matsim.contrib.freight.vrp;

/**
 * Configures solver for solving the vrp problem.
 */

import java.util.Collection;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.CarrierShipment;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.contrib.freight.vrp.algorithms.rr.InitialSolution;
import org.matsim.contrib.freight.vrp.algorithms.rr.PickupAndDeliveryTourWithTimeWindowsAlgoFactory;
import org.matsim.contrib.freight.vrp.basics.Costs;
import org.matsim.contrib.freight.vrp.constraints.PickAndDeliveryCapacityAndTWConstraint;

public class PDTWSolverFactory implements VRPSolverFactory{

	@Override
	public VRPSolver createSolver(Collection<CarrierShipment> shipments,Collection<CarrierVehicle> carrierVehicles, Network network, Costs costs) {
		DTWSolver rrSolver = new DTWSolver(shipments, carrierVehicles, costs, network, new InitialSolution());
		PickupAndDeliveryTourWithTimeWindowsAlgoFactory ruinAndRecreateFactory = new PickupAndDeliveryTourWithTimeWindowsAlgoFactory();
		rrSolver.setRuinAndRecreateFactory(ruinAndRecreateFactory);
		rrSolver.setnOfWarmupIterations(20);
		rrSolver.setnOfIterations(500);
		rrSolver.setGlobalConstraints(new PickAndDeliveryCapacityAndTWConstraint());
		return rrSolver;
	}

}
