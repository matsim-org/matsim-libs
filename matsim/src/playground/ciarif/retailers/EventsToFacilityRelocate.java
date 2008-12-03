package playground.ciarif.retailers;

import java.util.TreeMap;

import org.matsim.basic.v01.Id;
import org.matsim.events.ActEndEvent;
import org.matsim.events.ActStartEvent;
import org.matsim.events.handler.ActEndEventHandler;
import org.matsim.events.handler.ActStartEventHandler;
import org.matsim.facilities.Facilities;

public class EventsToFacilityRelocate implements ActStartEventHandler, ActEndEventHandler {

	public EventsToFacilityRelocate(Facilities facilities,
			TreeMap<Id, NewRetailerLocation> newRetailersLocations) {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void handleEvent(ActStartEvent event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleEvent(ActEndEvent event) {
		// TODO Auto-generated method stub
		
	}

}
