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

import playground.mzilske.freight.TSPTotalCostListener.TSPCostEvent;


/**
 * @author stscr
 *
 */
public class TSPAgent {
	
	private Logger logger = Logger.getLogger(TSPAgent.class);
	
	public static class CostParameter{
		public static double transshipmentHandlingCostPerUnit = 2.0;
		public static double transshipmentCost = 5;
	}
	
	private TransportServiceProviderImpl tsp;
	
	private Id id;
	
	private Collection<TransportChainAgent> transportChainAgents = new ArrayList<TransportChainAgent>();

	private Map<Shipment,TransportChainAgent> shipmentChainMap = new HashMap<Shipment, TransportChainAgent>();
	
	private Map<TSPOffer,List<CarrierOffer>> tspCarrierOfferMap = new HashMap<TSPOffer, List<CarrierOffer>>();
	
	private TSPOfferMaker offerMaker;
	
	private CostMemory costMemory;
	
	public void setCostMemory(CostMemory costMemory) {
		this.costMemory = costMemory;
	}

	private TSPAgentTracker tspAgentTracker;

	public void setTspAgentTracker(TSPAgentTracker tspAgentTracker) {
		this.tspAgentTracker = tspAgentTracker;
	}

	public TSPAgent(TransportServiceProviderImpl tsp){
		this.tsp = tsp;
		this.id = tsp.getId();
	}
	
	Id getId(){
		return this.id;
	}
	
	Map<Shipment, TransportChainAgent> getShipmentChainMap() {
		return shipmentChainMap;
	}

	List<Contract> createCarrierShipments(){
		clear();
		if(tsp.getSelectedPlan() == null){
			return Collections.EMPTY_LIST;
		}
		if(tsp.getSelectedPlan().getChains() == null){
			return Collections.EMPTY_LIST;
		}
		List<Contract> shipments = new ArrayList<Contract>();
		for(TransportChain chain : tsp.getSelectedPlan().getChains()){
			TransportChainAgent chainAgent = new TransportChainAgent(chain);
			transportChainAgents.add(chainAgent);
			List<Contract> chainShipments = chainAgent.createCarrierContracts();
			for(Contract t : chainShipments){
				shipments.add(t);
				shipmentChainMap.put(t.getShipment(), chainAgent);				
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
			tca.reset();
		}
		tspCarrierOfferMap.clear();
	}

	Collection<TransportChainAgent> getTransportChainAgents(){
		return Collections.unmodifiableCollection(transportChainAgents);
	}

	List<Tuple<TSPShipment,Double>> calculateCostsOfSelectedPlanPerShipment(){
		List<Tuple<TSPShipment,Double>> costsPerShipment = new ArrayList<Tuple<TSPShipment,Double>>();
		for(TransportChainAgent tca : transportChainAgents){
			double cost = tca.getCost() + umschlagskosten(tca.getNumberOfTranshipments()) + strafkosten(tca.hasSucceeded()); 
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
			cost += tca.getFees() + tca.getNumberOfTranshipments()*CostParameter.transshipmentCost;
		}
		return cost;
	}

	public TSPOffer requestService(Id from, Id to, int size, double startPickup, double endPickup, double startDelivery, double endDelivery) {
		Double memorizedCost = costMemory.getCost(from, to, size);
		return offerMaker.requestOffer(from, to, size, startPickup, endPickup, startDelivery, endDelivery, memorizedCost);
	}


	public Collection<Contract> registerChainAndGetCarrierContracts(TransportChain chain){
		TransportChainAgent chainAgent = new TransportChainAgent(chain);
		transportChainAgents.add(chainAgent);
		List<Contract> carrierContracts = chainAgent.createCarrierContracts();
		for(Contract c : carrierContracts){
			shipmentChainMap.put(c.getShipment(), chainAgent);
			logger.info("register shipment: " + c.getShipment());
		}
		return carrierContracts;
	}
	
	public Collection<Contract> removeChainAndGetAffectedCarrierContracts(TransportChain chain){
		Collection<Contract> associatedCarrierContracts = new ArrayList<Contract>();
		TransportChainAgent chainAgent = findChainAgent(chain);
		if(chainAgent != null){
			associatedCarrierContracts.addAll(chainAgent.getCarrierContracts());
			for(Shipment s : chainAgent.getShipments()){
				shipmentChainMap.remove(s);
				logger.info("remove shipment: " + s);
			}
			transportChainAgents.remove(chainAgent);
		}
		return associatedCarrierContracts;
	}

	private TransportChainAgent findChainAgent(TransportChain chain) {
		for(TransportChainAgent a : transportChainAgents){
			if(a.getTpChain() == chain){
				return a;
			}
		}
		return null;
	}

	public void shipmentPickedUp(Shipment shipment, double time) {
		shipmentChainMap.get(shipment).informPickup(shipment, time);
	}

	public void shipmentDelivered(Shipment shipment, double time) {
		shipmentChainMap.get(shipment).informDelivery(shipment, time);
		
	}

	public boolean hasShipment(Shipment shipment) {
		if(shipmentChainMap.containsKey(shipment)){
			return true;
		}
		return false;
	}

	public void setOfferMaker(TSPOfferMaker offerMaker) {
		this.offerMaker = offerMaker;
	}

	public void memorizeOffer(TSPOffer tspOffer, List<CarrierOffer> carrierOffers) {
		tspCarrierOfferMap.put(tspOffer, carrierOffers);
	}
	
	public List<CarrierOffer> getCarrierOffer(TSPOffer tspOffer){
		return tspCarrierOfferMap.get(tspOffer);
	}

	public void calculateCosts() {
		assertEqualTransportChainAgentsAndContracts(transportChainAgents.size(),tsp.getContracts().size());
		int volumes = 0;
		for(TransportChainAgent a : transportChainAgents){
			TSPShipment tspShipment = a.getTpChain().getShipment();
			volumes += tspShipment.getSize();
			double costOfChain = 0.0;
			costOfChain += a.getFees() + 
				a.getNumberOfTranshipments()*tspShipment.getSize()*CostParameter.transshipmentHandlingCostPerUnit;
			costMemory.memorizeCost(tspShipment.getFrom(), tspShipment.getTo(), tspShipment.getSize(), costOfChain);
		}
		TSPCostEvent costEvent = new TSPCostEvent(id);
		costEvent.setVolume(volumes);
		tspAgentTracker.informTotalCost(id,costEvent);
	}

	private void assertEqualTransportChainAgentsAndContracts(int nOfChainAgents, int nOfContracts) {
		if(nOfChainAgents != nOfContracts){
			throw new IllegalStateException("inconsistent state. we have " + nOfChainAgents + " chainAgents. but " + nOfContracts + " contracts");
		}
		
	}

	
}
