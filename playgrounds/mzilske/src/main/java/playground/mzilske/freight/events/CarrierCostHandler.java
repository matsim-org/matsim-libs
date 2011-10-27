package playground.mzilske.freight.events;

import org.matsim.contrib.freight.carrier.Shipment;

public interface CarrierCostHandler {
	public void informCost(Shipment shipment, Double cost);
}
