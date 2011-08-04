package city2000w;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.gbl.MatsimRandom;

import playground.mzilske.freight.CarrierAgentTracker;
import playground.mzilske.freight.CarrierOffer;
import playground.mzilske.freight.TSPAgentTracker;
import playground.mzilske.freight.TSPCapabilities;
import playground.mzilske.freight.TSPContract;
import playground.mzilske.freight.TSPPlan;
import playground.mzilske.freight.TSPShipment;
import playground.mzilske.freight.TSPShipment.TimeWindow;
import playground.mzilske.freight.TransportChain;
import playground.mzilske.freight.TransportChain.ChainTriple;
import playground.mzilske.freight.TransportChainBuilder;
import playground.mzilske.freight.TSPPlanBuilder;


public class RandomTSPPlanBuilder implements TSPPlanBuilder {
	
	private static Logger logger = Logger.getLogger(RandomTSPPlanBuilder.class);

	public static double TRANSHIPMENT_TIMESPAN = 4*3600;
	
	private CarrierAgentTracker carrierAgentTracker;
	
	private TSPAgentTracker tspAgentTracker;
	
	private OfferSelectorImpl<CarrierOffer> offerSelector;
	
	public void setOfferSelector(OfferSelectorImpl<CarrierOffer> offerSelector) {
		this.offerSelector = offerSelector;
	}

	public void setCarrierAgentTracker(CarrierAgentTracker carrierAgentTracker) {
		this.carrierAgentTracker = carrierAgentTracker;
	}


	public void setTspAgentTracker(TSPAgentTracker tspAgentTracker) {
		this.tspAgentTracker = tspAgentTracker;
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
			assertPickupTimes(s.getFrom(),s.getPickUpTimeWindow().getStart(), 0.0, 86400.0);
			TransportChainBuilder chainBuilder = new TransportChainBuilder(s);
			TimeWindow tspShipmentPickupTW = s.getPickUpTimeWindow();
			TimeWindow firstPickupTW = getTW(tspShipmentPickupTW.getStart(),tspShipmentPickupTW.getStart());
			TimeWindow lastPickupTW = firstPickupTW;
			Id lastPickupLocation = s.getFrom();
			chainBuilder.schedulePickup(s.getFrom(), firstPickupTW);
			int legIndex = 0;
			Id transshipmentCentre = null;
			if(!tspCapabilities.getTransshipmentCentres().isEmpty()){
				transshipmentCentre = tspCapabilities.getTransshipmentCentres().iterator().next();
			}
			if(transshipmentCentre != null){
				TimeWindow deliveryTWOfFirstLeg = getTW(firstPickupTW.getStart(),firstPickupTW.getStart() + TRANSHIPMENT_TIMESPAN);
				CarrierOffer offer = getBestOffer(c,lastPickupLocation,transshipmentCentre,s.getSize(),firstPickupTW,deliveryTWOfFirstLeg,legIndex);
				chainBuilder.scheduleLeg(offer);
				chainBuilder.scheduleDelivery(transshipmentCentre, deliveryTWOfFirstLeg);
				TimeWindow pickupTWOfSecondLeg = new TimeWindow(deliveryTWOfFirstLeg.getEnd(),deliveryTWOfFirstLeg.getEnd());
				chainBuilder.schedulePickup(transshipmentCentre, pickupTWOfSecondLeg);
				lastPickupTW = pickupTWOfSecondLeg;
				lastPickupLocation = transshipmentCentre;
				legIndex++;
			}
			TimeWindow lastDeliveryTW = getTW(lastPickupTW.getStart(),firstPickupTW.getStart() + 24*3600);
			CarrierOffer offer = getBestOffer(c,lastPickupLocation,s.getTo(),s.getSize(),lastPickupTW, lastDeliveryTW,legIndex);
			logger.info(offer.getId() + " get contract " + s + "; price=" + offer.getPrice());
			chainBuilder.scheduleLeg(offer);
			chainBuilder.scheduleDelivery(s.getTo(),lastDeliveryTW);
			TransportChain transportChain = chainBuilder.build();
			chains.add(transportChain);
			assertPickupTimes(transportChain, 0.0, 86400.0);

		}
		TSPPlan plan = new TSPPlan(chains);
		return plan;
	}

	
	private void assertPickupTimes(TransportChain transportChain, double d,double e) {
		for(ChainTriple chainTriple : transportChain.getChainTriples()){
			double start = chainTriple.getFirstActivity().getTimeWindow().getStart();
			Id fromLocation = chainTriple.getFirstActivity().getLocation();
			assertPickupTimes(fromLocation, start, d, e);
			
		}
		
	}

	private void assertPickupTimes(Id from, Double startPickup, double d, double e) {
		if(from.toString().equals("industry")){
			if(startPickup == d || startPickup == e){
				return;
			}
			else{
				throw new IllegalStateException("this should not be");
			}
		}

	}



	private TimeWindow getTW(double start, double end) {
		return new TimeWindow(start,end);
	}

	private TSPPlan getEmptyPlan(TSPCapabilities tspCapabilities) {
		TSPPlan plan = new TSPPlan(Collections.EMPTY_LIST);
		return plan;
	}

	private CarrierOffer getBestOffer(TSPContract c, Id from, Id to, int size, TimeWindow pickupTW, TimeWindow deliveryTW,int legIndex) {
		List<CarrierOffer> offers = tspAgentTracker.getCarrierOffers(c.getOffer());
		if(offers != null){
			return offers.get(legIndex);
		}
		else{
			logger.info("STRANGES! no offer found! => get offers from carriers");
			Collection<CarrierOffer> carrierOffers = carrierAgentTracker.getOffers(from, to, size, pickupTW.getStart(), pickupTW.getEnd(), 
					deliveryTW.getStart(), deliveryTW.getEnd()); 
			CarrierOffer cheapestOffer = offerSelector.selectOffer(carrierOffers);
			if(cheapestOffer == null){
				if(!carrierOffers.isEmpty()){
					cheapestOffer = pickRandom(carrierOffers);
				}
				else{
					throw new IllegalStateException("carrierOffers empty");
				}
			}
			return cheapestOffer;
		}
	}

	private CarrierOffer pickRandom(Collection<CarrierOffer> carrierOffers) {
		List<CarrierOffer> offers = new ArrayList<CarrierOffer>(carrierOffers);
		Collections.shuffle(offers,MatsimRandom.getRandom());
		return offers.get(0);
	}

	public RandomTSPPlanBuilder(Network network) {
		super();
	}
	
	

}
