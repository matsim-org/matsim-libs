/**
 * 
 */
package playground.mzilske.freight;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.core.utils.collections.Tuple;


/**
 * @author stscr
 *
 */
public class TSPAgentTracker implements CarrierCostListener, ShipmentStatusListener {
	
	private Logger logger = Logger.getLogger(TSPAgentTracker.class);	
	
	private Collection<TransportServiceProviderImpl> transportServiceProviders;
	
	private Collection<TSPAgent> tspAgents = new ArrayList<TSPAgent>();
	
	private Collection<TSPCostListener> costListeners = new ArrayList<TSPCostListener>();
	
	private TSPOfferMaker offerMaker;
	
	public void setOfferMaker(TSPOfferMaker offerMaker) {
		for(TSPAgent a : tspAgents){
			a.setOfferMaker(offerMaker);
		}
	}

	public TSPAgentTracker(Collection<TransportServiceProviderImpl> transportServiceProviders) {
		this.transportServiceProviders = transportServiceProviders;
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
			if(agent.getShipmentChainMap().containsKey(shipment)){
				return agent;
			}
		}
		throw new RuntimeException("No TSPAgent found for a shipment.");
	}

	@Override
	public void shipmentPickedUp(Shipment shipment, double time) {
		TSPAgent agent = findTSPAgentForShipment(shipment);
		TransportChainAgent chainAgent = agent.getShipmentChainMap().get(shipment);
		chainAgent.informPickup(shipment, time);
	}

	@Override
	public void shipmentDelivered(Shipment shipment, double time) {
		TSPAgent agent = findTSPAgentForShipment(shipment);
		TransportChainAgent chainAgent = agent.getShipmentChainMap().get(shipment);
		chainAgent.informDelivery(shipment, time);
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
			TSPAgent tspAgent = new TSPAgent(tsp);
//			tspAgent.setOfferMaker(new Carrie);
			tspAgents.add(tspAgent);
		}
	}
	
	public void reset(){
		for(TSPAgent a : tspAgents){
			logger.info("reset tspAgent");
			a.reset();
		}
	}
	
	public Collection<TSPOffer> requestServices(Collection<TSPShipment> shipments){
		Collection<TSPOffer> offers = new ArrayList<TSPOffer>();
		for(TSPAgent tspAgent : tspAgents){
			TSPOffer offer = tspAgent.requestService(shipments);
			offers.add(offer);
		}
		return offers;
	}
	
}
