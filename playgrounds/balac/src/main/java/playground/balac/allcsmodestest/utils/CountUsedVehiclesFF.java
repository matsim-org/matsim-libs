package playground.balac.allcsmodestest.utils;

import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;

import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;



public class CountUsedVehiclesFF {
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
			if (event.getVehicleId().toString().startsWith("FF")) {
				
				fahrzugIDs.add(event.getVehicleId().toString());
				
			}
		}
	
		
	}
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		CountUsedVehiclesFF c = new  CountUsedVehiclesFF();
		c.run(args[0]);
	}

}
