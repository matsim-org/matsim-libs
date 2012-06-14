package org.matsim.contrib.freight.replanning;

import java.util.ArrayList;
import java.util.Collection;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierFactory;
import org.matsim.contrib.freight.carrier.CarrierPlan;
import org.matsim.contrib.freight.carrier.ScheduledTour;
import org.matsim.contrib.freight.vrp.DTWSolverFactory;
import org.matsim.contrib.freight.vrp.DTWSolver;
import org.matsim.contrib.freight.vrp.algorithms.rr.DistributionTourWithTimeWindowsAlgoFactory;
import org.matsim.contrib.freight.vrp.algorithms.rr.InitialSolution;
import org.matsim.contrib.freight.vrp.algorithms.rr.RuinAndRecreateListener;
import org.matsim.contrib.freight.vrp.basics.Costs;
import org.matsim.contrib.freight.vrp.constraints.PickORDeliveryCapacityAndTWConstraint;

public class ReScheduleVehicles implements CarrierPlanStrategyModule{

	private Network network;

	private Costs costs;
	
	public Collection<RuinAndRecreateListener> listeners = new ArrayList<RuinAndRecreateListener>();
	
	public ReScheduleVehicles(Network network, Costs costs) {
		super();
		this.network = network;
		this.costs = costs;
	}

	@Override
	public void handleActor(Carrier carrier) {	
//		ShipmentBasedVRPSolver vrpSolver = new ShipmentBasedVRPSolver(new CarrierFactory().getShipments(carrier.getContracts()), 
//				new CarrierFactory().getVehicles(carrier.getCarrierCapabilities()), costs, network, new InitialSolution());
		DTWSolver vrpSolver = new DTWSolver(new CarrierFactory().getShipments(carrier.getContracts()), 
				new CarrierFactory().getVehicles(carrier.getCarrierCapabilities()), costs, network, carrier.getSelectedPlan());
		vrpSolver.setRuinAndRecreateFactory(new DistributionTourWithTimeWindowsAlgoFactory());
		vrpSolver.setGlobalConstraints(new PickORDeliveryCapacityAndTWConstraint());
		vrpSolver.setnOfWarmupIterations(20);
		vrpSolver.setnOfIterations(500);
		vrpSolver.listeners.addAll(listeners);
		Collection<ScheduledTour> scheduledTours = vrpSolver.solve();
		carrier.setSelectedPlan(new CarrierPlan(scheduledTours));
	}

}
