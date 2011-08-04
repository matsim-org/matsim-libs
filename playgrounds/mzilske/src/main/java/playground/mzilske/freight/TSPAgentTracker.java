/**
 * 
 */
package playground.mzilske.freight;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.collections.Tuple;

import playground.mzilske.freight.TSPTotalCostListener.TSPCostEvent;


/**
 * @author stscr
 *
 */
public class TSPAgentTracker implements CarrierCostListener, ShipmentStatusListener {
	
	private Logger logger = Logger.getLogger(TSPAgentTracker.class);	
	
	private Collection<TransportServiceProviderImpl> transportServiceProviders;
	
	private Collection<TSPAgent> tspAgents = new ArrayList<TSPAgent>();
	
	private Collection<TSPCostListener> costListeners = new ArrayList<TSPCostListener>();
	
	private Collection<TSPTotalCostListener> totalCostListeners = new ArrayList<TSPTotalCostListener>();
	
	private TSPAgentFactory tspAgentFactory;
	
	public TSPAgentTracker(Collection<TransportServiceProviderImpl> transportServiceProviders, TSPAgentFactory tspAgentFactory) {
		this.transportServiceProviders = transportServiceProviders;
		this.tspAgentFactory = tspAgentFactory;
		createTSPAgents();
	}

	public Collection<TSPCostListener> getCostListeners() {
		return costListeners;
	}

	public List<Contract> createCarrierContracts(){
		List<Contract> carrierShipments = new ArrayList<Contract>();
		for(TSPAgent agent : tspAgents){
			List<Contract> agentShipments = agent.createCarrierShipments();
			carrierShipments.addAll(agentShipments);
		}
		return carrierShipments;
	}
	
	@Override
	public void informCost(Shipment shipment, Double cost){
		TSPAgent agent = findTSPAgentForShipment(shipment);
		TransportChainAgent chainAgent = agent.getShipmentChainMap().get(shipment); 
		chainAgent.informCost(cost);
	}
	
	private TSPAgent findTSPAgentForShipment(Shipment shipment) {
		for(TSPAgent agent : tspAgents){
			if(agent.hasShipment(shipment)){
				return agent;
			}
		}
		throw new RuntimeException("No TSPAgent found for shipment: " + shipment);
	}

	@Override
	public void shipmentPickedUp(Shipment shipment, double time) {
		TSPAgent agent = findTSPAgentForShipment(shipment);
		agent.shipmentPickedUp(shipment,time);
	}

	@Override
	public void shipmentDelivered(Shipment shipment, double time) {
		TSPAgent agent = findTSPAgentForShipment(shipment);
		agent.shipmentDelivered(shipment,time);
	}

	public void calculateCostsScoreTSPAndInform(){
		for(TSPAgent tspAgent : tspAgents){
			tspAgent.scoreSelectedPlan();
			List<Tuple<TSPShipment,Double>> shipmentCostTuple = tspAgent.calculateCostsOfSelectedPlanPerShipment();
			for(Tuple<TSPShipment,Double> t : shipmentCostTuple){
				logger.info(t.getFirst()+";cost="+t.getSecond());
				informCostListeners(t.getFirst(),t.getSecond());
			}
		}
	}
	
	private void informCostListeners(TSPShipment shipment, Double cost) {
		for(TSPCostListener cl : costListeners){
			cl.informCost(shipment, cost);
		}
		
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
	
//	public Collection<TSPOffer> requestService(Collection<ServiceRequest> shipperRequests){
//		Collection<TSPOffer> offers = new ArrayList<TSPOffer>();
//		for(TSPAgent tspAgent : tspAgents){
//			TSPOffer offer = tspAgent.requestService(shipperRequests);
//			offers.add(offer);
//		}
//		return offers;
//	}

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

	public void informTotalCost(Id id, TSPCostEvent costEvent) {
		for(TSPTotalCostListener l : totalCostListeners){
			l.inform(costEvent);
		}
		
	}

	public Collection<TSPTotalCostListener> getTotalCostListeners() {
		return totalCostListeners;
	}
	
}
