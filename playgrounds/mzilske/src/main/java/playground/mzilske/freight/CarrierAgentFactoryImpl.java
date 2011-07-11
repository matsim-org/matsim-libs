package playground.mzilske.freight;

import org.matsim.api.core.v01.network.Network;
import org.matsim.population.algorithms.PlanAlgorithm;

public class CarrierAgentFactoryImpl implements CarrierAgentFactory{
	
	private Network network;
	
	private PlanAlgorithm router;
	
	public CarrierAgentFactoryImpl(Network network, PlanAlgorithm router) {
		super();
		this.network = network;
		this.router = router;
	}

	@Override
	public CarrierAgent createAgent(CarrierAgentTracker tracker, CarrierImpl carrier) {
		CarrierAgent carrierAgent = new CarrierAgent(tracker, carrier, router);
		carrierAgent.setCostFunction(new CarrierTimeDistanceCostFunction());
		carrierAgent.setCostAllocator(new CostAllocatorImpl(carrier, network));
		carrierAgent.setOfferMaker(new BeeLineOfferMaker(carrier, network, new CarrierTimeDistanceCostFunction()));
		carrierAgent.setNetwork(network);
		return carrierAgent;
	}

}
