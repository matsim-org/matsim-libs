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

import playground.mzilske.freight.events.TSPTotalCostHandler.TSPCostEvent;


/**
 * @author stscr
 *
 */
public class TSPAgentImpl extends BasicTSPAgentImpl {
	
	private Logger logger = Logger.getLogger(TSPAgentImpl.class);
	
	public static class CostParameter{
		public static double transshipmentHandlingCostPerUnit = 2.0;
		public static double transshipmentCost = 5;
	}
	
	private TSPOfferMaker offerMaker;
	
	private CostMemory costMemory;
	
	public void setCostMemory(CostMemory costMemory) {
		this.costMemory = costMemory;
	}

	private TSPAgentTracker tspAgentTracker;

	public void setTspAgentTracker(TSPAgentTracker tspAgentTracker) {
		this.tspAgentTracker = tspAgentTracker;
	}

	public TSPAgentImpl(TransportServiceProvider tsp, TransportChainAgentFactory chainAgentFactory){
		super(tsp,chainAgentFactory);
		this.tsp = tsp;
		this.id = tsp.getId();
	}


	/* (non-Javadoc)
	 * @see playground.mzilske.freight.TSPAgent#reset()
	 */
	@Override
	public void reset(){
		for(TransportChainAgent tca : chainAgentMap.values()){
			tca.reset();
		}
		tspCarrierOfferMap.clear();
	}

	
	/* (non-Javadoc)
	 * @see playground.mzilske.freight.TSPAgent#scoreSelectedPlan()
	 */
	@Override
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
		for(TransportChainAgent tca : chainAgentMap.values()){
			cost += tca.getFees() + tca.getNumberOfTranshipments()*CostParameter.transshipmentCost;
		}
		return cost;
	}

	/* (non-Javadoc)
	 * @see playground.mzilske.freight.TSPAgent#requestService(org.matsim.api.core.v01.Id, org.matsim.api.core.v01.Id, int, double, double, double, double)
	 */
	@Override
	public TSPOffer requestService(Id from, Id to, int size, double startPickup, double endPickup, double startDelivery, double endDelivery) {
		Double memorizedCost = costMemory.getCost(from, to, size);
		return offerMaker.requestOffer(from, to, size, startPickup, endPickup, startDelivery, endDelivery, memorizedCost);
	}

	public void setOfferMaker(TSPOfferMaker offerMaker) {
		this.offerMaker = offerMaker;
	}

	public void memorizeOffer(TSPOffer tspOffer, List<CarrierOffer> carrierOffers) {
		tspCarrierOfferMap.put(tspOffer, carrierOffers);
	}
	
	public List<CarrierOffer> getCarrierOffers(TSPOffer tspOffer){
		return tspCarrierOfferMap.get(tspOffer);
	}

	/* (non-Javadoc)
	 * @see playground.mzilske.freight.TSPAgent#calculateCosts()
	 */
	@Override
	public void calculateCosts() {
		assertEqualTransportChainAgentsAndContracts(chainAgentMap.values().size(),tsp.getContracts().size());
		int volumes = 0;
		for(TransportChainAgent a : chainAgentMap.values()){
			TSPShipment tspShipment = a.getTransportChain().getShipment();
			volumes += tspShipment.getSize();
			double costOfChain = 0.0;
			costOfChain += a.getFees() + 
				a.getNumberOfTranshipments()*tspShipment.getSize()*CostParameter.transshipmentHandlingCostPerUnit;
			costMemory.memorizeCost(tspShipment.getFrom(), tspShipment.getTo(), tspShipment.getSize(), costOfChain);
		}
		TSPCostEvent costEvent = new TSPCostEvent(id,volumes);
		tspAgentTracker.processEvent(costEvent);
	}

	private void assertEqualTransportChainAgentsAndContracts(int nOfChainAgents, int nOfContracts) {
		if(nOfChainAgents != nOfContracts){
			throw new IllegalStateException("inconsistent state. we have " + nOfChainAgents + " chainAgents. but " + nOfContracts + " contracts");
		}
		
	}


	@Override
	public void informShipperContractAccepted(Contract contract) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void informCarrierContractAccepted(Contract contract) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void informCarrierContractCanceled(Contract contract) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void informChainRemoved(TransportChain chain) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void informChainAdded(TransportChain chain) {
		// TODO Auto-generated method stub
		
	}

	
}
