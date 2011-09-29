package city2000w;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.gbl.MatsimRandom;

import playground.mzilske.freight.BasicTSPAgentImpl;
import playground.mzilske.freight.Contract;
import playground.mzilske.freight.CostMemory;
import playground.mzilske.freight.CostMemoryImpl;
import playground.mzilske.freight.TSPAgent;
import playground.mzilske.freight.TSPAgentTracker;
import playground.mzilske.freight.TSPOffer;
import playground.mzilske.freight.TSPShipment;
import playground.mzilske.freight.TransportChain;
import playground.mzilske.freight.TransportChain.ChainLeg;
import playground.mzilske.freight.TransportChainAgent;
import playground.mzilske.freight.TransportChainAgentFactory;
import playground.mzilske.freight.TransportServiceProvider;
import playground.mzilske.freight.api.TSPAgentFactory;

public class KarlsruheTSPAgentFactory implements TSPAgentFactory {

	public class KarlsruheTSPAgent extends BasicTSPAgentImpl {

		private Logger logger = Logger.getLogger(KarlsruheTSPAgent.class);
		
		private CostMemory costTable;
		
		public void setCostTable(CostMemory costTable) {
			this.costTable = costTable;
		}


		public KarlsruheTSPAgent(TransportServiceProvider tsp,TransportChainAgentFactory chainAgentFactory) {
			super(tsp, chainAgentFactory);
		}


		@Override
		public void reset() {
			for(TransportChainAgent tca : chainAgentMap.values()){
				tca.reset();
			}
			tspCarrierOfferMap.clear();
		}

		@Override
		public void scoreSelectedPlan() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public TSPOffer requestService(Id from, Id to, int size,double startPickup, double endPickup, double startDelivery,double endDelivery) {
			TSPOffer offer = new TSPOffer();
			if(costTable.getCost(from, to, size) != null){
				offer.setPrice(round(costTable.getCost(from, to, size)));
			}
			else{
				offer.setPrice(round(40000 + MatsimRandom.getRandom().nextInt(100)+1));
			}
			offer.setId(getId());
			return offer;
		}

		private double round(double value) {
			return Math.round(value);
		}


		@Override
		public void calculateCosts() {
			for(TransportChain chain : tsp.getSelectedPlan().getChains()){
				double fees = 0.0;
				for(ChainLeg leg : chain.getLegs()){
					fees += leg.getContract().getOffer().getPrice();
				}
				TSPShipment shipment = chain.getShipment();
				costTable.memorizeCost(shipment.getFrom(), shipment.getTo(), shipment.getSize(), fees);
			}
			
		}

		@Override
		public void informCarrierContractCanceled(Contract contract) {
			logger.info("ohh. one of my contracts was canceled. me: " + contract.getBuyer() + "; carrier: " + contract.getSeller() + "; shipment: " + contract.getShipment());
		}


		@Override
		public void informCarrierContractAccepted(Contract contract) {
			logger.info("huhu. I contracted a new carrier. me: " + contract.getBuyer() + "; carrier: " + contract.getSeller() + "; shipment: " + contract.getShipment());
		}
		
	}
	
	public TransportChainAgentFactory chainAgentFactory;
	
	public KarlsruheTSPAgentFactory(TransportChainAgentFactory chainAgentFactory) {
		super();
		this.chainAgentFactory = chainAgentFactory;
	}

	@Override
	public TSPAgent createTspAgent(TSPAgentTracker tspAgentTracker, TransportServiceProvider tsp) {
		KarlsruheTSPAgent karlsruheTSPAgent = new KarlsruheTSPAgent(tsp, chainAgentFactory);
		CostMemoryImpl costMemory = new CostMemoryImpl();
		costMemory.learningRate = 0.5;
		karlsruheTSPAgent.setCostTable(costMemory);
		return karlsruheTSPAgent;
	}

}
