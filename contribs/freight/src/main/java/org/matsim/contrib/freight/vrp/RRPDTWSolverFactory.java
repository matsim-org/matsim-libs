package org.matsim.contrib.freight.vrp;

/**
 * Configures solver for solving the vrp problem.
 */

import java.util.Collection;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.CarrierShipment;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.contrib.freight.vrp.algorithms.rr.InitialSolution;
import org.matsim.contrib.freight.vrp.algorithms.rr.factories.PickupAndDeliveryTourWithTimeWindowsAlgoFactory;
import org.matsim.contrib.freight.vrp.basics.Costs;
import org.matsim.contrib.freight.vrp.basics.TimeAndCapacityAndTWConstraints;

public class RRPDTWSolverFactory implements VRPSolverFactory{

	@Override
	public VRPSolver createSolver(Collection<CarrierShipment> shipments,Collection<CarrierVehicle> carrierVehicles, Network network, Costs costs) {
		ShipmentBasedVRPSolver rrSolver = new ShipmentBasedVRPSolver(shipments, carrierVehicles, network);
		rrSolver.setRuinAndRecreateFactory(new PickupAndDeliveryTourWithTimeWindowsAlgoFactory());
		rrSolver.setIniSolutionFactory(new InitialSolution());
		rrSolver.setnOfWarmupIterations(4);
		rrSolver.setnOfIterations(16);
		rrSolver.setCosts(costs);
		rrSolver.setConstraints(new TimeAndCapacityAndTWConstraints(10*3600));
		return rrSolver;
	}

}
