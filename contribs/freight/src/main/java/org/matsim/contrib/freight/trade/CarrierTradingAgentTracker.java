package org.matsim.contrib.freight.trade;

import java.util.ArrayList;
import java.util.Collection;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.freight.carrier.CarrierContract;
import org.matsim.contrib.freight.carrier.CarrierOffer;
import org.matsim.contrib.freight.carrier.NoOffer;
import org.matsim.contrib.freight.events.CarrierOfferAcceptEvent;
import org.matsim.contrib.freight.events.CarrierOfferAcceptEventHandler;
import org.matsim.contrib.freight.events.CarrierOfferRejectEvent;
import org.matsim.contrib.freight.events.CarrierOfferRejectEventHandler;
import org.matsim.contrib.freight.events.QueryCarrierOffersEvent;
import org.matsim.contrib.freight.events.QueryCarrierOffersEventHandler;
import org.matsim.contrib.freight.events.TSPCarrierContractAcceptEvent;
import org.matsim.contrib.freight.events.TSPCarrierContractAcceptEventHandler;
import org.matsim.contrib.freight.events.TSPCarrierContractCanceledEvent;
import org.matsim.contrib.freight.events.TSPCarrierContractCanceledEventHandler;

public class CarrierTradingAgentTracker implements QueryCarrierOffersEventHandler, CarrierOfferAcceptEventHandler, CarrierOfferRejectEventHandler,
TSPCarrierContractAcceptEventHandler, TSPCarrierContractCanceledEventHandler {

	private Collection<CarrierTradingAgent> carrierAgents = new ArrayList<CarrierTradingAgent>();
	
	@Override
	public void handleEvent(CarrierOfferRejectEvent event) {
		Id carrierId = event.getOffer().getId();
		CarrierTradingAgent agent = findCarrierAgent(carrierId);
		agent.informOfferRejected((CarrierOffer)event.getOffer());
	}

	@Override
	public void handleEvent(CarrierOfferAcceptEvent event) {
		Id carrierId = event.getContract().getOffer().getId();
		CarrierTradingAgent agent = findCarrierAgent(carrierId);
		agent.informOfferAccepted((CarrierContract)event.getContract());
		
	}

	@Override
	public void handleEvent(TSPCarrierContractAcceptEvent event) {
		CarrierTradingAgent agent = findCarrierAgent(event.getContract().getSeller());
		agent.informTSPContractAccepted((CarrierContract)event.getContract());
	}

	@Override
	public void handleEvent(TSPCarrierContractCanceledEvent event) {
		CarrierTradingAgent agent = findCarrierAgent(event.getContract().getSeller());
		agent.informTSPContractCanceled((CarrierContract)event.getContract());
	}
	
	@Override
	public void handleEvent(QueryCarrierOffersEvent event) {
		for (CarrierTradingAgent carrierAgent : carrierAgents) {
			CarrierOffer offer = carrierAgent.requestOffer(event.getService().getFrom(), event.getService().getTo(), event.getService().getSize(), 
					event.getService().getStartPickup(), event.getService().getEndPickup(), event.getService().getStartDelivery(), event.getService().getEndDelivery());
			if(offer instanceof NoOffer){
				continue;
			}
			else {
				event.getOffers().add(offer);
			}
		}
	}

	@Override
	public void reset(int iteration) {
		resetCarrierAgents();
	}

	private void resetCarrierAgents() {
		for(CarrierTradingAgent cA : carrierAgents){
			cA.reset();	
		}
	}
	
	private CarrierTradingAgent findCarrierAgent(Id id) {
		for(CarrierTradingAgent agent : carrierAgents){
			if(agent.getId().equals(id)){
				return agent;
			}
		}
		return null;
	}
	
}
