package org.matsim.contrib.freight.vrp;

import java.util.Collection;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.CarrierShipment;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.contrib.freight.vrp.algorithms.rr.RuinAndRecreateWithTimeWindowsFactory;
import org.matsim.contrib.freight.vrp.algorithms.rr.constraints.TimeAndCapacityPickupsDeliveriesSequenceConstraint;
import org.matsim.contrib.freight.vrp.api.Costs;
import org.matsim.contrib.freight.vrp.basics.IniSolution;

public class RRPDTWSolverFactory implements VRPSolverFactory{

	@Override
	public VRPSolver createSolver(Collection<CarrierShipment> shipments,Collection<CarrierVehicle> carrierVehicles, Network network, Costs costs) {
		ShipmentBasedSingleDepotVRPSolver rrSolver = new ShipmentBasedSingleDepotVRPSolver(shipments, carrierVehicles, network);
		rrSolver.setRuinAndRecreateFactory(new RuinAndRecreateWithTimeWindowsFactory());
//		rrSolver.setIniSolutionFactory(new SingleDepotInitialSolutionFactoryImpl());
		rrSolver.setIniSolutionFactory(new IniSolution(carrierVehicles.size()));
		rrSolver.setnOfWarmupIterations(4);
		rrSolver.setnOfIterations(16);

		rrSolver.setCosts(costs);
		rrSolver.setConstraints(new TimeAndCapacityPickupsDeliveriesSequenceConstraint(carrierVehicles.iterator().next().getCapacity(),
				10*3600,costs));
		return rrSolver;
	}

}
