package playground.ciarif.retailers;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.Id;
import org.matsim.utils.geometry.Coord;
import org.matsim.events.ActEndEvent;
import org.matsim.events.ActStartEvent;
import org.matsim.events.handler.ActEndEventHandler;
import org.matsim.events.handler.ActStartEventHandler;
import org.matsim.facilities.*;
import org.matsim.locationchoice.facilityload.EventsToFacilityLoad;
import org.matsim.locationchoice.facilityload.FacilityPenalty;

public class FacilityRelocator implements ActStartEventHandler, ActEndEventHandler {


private TreeMap<Id, FacilityRelocator> newRetailersLocation;
private final static Logger log = Logger.getLogger(EventsToFacilityLoad.class); //Chiedere Andreas perch� si puo/deve usare questo logger!!!
	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////
	
	public FacilityRelocator(Facilities facilities, TreeMap<Id, FacilityRelocator> newRetailersLocation  ) {
		super();
		
		this.newRetailersLocation = newRetailersLocation;
		
		log.info(facilities.getFacilities().values().size() +"facilities size"); //Chiedere Andreas, vedi sopra
		Iterator<? extends Facility> iter_fac = facilities.getFacilities().values().iterator();
		
		while (iter_fac.hasNext()){
			Facility f = iter_fac.next();
			for(int i=0;i<newRetailersLocation.size(); i=i+1) {
			
			}
		}
	}
	
	//////////////////////////////////////////////////////////////////////
	// run method
	//////////////////////////////////////////////////////////////////////

	public void run(Facilities facilities) {
		
	}

	public void handleEvent(ActStartEvent event) {
		// TODO Auto-generated method stub
		
	}

	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}

	public void handleEvent(ActEndEvent event) {
		// TODO Auto-generated method stub
		
	}

}
	
	