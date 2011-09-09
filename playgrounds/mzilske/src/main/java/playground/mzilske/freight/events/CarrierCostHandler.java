package playground.mzilske.freight.events;

import playground.mzilske.freight.Shipment;

public interface CarrierCostHandler {
	public void informCost(Shipment shipment, Double cost);
}
