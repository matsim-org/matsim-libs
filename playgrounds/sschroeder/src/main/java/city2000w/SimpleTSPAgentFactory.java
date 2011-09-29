package city2000w;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.gbl.MatsimRandom;

import playground.mzilske.freight.BasicTSPAgentImpl;
import playground.mzilske.freight.Contract;
import playground.mzilske.freight.TSPAgent;
import playground.mzilske.freight.TSPAgentTracker;
import playground.mzilske.freight.TSPContract;
import playground.mzilske.freight.TSPOffer;
import playground.mzilske.freight.TransportChain;
import playground.mzilske.freight.TransportChainAgentFactory;
import playground.mzilske.freight.TransportServiceProvider;
import playground.mzilske.freight.api.TSPAgentFactory;

public class SimpleTSPAgentFactory implements TSPAgentFactory{

	static class SimpleTSPAgentImpl extends BasicTSPAgentImpl {

		public double price = 10;
		
		private Logger logger = Logger.getLogger(SimpleTSPAgentImpl.class);
		
		private Collection<TSPContract> newContracts = new ArrayList<TSPContract>();
		
		private Collection<TSPContract> canceledContracts = new ArrayList<TSPContract>();
		
		public SimpleTSPAgentImpl(TransportServiceProvider tsp,TransportChainAgentFactory chainAgentFactory) {
			super(tsp, chainAgentFactory);
		}

		@Override
		public void reset() {
			
		}

		@Override
		public void scoreSelectedPlan() {

			
		}

		@Override
		public TSPOffer requestService(Id from, Id to, int size,double startPickup, double endPickup, double startDelivery,
				double endDelivery) {
			TSPOffer offer = new TSPOffer();
			offer.setId(tsp.getId());
			offer.setPrice(price);
			return offer;
		}

		@Override
		public void calculateCosts() {
			
		}

		@Override
		public void informShipperContractAccepted(Contract contract) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void informCarrierContractAccepted(Contract contract) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void informCarrierContractCanceled(Contract contract) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void informChainRemoved(TransportChain chain) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void informChainAdded(TransportChain chain) {
			// TODO Auto-generated method stub
			
		}

		
	}

	private TransportChainAgentFactory chainAgentFactory;
	
	public SimpleTSPAgentFactory(TransportChainAgentFactory chainAgentFactory) {
		super();
		this.chainAgentFactory = chainAgentFactory;
	}

	@Override
	public TSPAgent createTspAgent(TSPAgentTracker tspAgentTracker,
			TransportServiceProvider tsp) {
		double price = MatsimRandom.getRandom().nextInt(100);
		SimpleTSPAgentImpl tspAgent = new SimpleTSPAgentImpl(tsp, chainAgentFactory);
		tspAgent.price = price;
		return tspAgent;
	}

}
