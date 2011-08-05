package freight;

import org.matsim.api.core.v01.network.Network;

import playground.mzilske.freight.CarrierAgentTracker;
import playground.mzilske.freight.CarrierOffer;
import playground.mzilske.freight.CostMemoryImpl;
import playground.mzilske.freight.TSPAgent;
import playground.mzilske.freight.TSPAgentTracker;
import playground.mzilske.freight.TransportServiceProviderImpl;
import playground.mzilske.freight.api.TSPAgentFactory;
import city2000w.NotSoRandomTSPPlanBuilder;
import city2000w.RandomTSPPlanBuilder;
import freight.offermaker.CarrierCostRequester;
import freight.offermaker.OfferSelectorImpl;

public class TSPAgentFactoryImpl implements TSPAgentFactory {

	private CarrierAgentTracker carrierAgentTracker;
	
	private OfferSelectorImpl<CarrierOffer> offerSelectorImpl;
	
	private Network network;
	
	public void setNetwork(Network network) {
		this.network = network;
	}

	public TSPAgentFactoryImpl(CarrierAgentTracker carrierAgentTracker) {
		super();
		this.carrierAgentTracker = carrierAgentTracker;
	}
	
	public void setOfferSelector(OfferSelectorImpl<CarrierOffer> offerSelectorImpl) {
		this.offerSelectorImpl = offerSelectorImpl;
	}

	@Override
	public TSPAgent createTspAgent(TSPAgentTracker tspAgentTracker, TransportServiceProviderImpl tsp) {
		TSPAgent tspAgent = new TSPAgent(tsp);
		CarrierCostRequester carrierCostRequester = new CarrierCostRequester();
		carrierCostRequester.setTSP(tsp);
		carrierCostRequester.setTspAgent(tspAgent);
		
//		RandomTSPPlanBuilder tspPlanBuilder = new RandomTSPPlanBuilder(network);
		NotSoRandomTSPPlanBuilder tspPlanBuilder = new NotSoRandomTSPPlanBuilder(network);
		tspPlanBuilder.setCarrierAgentTracker(carrierAgentTracker);
		tspPlanBuilder.setTspAgentTracker(tspAgentTracker);
		tspPlanBuilder.setOfferSelector(offerSelectorImpl);
		carrierCostRequester.setTspPlanBuilder(tspPlanBuilder);
		
		tspAgent.setCostMemory(new CostMemoryImpl());
		CostMemoryImpl.learningRate = 0.8;
		tspAgent.setOfferMaker(carrierCostRequester);
		tspAgent.setTspAgentTracker(tspAgentTracker);
		return tspAgent;
	}
}
