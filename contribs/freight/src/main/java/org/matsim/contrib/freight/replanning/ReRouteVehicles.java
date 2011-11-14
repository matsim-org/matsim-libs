package org.matsim.contrib.freight.replanning;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.vrp.VRPCarrierPlanBuilder;
import org.matsim.contrib.freight.vrp.VRPSolverFactory;

public class ReRouteVehicles implements CarrierPlanStrategyModule{

	private Network network;
	
	private VRPSolverFactory vrpSolverFactory;
	
	public ReRouteVehicles(Network network, VRPSolverFactory vrpSolverFactory) {
		super();
		this.network = network;
		this.vrpSolverFactory = vrpSolverFactory;
	}

	@Override
	public void handleActor(Carrier carrier) {
		VRPCarrierPlanBuilder planBuilder = new VRPCarrierPlanBuilder(carrier.getCarrierCapabilities(), carrier.getContracts(), network);
		planBuilder.setVrpSolverFactory(vrpSolverFactory);
		carrier.setSelectedPlan(planBuilder.buildPlan());
	}

}
