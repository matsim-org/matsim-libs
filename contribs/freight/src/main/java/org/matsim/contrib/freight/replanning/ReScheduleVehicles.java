package org.matsim.contrib.freight.replanning;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.vrp.VRPCarrierPlanBuilder;
import org.matsim.contrib.freight.vrp.VRPSolverFactory;
import org.matsim.contrib.freight.vrp.basics.Costs;
import org.matsim.core.router.util.LeastCostPathCalculator;

public class ReScheduleVehicles implements CarrierPlanStrategyModule{

	private Network network;
	
	private VRPSolverFactory vrpSolverFactory;

	private Costs costs;
	
	private LeastCostPathCalculator router;
	
	public ReScheduleVehicles(Network network, Costs costs, VRPSolverFactory vrpSolverFactory) {
		super();
		this.network = network;
		this.costs = costs;
		this.vrpSolverFactory = vrpSolverFactory;
	}

	public void setRouter(LeastCostPathCalculator router) {
		this.router = router;
	}

	@Override
	public void handleActor(Carrier carrier) {
		VRPCarrierPlanBuilder planBuilder = new VRPCarrierPlanBuilder(carrier.getCarrierCapabilities(), carrier.getContracts(), network, costs);
		planBuilder.setRouter(router);
		planBuilder.setVrpSolverFactory(vrpSolverFactory);
		carrier.setSelectedPlan(planBuilder.buildPlan());
	}

}
