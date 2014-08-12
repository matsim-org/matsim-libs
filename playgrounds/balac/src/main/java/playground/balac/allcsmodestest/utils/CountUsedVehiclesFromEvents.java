package playground.balac.allcsmodestest.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;



public class CountUsedVehiclesFromEvents {
	EventsManager events = (EventsManager) EventsUtils.createEventsManager();
    EventsReaderXMLv1 reader = new EventsReaderXMLv1(events);
    public void run(String s) throws IOException{
    	
		Purpose purpose = new Purpose();
    	
    	events.addHandler(purpose);
    	reader.parse(s);
    	System.out.println(purpose.IDS().size());
    }
    
    private static class Purpose implements   PersonEntersVehicleEventHandler {
		Set<String> fahrzugIDs = new TreeSet<String>();

		public Set<String> IDS() {
			return fahrzugIDs;
		}
	
		@Override
		public void reset(int iteration) {
			// TODO Auto-generated method stub
			
		}

	
		@Override
		public void handleEvent(PersonEntersVehicleEvent event) {
			// TODO Auto-generated method stub
			if (event.getVehicleId().toString().startsWith("TW")) {
				
				fahrzugIDs.add(event.getVehicleId().toString());
				
			}
		}
	
		
	}
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		CountUsedVehiclesFromEvents c = new  CountUsedVehiclesFromEvents();
		c.run(args[0]);
	}

}
