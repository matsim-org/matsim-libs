package playground.ciarif.retailers;

import java.util.Iterator;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.Id;
import org.matsim.events.ActEndEvent;
import org.matsim.events.ActStartEvent;
import org.matsim.events.handler.ActEndEventHandler;
import org.matsim.events.handler.ActStartEventHandler;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.Facility;
import org.matsim.locationchoice.facilityload.EventsToFacilityLoad;
import org.matsim.locationchoice.facilityload.FacilityPenalty;

public class EventsToFacilityRelocate {//implements ActStartEventHandler, ActEndEventHandler {

	private TreeMap<Id, NewRetailerLocation> facilityLocations;
	private final static Logger log = Logger.getLogger(EventsToFacilityLoad.class);
	private Facilities facilities;
	
	public EventsToFacilityRelocate(Facilities facilities,
			TreeMap<Id, NewRetailerLocation> newRetailersLocations) {
		super();
		
		this.facilityLocations = facilityLocations;
		
		log.info(facilities.getFacilities().values().size() +"facilities size");
		Iterator<? extends Facility> iter_fac = facilities.getFacilities().values().iterator();
		
		while (iter_fac.hasNext()){
			Facility f = iter_fac.next();
			//this.facilities.// Here one single facility should be copied and then deleted.
		}
				
	}

//	@Override
//	public void handleEvent(ActStartEvent event) {
//		// TODO Auto-generated method stub
//		
//	}
//
//	@Override
//	public void reset(int iteration) {
//		// TODO Auto-generated method stub
//		
//	}
//
//	@Override
//	public void handleEvent(ActEndEvent event) {
//		// TODO Auto-generated method stub
//		
//	}

}
