package freight;

import java.util.Collection;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.mzilske.freight.CarrierAgent;
import playground.mzilske.freight.CarrierAgentFactory;
import playground.mzilske.freight.CarrierAgentTracker;
import playground.mzilske.freight.CarrierImpl;
import playground.mzilske.freight.CarrierTimeDistanceCostFunction;
import playground.mzilske.freight.CostAllocatorImpl;

public class AnotherCarrierAgentFactory implements CarrierAgentFactory{

	private Network network;

	private PlanAlgorithm router;
	
	private LocationsImpl locations;
	
	public AnotherCarrierAgentFactory(Network network, PlanAlgorithm router) {
		super();
		this.network = network;
		this.router = router;
		makeLocations();
	}

	private void makeLocations() {
		locations = new LocationsImpl();
		locations.addAllLinks((Collection<Link>) network.getLinks().values());
	}

	@Override
	public CarrierAgent createAgent(CarrierAgentTracker tracker,CarrierImpl carrier) {
		CarrierAgent carrierAgent = new CarrierAgent(tracker, carrier, router);
		carrierAgent.setCostFunction(new CarrierTimeDistanceCostFunction());
		carrierAgent.setCostAllocator(new CostAllocatorImpl(carrier, network));
//		carrierAgent.setOfferMaker(new RuinAndRecreateMarginalCostOM(carrier, locations));
//		carrierAgent.setOfferMaker(new RuinAndRecreateAverageMarginalCostOM(carrier, locations));
		carrierAgent.setOfferMaker(new MarginalCostOM(carrier,locations));
		carrierAgent.setNetwork(network);
		return carrierAgent;
	}

}
