package city2000w.replanning;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.matsim.core.gbl.MatsimRandom;

import playground.mzilske.freight.TSPAgentTracker;
import playground.mzilske.freight.TSPPlan;
import playground.mzilske.freight.TransportChain;
import playground.mzilske.freight.TransportChain.ChainLeg;
import playground.mzilske.freight.TransportChainBuilder;
import playground.mzilske.freight.TransportServiceProvider;
import playground.mzilske.freight.api.Offer;
import playground.mzilske.freight.carrier.CarrierContract;
import playground.mzilske.freight.carrier.CarrierOffer;
import playground.mzilske.freight.carrier.CarrierShipment;
import playground.mzilske.freight.carrier.CarrierUtils;
import playground.mzilske.freight.carrier.Shipment;
import playground.mzilske.freight.events.CarrierOfferRejectEvent;
import playground.mzilske.freight.events.OfferUtils;
import playground.mzilske.freight.events.QueryCarrierOffersEvent;
import playground.mzilske.freight.events.Service;
import playground.mzilske.freight.events.TSPCarrierContractAcceptEvent;
import playground.mzilske.freight.events.TSPCarrierContractCanceledEvent;
import playground.mzilske.freight.events.TransportChainAddedEvent;
import playground.mzilske.freight.events.TransportChainRemovedEvent;

public class CarrierSelector implements TSPPlanStrategyModule{

	private TSPAgentTracker tspAgentTracker;

	public CarrierSelector(TSPAgentTracker tspAgentTracker) {
		super();
		this.tspAgentTracker = tspAgentTracker;
	}


	@Override
	public void handleActor(TransportServiceProvider tsp) {
		TSPPlan newPlan = tsp.getSelectedPlan();
		TransportChain selectedChain = selectChain(newPlan.getChains());
		for(ChainLeg leg : selectedChain.getLegs()){
			tspAgentTracker.processEvent(new TSPCarrierContractCanceledEvent(leg.getContract()));
		}
		
		TransportChainBuilder chainBuilder = new TransportChainBuilder(selectedChain.getShipment());
		chainBuilder.schedulePickup(selectedChain.getShipment().getFrom(), selectedChain.getShipment().getPickupTimeWindow());
		CarrierOffer bestOffer = getOffer(getService(selectedChain.getShipment()));
		CarrierShipment shipment = CarrierUtils.createShipment(selectedChain.getShipment().getFrom(), selectedChain.getShipment().getTo(), selectedChain.getShipment().getSize(), selectedChain.getShipment().getPickupTimeWindow().getStart(), 
				selectedChain.getShipment().getPickupTimeWindow().getEnd(), selectedChain.getShipment().getDeliveryTimeWindow().getStart(), selectedChain.getShipment().getDeliveryTimeWindow().getEnd());
		CarrierContract contract = new CarrierContract(tsp.getId(),bestOffer.getId(), shipment, bestOffer);
		chainBuilder.scheduleLeg(contract);
		chainBuilder.scheduleDelivery(selectedChain.getShipment().getTo(), selectedChain.getShipment().getDeliveryTimeWindow());
		TransportChain newChain = chainBuilder.build();
		
		tspAgentTracker.processEvent(new TSPCarrierContractAcceptEvent(contract));
		tspAgentTracker.processEvent(new TransportChainRemovedEvent(tsp.getId(),selectedChain));
		tspAgentTracker.processEvent(new TransportChainAddedEvent(tsp.getId(),newChain));
		
		Collection<TransportChain> chains = new ArrayList<TransportChain>();
		chains.add(newChain);
		for(TransportChain tpChain : newPlan.getChains()){
			if(!tpChain.equals(selectedChain)){
				chains.add(tpChain);
			}
		}
		newPlan = new TSPPlan(chains);
		tsp.setSelectedPlan(newPlan);
	}
	
	private TransportChain selectChain(Collection<TransportChain> chains) {
		List<TransportChain> tpChains = new ArrayList<TransportChain>(chains);
		int randIndex = MatsimRandom.getRandom().nextInt(tpChains.size());
		return tpChains.get(randIndex);
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
