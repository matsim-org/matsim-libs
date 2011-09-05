package city2000w;

import playground.mzilske.freight.Shipment;

public interface ShipmentFilter {
	public boolean judge(Shipment shipment);
}
