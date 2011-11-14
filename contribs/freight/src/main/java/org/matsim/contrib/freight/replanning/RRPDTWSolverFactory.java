package org.matsim.contrib.freight.replanning;

import java.util.Collection;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.CarrierShipment;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.contrib.freight.vrp.algorithms.rr.constraints.TimeAndCapacityPickupsDeliveriesSequenceConstraint;
import org.matsim.contrib.freight.vrp.algorithms.rr.factories.RuinAndRecreateWithTimeWindowsFactory;
import org.matsim.contrib.freight.vrp.basics.CrowFlyCosts;
import org.matsim.contrib.freight.vrp.basics.SingleDepotInitialSolutionFactoryImpl;

public class RRPDTWSolverFactory implements VRPSolverFactory{

	@Override
	public VRPSolver createSolver(Collection<CarrierShipment> shipments,Collection<CarrierVehicle> carrierVehicles, Network network) {
		ShipmentBasedSingleDepotVRPSolver rrSolver = new ShipmentBasedSingleDepotVRPSolver(shipments, carrierVehicles, network);
		rrSolver.setRuinAndRecreateFactory(new RuinAndRecreateWithTimeWindowsFactory());
		rrSolver.setIniSolutionFactory(new SingleDepotInitialSolutionFactoryImpl());
		rrSolver.setnOfWarmupIterations(4);
		rrSolver.setnOfIterations(16);
		CrowFlyCosts crowFlyDistance = new CrowFlyCosts();
		crowFlyDistance.speed = 18;
		crowFlyDistance.detourFactor = 1.2;
		rrSolver.setCosts(crowFlyDistance);
		rrSolver.setConstraints(new TimeAndCapacityPickupsDeliveriesSequenceConstraint(carrierVehicles.iterator().next().getCapacity(),
				10*3600,crowFlyDistance));
		return rrSolver;
	}

}
