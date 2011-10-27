package freight.utils;

import org.matsim.contrib.freight.carrier.CarrierShipment;

import java.util.Collection;
import java.util.Map;

public interface GreedyShipmentAggregator {
	
	public Map<CarrierShipment,Collection<CarrierShipment>> aggregateShipments(Collection<CarrierShipment> shipments);
	
	public void reset();

}
