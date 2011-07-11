package city2000w;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.geometry.CoordUtils;

import playground.mzilske.freight.CarrierImpl;
import playground.mzilske.freight.Offer;
import playground.mzilske.freight.TSPCapabilities;
import playground.mzilske.freight.TSPContract;
import playground.mzilske.freight.TSPPlan;
import playground.mzilske.freight.TSPShipment;
import playground.mzilske.freight.TSPShipment.TimeWindow;
import playground.mzilske.freight.TransportChain;
import playground.mzilske.freight.TransportChainBuilder;


public class SimpleTSPPlanBuilder {

	private Collection<CarrierImpl> carriers;
	
	private List<Id> transshipmentCentres;
	
	private Network network;
	
	public void setCarriers(Collection<CarrierImpl> carriers) {
		this.carriers = carriers;
	}

	public void setTransshipmentCentres(List<Id> transshipmentCentres) {
		this.transshipmentCentres = transshipmentCentres;
	}

	public TSPPlan buildPlan(Collection<TSPContract> contracts, TSPCapabilities tspCapabilities) {
		Collection<TransportChain> chains = new ArrayList<TransportChain>();
		for(TSPContract c : contracts){
			TSPShipment s = c.getShipment();
			TransportChainBuilder chainBuilder = new TransportChainBuilder(s);
			chainBuilder.schedulePickup(s.getFrom(), s.getPickUpTimeWindow());
			for (Id transshipmentCentre : transshipmentCentres) {
				CarrierImpl carrier = pickCarrier(s.getFrom());
				Offer offer = new Offer();
				offer.setCarrierId(carrier.getId());
				chainBuilder.scheduleLeg(offer);
				chainBuilder.scheduleDelivery(transshipmentCentre, new TimeWindow(0.0,24*3600));
				chainBuilder.schedulePickup(transshipmentCentre, new TimeWindow(3600,24*3600));
			}
			CarrierImpl carrier = pickCarrier(s.getTo());
			Offer offer = new Offer();
			offer.setCarrierId(carrier.getId());
			chainBuilder.scheduleLeg(offer);
			chainBuilder.scheduleDelivery(s.getTo(),s.getDeliveryTimeWindow());
			chains.add(chainBuilder.build());

		}
		TSPPlan plan = new TSPPlan(chains);
		return plan;
	}

	private CarrierImpl pickCarrier(Id destinationLinkId) {
		List<CarrierImpl> carrierList = new ArrayList<CarrierImpl>(carriers);
		
			double minDist = Double.POSITIVE_INFINITY;
			CarrierImpl minDistCarrier = null;
			for (CarrierImpl carrier : carrierList) {
				Coord depotLocation = null;
				if(network.getLinks().containsKey(carrier.getDepotLinkId())){
					depotLocation = network.getLinks().get(carrier.getDepotLinkId()).getCoord();
				}
				else{
					throw new RuntimeException(carrier.getDepotLinkId() + " does not exist");
				}
				Coord destinationLocation = null;
				if(network.getLinks().containsKey(destinationLinkId)){
					destinationLocation = network.getLinks().get(destinationLinkId).getCoord();
				}
				else{
					throw new RuntimeException(destinationLinkId + " does not exist");
				}
				double dist = CoordUtils.calcDistance(depotLocation, destinationLocation);
				if (dist < minDist) {
					minDist = dist;
					minDistCarrier = carrier;
				}
			}
			return minDistCarrier;
		
		
	}

	public SimpleTSPPlanBuilder(Network network) {
		super();
		this.network = network;
	}
	
	

}
