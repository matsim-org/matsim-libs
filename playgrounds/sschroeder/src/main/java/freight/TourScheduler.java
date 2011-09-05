package freight;

import java.util.Collection;
import java.util.Map;

import playground.mzilske.freight.ScheduledTour;
import playground.mzilske.freight.Shipment;
import playground.mzilske.freight.Tour;

public interface TourScheduler {
	
	public Collection<ScheduledTour> getScheduledTours(Collection<Tour> tours, Map<Shipment, Collection<Shipment>> aggregatedShipments);
	
	public void reset();

}
