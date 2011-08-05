package freight.offermaker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.gbl.MatsimRandom;

import playground.mzilske.freight.CarrierAgentTracker;
import playground.mzilske.freight.CarrierOffer;
import playground.mzilske.freight.TSPAgent;
import playground.mzilske.freight.TSPContract;
import playground.mzilske.freight.TSPOffer;
import playground.mzilske.freight.TSPOfferMaker;
import playground.mzilske.freight.TSPPlan;
import playground.mzilske.freight.TSPShipment;
import playground.mzilske.freight.TransportChain;
import playground.mzilske.freight.TransportServiceProviderImpl;
import playground.mzilske.freight.TSPPlanBuilder;
import playground.mzilske.freight.TransportChain.ChainTriple;
import freight.TSPUtils;

public class CarrierCostRequester implements TSPOfferMaker{

	static class Leg {
		Id from;
		Id to;
		int size;
		public Leg(Id from, Id to, int size) {
			super();
			this.from = from;
			this.to = to;
			this.size = size;
		}
		
	}
	
	private static Logger logger = Logger.getLogger(CarrierCostRequester.class);
	
	private TransportServiceProviderImpl tsp;
	
	private TSPAgent tspAgent;
	
	private TSPPlanBuilder tspPlanBuilder;
	
	public void setTspPlanBuilder(TSPPlanBuilder tspPlanBuilder) {
		this.tspPlanBuilder = tspPlanBuilder;
	}
	
	public void setTspAgent(TSPAgent tspAgent) {
		this.tspAgent = tspAgent;
	}

	public void setTSP(TransportServiceProviderImpl tsp) {
		this.tsp = tsp;
	}

	@Override
	public TSPOffer requestOffer(Id from, Id to, int size, Double startPickup, Double endPickup, Double startDelivery, Double endDelivery,Double memorizedPrice) {
		TSPOffer tspOffer = new TSPOffer();
		if(memorizedPrice != null){
			if(MatsimRandom.getRandom().nextDouble() < 0.7){
				tspOffer.setId(tsp.getId());
				tspOffer.setPrice(memorizedPrice);
				return tspOffer;
			}
		}
		TransportChain chain = getTransportChain(from, to, size, startPickup, endPickup, startDelivery, endDelivery);
		assertPickupTimes(from, startPickup, 0.0, 86400.0);
		
		Double price = null;
		int nOfTranshipments = chain.getChainTriples().size() - 1;
		List<CarrierOffer> carrierOffers = new ArrayList<CarrierOffer>();
		for(ChainTriple chainTriple : chain.getChainTriples()){
			CarrierOffer carrierOffer = chainTriple.getLeg().getAcceptedOffer();
			if(price == null){
				price = carrierOffer.getPrice();
				
			}
			else{
				price += carrierOffer.getPrice();
			}
			carrierOffers.add(carrierOffer);
		}
		if(price != null){
			tspOffer.setId(tsp.getId());
			tspOffer.setPrice(price  + TSPAgent.CostParameter.transshipmentHandlingCostPerUnit*nOfTranshipments*size);
			tspAgent.memorizeOffer(tspOffer,carrierOffers);
			return tspOffer;
		}
		return null;
	}
	
	private void assertPickupTimes(Id from, Double startPickup, double d, double e) {
		if(from.toString().equals("industry")){
			if(startPickup == d || startPickup == e){
				return;
			}
			else{
				throw new IllegalStateException("this should not be");
			}
		}
		
	}
	
	private void assertStartPickupEither(double d, double e, double f) {
		if( d == e || d== f){
			return;
		}
		else {
			throw new IllegalStateException("this should not be");
		}
		
	}

	private TransportChain getTransportChain(Id from, Id to, int size, Double startPickup, Double endPickup, Double startDelivery, Double endDelivery) {
		TSPShipment tspShipment = TSPUtils.createTSPShipment(from, to, size, startPickup, endPickup, startDelivery, endDelivery);
		TSPOffer tspOffer = new TSPOffer();
		tspOffer.setId(tsp.getId());
		TSPContract contract = TSPUtils.createTSPContract(tspShipment, tspOffer);
		List<TSPContract> contracts = new ArrayList<TSPContract>();
		contracts.add(contract);
		TSPPlan plan = tspPlanBuilder.buildPlan(contracts,tsp.getTspCapabilities());
		TransportChain transportChain = plan.getChains().iterator().next();
		return transportChain;
	}

	public void reset(int iteration){
		
	}

}
