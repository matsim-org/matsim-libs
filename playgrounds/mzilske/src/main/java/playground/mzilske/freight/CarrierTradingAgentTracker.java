package playground.mzilske.freight;

import java.util.ArrayList;
import java.util.Collection;

import org.matsim.api.core.v01.Id;

import playground.mzilske.freight.carrier.CarrierContract;
import playground.mzilske.freight.carrier.CarrierOffer;
import playground.mzilske.freight.carrier.NoOffer;
import playground.mzilske.freight.events.CarrierOfferAcceptEvent;
import playground.mzilske.freight.events.CarrierOfferAcceptEventHandler;
import playground.mzilske.freight.events.CarrierOfferRejectEvent;
import playground.mzilske.freight.events.CarrierOfferRejectEventHandler;
import playground.mzilske.freight.events.QueryCarrierOffersEvent;
import playground.mzilske.freight.events.QueryCarrierOffersEventHandler;
import playground.mzilske.freight.events.TSPCarrierContractAcceptEvent;
import playground.mzilske.freight.events.TSPCarrierContractAcceptEventHandler;
import playground.mzilske.freight.events.TSPCarrierContractCanceledEvent;
import playground.mzilske.freight.events.TSPCarrierContractCanceledEventHandler;

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
