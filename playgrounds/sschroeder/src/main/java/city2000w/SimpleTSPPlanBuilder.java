package city2000w;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.geometry.CoordUtils;

import playground.mzilske.freight.CarrierAgentTracker;
import playground.mzilske.freight.CarrierImpl;
import playground.mzilske.freight.CarrierOffer;
import playground.mzilske.freight.TSPCapabilities;
import playground.mzilske.freight.TSPContract;
import playground.mzilske.freight.TSPPlan;
import playground.mzilske.freight.TSPShipment;
import playground.mzilske.freight.TSPPlanBuilder;
import playground.mzilske.freight.TSPShipment.TimeWindow;
import playground.mzilske.freight.TransportChain;
import playground.mzilske.freight.TransportChainBuilder;


public class SimpleTSPPlanBuilder implements TSPPlanBuilder {
	
	private static Logger logger = Logger.getLogger(SimpleTSPPlanBuilder.class);

	private Collection<CarrierImpl> carriers;
	
	private List<Id> transshipmentCentres;
	
	private Network network;
	
	private CarrierAgentTracker carrierAgentTracker;
	
	public void setCarrierAgentTracker(CarrierAgentTracker carrierAgentTracker) {
		this.carrierAgentTracker = carrierAgentTracker;
	}

	public void setCarriers(Collection<CarrierImpl> carriers) {
		this.carriers = carriers;
	}

	public void setTransshipmentCentres(List<Id> transshipmentCentres) {
		this.transshipmentCentres = transshipmentCentres;
	}

	/* (non-Javadoc)
	 * @see city2000w.TspPlanBuilder#buildPlan(java.util.Collection, playground.mzilske.freight.TSPCapabilities)
	 */
	@Override
	public TSPPlan buildPlan(Collection<TSPContract> contracts, TSPCapabilities tspCapabilities) {
		if(contracts.isEmpty()){
			return getEmptyPlan(tspCapabilities);
		}
		Collection<TransportChain> chains = new ArrayList<TransportChain>();
		for(TSPContract c : contracts){
			TSPShipment s = c.getShipment();
			TransportChainBuilder chainBuilder = new TransportChainBuilder(s);
			chainBuilder.schedulePickup(s.getFrom(), s.getPickUpTimeWindow());
			Id lastLocation = s.getFrom();
			TimeWindow lastPickupTW = s.getPickUpTimeWindow();
			for (Id transshipmentCentre : transshipmentCentres) {
				TimeWindow deliveryTW = new TimeWindow(lastPickupTW.getStart(),lastPickupTW.getStart() + 2*3600);
				CarrierOffer offer = getBestOffer(lastLocation,transshipmentCentre,s.getSize(),s.getPickUpTimeWindow(),deliveryTW);
				chainBuilder.scheduleLeg(offer);
				chainBuilder.scheduleDelivery(transshipmentCentre, deliveryTW);
				TimeWindow pickupTW = new TimeWindow(deliveryTW.getEnd(),24*3600);
				chainBuilder.schedulePickup(transshipmentCentre, pickupTW);
				lastPickupTW = pickupTW;
			}
			CarrierOffer offer = getBestOffer(lastLocation,s.getTo(),s.getSize(),lastPickupTW, s.getDeliveryTimeWindow());
			logger.info(offer.getId() + " get contract " + s + "; price=" + offer.getPrice());
			chainBuilder.scheduleLeg(offer);
			chainBuilder.scheduleDelivery(s.getTo(),s.getDeliveryTimeWindow());
			chains.add(chainBuilder.build());

		}
		TSPPlan plan = new TSPPlan(chains);
		return plan;
	}

	private TSPPlan getEmptyPlan(TSPCapabilities tspCapabilities) {
		TSPPlan plan = new TSPPlan(Collections.EMPTY_LIST);
		return plan;
	}

	private CarrierOffer getBestOffer(Id from, Id to, int size, TimeWindow pickupTW, TimeWindow deliveryTW) {
		Collection<CarrierOffer> offers = carrierAgentTracker.getOffers(from, to, size, pickupTW.getStart(), pickupTW.getEnd(), 
				deliveryTW.getStart(), deliveryTW.getEnd());
		CarrierOffer cheapestOffer = pickCheapest(offers);
		return cheapestOffer;
	}

	private CarrierOffer pickCheapest(Collection<CarrierOffer> offers) {
		CarrierOffer bestOffer = null;
		for(CarrierOffer o : offers){
			if(bestOffer == null){
				bestOffer = o;
			}
			else{
				if(o.getPrice() < bestOffer.getPrice()){
					bestOffer = o;
				}
			}
		}
		return bestOffer;
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
