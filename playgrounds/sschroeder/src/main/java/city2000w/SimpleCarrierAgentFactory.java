package city2000w;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.mzilske.freight.Contract;
import playground.mzilske.freight.api.CarrierAgentFactory;
import playground.mzilske.freight.carrier.Carrier;
import playground.mzilske.freight.carrier.CarrierAgent;
import playground.mzilske.freight.carrier.CarrierAgentImpl;
import playground.mzilske.freight.carrier.CarrierAgentTracker;
import playground.mzilske.freight.carrier.CarrierContract;
import playground.mzilske.freight.carrier.CarrierDriverAgentFactory;
import playground.mzilske.freight.carrier.CarrierOffer;

public class SimpleCarrierAgentFactory implements CarrierAgentFactory{

	static class SimpleCarrierAgentImpl extends CarrierAgentImpl{

		private Logger logger = Logger.getLogger(SimpleCarrierAgentImpl.class);
		
		public double price = 10;
		
		private Collection<Contract> canceledContracts = new ArrayList<Contract>();
		
		private Collection<Contract> newContracts = new ArrayList<Contract>();
		
		public SimpleCarrierAgentImpl(CarrierAgentTracker carrierAgentTracker,Carrier carrier, PlanAlgorithm router,
				CarrierDriverAgentFactory driverAgentFactory) {
			super(carrierAgentTracker, carrier, router, driverAgentFactory);
			// TODO Auto-generated constructor stub
		}

		@Override
		public void scoreSelectedPlan() {
			
			
		}

		@Override
		public CarrierOffer requestOffer(Id linkId, Id linkId2,int shipmentSize, double startPickup, double endPickup,
				double startDelivery, double endDelivery) {
			CarrierOffer offer = new CarrierOffer();
			offer.setId(carrier.getId());
			offer.setPrice(price);
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
			// TODO Auto-generated method stub
			
		}

		
		@Override
		public void informOfferRejected(CarrierOffer offer) {
			logger.info(carrier.getId() + "; my offer was rejected. i offered " + offer.getPrice());
		}

		@Override
		public void informOfferAccepted(CarrierContract contract) {
			logger.info(carrier.getId() + "; offer accepted :). i offered " + contract.getOffer().getPrice());
			carrier.getContracts().add(contract);
			newContracts.add(contract);
		}

	}

	private PlanAlgorithm router;
	private CarrierDriverAgentFactory driverAgentFactory;
	
	public SimpleCarrierAgentFactory(PlanAlgorithm router,
			CarrierDriverAgentFactory driverAgentFactory) {
		super();
		this.router = router;
		this.driverAgentFactory = driverAgentFactory;
	}

	@Override
	public CarrierAgent createAgent(CarrierAgentTracker tracker, Carrier carrier) {
		double price = MatsimRandom.getRandom().nextInt(100) + 1;
		SimpleCarrierAgentImpl carrierAgent = new SimpleCarrierAgentImpl(tracker, carrier, router, driverAgentFactory);
		carrierAgent.price = price;
		return carrierAgent;
	}

}
