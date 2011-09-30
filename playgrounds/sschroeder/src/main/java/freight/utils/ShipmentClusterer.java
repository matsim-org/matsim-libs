package freight.utils;

import java.util.Collection;
import java.util.Map;

import playground.mzilske.freight.carrier.Shipment;

public interface ShipmentClusterer {
	
	public Map<TimePeriod,Collection<Shipment>> clusterShipments(Collection<Shipment> shipments);

}
