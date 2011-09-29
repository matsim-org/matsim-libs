package freight;

import java.util.Collection;

import playground.mzilske.freight.Contract;
import playground.mzilske.freight.TSPOffer;

public class ScheduledCommodityFlow {
	
	private CommodityFlow commodityFlow;
	
	private Collection<ShipperShipment> shipments;
	
	private TSPOffer tspOffer;
	
	private Collection<Contract> contracts;

	public ScheduledCommodityFlow(CommodityFlow commodityFlow, Collection<ShipperShipment> shipments, TSPOffer tspOffer) {
		super();
		this.commodityFlow = commodityFlow;
		this.shipments = shipments;
		this.tspOffer = tspOffer;
	}

	public ScheduledCommodityFlow(CommodityFlow commodityFlow,Collection<ShipperShipment> shipments, Collection<Contract> tspContracts) {
		super();
		this.commodityFlow = commodityFlow;
		this.shipments = shipments;
		this.contracts = tspContracts;
	}

	public CommodityFlow getCommodityFlow() {
		return commodityFlow;
	}

	public Collection<ShipperShipment> getShipments() {
		return shipments;
	}

	public TSPOffer getTspOffer() {
		return tspOffer;
	}

	public Collection<Contract> getContracts() {
		return contracts;
	}

}
