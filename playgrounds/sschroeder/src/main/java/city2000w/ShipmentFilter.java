package city2000w;

import playground.mzilske.freight.carrier.Shipment;

public interface ShipmentFilter {
	public boolean judge(Shipment shipment);
}
