/**
 * 
 */
package playground.mzilske.freight;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.collections.Tuple;


/**
 * @author stscr
 *
 */
public class TSPAgentTracker implements CarrierCostListener{
	
	private Logger logger = Logger.getLogger(TSPAgentTracker.class);	
	
	private Collection<TransportServiceProviderImpl> transportServiceProviders;
	
	private Collection<TSPAgent> tspAgents = new ArrayList<TSPAgent>();
	
	private Collection<TSPCostListener> costListeners = new ArrayList<TSPCostListener>();
	
	public TSPAgentTracker(Collection<TransportServiceProviderImpl> transportServiceProviders) {
		this.transportServiceProviders = transportServiceProviders;
		createTSPAgents();
	}

	public Collection<TSPCostListener> getCostListeners() {
		return costListeners;
	}

	public Map<Id,List<Shipment>> createCarrierShipments(){
		Map<Id,List<Shipment>> carrierShipments = new HashMap<Id, List<Shipment>>();
		for(TSPAgent agent : tspAgents){
			List<Tuple<Id,Shipment>> agentShipments = agent.createCarrierShipments();
			for(Tuple<Id,Shipment> t : agentShipments){
				if(carrierShipments.containsKey(t.getFirst())){
					carrierShipments.get(t.getFirst()).add(t.getSecond());
				}
				else{
					List<Shipment> shipments = new ArrayList<Shipment>();
					shipments.add(t.getSecond());
					carrierShipments.put(t.getFirst(), shipments);
				}								
			}
		}
		return carrierShipments;
	}
	
	@Override
	public void informCost(Shipment shipment, Double cost){
		for(TSPAgent agent : tspAgents){
			if(agent.getShipmentChainMap().containsKey(shipment)){
				TransportChainAgent chainAgent = agent.getShipmentChainMap().get(shipment); 
				chainAgent.informCost(cost);
			}
		}
	}
	
	public void calculateCostsScoreTSPAndInform(){
		for(TSPAgent tspAgent : tspAgents){
			tspAgent.calculateCostsOfSelectedPlan();
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
			tspAgents.add(tspAgent);
		}
	}
	
	public void reset(){
		for(TSPAgent a : tspAgents){
			logger.info("reset tspAgent");
			a.reset();
		}
	}

	
	
	
}
