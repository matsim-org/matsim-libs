package freight.utils;

import java.util.Collection;
import java.util.Map;

import playground.mzilske.freight.CarrierShipment;

public interface GreedyShipmentAggregator {
	
	public Map<CarrierShipment,Collection<CarrierShipment>> aggregateShipments(Collection<CarrierShipment> shipments);
	
	public void reset();

}
