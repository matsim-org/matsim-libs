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
import playground.mzilske.freight.TSPPlan;
import playground.mzilske.freight.TSPShipment;
import playground.mzilske.freight.TSPShipment.TimeWindow;
import playground.mzilske.freight.TransportChain;
import playground.mzilske.freight.TransportChainBuilder;


public class CheapestCarrierTSPPlanBuilder {
	
	private List<Id> transshipmentCentres;

	private CarrierAgentTracker carrierAgentTracker;
	
	public void setCarrierAgentTracker(CarrierAgentTracker carrierAgentTracker) {
		this.carrierAgentTracker = carrierAgentTracker;
	}

	public void setTransshipmentCentres(List<Id> transshipmentCentres) {
		this.transshipmentCentres = transshipmentCentres;
	}

	public TSPPlan buildPlan(Collection<TSPContract> contracts, TSPCapabilities tspCapabilities) {
		Collection<TransportChain> chains = new ArrayList<TransportChain>();
		for(TSPContract c : contracts){

			TSPShipment s = c.getShipment();
			Id fromLocation = s.getFrom();
			TransportChainBuilder chainBuilder = new TransportChainBuilder(s);
			chainBuilder.schedulePickup(fromLocation, s.getPickUpTimeWindow());
			for (Id transshipmentCentre : transshipmentCentres) { 
				Offer acceptedOffer = pickOffer(fromLocation, transshipmentCentre, s.getSize());
				chainBuilder.scheduleLeg(acceptedOffer);
				chainBuilder.scheduleDelivery(transshipmentCentre, new TimeWindow(0.0,24*3600));
				chainBuilder.schedulePickup(transshipmentCentre, new TimeWindow(1400,24*3600)); // works
				// chainBuilder.schedulePickup(transshipmentCentre, new TimeWindow(120,24*3600)); // too early
				fromLocation = transshipmentCentre;
			}
			Offer acceptedOffer = pickOffer(fromLocation, s.getTo(), s.getSize());
			chainBuilder.scheduleLeg(acceptedOffer);
			chainBuilder.scheduleDelivery(s.getTo(),s.getDeliveryTimeWindow());
			chains.add(chainBuilder.build());

		}
		TSPPlan plan = new TSPPlan(chains);
		return plan;
	}

	private Offer pickOffer(Id sourceLinkId, Id destinationLinkId, int size) {
		ArrayList<Offer> offers = new ArrayList<Offer>(carrierAgentTracker.getOffers(sourceLinkId, destinationLinkId, size));
		Collections.sort(offers, new Comparator<Offer>() {

			@Override
			public int compare(Offer arg0, Offer arg1) {
				return arg0.getPrice().compareTo(arg1.getPrice());
			}
			
		});
		Offer bestOffer = offers.get(0);
		return bestOffer;
	}
	

}
