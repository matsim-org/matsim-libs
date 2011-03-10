package playground.mzilske.city2000w;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.geometry.CoordUtils;

import playground.mzilske.freight.CarrierAgentTracker;
import playground.mzilske.freight.Carriers;
import playground.mzilske.freight.Offer;
import playground.mzilske.freight.TSPCapabilities;
import playground.mzilske.freight.TSPContract;
import playground.mzilske.freight.TSPPlan;
import playground.mzilske.freight.TSPShipment;
import playground.mzilske.freight.TSPShipment.TimeWindow;
import playground.mzilske.freight.TransportChain;
import playground.mzilske.freight.TransportChainBuilder;


public class MinimumDepotDistanceToDestinationTSPPlanBuilder {

	private Carriers carriers;
	
	private List<Id> transshipmentCentres;
	
	private Network network;

	private CarrierAgentTracker carrierAgentTracker;
	
	public void setCarriers(Carriers carriers, CarrierAgentTracker carrierAgentTracker) {
		this.carriers = carriers;
		this.carrierAgentTracker = carrierAgentTracker;
	}

	public void setTransshipmentCentres(List<Id> transshipmentCentres) {
		this.transshipmentCentres = transshipmentCentres;
	}

	public TSPPlan buildPlan(Collection<TSPContract> contracts, TSPCapabilities tspCapabilities) {
		Collection<TransportChain> chains = new ArrayList<TransportChain>();
		for(TSPContract c : contracts){
			for(TSPShipment s : c.getShipments()){
				Id fromLocation = s.getFrom();
				TransportChainBuilder chainBuilder = new TransportChainBuilder(s);
				chainBuilder.schedulePickup(fromLocation, s.getPickUpTimeWindow());
				for (Id transshipmentCentre : transshipmentCentres) {
					Offer carrier = pickCarrier(fromLocation, transshipmentCentre, s.getSize());
					chainBuilder.scheduleLeg(carrier);
					chainBuilder.scheduleDelivery(transshipmentCentre, new TimeWindow(0.0,24*3600));
					chainBuilder.schedulePickup(transshipmentCentre, new TimeWindow(300,24*3600)); // works
					// chainBuilder.schedulePickup(transshipmentCentre, new TimeWindow(120,24*3600)); // too early
					fromLocation = transshipmentCentre;
				}
				Offer carrier = pickCarrier(fromLocation, s.getTo(), s.getSize());
				chainBuilder.scheduleLeg(carrier);
				chainBuilder.scheduleDelivery(s.getTo(),s.getDeliveryTimeWindow());
				chains.add(chainBuilder.build());
			}
		}
		TSPPlan plan = new TSPPlan(chains);
		return plan;
	}

	private Offer pickCarrier(Id sourceLinkId, Id destinationLinkId, int size) {
		Collection<Offer> offers = carrierAgentTracker.getOffers(sourceLinkId, destinationLinkId, size);
		double minDist = Double.POSITIVE_INFINITY;
		Offer minDistCarrier = null;
		for (Offer offer : offers) {
			Coord depotLocation = network.getLinks().get(carriers.getCarriers().get(offer.getCarrierId()).getDepotLinkId()).getCoord();
			Coord destinationLocation = network.getLinks().get(destinationLinkId).getCoord();
			double dist = CoordUtils.calcDistance(depotLocation, destinationLocation);
			if (dist < minDist) {
				minDist = dist;
				minDistCarrier = offer;
			}
		}
		return minDistCarrier;
	}

	public MinimumDepotDistanceToDestinationTSPPlanBuilder(Network network) {
		super();
		this.network = network;
	}
	
	

}
