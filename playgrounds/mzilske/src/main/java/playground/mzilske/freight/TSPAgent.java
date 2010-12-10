/**
 * 
 */
package playground.mzilske.freight;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.collections.Tuple;


/**
 * @author stscr
 *
 */
public class TSPAgent {
	
	private Logger logger = Logger.getLogger(TSPAgent.class);
	
	public static class CostParameter{
		public static double transshipmentHandlingCost_per_unit = 0.0;
	}
	
	private TransportServiceProviderImpl tsp;
	
	public TSPAgent(TransportServiceProviderImpl tsp){
		this.tsp = tsp;
	}
	
	private Collection<TransportChainAgent> transportChainAgents = new ArrayList<TransportChainAgent>();
	
	private Map<Shipment,TransportChainAgent> shipmentChainMap = 
		new HashMap<Shipment, TransportChainAgent>();

	Map<Shipment, TransportChainAgent> getShipmentChainMap() {
		return shipmentChainMap;
	}

	List<Tuple<Id,Shipment>> createCarrierShipments(){
		clear();
		List<Tuple<Id,Shipment>> shipments = new ArrayList<Tuple<Id,Shipment>>();
		for(TransportChain chain : tsp.getSelectedPlan().getChains()){
			TransportChainAgent chainAgent = new TransportChainAgent(chain);
			transportChainAgents.add(chainAgent);
			List<Tuple<Id,Shipment>> chainShipments = chainAgent.createCarrierShipments();
			for(Tuple<Id,Shipment> t : chainShipments){
				shipments.add(t);
				shipmentChainMap.put(t.getSecond(), chainAgent);				
			}
		}
		return shipments;
	}
	
	private void clear() {
		transportChainAgents.clear();
		shipmentChainMap.clear();
	}
	
	public void reset(){
		for(TransportChainAgent tca : transportChainAgents){
			logger.info("reset tca");
			tca.reset();
		}
	}

	Collection<TransportChainAgent> getTransportChainAgents(){
		return Collections.unmodifiableCollection(transportChainAgents);
	}

	List<Tuple<TSPShipment,Double>> calculateCostsOfSelectedPlanPerShipment(){
		List<Tuple<TSPShipment,Double>> costsPerShipment = new ArrayList<Tuple<TSPShipment,Double>>();
		for(TransportChainAgent tca : transportChainAgents){
			double cost = tca.getCost() + umschlagskosten(tca.getNumberOfStopps()) + strafkosten(tca.hasSucceeded()); 
			Tuple<TSPShipment,Double> shipmentCostTuple = new Tuple<TSPShipment,Double>(tca.getTpChain().getShipment(),cost);
			costsPerShipment.add(shipmentCostTuple);
		}
		return costsPerShipment;
	}
	
	private double strafkosten(boolean hasSucceeded) {
		if (hasSucceeded) {
			return 0.0;
		} else {
			return 100000.0;
		}
	}

	private double umschlagskosten(int numberOfStopps) {
		return 0;
	}

	public void scoreSelectedPlan() {
		double cost = calculateCost();
		tsp.getSelectedPlan().setScore(cost * (-1));
	}
	
	private double calculateCost() {
		double cost = 0.0;
		for(Tuple<TSPShipment,Double> t : calculateCostsOfSelectedPlanPerShipment()) {
			cost += t.getSecond();
		}
		return cost;
	}
	
}
