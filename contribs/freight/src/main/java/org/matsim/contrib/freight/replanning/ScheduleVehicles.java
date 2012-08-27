package org.matsim.contrib.freight.replanning;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierPlan;
import org.matsim.contrib.freight.carrier.CarrierUtils;
import org.matsim.contrib.freight.vrp.MatsimVrpSolver;
import org.matsim.contrib.freight.vrp.MatsimVrpSolverFactory;
import org.matsim.contrib.freight.vrp.algorithms.rr.serviceProvider.TourCost;
import org.matsim.contrib.freight.vrp.basics.VehicleRoutingCosts;

public class ScheduleVehicles implements CarrierPlanStrategyModule{

	private Network network;
	
	private MatsimVrpSolverFactory vrpSolverFactory;

	private VehicleRoutingCosts costs;

	private TourCost tourCost;
	
	public ScheduleVehicles(Network network, TourCost tourCost, VehicleRoutingCosts costs, MatsimVrpSolverFactory vrpSolverFactory) {
		super();
		this.network = network;
		this.costs = costs;
		this.vrpSolverFactory = vrpSolverFactory;
		this.tourCost = tourCost;
	}

	@Override
	public void handleActor(Carrier carrier) {
		MatsimVrpSolver solver = vrpSolverFactory.createSolver(CarrierUtils.getCarrierShipments(carrier.getContracts()), 
				CarrierUtils.getCarrierVehicles(carrier.getCarrierCapabilities()), network, tourCost, costs);
		CarrierPlan plan = new CarrierPlan(solver.solve());
		carrier.setSelectedPlan(plan);
	}

}
