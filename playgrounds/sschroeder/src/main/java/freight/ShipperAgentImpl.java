package freight;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.Id;

import playground.mzilske.freight.Contract;
import playground.mzilske.freight.TSPContract;
import playground.mzilske.freight.TSPShipment;

/**
 * 
 * @author stefan
 *
 */

public class ShipperAgentImpl implements ShipperAgent {
	
	private ShipperImpl shipper;
	
	private Id id;
	
	private ShipperCostFunction costFunction;
	
	private ShipperAgentTracker shipperAgentTracker;
	
	public void setShipperAgentTracker(ShipperAgentTracker shipperAgentTracker) {
		this.shipperAgentTracker = shipperAgentTracker;
	}

	private Set<ScheduledCommodityFlow> comFlows = new HashSet<ScheduledCommodityFlow>();
	

	public ShipperAgentImpl(ShipperImpl shipper) {
		super();
		this.shipper = shipper;
		this.id = shipper.getId();
	}

	public ShipperCostFunction getCostFunction() {
		return costFunction;
	}

	public void setCostFunction(ShipperCostFunction costFunction) {
		this.costFunction = costFunction;
	}

	public Id getId(){
		return id;
	}

	/* (non-Javadoc)
	 * @see freight.ShipperAgent#createTSPContracts()
	 */
	@Override
	public List<TSPContract> createTSPContracts() {
		List<TSPContract> contracts = new ArrayList<TSPContract>();
		for(ScheduledCommodityFlow sComFlow : shipper.getSelectedPlan().getScheduledFlows()){
			comFlows.add(sComFlow);
			for(Contract c : sComFlow.getContracts()){
				contracts.add((TSPContract)c);
			}
		}
//		for(ScheduledCommodityFlow sComFlow : shipper.getSelectedPlan().getScheduledFlows()){
//			Collection<TSPContract> tspContracts = makeTspContracts(sComFlow);
//			contracts.addAll(tspContracts);
//			contractMap.put(sComFlow, tspContracts);
//		}
		return contracts;
	}

	private Collection<TSPContract> makeTspContracts(ScheduledCommodityFlow sComFlow) {
		Collection<TSPContract> tspContracts = new ArrayList<TSPContract>();
		for(ShipperShipment s : sComFlow.getShipments()){
			Id seller = sComFlow.getTspOffer().getId();
			Id buyer = shipper.getId();
			TSPShipment shipment = TSPUtils.createTSPShipment(s.getFrom(), s.getTo(), s.getSize(), 
					s.getPickupTimeWindow().getStart(), s.getPickupTimeWindow().getEnd(), s.getDeliveryTimeWindow().getStart(), 
					s.getDeliveryTimeWindow().getEnd());
			TSPContract tspContract = new TSPContract(buyer, seller, shipment, sComFlow.getTspOffer());
			tspContracts.add(tspContract);	
		}
		return tspContracts;
	}
	
	public boolean hasCommodityFlow(ScheduledCommodityFlow scheduledCommodityFlow){
		return comFlows.contains(scheduledCommodityFlow);
	}

//	public Collection<TSPContract> removeScheduledComFlowAndGetAffectedTspContracts(ScheduledCommodityFlow flow) {
//		Collection<TSPContract> contracts = contractMap.get(flow);
//		contractMap.remove(flow);
//		return contracts;
//	}
//
//	public Collection<TSPContract> registerScheduledComFlowAndGetAffectedTspContracts(ScheduledCommodityFlow flow) {
//		Collection<TSPContract> contracts = makeTspContracts(flow);
//		contractMap.put(flow, contracts);
//		return contracts;
//	}
	
	/* (non-Javadoc)
	 * @see freight.ShipperAgent#scoreSelectedPlan()
	 */
	@Override
	public void scoreSelectedPlan(){
		double score = 0.0;
		for(ScheduledCommodityFlow commodityFlow : shipper.getSelectedPlan().getScheduledFlows()){
			int shipmentSize = commodityFlow.getShipments().iterator().next().getSize();
			int frequency = commodityFlow.getShipments().size();
			double tlc = costFunction.getCosts(commodityFlow.getCommodityFlow(), shipmentSize, commodityFlow.getShipments().size(), 
					commodityFlow.getTspOffer().getPrice());
			double transportation = costFunction.getTransportCost(commodityFlow.getCommodityFlow(), frequency, commodityFlow.getTspOffer().getPrice());
			double inventory = costFunction.getInventoryCost(commodityFlow.getCommodityFlow(), shipmentSize);
			score += tlc;
			informDetailedCosts(commodityFlow.getCommodityFlow(), tlc, transportation, inventory, shipmentSize, frequency);
		}
		shipper.getSelectedPlan().setScore(score);
		informTotalCost(score);
	}

	private void informTotalCost(double score) {
		shipperAgentTracker.processEvent(new TotalCostStatusEvent(id, score));
	}

	private void informDetailedCosts(CommodityFlow commodityFlow, double tlc, double transportation, double inventory, int shipmentSize,
			int frequency) {
		DetailedCostStatusEvent detailedCost = new DetailedCostStatusEvent(this.id, commodityFlow, shipmentSize, frequency, tlc, transportation, inventory);
		shipperAgentTracker.processEvent(detailedCost);
	}

	/* (non-Javadoc)
	 * @see freight.ShipperAgent#informTSPContractAccept(playground.mzilske.freight.Contract)
	 */
	@Override
	public void informTSPContractAccept(Contract contract) {
		
		
	}

	/* (non-Javadoc)
	 * @see freight.ShipperAgent#informTSPContractCanceled(playground.mzilske.freight.Contract)
	 */
	@Override
	public void informTSPContractCanceled(Contract contract) {
		
		
	}
}
