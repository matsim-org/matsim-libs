package freight;

import org.matsim.contrib.freight.carrier.CarrierShipment;
import org.matsim.contrib.freight.carrier.ScheduledTour;
import org.matsim.contrib.freight.carrier.Tour;

import java.util.Collection;
import java.util.Map;

public interface TourScheduler {
	
	public Collection<ScheduledTour> getScheduledTours(Collection<Tour> tours, Map<CarrierShipment, Collection<CarrierShipment>> aggregatedShipments);
	
	public void reset();

}
