package freight.vrp;

import java.util.Collection;

import org.matsim.api.core.v01.network.Network;

import playground.mzilske.freight.carrier.CarrierShipment;
import playground.mzilske.freight.carrier.CarrierVehicle;
import vrp.algorithms.ruinAndRecreate.constraints.TimeAndCapacityPickupsDeliveriesSequenceConstraint;
import vrp.basics.CrowFlyDistance;
import vrp.basics.MultipleDepotsInitialSolutionFactory;

public class RRPDTWSolverFactory implements VRPSolverFactory{

	@Override
	public VRPSolver createSolver(Collection<CarrierShipment> shipments,Collection<CarrierVehicle> carrierVehicles, Network network) {
		RRPDTWSolver rrSolver = new RRPDTWSolver(shipments, carrierVehicles, network);
		rrSolver.setIniSolutionFactory(new MultipleDepotsInitialSolutionFactory());
		rrSolver.setnOfWarmupIterations(4);
		rrSolver.setnOfIterations(16);
		CrowFlyDistance crowFlyDistance = new CrowFlyDistance();
		crowFlyDistance.speed = 18;
		crowFlyDistance.detourFactor = 1.2;
		rrSolver.setCosts(crowFlyDistance);
		rrSolver.setConstraints(new TimeAndCapacityPickupsDeliveriesSequenceConstraint(carrierVehicles.iterator().next().getCapacity(),
				10*3600,crowFlyDistance));
		return rrSolver;
	}

}
