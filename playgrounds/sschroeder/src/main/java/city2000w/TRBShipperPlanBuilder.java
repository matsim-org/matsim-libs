package city2000w;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.matsim.core.gbl.MatsimRandom;

import playground.mzilske.freight.TSPAgentTracker;
import playground.mzilske.freight.TSPOffer;
import city2000w.TRBShippersContractGenerator.TimeProfile;
import freight.ScheduledCommodityFlow;
import freight.ShipperContract;
import freight.ShipperKnowledge;
import freight.ShipperPlan;
import freight.ShipperShipment;
import freight.ShipperUtils;

public class TRBShipperPlanBuilder {
	
	private TSPAgentTracker tspAgentTracker;
	private OfferSelectorImpl<TSPOffer> tspOfferSelector;
	
	public ShipperPlan buildPlan(ShipperKnowledge shipperKnowledge, Collection<ShipperContract> contracts) {
		Collection<ScheduledCommodityFlow> scheduledFlows = new ArrayList<ScheduledCommodityFlow>();
		for(ShipperContract c : contracts){
			Collection<ShipperShipment> shipments = new ArrayList<ShipperShipment>();
			if(MatsimRandom.getRandom().nextDouble() < 0.5){
				TimeProfile timeProfile = shipperKnowledge.getTimeProfile(1).iterator().next();
				ShipperShipment shipment = ShipperUtils.createShipment(c.getCommodityFlow().getFrom(), c.getCommodityFlow().getTo(), c.getCommodityFlow().getSize(), 
						timeProfile.pickupStart, timeProfile.pickupEnd, timeProfile.deliveryStart, timeProfile.deliveryEnd);
				shipments.add(shipment);
			}
			else{
				Collection<TimeProfile> profiles = shipperKnowledge.getTimeProfile(2);
				List<TimeProfile> profList = new ArrayList<TRBShippersContractGenerator.TimeProfile>(profiles);
				TimeProfile morning = profList.get(0);
				TimeProfile afternoon = profList.get(1);
				ShipperShipment shipment_morning = ShipperUtils.createShipment(c.getCommodityFlow().getFrom(), c.getCommodityFlow().getTo(), c.getCommodityFlow().getSize()/2, 
						morning.pickupStart, morning.pickupEnd, morning.deliveryStart, morning.deliveryEnd);
				ShipperShipment shipment_afternoon = ShipperUtils.createShipment(c.getCommodityFlow().getFrom(), c.getCommodityFlow().getTo(), c.getCommodityFlow().getSize()/2, 
						afternoon.pickupStart, afternoon.pickupEnd, afternoon.deliveryStart, afternoon.deliveryEnd);
				shipments.add(shipment_morning);
				shipments.add(shipment_afternoon);
			}		
			ScheduledCommodityFlow sComFlow = new ScheduledCommodityFlow(c.getCommodityFlow(), shipments, getRandomOffer(shipments));
			scheduledFlows.add(sComFlow);
		}
		ShipperPlan plan = new ShipperPlan(scheduledFlows);
		return plan;

	}

	private TSPOffer getRandomOffer(Collection<ShipperShipment> shipments) {
		ShipperShipment shipment = shipments.iterator().next();
		Collection<TSPOffer> offers = tspAgentTracker.requestService(shipment.getFrom(), shipment.getTo(), shipment.getSize(), 
				shipment.getPickTimeWindow().getStart(), shipment.getPickTimeWindow().getEnd(), 
				shipment.getDeliveryTimeWindow().getStart(), shipment.getDeliveryTimeWindow().getEnd());
		TSPOffer offer = pickOffer(offers);
		return offer;
	}

	private TSPOffer pickOffer(Collection<TSPOffer> offers) {
		TSPOffer bestOffer = tspOfferSelector.selectOffer(offers);
		if(bestOffer == null){
			List<TSPOffer> shuffeledOffers = new ArrayList<TSPOffer>(offers);
			Collections.shuffle(shuffeledOffers,MatsimRandom.getRandom());
			bestOffer = shuffeledOffers.get(0);
		}
		return bestOffer;
	}

	public void setTspAgentTracker(TSPAgentTracker tspAgentTracker) {
		this.tspAgentTracker = tspAgentTracker;
	}

	public void setOfferSelector(OfferSelectorImpl<TSPOffer> tspOfferSelector) {
		this.tspOfferSelector = tspOfferSelector;
		
	}
	
	

}
