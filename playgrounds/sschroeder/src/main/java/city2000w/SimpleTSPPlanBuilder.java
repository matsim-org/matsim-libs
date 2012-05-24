/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package city2000w;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.CarrierContract;
import org.matsim.contrib.freight.carrier.CarrierOffer;
import org.matsim.contrib.freight.carrier.CarrierShipment;
import org.matsim.contrib.freight.carrier.CarrierUtils;
import org.matsim.contrib.freight.carrier.Offer;
import org.matsim.contrib.freight.carrier.TimeWindow;
import org.matsim.contrib.freight.events.QueryCarrierOffersEvent;
import org.matsim.contrib.freight.mobsim.CarrierAgentTracker;
import org.matsim.contrib.freight.trade.Service;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.gbl.MatsimRandom;

import playground.mzilske.freight.TSPAgentTracker;
import playground.mzilske.freight.TSPCapabilities;
import playground.mzilske.freight.TSPContract;
import playground.mzilske.freight.TSPPlan;
import playground.mzilske.freight.TSPShipment;
import playground.mzilske.freight.TransportChain;
import playground.mzilske.freight.TransportChain.ChainTriple;
import playground.mzilske.freight.TransportChainBuilder;
import playground.mzilske.freight.events.OfferUtils;
import freight.offermaker.OfferSelectorImpl;

public class SimpleTSPPlanBuilder {
	
	private static Logger logger = Logger.getLogger(SimpleTSPPlanBuilder.class);

	public static double TRANSHIPMENT_TIMESPAN = 4*3600;
	
	private CarrierAgentTracker carrierAgentTracker;
	
	private TSPAgentTracker tspAgentTracker;
	
	private OfferSelectorImpl<CarrierOffer> offerSelector;
	
	private EventsManager eventsManager;
	
	public SimpleTSPPlanBuilder(Network network, EventsManager eventsManager) {
		this.eventsManager = eventsManager;
	}

	public void setOfferSelector(OfferSelectorImpl<CarrierOffer> offerSelector) {
		this.offerSelector = offerSelector;
	}

	public void setCarrierAgentTracker(CarrierAgentTracker carrierAgentTracker) {
		this.carrierAgentTracker = carrierAgentTracker;
	}


	public void setTspAgentTracker(TSPAgentTracker tspAgentTracker) {
		this.tspAgentTracker = tspAgentTracker;
	}

	public TSPPlan buildPlan(Id tspId, Collection<TSPContract> contracts, TSPCapabilities tspCapabilities) {
		if(contracts.isEmpty()){
			return getEmptyPlan(tspCapabilities);
		}
		Collection<TransportChain> chains = new ArrayList<TransportChain>();
		for(TSPContract c : contracts){
			TSPShipment s = c.getShipment();
			assertPickupTimes(s.getFrom(),s.getPickupTimeWindow().getStart(), 0.0, 86400.0);
			TransportChainBuilder chainBuilder = new TransportChainBuilder(s);
			TimeWindow tspShipmentPickupTW = s.getPickupTimeWindow();
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
				CarrierShipment shipment = CarrierUtils.createShipment(lastPickupLocation, transshipmentCentre, s.getSize(), firstPickupTW.getStart(), firstPickupTW.getEnd(), deliveryTWOfFirstLeg.getStart(), deliveryTWOfFirstLeg.getEnd());
				CarrierContract contract = new CarrierContract(tspId,offer.getId(),shipment,offer);
				chainBuilder.scheduleLeg(contract);
//				eventsManager.processEvent(new )
				chainBuilder.scheduleDelivery(transshipmentCentre, deliveryTWOfFirstLeg);
				TimeWindow pickupTWOfSecondLeg = new TimeWindow(deliveryTWOfFirstLeg.getEnd(),deliveryTWOfFirstLeg.getEnd());
				chainBuilder.schedulePickup(transshipmentCentre, pickupTWOfSecondLeg);
				lastPickupTW = pickupTWOfSecondLeg;
				lastPickupLocation = transshipmentCentre;
				legIndex++;
			}
			TimeWindow lastDeliveryTW = getTW(lastPickupTW.getStart(),firstPickupTW.getStart() + 24*3600);
			CarrierOffer offer = getBestOffer(c,lastPickupLocation,s.getTo(),s.getSize(),lastPickupTW, lastDeliveryTW,legIndex);
			CarrierShipment shipment = CarrierUtils.createShipment(lastPickupLocation, transshipmentCentre, s.getSize(), lastPickupTW.getStart(), lastPickupTW.getEnd(), lastDeliveryTW.getStart(), lastDeliveryTW.getEnd());
			CarrierContract contract = new CarrierContract(tspId,offer.getId(),shipment,offer);
			chainBuilder.scheduleLeg(contract);
//			chainBuilder.scheduleLeg(offer);
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
		Collection<Offer> carrierOffers = new ArrayList<Offer>(); 
		Service service = OfferUtils.createService(from, to, size, pickupTW, deliveryTW);
		eventsManager.processEvent(new QueryCarrierOffersEvent(carrierOffers, service));
		CarrierOffer cheapestOffer = pickOffer(carrierOffers); 
		return cheapestOffer;	
	}

	private CarrierOffer pickOffer(Collection<Offer> carrierOffers) {
		Collection<CarrierOffer> offers = new ArrayList<CarrierOffer>();
		for(Offer o : carrierOffers){
			offers.add((CarrierOffer)o);
		}
		CarrierOffer cheapestOffer = offerSelector.selectOffer(offers);
		if(cheapestOffer == null){
			if(!carrierOffers.isEmpty()){
				cheapestOffer = pickRandom(offers);
			}
			else{
				throw new IllegalStateException("carrierOffers empty");
			}
		}
		return cheapestOffer;
	}

	private CarrierOffer pickRandom(Collection<CarrierOffer> carrierOffers) {
		List<CarrierOffer> offers = new ArrayList<CarrierOffer>(carrierOffers);
		Collections.shuffle(offers,MatsimRandom.getRandom());
		return offers.get(0);
	}
	
	


}
