package freight.replanning;

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
import playground.mzilske.freight.api.Offer;
import city2000w.TRBShippersContractGenerator;
import city2000w.TRBShippersContractGenerator.TimeProfile;
import freight.CommodityFlow;
import freight.ScheduledCommodityFlow;
import freight.ShipperAgent;
import freight.ShipperAgentTracker;
import freight.ShipperContract;
import freight.ShipperImpl;
import freight.ShipperPlan;
import freight.ShipperShipment;
import freight.ShipperUtils;
import freight.offermaker.OfferSelectorImpl;

public class AnotherShipperPlanStrategy {
	
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
	
	static class FrequencyAlternatives {
		public Collection<ScheduledCommodityFlow> scheduledComFlows = new ArrayList<ScheduledCommodityFlow>();
		
		void add(ScheduledCommodityFlow sComFlow){
			scheduledComFlows.add(sComFlow);
		}
	}
	
	private static Logger logger = Logger.getLogger(AnotherShipperPlanStrategy.class);
	
	private ShipperAgentTracker shipperAgentTracker;
	
	private TSPAgentTracker tspAgentTracker;
	
	private CarrierAgentTracker carrierAgentTracker;
	
	private Map<Id,Collection<TransportChain>> removedTransportChains = new HashMap<Id, Collection<TransportChain>>();
	
	private Map<Id,Collection<TransportChain>> newTransportChains = new HashMap<Id, Collection<TransportChain>>();
	
	private Collection<ScheduledCommodityFlow> oldScheduledCommodityFlows = new ArrayList<ScheduledCommodityFlow>();
	
