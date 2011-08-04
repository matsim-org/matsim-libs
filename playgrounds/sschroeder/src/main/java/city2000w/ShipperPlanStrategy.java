package city2000w;

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
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.gbl.MatsimRandom;

import playground.mzilske.freight.CarrierAgentTracker;
import playground.mzilske.freight.CarrierImpl;
import playground.mzilske.freight.CarrierPlan;
import playground.mzilske.freight.CarrierPlanBuilder;
import playground.mzilske.freight.Contract;
import playground.mzilske.freight.TSPAgentTracker;
import playground.mzilske.freight.TSPContract;
import playground.mzilske.freight.TSPOffer;
import playground.mzilske.freight.TSPPlan;
import playground.mzilske.freight.TSPShipment;
import playground.mzilske.freight.TransportChain;
import playground.mzilske.freight.TransportServiceProviderImpl;
import playground.mzilske.freight.TSPPlanBuilder;
import city2000w.MarginalCostCalculator.CostTableKey;
import city2000w.TRBShippersContractGenerator.TimeProfile;
import freight.ScheduledCommodityFlow;
import freight.ShipperAgent;
import freight.ShipperAgentTracker;
import freight.ShipperImpl;
import freight.ShipperPlan;
import freight.ShipperShipment;
import freight.ShipperUtils;

public class ShipperPlanStrategy {
	
	/*
	 * once shippers change their plans, affected tsp and carrier also change their plans
	 */
	
	static class FrequencyChoiceResult {
		public ScheduledCommodityFlow scheduledCommodityFlow;
		
		public double totalCosts;
		
		public TSPOffer tspOffer;

		public FrequencyChoiceResult(ScheduledCommodityFlow sComFlow,
				double totalCosts) {
			super();
			this.scheduledCommodityFlow = sComFlow;
			this.totalCosts = totalCosts;
		}
	}
	
	private static Logger logger = Logger.getLogger(ShipperPlanStrategy.class);
	
	private ShipperAgentTracker shipperAgentTracker;
	
	private TSPAgentTracker tspAgentTracker;
	
	private CarrierAgentTracker carrierAgentTracker;
	
	private Network network;
	
	private Map<Id,Collection<TransportChain>> removedTransportChains = new HashMap<Id, Collection<TransportChain>>();
	
	private Map<Id,Collection<TransportChain>> newTransportChains = new HashMap<Id, Collection<TransportChain>>();
	
	private Map<ScheduledCommodityFlow,ScheduledCommodityFlow> oldFlowNewFlowChangeMap = new HashMap<ScheduledCommodityFlow, ScheduledCommodityFlow>();
	
	private TSPPlanBuilder tspPlanBuilder;
	
	private CarrierPlanBuilder carrierPlanBuilder;

	private OfferSelectorImpl<TSPOffer> tspOfferSelector;
	
	public void setTspPlanBuilder(TSPPlanBuilder tspPlanBuilder) {
		this.tspPlanBuilder = tspPlanBuilder;
	}

	public void setCarrierPlanBuilder(CarrierPlanBuilder carrierPlanBuilder) {
		this.carrierPlanBuilder = carrierPlanBuilder;
	}

	public void setNetwork(Network network) {
		this.network = network;
	}

	public void setTspAgentTracker(TSPAgentTracker tspAgentTracker) {
		this.tspAgentTracker = tspAgentTracker;
	}

	public void setCarrierAgentTracker(CarrierAgentTracker carrierAgentTracker) {
		this.carrierAgentTracker = carrierAgentTracker;
	}

	public void run(ShipperImpl shipper){
		logger.info("replan shipper");
		ShipperPlan plan = shipper.getSelectedPlan();
		Collection<ScheduledCommodityFlow> newFlows = new ArrayList<ScheduledCommodityFlow>();
		for(ScheduledCommodityFlow sComFlow : plan.getScheduledFlows()){
			if(MatsimRandom.getRandom().nextDouble() < 0.1){
				ScheduledCommodityFlow scheduledCommodityFlow = new ScheduledCommodityFlow(sComFlow.getCommodityFlow(), 
						sComFlow.getShipments(), sComFlow.getTspOffer());
				newFlows.add(scheduledCommodityFlow);
				oldFlowNewFlowChangeMap.put(sComFlow, scheduledCommodityFlow);
			}
			else{
				newFlows.add(sComFlow);
			}
		}
		ShipperPlan newPlan =  new ShipperPlan(newFlows);
		shipper.setSelectedPlan(newPlan);
		replanAffectedAgents(shipper);
	}
	
