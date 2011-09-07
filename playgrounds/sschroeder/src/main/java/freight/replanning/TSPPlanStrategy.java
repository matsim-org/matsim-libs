package freight.replanning;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.core.gbl.MatsimRandom;

import playground.mzilske.freight.Carrier;
import playground.mzilske.freight.CarrierAgentTracker;
import playground.mzilske.freight.CarrierPlan;
import playground.mzilske.freight.CarrierPlanBuilder;
import playground.mzilske.freight.Contract;
import playground.mzilske.freight.TSPAgentTracker;
import playground.mzilske.freight.TSPContract;
import playground.mzilske.freight.TSPOffer;
import playground.mzilske.freight.TSPPlan;
import playground.mzilske.freight.TransportChain;
import playground.mzilske.freight.TransportServiceProviderImpl;
import playground.mzilske.freight.TSPPlanBuilder;
import freight.TSPUtils;

public class TSPPlanStrategy {
	
	public double share2replan = 0.2;
	
	private TSPPlanBuilder tspPlanBuilder;
	
	private CarrierPlanBuilder carrierPlanBuilder;
	
	public void setCarrierPlanBuilder(CarrierPlanBuilder carrierPlanBuilder) {
		this.carrierPlanBuilder = carrierPlanBuilder;
	}

	private TSPAgentTracker tspAgentTracker;
	
	private CarrierAgentTracker carrierAgentTracker;

	public void setTspAgentTracker(TSPAgentTracker tspAgentTracker) {
		this.tspAgentTracker = tspAgentTracker;
	}

	public void setCarrierAgentTracker(CarrierAgentTracker carrierAgentTracker) {
		this.carrierAgentTracker = carrierAgentTracker;
	}

	private Collection<TransportChain> removedTransportChains = new ArrayList<TransportChain>();
	
	private Collection<TransportChain> newTransportChains = new ArrayList<TransportChain>();
	
	public void setTspPlanBuilder(TSPPlanBuilder tspPlanBuilder) {
		this.tspPlanBuilder = tspPlanBuilder;
	}

	public void run(TransportServiceProviderImpl tsp){
		Collection<TransportChain> chains2replan = new ArrayList<TransportChain>();
		Collection<TransportChain> newTransportChains = new ArrayList<TransportChain>();
		for(TransportChain transportChain : tsp.getSelectedPlan().getChains()){
			if(MatsimRandom.getRandom().nextDouble() < share2replan){
				chains2replan.add(transportChain);
				removedTransportChains.add(transportChain);
			}
			else{
				newTransportChains.add(transportChain);
			}
		}
		Collection<TSPContract> cons = getContracts(tsp.getId(),chains2replan);
		TSPPlan partialPlan = tspPlanBuilder.buildPlan(cons, tsp.getTspCapabilities());
		Collection<TransportChain> chains = partialPlan.getChains(); 
		newTransportChains.addAll(chains);
		TSPPlan newPlan = new TSPPlan(newTransportChains);
		tsp.setSelectedPlan(newPlan);
		replanAffectedCarriers(tsp.getId());
	}

	private void replanAffectedCarriers(Id tspId) {
		Collection<Contract> oldContracts = new ArrayList<Contract>();
		for(TransportChain chain : removedTransportChains){
			Collection<Contract> carrierContracts = tspAgentTracker.removeChainAndGetAffectedCarrierContract(tspId,chain);
			oldContracts.addAll(carrierContracts);
			carrierAgentTracker.removeContracts(carrierContracts);
		}
		Collection<Contract> newContracts = new ArrayList<Contract>();
		for(TransportChain chain : newTransportChains){
			Collection<Contract> carrierContracts = tspAgentTracker.registerChainAndGetAffectedCarrierContract(tspId, chain);
			newContracts.addAll(carrierContracts);
			carrierAgentTracker.addContracts(carrierContracts);
		}

		Set<Id> carriers = getCarriersToReplan(oldContracts,newContracts);
		for(Id id : carriers){
			Carrier carrier = carrierAgentTracker.getCarrier(id);
			CarrierPlan plan = carrierPlanBuilder.buildPlan(carrier.getCarrierCapabilities(), carrier.getContracts());
			carrier.setSelectedPlan(plan);
		}		
	}

	private Collection<TSPContract> getContracts(Id id, Collection<TransportChain> chains2replan) {
		Collection<TSPContract> contracts = new ArrayList<TSPContract>();
		for(TransportChain chain : chains2replan){
			TSPOffer offer = new TSPOffer();
			offer.setId(id);
			TSPContract tspContract = TSPUtils.createTSPContract(chain.getShipment(), offer);
			contracts.add(tspContract);
		}
		return contracts;
	}
	
	private Set<Id> getCarriersToReplan(Collection<Contract> newContracts, Collection<Contract> oldContracts) {
		Set<Id> carriers = new HashSet<Id>();
		for(Contract c : newContracts){
			carriers.add(c.getOffer().getId());
		}
		for(Contract c : oldContracts){
			carriers.add(c.getOffer().getId());
		}
		return carriers;
	}

}
