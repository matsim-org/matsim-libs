package city2000w;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.mzilske.freight.BasicCarrierAgentImpl;
import playground.mzilske.freight.Carrier;
import playground.mzilske.freight.CarrierAgent;
import playground.mzilske.freight.CarrierAgentTracker;
import playground.mzilske.freight.CarrierDriverAgentFactory;
import playground.mzilske.freight.CarrierOffer;
import playground.mzilske.freight.Contract;
import playground.mzilske.freight.api.CarrierAgentFactory;
import playground.mzilske.freight.api.Offer;

public class KarlsruheCarrierAgentFactory implements CarrierAgentFactory{

	class KarlsruheAgent extends BasicCarrierAgentImpl  {

		private Logger logger = Logger.getLogger(KarlsruheAgent.class);
		
		public KarlsruheAgent(CarrierAgentTracker carrierAgentTracker,Carrier carrier, PlanAlgorithm router,CarrierDriverAgentFactory driverAgentFactory) {
			super(carrierAgentTracker, carrier, router, driverAgentFactory);
		}

		@Override
		public void scoreSelectedPlan() {
			
		}

		@Override
		public CarrierOffer requestOffer(Id linkId, Id linkId2,int shipmentSize, double startPickup, double endPickup,double startDelivery, double endDelivery) {
			CarrierOffer offer = new CarrierOffer();
			offer.setId(getId());
			offer.setPrice(MatsimRandom.getRandom().nextInt(100));
			return offer;
		}

		@Override
		public void reset() {
			
		}

		@Override
		public void calculateCosts() {
			
		}

		@Override
		public void tellLink(Id personId, Id linkId) {
			
		}

		@Override
		public void informOfferRejected(Offer offer) {
			logger.info("i am " + offer.getId() + " and my offer was rejected :(. offer: " + offer.getPrice());
		}

		@Override
		public void informOfferAccepted(Contract contract) {
			logger.info("i am " + contract.getOffer().getId() + " and my offer was accepted :)). offer: " + contract.getOffer().getPrice());
		}

	
		
	}



	private PlanAlgorithm router;
	private CarrierDriverAgentFactory driverAgentFactory;
	
	
	
	@Override
	public CarrierAgent createAgent(CarrierAgentTracker tracker, Carrier carrier) {
		return new KarlsruheAgent(tracker, carrier, router, driverAgentFactory);
	}



	public KarlsruheCarrierAgentFactory(PlanAlgorithm router,
			CarrierDriverAgentFactory driverAgentFactory) {
		super();
		this.router = router;
		this.driverAgentFactory = driverAgentFactory;
	}





}
