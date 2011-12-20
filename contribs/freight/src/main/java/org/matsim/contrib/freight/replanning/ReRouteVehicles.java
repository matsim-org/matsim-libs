package org.matsim.contrib.freight.replanning;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.vrp.VRPCarrierPlanBuilder;
import org.matsim.contrib.freight.vrp.VRPSolverFactory;
import org.matsim.contrib.freight.vrp.basics.Costs;

public class ReRouteVehicles implements CarrierPlanStrategyModule{

	private Network network;
	
	private VRPSolverFactory vrpSolverFactory;

	private Costs costs;
	
	public ReRouteVehicles(Network network, Costs costs, VRPSolverFactory vrpSolverFactory) {
		super();
		this.network = network;
		this.costs = costs;
		this.vrpSolverFactory = vrpSolverFactory;
	}

	@Override
	public void handleActor(Carrier carrier) {
		VRPCarrierPlanBuilder planBuilder = new VRPCarrierPlanBuilder(carrier.getCarrierCapabilities(), carrier.getContracts(), network, costs);
		planBuilder.setVrpSolverFactory(vrpSolverFactory);
		carrier.setSelectedPlan(planBuilder.buildPlan());
	}

}
