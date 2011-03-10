package playground.mzilske.city2000w;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.matsim.api.core.v01.Id;

import playground.mzilske.freight.CarrierAgentTracker;
import playground.mzilske.freight.Offer;
import playground.mzilske.freight.TSPCapabilities;
import playground.mzilske.freight.TSPContract;
import playground.mzilske.freight.TSPKnowledge;
import playground.mzilske.freight.TSPPlan;
import playground.mzilske.freight.TSPShipment;
import playground.mzilske.freight.TSPShipment.TimeWindow;
import playground.mzilske.freight.TransportChain;
import playground.mzilske.freight.TransportChainBuilder;


public class CheapestCarrierWithVorlaufTSPPlanBuilder {
	
	private List<Id> transshipmentCentres;

	private CarrierAgentTracker carrierAgentTracker;
	
	public void setCarrierAgentTracker(CarrierAgentTracker carrierAgentTracker) {
		this.carrierAgentTracker = carrierAgentTracker;
	}

	public void setTransshipmentCentres(List<Id> transshipmentCentres) {
		this.transshipmentCentres = transshipmentCentres;
	}

	public TSPPlan buildPlan(Collection<TSPContract> contracts, TSPCapabilities tspCapabilities, TSPKnowledge tspKnowledge) {
		Collection<TransportChain> chains = new ArrayList<TransportChain>();
		for(TSPContract c : contracts){
			for(TSPShipment s : c.getShipments()){
				Id fromLocation = s.getFrom();
				TransportChainBuilder chainBuilder = new TransportChainBuilder(s);
				chainBuilder.schedulePickup(fromLocation, s.getPickUpTimeWindow());
				for (Id transshipmentCentre : transshipmentCentres) { 
					Offer acceptedOffer = pickKnownOffer(fromLocation, transshipmentCentre, s.getSize(), tspKnowledge);
					
//					Offer acceptedOffer = pickOffer(fromLocation, transshipmentCentre, s.getSize());
					chainBuilder.scheduleLeg(acceptedOffer);
					chainBuilder.scheduleDelivery(transshipmentCentre, new TimeWindow(0.0,24*3600));
					chainBuilder.schedulePickup(transshipmentCentre, new TimeWindow(240,24*3600)); // works
					// chainBuilder.schedulePickup(transshipmentCentre, new TimeWindow(120,24*3600)); // too early
					fromLocation = transshipmentCentre;
				}
				Offer acceptedOffer = pickUnknownOffer(fromLocation, s.getTo(), s.getSize(), tspKnowledge);
				chainBuilder.scheduleLeg(acceptedOffer);
				chainBuilder.scheduleDelivery(s.getTo(),s.getDeliveryTimeWindow());
				chains.add(chainBuilder.build());
			}
		}
		TSPPlan plan = new TSPPlan(chains);
		return plan;
	}

	private Offer pickKnownOffer(Id sourceLinkId, Id destinationLinkId, int size, TSPKnowledge tspKnowledge) {
		ArrayList<Offer> offers = new ArrayList<Offer>(carrierAgentTracker.getOffers(sourceLinkId, destinationLinkId, size));
		boolean offerFound = false;
		Offer cheapestKnownOffer = null;
		for(Offer o : offers){
			if(tspKnowledge.getKnownCarriers().contains(o.getCarrierId())){
				offerFound = true;
				if(cheapestKnownOffer == null){
					cheapestKnownOffer = o;
				}
				else if(o.getPrice() < cheapestKnownOffer.getPrice()){
					cheapestKnownOffer = o;
				}
			}
		}
		if(offerFound){
			return cheapestKnownOffer;
		}
		else{
			sortOffers(offers);
			return offers.get(0);
		}
	}

	private Offer pickUnknownOffer(Id sourceLinkId, Id destinationLinkId, int size, TSPKnowledge tspKnowledge) {
		ArrayList<Offer> offers = new ArrayList<Offer>(carrierAgentTracker.getOffers(sourceLinkId, destinationLinkId, size));
		Offer cheapestUnknownOffer = null;
		for(Offer o : offers){
			if(!tspKnowledge.getKnownCarriers().contains(o.getCarrierId())){
				if(cheapestUnknownOffer == null){
					cheapestUnknownOffer = o;
				}
				else if(o.getPrice() < cheapestUnknownOffer.getPrice()){
					cheapestUnknownOffer = o;
				}
			}
		}
		return cheapestUnknownOffer;
	}

	private void sortOffers(ArrayList<Offer> offers) {
		Collections.sort(offers, new Comparator<Offer>() {

			@Override
			public int compare(Offer arg0, Offer arg1) {
				return arg0.getPrice().compareTo(arg1.getPrice());
			}
			
		});
	}
	

}
