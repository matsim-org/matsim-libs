package freight;

import java.util.Collection;

import playground.mzilske.freight.TSPOffer;

public class ScheduledCommodityFlow {
	
	private CommodityFlow commodityFlow;
	
	private Collection<ShipperShipment> shipments;
	
	private TSPOffer tspOffer;

	public ScheduledCommodityFlow(CommodityFlow commodityFlow, Collection<ShipperShipment> shipments, TSPOffer tspOffer) {
		super();
		this.commodityFlow = commodityFlow;
		this.shipments = shipments;
		this.tspOffer = tspOffer;
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

}
