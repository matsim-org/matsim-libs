package city2000w.replanning;

import org.matsim.api.core.v01.network.Network;

import playground.mzilske.freight.carrier.Carrier;
import city2000w.VRPCarrierPlanBuilder;
import freight.vrp.VRPSolverFactory;

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
