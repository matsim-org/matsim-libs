package freight;

import java.util.Collection;
import java.util.Map;

import org.matsim.contrib.freight.carrier.CarrierShipment;
import org.matsim.contrib.freight.carrier.ScheduledTour;
import org.matsim.contrib.freight.carrier.Tour;

public interface TourScheduler {
	
	public Collection<ScheduledTour> getScheduledTours(Collection<Tour> tours, Map<CarrierShipment, Collection<CarrierShipment>> aggregatedShipments);
	
	public void reset();

}
