/**
 * 
 */
package playground.mzilske.freight;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;

import playground.mzilske.freight.api.TSPAgentFactory;
import playground.mzilske.freight.carrier.CarrierContract;
import playground.mzilske.freight.carrier.Shipment;
import playground.mzilske.freight.events.QueryTSPOffersEvent;
import playground.mzilske.freight.events.QueryTSPOffersEventHandler;
import playground.mzilske.freight.events.ShipmentDeliveredEvent;
import playground.mzilske.freight.events.ShipmentDeliveredEventHandler;
import playground.mzilske.freight.events.ShipmentPickedUpEvent;
import playground.mzilske.freight.events.ShipmentPickedUpEventHandler;
import playground.mzilske.freight.events.ShipperTSPContractAcceptEvent;
import playground.mzilske.freight.events.ShipperTSPContractAcceptEventHandler;
import playground.mzilske.freight.events.ShipperTSPContractCanceledEvent;
import playground.mzilske.freight.events.ShipperTSPContractCanceledEventHandler;
import playground.mzilske.freight.events.TSPCarrierContractAcceptEvent;
import playground.mzilske.freight.events.TSPCarrierContractAcceptEventHandler;
import playground.mzilske.freight.events.TSPCarrierContractCanceledEvent;
import playground.mzilske.freight.events.TSPCarrierContractCanceledEventHandler;
import playground.mzilske.freight.events.TransportChainAddedEvent;
import playground.mzilske.freight.events.TransportChainAddedEventHandler;
import playground.mzilske.freight.events.TransportChainRemovedEvent;
import playground.mzilske.freight.events.TransportChainRemovedEventHandler;


/**
 * @author stscr
 *
 */
public class TSPAgentTracker implements ShipmentPickedUpEventHandler, ShipmentDeliveredEventHandler, ShipperTSPContractAcceptEventHandler, ShipperTSPContractCanceledEventHandler,
	TSPCarrierContractAcceptEventHandler, TSPCarrierContractCanceledEventHandler, TransportChainAddedEventHandler, TransportChainRemovedEventHandler, QueryTSPOffersEventHandler {
	
	private Logger logger = Logger.getLogger(TSPAgentTracker.class);	
	
	private Collection<TransportServiceProvider> transportServiceProviders;
	
	private Collection<TSPAgent> tspAgents = new ArrayList<TSPAgent>();
	
	private EventsManager eventsManager;
	
	private TSPAgentFactory tspAgentFactory;
	
	public TSPAgentTracker(Collection<TransportServiceProvider> transportServiceProviders, TSPAgentFactory tspAgentFactory) {
		this.transportServiceProviders = transportServiceProviders;
		this.tspAgentFactory = tspAgentFactory;
		createTSPAgents();
		eventsManager = EventsUtils.createEventsManager();
	}

	public List<CarrierContract> createCarrierContracts(){
		List<CarrierContract> carrierContracts = new ArrayList<CarrierContract>();
		for(TSPAgent agent : tspAgents){
			List<CarrierContract> agentShipments = agent.getCarrierContracts();
			carrierContracts.addAll(agentShipments);
		}
		return carrierContracts;
	}
	
	private TSPAgent findTSPAgentForShipment(Shipment shipment) {
		for(TSPAgent agent : tspAgents){
			if(agent.hasShipment(shipment)){
				return agent;
			}
		}
		throw new RuntimeException("No TSPAgent found for shipment: " + shipment);
	}

	private void createTSPAgents() {
		for(TransportServiceProvider tsp : transportServiceProviders){
			TSPAgent tspAgent = tspAgentFactory.createTspAgent(this, tsp);
			tspAgents.add(tspAgent);
		}
	}
	
	public void reset(){
		for(TSPAgent a : tspAgents){
			logger.info("reset tspAgent");
			a.reset();
		}
	}
	
	private TSPAgent findAgent(Id tspId) {
		for(TSPAgent a : tspAgents){
			if(a.getId().equals(tspId)){
				return a;
			}
		}
		return null;
	}

	private TransportServiceProvider findTsp(Id tspId) {
		for(TransportServiceProvider tsp : transportServiceProviders){
			if(tsp.getId().equals(tspId)){
				return tsp;
			}
		}
		return null;
	}

	public TransportServiceProvider getTsp(Id tspId) {
		return findTsp(tspId);
	}
	
	public void calculateCosts(){
		for(TSPAgent a : tspAgents){
			a.calculateCosts();
		}
	}

	public EventsManager getEventsManager() {
		return eventsManager;
	}
	
	public void processEvent(Event event){
		eventsManager.processEvent(event);
	}

	@Override
	public void finish() {
		
		
	}

	@Override
	public void reset(int iteration) {
		
		
	}

	@Override
	public void handleEvent(ShipmentDeliveredEvent event) {
		TSPAgent agent = findTSPAgentForShipment(event.getShipment());
		agent.shipmentDelivered(event.getShipment(),event.getTime());
	}

	@Override
	public void handleEvent(ShipmentPickedUpEvent event) {
		TSPAgent agent = findTSPAgentForShipment(event.getShipment());
		agent.shipmentPickedUp(event.getShipment(),event.getTime());
	}

	@Override
	public void handleEvent(ShipperTSPContractAcceptEvent event) {
		TSPAgent agent = findAgent(event.getContract().getOffer().getId());
		agent.informShipperContractAccepted(event.getContract());
	}

	@Override
	public void handleEvent(ShipperTSPContractCanceledEvent event) {
		TSPAgent agent = findAgent(event.getContract().getOffer().getId());
		agent.informShipperContractCanceled(event.getContract());
		
	}

	@Override
	public void handleEvent(TSPCarrierContractAcceptEvent event) {
		TSPAgent agent = findAgent(event.getContract().getBuyer());
		agent.informCarrierContractAccepted(event.getContract());
	}

	@Override
	public void handleEvent(TSPCarrierContractCanceledEvent event) {
		TSPAgent agent = findAgent(event.getContract().getBuyer());
		agent.informCarrierContractCanceled(event.getContract());
	}

	@Override
	public void handleEvent(TransportChainRemovedEvent event) {
		TSPAgent agent = findAgent(event.getTspId());
		agent.informChainRemoved(event.getChain());
		
	}

	@Override
	public void handleEvent(TransportChainAddedEvent event) {
		TSPAgent agent = findAgent(event.getTspId());
		agent.informChainAdded(event.getChain());
	}

	@Override
	public void handleEvent(QueryTSPOffersEvent event) {
		for(TSPAgent a : tspAgents){
			TSPOffer offer = a.requestService(event.getService().getFrom(), event.getService().getTo(), event.getService().getSize(), 
					event.getService().getStartPickup(), event.getService().getEndDelivery(), event.getService().getStartDelivery(), event.getService().getEndDelivery());
			event.getOffers().add(offer);
		}
	}
	
}
