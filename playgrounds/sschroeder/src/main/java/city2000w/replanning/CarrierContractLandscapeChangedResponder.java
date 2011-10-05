package city2000w.replanning;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;

import playground.mzilske.freight.carrier.Carrier;
import playground.mzilske.freight.carrier.CarrierPlan;
import city2000w.VRPCarrierPlanBuilder;
import freight.vrp.VRPSolverFactory;

public class CarrierContractLandscapeChangedResponder implements CarrierPlanStrategyModule{

	private Logger logger = Logger.getLogger(CarrierContractLandscapeChangedResponder.class);
	
	private Network network;
	
	private VRPSolverFactory vrpSolverFactory;
	
	public CarrierContractLandscapeChangedResponder(Network network, VRPSolverFactory vrpSolverFactory) {
		super();
		this.network = network;
		this.vrpSolverFactory = vrpSolverFactory;
	}

	@Override
	public void handleActor(Carrier carrier) {
		CarrierPlan newPlan = carrier.getSelectedPlan();
		if(!carrier.getNewContracts().isEmpty() || !carrier.getExpiredContracts().isEmpty()){
			logger.info("hohohohoh. obviously, i have to plan a new contract");
			VRPCarrierPlanBuilder planBuilder = new VRPCarrierPlanBuilder(carrier.getCarrierCapabilities(), carrier.getContracts(), network);
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
