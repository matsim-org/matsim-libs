package freight.utils;

import org.matsim.contrib.freight.carrier.Shipment;

import java.util.Collection;
import java.util.Map;

public interface ShipmentClusterer {
	
	public Map<TimePeriod,Collection<Shipment>> clusterShipments(Collection<Shipment> shipments);

}