	private Collection<ScheduledCommodityFlow> newScheduledCommodityFlows = new ArrayList<ScheduledCommodityFlow>();
	
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
	}

	public void setTspAgentTracker(TSPAgentTracker tspAgentTracker) {
		this.tspAgentTracker = tspAgentTracker;
	}

	public void setCarrierAgentTracker(CarrierAgentTracker carrierAgentTracker) {
		this.carrierAgentTracker = carrierAgentTracker;
	}

	static class Proposal implements Offer{
		public ScheduledCommodityFlow scheduledCommodityFlow;
		public double tlc;
		private Id id;
		
		public Proposal() {
			super();
		}
		
		public void setScheduledCommodityFlow(ScheduledCommodityFlow comFlow){
			scheduledCommodityFlow = comFlow;
		}
		
		@Override
		public Id getId() {
			return id;
		}
		@Override
		public Double getPrice() {
			return tlc;
		}
		@Override
		public void setId(Id id) {
			this.id = id;
		}
		@Override
		public void setPrice(double d) {
			this.tlc = d;
		}
	}
	public void run(ShipperImpl shipper){
		logger.info("replan shipper");
//		System.out.println("replan shipper " + shipper.getId());
		ShipperPlan plan = shipper.getSelectedPlan();
		oldScheduledCommodityFlows.addAll(plan.getScheduledFlows());
		Map<Id,Collection<Proposal>> bestTspProposalOverAllContracts = new HashMap<Id, Collection<Proposal>>();
		for(ShipperContract contract : shipper.getContracts()){
			CommodityFlow comFlow = contract.getCommodityFlow();
			FrequencyAlternatives frequencyAlternatives = getFrequencyAlternatives(shipper,comFlow);
			Map<Id,Proposal> bestProposals = new HashMap<Id, Proposal>();
			for(ScheduledCommodityFlow frequencyAlternative : frequencyAlternatives.scheduledComFlows){
				ShipperShipment shipment = frequencyAlternative.getShipments().iterator().next(); 
				Collection<TSPOffer> tspOffers = tspAgentTracker.requestService(frequencyAlternative.getCommodityFlow().getFrom(), frequencyAlternative.getCommodityFlow().getTo(), 
						shipment.getSize(), shipment.getPickTimeWindow().getStart(), shipment.getPickTimeWindow().getEnd(), 
						shipment.getDeliveryTimeWindow().getStart(), shipment.getDeliveryTimeWindow().getEnd());
				for(TSPOffer tspOffer : tspOffers){
					ScheduledCommodityFlow scheduledCommodityFlow = getScheduledComFlow(frequencyAlternative,tspOffer);
					ShipperAgent shipperAgent = shipperAgentTracker.getShipperAgent(shipper.getId());
					double tpCostPerShipment = tspOffer.getPrice();
					double tlc = shipperAgent.getCostFunction().getCosts(comFlow, 
							shipment.getSize(), scheduledCommodityFlow.getShipments().size(), tpCostPerShipment);
					Id tspId = tspOffer.getId();
					if(bestProposals.containsKey(tspId)){
						if(tlc < bestProposals.get(tspId).tlc){
							bestProposals.put(tspId, makeProposal(tspId,scheduledCommodityFlow, tlc));
						}
					}
					else{
						bestProposals.put(tspId, makeProposal(tspId,scheduledCommodityFlow, tlc));
					}
				}
			}
			for(Id tspId : bestProposals.keySet()){
				Proposal proposal = bestProposals.get(tspId);
				if(bestTspProposalOverAllContracts.containsKey(tspId)){
					bestTspProposalOverAllContracts.get(tspId).add(proposal);
				}
				else{
					Collection<Proposal> list = new ArrayList<Proposal>();
					list.add(proposal);
					bestTspProposalOverAllContracts.put(tspId, list);
				}
			}
		}
		Proposal selectedProposal = bestProposal(bestTspProposalOverAllContracts);
		double oldScore = Double.MAX_VALUE;
		if(shipper.getSelectedPlan().getScore() != null){
			oldScore = shipper.getSelectedPlan().getScore();
		}
		if(selectedProposal.getPrice() < oldScore + MatsimRandom.getRandom().nextDouble()*oldScore*0.05){
			Collection<ScheduledCommodityFlow> sComflows = getScheduledComFlows(bestTspProposalOverAllContracts.get(selectedProposal.getId()));
			ShipperPlan newPlan = new ShipperPlan(sComflows);
			newPlan.setScore(selectedProposal.getPrice());
			newScheduledCommodityFlows.addAll(newPlan.getScheduledFlows());
			shipper.setSelectedPlan(newPlan);
			replanAffectedAgents(shipper);
		}
		return;
	}

	private Collection<ScheduledCommodityFlow> getScheduledComFlows(Collection<Proposal> collection) {
		Collection<ScheduledCommodityFlow> flows = new ArrayList<ScheduledCommodityFlow>();
		for(Proposal p : collection){
			flows.add(p.scheduledCommodityFlow);
		}
		return flows;
	}

	private Proposal bestProposal(Map<Id, Collection<Proposal>> bestTspProposalOverAllContracts) {
		Collection<Proposal> overallProposals = new ArrayList<AnotherShipperPlanStrategy.Proposal>();
		for(Id id : bestTspProposalOverAllContracts.keySet()){
			double totalTlc = 0.0;
			for(Proposal proposal : bestTspProposalOverAllContracts.get(id)){
				totalTlc += proposal.tlc;
			}
			Proposal overallProposal = new Proposal();
			overallProposal.setId(id);
			overallProposal.setPrice(totalTlc);
			overallProposals.add(overallProposal);
		}
		for(Proposal p : overallProposals){
//			System.out.println(p.getId() + " tlc=" + p.getPrice());
		}
		OfferSelectorImpl<Proposal> offerSelector = new OfferSelectorImpl<AnotherShipperPlanStrategy.Proposal>(tspOfferSelector.beta);
		Proposal proposal = offerSelector.selectOffer(overallProposals);
//		System.out.println(proposal.getId() + " " + proposal.getPrice() + " chosen");
		return proposal;
	}

	private Proposal makeProposal(Id tspId, ScheduledCommodityFlow scheduledCommodityFlow, double tlc) {
		Proposal proposal = new Proposal();
		proposal.setScheduledCommodityFlow(scheduledCommodityFlow);
		proposal.setPrice(tlc);
		proposal.setId(tspId);
		return proposal;
	}
	
	private ScheduledCommodityFlow getScheduledComFlow(ScheduledCommodityFlow sComFlow, TSPOffer offer) {
		return new ScheduledCommodityFlow(sComFlow.getCommodityFlow(), sComFlow.getShipments(), offer);
	}

	private FrequencyAlternatives getFrequencyAlternatives(ShipperImpl shipper, CommodityFlow comFlow) {
		ScheduledCommodityFlow alt1 = makeNewOne(shipper, comFlow, 1);
		ScheduledCommodityFlow alt2 = makeNewOne(shipper, comFlow, 2);
		FrequencyAlternatives alternatives = new FrequencyAlternatives();
		alternatives.add(alt1);
		alternatives.add(alt2);
		return alternatives;
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
		for(ScheduledCommodityFlow oldFlow : oldScheduledCommodityFlows){
			Collection<TSPContract> oldCons = shipperAgentTracker.removeScheduledComFlowAndGetAffectedTspContracts(shipper.getId(), oldFlow);
			tspAgentTracker.removeContracts(oldCons);
			oldContracts.addAll(oldCons);
		}
		for(ScheduledCommodityFlow newFlow : newScheduledCommodityFlows){
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

	private ScheduledCommodityFlow makeNewOne(ShipperImpl shipper, CommodityFlow comFlow, int i) {
		Collection<ShipperShipment> shipments = new ArrayList<ShipperShipment>();
		if(i==1){
			TimeProfile timeProfile = shipper.getShipperKnowledge().getTimeProfile(1).iterator().next();
			ShipperShipment s = ShipperUtils.createShipment(comFlow.getFrom(), comFlow.getTo(), 
					10, timeProfile.pickupStart, timeProfile.pickupEnd, timeProfile.deliveryStart, timeProfile.deliveryEnd);
			shipments.add(s);
		}
		else{
			Collection<TimeProfile> profiles = shipper.getShipperKnowledge().getTimeProfile(2);
			List<TimeProfile> profList = new ArrayList<TRBShippersContractGenerator.TimeProfile>(profiles);
			TimeProfile morning = profList.get(0);
			ShipperShipment s1 = ShipperUtils.createShipment(comFlow.getFrom(), comFlow.getTo(), 
					5, morning.pickupStart, morning.pickupEnd, morning.deliveryStart, morning.deliveryEnd);
			TimeProfile afternoon = profList.get(1);
			ShipperShipment s2 = ShipperUtils.createShipment(comFlow.getFrom(), comFlow.getTo(), 
					5, afternoon.pickupStart, afternoon.pickupEnd, afternoon.deliveryStart, afternoon.deliveryEnd);
			//startPickup="10200.0" endPickup="12000" startDelivery="0.0" endDelivery="14000"
			shipments.add(s1);
			shipments.add(s2);
		}
		return ShipperUtils.createScheduledCommodityFlow(comFlow, shipments, new TSPOffer());
	}

	public void setShipperAgentTracker(ShipperAgentTracker shipperAgentTracker) {
		this.shipperAgentTracker = shipperAgentTracker; 
	}

	public void setTspOfferSelector(OfferSelectorImpl<TSPOffer> tspOfferSelector) {
		this.tspOfferSelector = tspOfferSelector;
		
	}


}
