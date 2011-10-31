package city2000w.replanning;

import org.apache.log4j.Logger;
import org.matsim.contrib.freight.carrier.Contract;
import org.matsim.contrib.freight.carrier.Offer;
import org.matsim.contrib.freight.carrier.*;
import org.matsim.contrib.freight.events.CarrierOfferRejectEvent;
import org.matsim.contrib.freight.events.QueryCarrierOffersEvent;
import org.matsim.contrib.freight.events.TSPCarrierContractAcceptEvent;
import org.matsim.contrib.freight.events.TSPCarrierContractCanceledEvent;
import org.matsim.contrib.freight.trade.Service;
import playground.mzilske.freight.*;
import playground.mzilske.freight.TransportChain.ChainLeg;
import playground.mzilske.freight.events.*;

import java.util.*;

public class TSPContractLandscapeChangedResponder implements TSPPlanStrategyModule{

	private Logger logger = Logger.getLogger(TSPContractLandscapeChangedResponder.class);
	
	private TSPAgentTracker tspAgentTracker;
	
	public TSPContractLandscapeChangedResponder(TSPAgentTracker tspAgentTracker) {
		super();
		this.tspAgentTracker = tspAgentTracker;
	}

	@Override
	public void handleActor(TransportServiceProvider tsp) {
		TSPPlan newPlan = tsp.getSelectedPlan();
		if(!tsp.getNewContracts().isEmpty()){
			logger.info("ohhh. i got a new contract and thus have to replan!!!");
			Collection<TransportChain> chains = new ArrayList<TransportChain>();
			for(Contract c : tsp.getNewContracts()){
				TSPShipment tspShipment = (TSPShipment)c.getShipment();
				TransportChainBuilder chainBuilder = new TransportChainBuilder(tspShipment);
				chainBuilder.schedulePickup(tspShipment.getFrom(), tspShipment.getPickupTimeWindow());
				CarrierOffer bestOffer = getOffer(getService(tspShipment));
				CarrierShipment shipment = CarrierUtils.createShipment(tspShipment.getFrom(), tspShipment.getTo(), tspShipment.getSize(), tspShipment.getPickupTimeWindow().getStart(), 
						tspShipment.getPickupTimeWindow().getEnd(), tspShipment.getDeliveryTimeWindow().getStart(), tspShipment.getDeliveryTimeWindow().getEnd());
				CarrierContract contract = new CarrierContract(tsp.getId(),bestOffer.getId(), shipment, bestOffer);
				chainBuilder.scheduleLeg(contract);
				chainBuilder.scheduleDelivery(tspShipment.getTo(), tspShipment.getDeliveryTimeWindow());
				TransportChain newChain = chainBuilder.build();
				tspAgentTracker.processEvent(new TSPCarrierContractAcceptEvent(contract));
				tspAgentTracker.processEvent(new TransportChainAddedEvent(tsp.getId(),newChain));;
				chains.add(newChain);
			}
			chains.addAll(newPlan.getChains());
			newPlan = new TSPPlan(chains);
			tsp.getNewContracts().clear();
		}
		
		if(!tsp.getExpiredContracts().isEmpty()){
			logger.info("ohhh. i old contracts and thus have to replan!!!");
			Set<TSPShipment> tspOldTSPShipments = new HashSet<TSPShipment>();
			for(Contract c : tsp.getExpiredContracts()){
				tspOldTSPShipments.add((TSPShipment)c.getShipment());
			}
			Collection<TransportChain> chains = new ArrayList<TransportChain>();
			for(TransportChain chain : newPlan.getChains()){
				if(!tspOldTSPShipments.contains(chain.getShipment())){
					chains.add(chain);
				}
				else{
					for(ChainLeg leg : chain.getLegs()){
						tspAgentTracker.processEvent(new TSPCarrierContractCanceledEvent(leg.getContract()));
					}
					tspAgentTracker.processEvent(new TransportChainRemovedEvent(tsp.getId(),chain));;
				}
			}
			newPlan = new TSPPlan(chains);
			tsp.getExpiredContracts().clear();
		}
		tsp.setSelectedPlan(newPlan);
	}
	
	private CarrierOffer getOffer(Service service) {
		Collection<Offer> offers = new ArrayList<Offer>();
//		tspAgentTracker.processEvent(new TSPCarrierContractCanceledEvent(c));
		QueryCarrierOffersEvent queryEvent = new QueryCarrierOffersEvent(offers, service);
		tspAgentTracker.processEvent(queryEvent);
		List<Offer> offerList = new ArrayList<Offer>(queryEvent.getOffers());
		Collections.sort(offerList, new Comparator<Offer>(){

			@Override
			public int compare(Offer arg0, Offer arg1) {
				if(arg0.getPrice() < arg1.getPrice()){
					return -1;
				}
				else{
					return 1;
				}
			}
		});
		CarrierOffer bestOffer = (CarrierOffer)offerList.get(0);
		for(Offer o : offers){
			if(o != bestOffer){
				tspAgentTracker.processEvent(new CarrierOfferRejectEvent(o.getId(),o));
			}
		}
		return bestOffer;
	}
	
	private Service getService(Shipment shipment) {
		return OfferUtils.createService(shipment);
	}

}