	private FrequencyChoiceResult pickRandomAlternative(Collection<FrequencyChoiceResult> results) {
		List<FrequencyChoiceResult> shuffeledList = new ArrayList<ShipperPlanStrategy.FrequencyChoiceResult>(results);
		Collections.shuffle(shuffeledList, MatsimRandom.getRandom());
		return shuffeledList.get(0);
	}

	private void printAlternative(Id shipperId, FrequencyChoiceResult bestAlt) {
		logger.info("shipperId=" + shipperId + "; comFlow=" + bestAlt.scheduledCommodityFlow.getCommodityFlow() + 
				"; totCost=" + bestAlt.totalCosts + "; tspId=" + bestAlt.tspOffer.getId() + "; tspPrice= " + bestAlt.tspOffer.getPrice() + 
				"; frequency=" + bestAlt.scheduledCommodityFlow.getShipments().size());
	}
	
	

	private void printAlternatives(Id shipperId, Collection<FrequencyChoiceResult> results) {
		for(FrequencyChoiceResult fcr : results){
			printAlternative(shipperId, fcr);
		}
		
	}

	private FrequencyChoiceResult pickBestAlternative(Collection<FrequencyChoiceResult> results) {
		FrequencyChoiceResult best = null;
		for(FrequencyChoiceResult fcr : results){
			if(best == null){
				best = fcr;
			}
			else{
				if(fcr.totalCosts < best.totalCosts){
					best = fcr;
				}
			}
		}
		return best;
	}

	private Collection<FrequencyChoiceResult> generateAlternatives(ScheduledCommodityFlow sComFlow, ShipperImpl shipper) {
		ScheduledCommodityFlow alt1 = makeNewOne(shipper, sComFlow, 1);
		ScheduledCommodityFlow alt2 = makeNewOne(shipper, sComFlow, 2);
		List<ScheduledCommodityFlow> flows = new ArrayList<ScheduledCommodityFlow>();
		flows.add(alt1);
		flows.add(alt2);
		Collection<FrequencyChoiceResult> choiceResults = new ArrayList<ShipperPlanStrategy.FrequencyChoiceResult>();
		for(ScheduledCommodityFlow scheduledFlow : flows){
			ShipperShipment shipment = pickRandomShipment(scheduledFlow.getShipments());
			Collection<TSPOffer> offers = tspAgentTracker.requestService(shipment.getFrom(), shipment.getTo(),
					shipment.getSize(),shipment.getPickTimeWindow().getStart(),shipment.getPickTimeWindow().getEnd(),
					shipment.getDeliveryTimeWindow().getStart(), shipment.getDeliveryTimeWindow().getEnd());
			TSPOffer bestOffer = null;
			bestOffer = tspOfferSelector.selectOffer(offers); 
			if(bestOffer == null){
				pickBestOffer(offers);
			}
			if(bestOffer == null){
				throw new IllegalStateException("strange");
			}
			ShipperAgent shipperAgent = shipperAgentTracker.getShipperAgent(shipper.getId());
			double tpCostPerShipment = bestOffer.getPrice();
			double tlc = shipperAgent.getCostFunction().getCosts(scheduledFlow.getCommodityFlow(), 
					shipment.getSize(), scheduledFlow.getShipments().size(), tpCostPerShipment);
			FrequencyChoiceResult frequencyChoiceResult = new FrequencyChoiceResult(scheduledFlow, tlc);
			frequencyChoiceResult.tspOffer = bestOffer;
			choiceResults.add(frequencyChoiceResult);
		}
		return choiceResults;
	}

	private ShipperShipment pickRandomShipment(Collection<ShipperShipment> shipments) {
		List<ShipperShipment> shuffeledShipments = new ArrayList<ShipperShipment>(shipments);
		Collections.shuffle(shuffeledShipments,MatsimRandom.getRandom());
		return shuffeledShipments.get(0);
	}

