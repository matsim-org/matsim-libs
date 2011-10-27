package city2000w;

import org.matsim.contrib.freight.carrier.Shipment;

public interface ShipmentFilter {
	public boolean judge(Shipment shipment);
}
