package freight;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;

import playground.mzilske.freight.TSPContract;
import playground.mzilske.freight.TSPShipment;

/**
 * 
 * @author stefan
 *
 */

public class ShipperAgent {
	
	public static class DetailedCost {
		public Id shipperId;
		public CommodityFlow comFlow;
		public int shipmentSize;
		public int frequency;
		public double tlc;
		public double transportation;
		public double inventory;
		public DetailedCost(Id shipperId, CommodityFlow comFlow,
				int shipmentSize, int frequency, double tlc,
				double transportation, double inventory) {
			super();
			this.shipperId = shipperId;
			this.comFlow = comFlow;
			this.shipmentSize = shipmentSize;
			this.frequency = frequency;
			this.tlc = tlc;
			this.transportation = transportation;
			this.inventory = inventory;
		}
	}
	
	public static class TotalCost {
		public Id shipperId;
		public double totCost;
		public TotalCost(Id shipperId, double totCost) {
			super();
			this.shipperId = shipperId;
			this.totCost = totCost;
		}
		
	}
	
	private ShipperImpl shipper;
	
	private Id id;
	
	private ShipperCostFunction costFunction;
	
	private ShipperAgentTracker shipperAgentTracker;
	
	public void setShipperAgentTracker(ShipperAgentTracker shipperAgentTracker) {
		this.shipperAgentTracker = shipperAgentTracker;
	}

	private Map<ScheduledCommodityFlow, Collection<TSPContract>> contractMap = new HashMap<ScheduledCommodityFlow, Collection<TSPContract>>();

	public ShipperAgent(ShipperImpl shipper) {
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

	Id getId(){
		return id;
	}

	public List<TSPContract> createTSPContracts() {
		List<TSPContract> contracts = new ArrayList<TSPContract>();
		for(ScheduledCommodityFlow sComFlow : shipper.getSelectedPlan().getScheduledFlows()){
			Collection<TSPContract> tspContracts = makeTspContracts(sComFlow);
			contracts.addAll(tspContracts);
			contractMap.put(sComFlow, tspContracts);
		}
		return contracts;
	}

	private Collection<TSPContract> makeTspContracts(
			ScheduledCommodityFlow sComFlow) {
		Collection<TSPContract> tspContracts = new ArrayList<TSPContract>();
		for(ShipperShipment s : sComFlow.getShipments()){
			TSPShipment shipment = TSPUtils.createTSPShipment(s.getFrom(), s.getTo(), s.getSize(), 
					s.getPickTimeWindow().getStart(), s.getPickTimeWindow().getEnd(), s.getDeliveryTimeWindow().getStart(), 
					s.getDeliveryTimeWindow().getEnd());
			TSPContract tspContract = TSPUtils.createTSPContract(shipment,sComFlow.getTspOffer());
			tspContracts.add(tspContract);	
		}
		return tspContracts;
	}
	
	public boolean hasCommodityFlow(ScheduledCommodityFlow scheduledCommodityFlow){
		return contractMap.containsKey(scheduledCommodityFlow);
	}

	public Collection<TSPContract> removeScheduledComFlowAndGetAffectedTspContracts(ScheduledCommodityFlow flow) {
		Collection<TSPContract> contracts = contractMap.get(flow);
		contractMap.remove(flow);
		return contracts;
	}

	public Collection<TSPContract> registerScheduledComFlowAndGetAffectedTspContracts(ScheduledCommodityFlow flow) {
		Collection<TSPContract> contracts = makeTspContracts(flow);
		contractMap.put(flow, contracts);
		return contracts;
	}
	
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
		shipperAgentTracker.informTotalCost(new TotalCost(id, score));
	}

	private void informDetailedCosts(CommodityFlow commodityFlow, double tlc, double transportation, double inventory, int shipmentSize,
			int frequency) {
		DetailedCost detailedCost = new DetailedCost(this.id, commodityFlow, shipmentSize, frequency, tlc, transportation, inventory);
		shipperAgentTracker.informDetailedCost(detailedCost);
	}
}