	private TSPOffer pickBestOffer(Collection<TSPOffer> offers) {
		TSPOffer best = null;
		for(TSPOffer o : offers){
			if(best == null){
				best = o;
			}
			else{
				if(o.getPrice() < best.getPrice()){
					best = o;
				}
			}
		}
		return best;
	}

	private void replanAffectedAgents(ShipperImpl shipper) {
		logger.info("replan affected agents");
		replanTransportServiceProvider(shipper);
		replanCarriers();
	}

	private void replanCarriers() {
		logger.info("replan affected carriers");
		Collection<Contract> oldContracts = new ArrayList<Contract>();
		for(Id tspId : removedTransportChains.keySet()){
			for(TransportChain chain : removedTransportChains.get(tspId)){
				Collection<Contract> carrierContracts = tspAgentTracker.removeChainAndGetAffectedCarrierContract(tspId,chain);
				oldContracts.addAll(carrierContracts);
				carrierAgentTracker.removeContracts(carrierContracts);
			}
		}
		
		Collection<Contract> newContracts = new ArrayList<Contract>();
		for(Id tspId : newTransportChains.keySet()){
			for(TransportChain chain : newTransportChains.get(tspId)){
				Collection<Contract> carrierContracts = tspAgentTracker.registerChainAndGetAffectedCarrierContract(tspId, chain);
				newContracts.addAll(carrierContracts);
				carrierAgentTracker.addContracts(carrierContracts);
			}
		}
		
		Set<Id> carriers = getCarriersToReplan(oldContracts,newContracts);
		logger.info("carrierSize=" + carriers.size());
		for(Id id : carriers){
			CarrierImpl carrier = carrierAgentTracker.getCarrier(id);
			CarrierPlan plan = carrierPlanBuilder.buildPlan(carrier.getCarrierCapabilities(), carrier.getContracts());
			carrier.getPlans().add(plan);
			carrier.setSelectedPlan(plan);
		}
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

	private void replanTransportServiceProvider(ShipperImpl shipper) {
		logger.info("replan affected transport service provider");
		Collection<TSPContract> oldContracts = new ArrayList<TSPContract>();
		Collection<TSPContract> newContracts = new ArrayList<TSPContract>();
		for(ScheduledCommodityFlow oldFlow : oldFlowNewFlowChangeMap.keySet()){
			ScheduledCommodityFlow newFlow = oldFlowNewFlowChangeMap.get(oldFlow);
			Collection<TSPContract> oldCons = shipperAgentTracker.removeScheduledComFlowAndGetAffectedTspContracts(shipper.getId(), oldFlow);
			tspAgentTracker.removeContracts(oldCons);
			oldContracts.addAll(oldCons);
			Collection<TSPContract> newCons = shipperAgentTracker.registerScheduledComFlowAndGetAffectedTspContracts(shipper.getId(), newFlow);
			tspAgentTracker.addContracts(newCons);
			newContracts.addAll(newCons);
		}
		
		Map<Id,Collection<TSPContract>> oldClusteredContracts = clusterContractsAccToTsp(oldContracts);
		for(Id tspId : oldClusteredContracts.keySet()){
			Collection<TSPContract> cons = oldClusteredContracts.get(tspId);
			TransportServiceProviderImpl tsp = tspAgentTracker.getTsp(tspId);
			TSPPlan plan = removeShipmentsAndGetNewTspPlan(tsp,cons);
			tsp.setSelectedPlan(plan);
		}

		Map<Id,Collection<TSPContract>> newClusteredContracts = clusterContractsAccToTsp(newContracts);
		for(Id tspId : newClusteredContracts.keySet()){
			Collection<TSPContract> cons = newClusteredContracts.get(tspId);
			TransportServiceProviderImpl tsp = tspAgentTracker.getTsp(tspId);
			TSPPlan plan = addShipmentsAndGetNewTspPlan(tsp, cons);
			tsp.setSelectedPlan(plan);
		}
		
	}

	private TSPPlan addShipmentsAndGetNewTspPlan(TransportServiceProviderImpl tsp, Collection<TSPContract> cons) {
		Collection<TransportChain> chains = new ArrayList<TransportChain>();
		chains.addAll(tsp.getSelectedPlan().getChains());
		TSPPlan partialPlan = tspPlanBuilder.buildPlan(cons, tsp.getTspCapabilities());
		chains.addAll(partialPlan.getChains());
		newTransportChains.put(tsp.getId(), partialPlan.getChains());
		return new TSPPlan(chains);
	}

	private TSPPlan removeShipmentsAndGetNewTspPlan(TransportServiceProviderImpl tsp, Collection<TSPContract> cons) {
		TSPPlan selectedPlan = tsp.getSelectedPlan();
		Collection<TransportChain> chains = new ArrayList<TransportChain>();
		Set<TSPShipment> shipments = makeShipmentSet(cons);
		for(TransportChain chain : selectedPlan.getChains()){
			if(!shipments.contains(chain.getShipment())){
				chains.add(chain);
			}
			else{
				addToRemovedTransportChains(tsp.getId(), chain);
			}
		}
		return new TSPPlan(chains);
	}

	private void addToRemovedTransportChains(Id id, TransportChain chain) {
		if(removedTransportChains.containsKey(id)){
			removedTransportChains.get(id).add(chain);
		}
		else{
			Collection<TransportChain> chains = new ArrayList<TransportChain>();
			chains.add(chain);
			removedTransportChains.put(id, chains);
		}	
	}

	private Set<TSPShipment> makeShipmentSet(Collection<TSPContract> cons) {
		Set<TSPShipment> shipments = new HashSet<TSPShipment>();
		for(TSPContract c : cons){
			shipments.add(c.getShipment());
		}
		return shipments;
	}

	private Map<Id, Collection<TSPContract>> clusterContractsAccToTsp(Collection<TSPContract> oldContracts) {
		Map<Id,Collection<TSPContract>> contractMap = new HashMap<Id, Collection<TSPContract>>();
		for(TSPContract c : oldContracts){
			Id tspId = c.getOffer().getId();
			if(contractMap.containsKey(tspId)){
				contractMap.get(tspId).add(c);
			}
			else{
				Collection<TSPContract> contractColl = new ArrayList<TSPContract>();
				contractColl.add(c);
				contractMap.put(tspId, contractColl);
			}
		}
		return contractMap;
	}

	private ScheduledCommodityFlow makeNewOne(ShipperImpl shipper, ScheduledCommodityFlow comFlow, int i) {
		Collection<ShipperShipment> shipments = new ArrayList<ShipperShipment>();
		if(i==1){
			TimeProfile timeProfile = shipper.getShipperKnowledge().getTimeProfile(1).iterator().next();
			ShipperShipment s = ShipperUtils.createShipment(comFlow.getCommodityFlow().getFrom(), comFlow.getCommodityFlow().getTo(), 
					10, timeProfile.pickupStart, timeProfile.pickupEnd, timeProfile.deliveryStart, timeProfile.deliveryEnd);
			shipments.add(s);
		}
		else{
			Collection<TimeProfile> profiles = shipper.getShipperKnowledge().getTimeProfile(2);
			List<TimeProfile> profList = new ArrayList<TRBShippersContractGenerator.TimeProfile>(profiles);
			TimeProfile morning = profList.get(0);
			ShipperShipment s1 = ShipperUtils.createShipment(comFlow.getCommodityFlow().getFrom(), comFlow.getCommodityFlow().getTo(), 
					5, morning.pickupStart, morning.pickupEnd, morning.deliveryStart, morning.deliveryEnd);
			TimeProfile afternoon = profList.get(1);
			ShipperShipment s2 = ShipperUtils.createShipment(comFlow.getCommodityFlow().getFrom(), comFlow.getCommodityFlow().getTo(), 
					5, afternoon.pickupStart, afternoon.pickupEnd, afternoon.deliveryStart, afternoon.deliveryEnd);
			//startPickup="10200.0" endPickup="12000" startDelivery="0.0" endDelivery="14000"
			shipments.add(s1);
			shipments.add(s2);
		}
		return ShipperUtils.createScheduledCommodityFlow(comFlow.getCommodityFlow(), shipments, comFlow.getTspOffer());
	}

	public void setShipperAgentTracker(ShipperAgentTracker shipperAgentTracker) {
		this.shipperAgentTracker = shipperAgentTracker; 
	}

	public void setTspOfferSelector(OfferSelectorImpl<TSPOffer> tspOfferSelector) {
		this.tspOfferSelector = tspOfferSelector;
		
	}


}
