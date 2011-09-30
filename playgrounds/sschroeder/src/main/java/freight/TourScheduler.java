package freight;

import java.util.Collection;
import java.util.Map;

import playground.mzilske.freight.carrier.CarrierShipment;
import playground.mzilske.freight.carrier.ScheduledTour;
import playground.mzilske.freight.carrier.Tour;

public interface TourScheduler {
	
	public Collection<ScheduledTour> getScheduledTours(Collection<Tour> tours, Map<CarrierShipment, Collection<CarrierShipment>> aggregatedShipments);
	
	public void reset();

}
