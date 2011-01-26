package playground.mzilske.city2000w;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.geometry.CoordUtils;

import playground.mzilske.freight.CarrierImpl;
import playground.mzilske.freight.TSPCapabilities;
import playground.mzilske.freight.TSPContract;
import playground.mzilske.freight.TSPPlan;
import playground.mzilske.freight.TSPShipment;
import playground.mzilske.freight.TSPShipment.TimeWindow;
import playground.mzilske.freight.TransportChain;
import playground.mzilske.freight.TransportChainBuilder;


public class MinimumDepotDistanceToDestinationTSPPlanBuilder {

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
			for(TSPShipment s : c.getShipments()){
				TransportChainBuilder chainBuilder = new TransportChainBuilder(s);
				chainBuilder.schedulePickup(s.getFrom(), s.getPickUpTimeWindow());
				for (Id transshipmentCentre : transshipmentCentres) {
					CarrierImpl carrier = pickCarrier(transshipmentCentre);
					chainBuilder.scheduleLeg(carrier.getId());
					chainBuilder.scheduleDelivery(transshipmentCentre, new TimeWindow(0.0,24*3600));
					// chainBuilder.schedulePickup(transshipmentCentre, new TimeWindow(1800,24*3600)); // works
					chainBuilder.schedulePickup(transshipmentCentre, new TimeWindow(120,24*3600)); // too early
				}
				CarrierImpl carrier = pickCarrier(s.getTo());
				chainBuilder.scheduleLeg(carrier.getId());
				chainBuilder.scheduleDelivery(s.getTo(),s.getDeliveryTimeWindow());
				chains.add(chainBuilder.build());
			}
		}
		TSPPlan plan = new TSPPlan(chains);
		return plan;
	}

	private CarrierImpl pickCarrier(Id destinationLinkId) {
		List<CarrierImpl> carrierList = new ArrayList<CarrierImpl>(carriers);
		double minDist = Double.POSITIVE_INFINITY;
		CarrierImpl minDistCarrier = null;
		for (CarrierImpl carrier : carrierList) {
			Coord depotLocation = network.getLinks().get(carrier.getDepotLinkId()).getCoord();
			Coord destinationLocation = network.getLinks().get(destinationLinkId).getCoord();
			double dist = CoordUtils.calcDistance(depotLocation, destinationLocation);
			if (dist < minDist) {
				minDist = dist;
				minDistCarrier = carrier;
			}
		}
		return minDistCarrier;
	}

	public MinimumDepotDistanceToDestinationTSPPlanBuilder(Network network) {
		super();
		this.network = network;
	}
	
	

}
