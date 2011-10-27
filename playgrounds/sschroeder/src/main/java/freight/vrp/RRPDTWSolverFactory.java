package freight.vrp;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.CarrierShipment;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import vrp.algorithms.ruinAndRecreate.constraints.TimeAndCapacityPickupsDeliveriesSequenceConstraint;
import vrp.algorithms.ruinAndRecreate.factories.RuinAndRecreateWithTimeWindowsFactory;
import vrp.basics.CrowFlyCosts;
import vrp.basics.SingleDepotInitialSolutionFactoryImpl;

import java.util.Collection;

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
