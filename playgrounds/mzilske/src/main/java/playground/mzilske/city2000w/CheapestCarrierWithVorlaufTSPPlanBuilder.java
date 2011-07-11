package playground.mzilske.city2000w;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.log4j.Logger;
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
	
	private Logger logger = Logger.getLogger(CheapestCarrierWithVorlaufTSPPlanBuilder.class);
	
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
			
			TSPShipment s = c.getShipment();
			Id fromLocation = s.getFrom();
			TransportChainBuilder chainBuilder = new TransportChainBuilder(s);
			chainBuilder.schedulePickup(fromLocation, s.getPickUpTimeWindow());
			for (Id transshipmentCentre : transshipmentCentres) { 
				Offer acceptedOffer = pickUnknownOffer(fromLocation, transshipmentCentre, s.getSize(), tspKnowledge);

				//					Offer acceptedOffer = pickOffer(fromLocation, transshipmentCentre, s.getSize());
				chainBuilder.scheduleLeg(acceptedOffer);
				chainBuilder.scheduleDelivery(transshipmentCentre, new TimeWindow(0.0,24*3600));
				chainBuilder.schedulePickup(transshipmentCentre, new TimeWindow(6*3600,24*3600)); // works
				// chainBuilder.schedulePickup(transshipmentCentre, new TimeWindow(120,24*3600)); // too early
				fromLocation = transshipmentCentre;
			}
			Offer acceptedOffer = pickUnknownOffer(fromLocation, s.getTo(), s.getSize(), tspKnowledge);
			chainBuilder.scheduleLeg(acceptedOffer);
			chainBuilder.scheduleDelivery(s.getTo(),s.getDeliveryTimeWindow());
			chains.add(chainBuilder.build());
			
		}
		TSPPlan plan = new TSPPlan(chains);
		return plan;
	}

	private Offer pickUnknownOffer(Id sourceLinkId, Id destinationLinkId, int size, TSPKnowledge tspKnowledge) {
		ArrayList<Offer> offers = new ArrayList<Offer>(carrierAgentTracker.getOffers(sourceLinkId, destinationLinkId, size));
		sortOffers(offers);
		logger.info("pick carrierId=" + offers.get(0).getCarrierId() + " for " + sourceLinkId + " to " + destinationLinkId + " with price of " + offers.get(0).getPrice());
		return offers.get(0);
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
