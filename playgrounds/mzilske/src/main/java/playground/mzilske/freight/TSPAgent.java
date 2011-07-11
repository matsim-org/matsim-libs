/**
 * 
 */
package playground.mzilske.freight;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
public class TSPAgent {
	
	private Logger logger = Logger.getLogger(TSPAgent.class);
	
	public static class CostParameter{
		public static double transshipmentHandlingCost_per_unit = 0.0;
		public static double transshipmentCost = 25;
	}
	
	private TransportServiceProviderImpl tsp;
	
	private Collection<TransportChainAgent> transportChainAgents = new ArrayList<TransportChainAgent>();

	private Map<Shipment,TransportChainAgent> shipmentChainMap = new HashMap<Shipment, TransportChainAgent>();
	
	private TSPOfferMaker offerMaker;

	public TSPAgent(TransportServiceProviderImpl tsp){
		this.tsp = tsp;
	}
	
	Map<Shipment, TransportChainAgent> getShipmentChainMap() {
		return shipmentChainMap;
	}

	List<Contract> createCarrierShipments(){
		clear();
		List<Contract> shipments = new ArrayList<Contract>();
		for(TransportChain chain : tsp.getSelectedPlan().getChains()){
			TransportChainAgent chainAgent = new TransportChainAgent(chain);
			transportChainAgents.add(chainAgent);
			List<Contract> chainShipments = chainAgent.createCarrierShipments();
			for(Contract t : chainShipments){
				shipments.add(t);
				shipmentChainMap.put(t.getShipment(), chainAgent);				
			}
		}
		return shipments;
	}
	
	public void setOfferMaker(TSPOfferMaker offerMaker) {
		offerMaker.setTSP(tsp);
		this.offerMaker = offerMaker;
	}

	private void clear() {
		transportChainAgents.clear();
		shipmentChainMap.clear();
	}
	
	public void reset(){
		for(TransportChainAgent tca : transportChainAgents){
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
		double sumOfFees = calculateFeesAndTransshipmentCosts();
		double opportunityCosts = calculateOpportunityCosts();
		double otherCosts = calculateOtherCosts();
		tsp.getSelectedPlan().setScore((sumOfFees + opportunityCosts + otherCosts) * (-1));
	}
	
	private double calculateOtherCosts() {
		double cost = 0.0;
		return 0;
	}

	private double calculateOpportunityCosts() {
		return 0;
	}

	private double calculateFeesAndTransshipmentCosts() {
		double cost = 0.0;
		for(TransportChainAgent tca : transportChainAgents){
			cost += tca.getFees() + tca.getNumberOfStopps()*CostParameter.transshipmentCost;
		}
		return cost;
	}

	public TSPOffer requestService(Id from, Id to, int size) {
		return offerMaker.getOffer(from,to,size,getMemorizedPrice(from,to,size));
	}

	private double getMemorizedPrice(Id from, Id to, int size) {
		return 0.0;
	}
	
	public Collection<Contract> registerChainAndGetCarrierContracts(TransportChain chain){
		Collection<Contract> carrierContracts = new ArrayList<Contract>();
		TransportChainAgent chainAgent = new TransportChainAgent(chain);
		transportChainAgents.add(chainAgent);
		List<Contract> chainShipments = chainAgent.createCarrierShipments();
		for(Contract c : chainShipments){
			carrierContracts.add(c);
			shipmentChainMap.put(c.getShipment(), chainAgent);				
		}
		return carrierContracts;
	}
	
	public void removeChain(TransportChain chain){
		TransportChainAgent chainAgent = findChainAgent(chain);
		if(chainAgent != null){
			transportChainAgents.remove(chainAgent);
			for(Shipment s : chainAgent.getShipments()){
				shipmentChainMap.remove(s);
			}
		}
	}

	private TransportChainAgent findChainAgent(TransportChain chain) {
		for(TransportChainAgent a : transportChainAgents){
			if(a.getTpChain() == chain){
				return a;
			}
		}
		return null;
	}
}
