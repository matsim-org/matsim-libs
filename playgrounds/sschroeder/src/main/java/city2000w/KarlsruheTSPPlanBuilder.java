package city2000w;

import org.matsim.contrib.freight.carrier.Offer;
import org.matsim.contrib.freight.carrier.*;
import org.matsim.contrib.freight.events.CarrierOfferRejectEvent;
import org.matsim.contrib.freight.events.QueryCarrierOffersEvent;
import org.matsim.contrib.freight.trade.Service;
import playground.mzilske.freight.*;
import playground.mzilske.freight.events.OfferUtils;

import java.util.*;

public class KarlsruheTSPPlanBuilder {
	
	private TSPAgentTracker tspAgentTracker;
	
	private TransportServiceProvider tsp;
	
	public KarlsruheTSPPlanBuilder(TSPAgentTracker tspAgentTracker, TransportServiceProvider tsp) {
		super();
		this.tspAgentTracker = tspAgentTracker;
		this.tsp = tsp;
	}

	public TSPPlan buildPlan(Collection<TSPContract> contracts, TSPCapabilities tspCapabilities) {
		if(contracts.isEmpty()){
			return getEmptyPlan(tspCapabilities);
		}
		Collection<TransportChain> chains = new ArrayList<TransportChain>();
		for(TSPContract contract : contracts){
			TSPShipment shipment = contract.getShipment();
			TransportChainBuilder chainBuilder = new TransportChainBuilder(shipment);
			chainBuilder.schedulePickup(shipment.getFrom(), shipment.getPickupTimeWindow());
			CarrierOffer bestOffer = getOffer(getService(shipment));
			CarrierShipment carrierShipment = CarrierUtils.createShipment(shipment.getFrom(), shipment.getTo(), 10, shipment.getPickupTimeWindow().getStart(), 
					shipment.getPickupTimeWindow().getEnd(), shipment.getDeliveryTimeWindow().getStart(), shipment.getDeliveryTimeWindow().getEnd());
			CarrierContract carrierContract = new CarrierContract(tsp.getId(),bestOffer.getId(), carrierShipment, bestOffer);
			chainBuilder.scheduleLeg(carrierContract);
			chainBuilder.scheduleDelivery(shipment.getTo(), shipment.getDeliveryTimeWindow());
			TransportChain newChain = chainBuilder.build();
			chains.add(newChain);
		}
		return new TSPPlan(chains);
	}
	
	private TSPPlan getEmptyPlan(TSPCapabilities tspCapabilities) {
		TSPPlan plan = new TSPPlan(Collections.EMPTY_LIST);
		return plan;
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

	private Service getService(TSPShipment shipment) {
		return OfferUtils.createService(shipment);
	}
	
}
