package org.matsim.contrib.freight.vrp;

/**
 * Configures solver for solving the single depot delivery vrp problem.
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.CarrierShipment;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.contrib.freight.vrp.algorithms.rr.DistributionTourWithTimeWindowsAlgoFactory;
import org.matsim.contrib.freight.vrp.algorithms.rr.InitialSolution;
import org.matsim.contrib.freight.vrp.algorithms.rr.RuinAndRecreateListener;
import org.matsim.contrib.freight.vrp.basics.Costs;
import org.matsim.contrib.freight.vrp.constraints.PickORDeliveryCapacityAndTWConstraint;
import org.matsim.core.gbl.MatsimRandom;

public class DTWSolverFactory implements VRPSolverFactory{
	
	public List<RuinAndRecreateListener> listeners = new ArrayList<RuinAndRecreateListener>();

	private Random random = MatsimRandom.getRandom();
	
	public DTWSolverFactory() {

	}
	
	public void setRandom(Random random) {
		this.random = random;
	}

	@Override
	public VRPSolver createSolver(Collection<CarrierShipment> shipments,Collection<CarrierVehicle> carrierVehicles, Network network, Costs costs) {
		ShipmentBasedVRPSolver rrSolver = new ShipmentBasedVRPSolver(shipments, carrierVehicles, costs, network, new InitialSolution());
		DistributionTourWithTimeWindowsAlgoFactory ruinAndRecreateFactory = new DistributionTourWithTimeWindowsAlgoFactory();
		addListeners(ruinAndRecreateFactory);
		rrSolver.setRuinAndRecreateFactory(ruinAndRecreateFactory);
		rrSolver.setnOfWarmupIterations(20);
		rrSolver.setnOfIterations(200);
		PickORDeliveryCapacityAndTWConstraint constraints = new PickORDeliveryCapacityAndTWConstraint();
		rrSolver.setGlobalConstraints(constraints);
		
		return rrSolver;
	}

	private void addListeners(DistributionTourWithTimeWindowsAlgoFactory ruinAndRecreateFactory) {
		for(RuinAndRecreateListener l : listeners){
			ruinAndRecreateFactory.addRuinAndRecreateListener(l);
		}
		
	}

}
