package city2000w;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.mzilske.freight.BasicCarrierAgentImpl;
import playground.mzilske.freight.Carrier;
import playground.mzilske.freight.CarrierAgent;
import playground.mzilske.freight.CarrierAgentTracker;
import playground.mzilske.freight.CarrierContract;
import playground.mzilske.freight.CarrierDriverAgent;
import playground.mzilske.freight.CarrierDriverAgentFactory;
import playground.mzilske.freight.CarrierOffer;
import playground.mzilske.freight.CostMemory;
import playground.mzilske.freight.CostMemoryImpl;
import playground.mzilske.freight.MarginalCostOfContractCalculator;
import playground.mzilske.freight.api.CarrierAgentFactory;

public class KarlsruheCarrierAgentFactory implements CarrierAgentFactory{

	public class KarlsruheCarrierAgent extends BasicCarrierAgentImpl  {

		public Logger logger = Logger.getLogger(KarlsruheCarrierAgent.class);
		
		private CostMemory costTable;
		
		private MarginalCostOfContractCalculator marginalCostOfContractCalculator;
		
		public KarlsruheCarrierAgent(CarrierAgentTracker carrierAgentTracker,Carrier carrier, PlanAlgorithm router,CarrierDriverAgentFactory driverAgentFactory) {
			super(carrierAgentTracker, carrier, router, driverAgentFactory);
		}

		public void setCostOfContractCalculator(MarginalCostOfContractCalculator costOfContractCalculator) {
			this.marginalCostOfContractCalculator = costOfContractCalculator;
		}

		public void setCostTable(CostMemory costTable) {
			this.costTable = costTable;
		}

		@Override
		public void scoreSelectedPlan() {
			
		}

		@Override
		public CarrierOffer requestOffer(Id from, Id to, int shipmentSize, double startPickup, double endPickup, double startDelivery, double endDelivery) {
			CarrierOffer offer = new CarrierOffer();
			if(costTable.getCost(from, to, shipmentSize) != null){
				offer.setPrice(round(costTable.getCost(from, to, shipmentSize)));
			}
			else{
				double capacity = carrier.getCarrierCapabilities().getCarrierVehicles().iterator().next().getCapacity();
				double loadFactor = 0.5;
				double beeLineDistance = marginalCostOfContractCalculator.getBeeLineDistance(carrier.getDepotLinkId(), from, to);
				double costs = beeLineDistance * (shipmentSize/Math.max(shipmentSize, capacity*loadFactor));
				offer.setPrice(round(costs));
			}
			offer.setId(getId());
			return offer;
		}

		private double round(double value) {
			return Math.round(value);
		}

		@Override
		public void reset() {
			
		}

		@Override
		public void calculateCosts() {
			double totalCostsToAllocate = 0.0;
			for(CarrierDriverAgent driver : carrierDriverAgents.values()){
				totalCostsToAllocate += driver.getDistance();
			}
			marginalCostOfContractCalculator.run(carrier.getDepotLinkId(), carrier.getContracts(), totalCostsToAllocate);
		}

		@Override
		public void tellLink(Id personId, Id linkId) {
			
		}

		@Override
		public void informOfferAccepted(CarrierContract contract) {
			logger.info("i am " + contract.getOffer().getId() + " and my offer was accepted :)). offer: " + contract.getOffer().getPrice());
		}

		@Override
		public void informOfferRejected(CarrierOffer offer) {
			logger.info("i am " + offer.getId() + " and my offer was rejected ;)). offer: " + offer.getPrice());
		}		
	}

	private PlanAlgorithm router;
	private CarrierDriverAgentFactory driverAgentFactory;
	private Network network;
	
	public void setNetwork(Network network) {
		this.network = network;
	}

	@Override
	public CarrierAgent createAgent(CarrierAgentTracker tracker, Carrier carrier) {
		KarlsruheCarrierAgent karlsruheCarrierAgent = new KarlsruheCarrierAgent(tracker, carrier, router, driverAgentFactory);
		CostMemoryImpl costMemory = new CostMemoryImpl();
		costMemory.learningRate = 0.75;
		karlsruheCarrierAgent.setCostTable(costMemory);
		karlsruheCarrierAgent.setCostOfContractCalculator(new MarginalCostOfContractCalculator(network, costMemory));
		return karlsruheCarrierAgent;
	}

	public KarlsruheCarrierAgentFactory(PlanAlgorithm router,CarrierDriverAgentFactory driverAgentFactory) {
		super();
		this.router = router;
		this.driverAgentFactory = driverAgentFactory;
	}
}
