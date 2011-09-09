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
import playground.mzilske.freight.events.ShipmentDeliveredEvent;
import playground.mzilske.freight.events.ShipmentDeliveredEventHandler;
import playground.mzilske.freight.events.ShipmentPickedUpEvent;
import playground.mzilske.freight.events.ShipmentPickedUpEventHandler;


/**
 * @author stscr
 *
 */
public class TSPAgentTracker implements ShipmentPickedUpEventHandler, ShipmentDeliveredEventHandler {
	
	private Logger logger = Logger.getLogger(TSPAgentTracker.class);	
	
	private Collection<TransportServiceProviderImpl> transportServiceProviders;
	
	private Collection<TSPAgent> tspAgents = new ArrayList<TSPAgent>();
	
	private EventsManager eventsManager;
	
	private TSPAgentFactory tspAgentFactory;
	
	public TSPAgentTracker(Collection<TransportServiceProviderImpl> transportServiceProviders, TSPAgentFactory tspAgentFactory) {
		this.transportServiceProviders = transportServiceProviders;
		this.tspAgentFactory = tspAgentFactory;
		createTSPAgents();
		eventsManager = EventsUtils.createEventsManager();
	}

	public List<Contract> createCarrierContracts(){
		List<Contract> carrierShipments = new ArrayList<Contract>();
		for(TSPAgent agent : tspAgents){
			List<Contract> agentShipments = agent.createCarrierShipments();
			carrierShipments.addAll(agentShipments);
		}
		return carrierShipments;
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
		for(TransportServiceProviderImpl tsp : transportServiceProviders){
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
	
	public Collection<TSPOffer> requestService(Id from, Id to, int size, double startPickup, double endPickup, double startDelivery, double endDelivery){
		Collection<TSPOffer> offers = new ArrayList<TSPOffer>();
		for(TSPAgent tspAgent : tspAgents){
			TSPOffer offer = tspAgent.requestService(from,to,size,startPickup,endPickup,startDelivery,endDelivery);
			offers.add(offer);
		}
		return offers;
	}
	
	public void offerGranted(TSPContract contract){
		/*
		 * add contract to tspContracts
		 */
		Id tspId = contract.getOffer().getId();
		TransportServiceProviderImpl tsp = findTsp(tspId);
		if(tsp == null){
			throw new IllegalStateException("tsp " + tspId + " does not exist");
		}
		tsp.getContracts().add(contract);
		TSPAgent tspAgent = findAgentForTSP(tspId);
		
	}
	
	public void offerRejected(TSPOffer offer){
		/*
		 * remove open offers and according transportChain
		 */
	}
	
	public void contractAnnulled(TSPContract contract){
		/*
		 * remove offer from tspAgent
		 */
	}

	private TSPAgent findAgentForTSP(Id tspId) {
		for(TSPAgent a : tspAgents){
			if(a.getId().equals(tspId)){
				return a;
			}
		}
		return null;
	}

	public Collection<Contract> registerChainAndGetAffectedCarrierContract(Id tspId, TransportChain chain) {
		TSPAgent agent = findAgentForTSP(tspId);
		Collection<Contract> carrierContracts = agent.registerChainAndGetCarrierContracts(chain);
		return carrierContracts;
	}

	public Collection<Contract> removeChainAndGetAffectedCarrierContract(Id tspId, TransportChain chain) {
		TSPAgent agent = findAgentForTSP(tspId);
		Collection<Contract> carrierContracts = agent.removeChainAndGetAffectedCarrierContracts(chain);
		return carrierContracts;
	}

	public void removeContracts(Collection<TSPContract> contracts) {
		for(TSPContract c : contracts){
			TransportServiceProviderImpl tsp = findTsp(c.getOffer().getId());
			if(tsp != null){
				tsp.getContracts().remove(c);
				logger.info("remove tspContract: " + c.getShipment());
			}
			else{
				logger.warn("contract " + c + " could not be removed. No tsp found.");
			}
		}
	}

	private TransportServiceProviderImpl findTsp(Id tspId) {
		for(TransportServiceProviderImpl tsp : transportServiceProviders){
			if(tsp.getId().equals(tspId)){
				return tsp;
			}
		}
		return null;
	}

	public void addContracts(Collection<TSPContract> contracts) {
		for(TSPContract contract : contracts){
			TransportServiceProviderImpl tsp = findTsp(contract.getOffer().getId());
			if(tsp != null){
				tsp.getContracts().add(contract);
				logger.info("add tspContract: " + contract.getShipment());
			}
			else{
				logger.warn("contract " + contract + " could not be added. No tsp found.");
			}
		}
	}

	public TransportServiceProviderImpl getTsp(Id tspId) {
		return findTsp(tspId);
	}

	public List<CarrierOffer> getCarrierOffers(TSPOffer offer) {
		TSPAgent tspAgent = findAgentForTSP(offer.getId());
		if(tspAgent != null){
			return tspAgent.getCarrierOffer(offer);
		}
		else{
			throw new IllegalStateException("no tspAgent found for id "+ offer.getId());
		}
		
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
	
}
