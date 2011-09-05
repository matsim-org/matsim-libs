package freight;

import java.util.Collection;
import java.util.Map;

import playground.mzilske.freight.Shipment;

public interface GreedyShipmentAggregator {
	public Map<Shipment,Collection<Shipment>> aggregateShipments(Collection<Shipment> shipments);
	
	public void reset();

}
