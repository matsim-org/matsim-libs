package org.matsim.contrib.freight.replanning;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierPlan;
import org.matsim.contrib.freight.vrp.VRPCarrierPlanBuilder;
import org.matsim.contrib.freight.vrp.VRPSolverFactory;
import org.matsim.contrib.freight.vrp.basics.Costs;

public class CarrierContractLandscapeChangedResponder implements CarrierPlanStrategyModule{

	private Logger logger = Logger.getLogger(CarrierContractLandscapeChangedResponder.class);
	
	private Network network;
	
	private VRPSolverFactory vrpSolverFactory;

	private Costs costs;
	
	public CarrierContractLandscapeChangedResponder(Network network, Costs costs, VRPSolverFactory vrpSolverFactory) {
		super();
		this.network = network;
		this.vrpSolverFactory = vrpSolverFactory;
		this.costs = costs;
	}

	@Override
	public void handleActor(Carrier carrier) {
		CarrierPlan newPlan = carrier.getSelectedPlan();
		if(!carrier.getNewContracts().isEmpty() || !carrier.getExpiredContracts().isEmpty()){
			logger.info("hohohohoh. obviously, i have to plan a new contract");
			VRPCarrierPlanBuilder planBuilder = new VRPCarrierPlanBuilder(carrier.getCarrierCapabilities(), carrier.getContracts(), network, costs);
			planBuilder.setVrpSolverFactory(vrpSolverFactory);
			newPlan = planBuilder.buildPlan();
//				carrier.getPlans().add(plan);
//			carrier.setSelectedPlan(newPlan);
			carrier.getNewContracts().clear();
//			plan = newPlan;
		}
		carrier.setSelectedPlan(newPlan);
	}

}
