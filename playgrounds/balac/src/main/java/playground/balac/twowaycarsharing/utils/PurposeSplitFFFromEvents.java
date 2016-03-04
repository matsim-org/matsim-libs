package playground.balac.twowaycarsharing.utils;

import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;


public class PurposeSplitFFFromEvents {
	
	EventsManager events = (EventsManager) EventsUtils.createEventsManager();
    EventsReaderXMLv1 reader = new EventsReaderXMLv1(events);


	public void run(String s){
    	
		Purpose purpose = new Purpose();
    	
    	events.addHandler(purpose);
    	reader.parse(s);
    	int[] purposeSplit = purpose.purposeSplit();
    	
    	for (int i = 0; i < 5; i++) { 
    		System.out.println((double)purposeSplit[i]/(double)purpose.numberOfTrips() * 100.0);
						
    	}
    	System.out.println(purpose.numberOfTrips());
    	
    	System.out.println(purpose.averageWorkTime());
    }
	private static class Purpose implements ActivityStartEventHandler, PersonArrivalEventHandler, ActivityEndEventHandler {

		int[] purposeSplit = new int[5];
		int count = 0;
		HashMap<Id, Boolean> mapFix = new HashMap<Id, Boolean>();
		HashMap<Id, Double> startTimes = new HashMap<Id, Double>();
		double time = 0.0;
		int number = 0;
		@Override
		public void reset(int iteration) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void handleEvent(ActivityStartEvent event) {
			
			if (mapFix.get(event.getPersonId())) {
				
				if (event.getActType().startsWith("work")){
					purposeSplit[0]++;
					startTimes.put(event.getPersonId(), event.getTime());
					count++; 
					}
				else if (event.getActType().startsWith("shop")){
						purposeSplit[1]++;
						count++; 
						}
				else if (event.getActType().startsWith("leisure")){
						purposeSplit[2]++;
						count++; 
						}
				else if (event.getActType().startsWith("education")){
						purposeSplit[3]++;
						count++; 
						}
				else if (event.getActType().startsWith("home")) {
						purposeSplit[4]++;
				
				count++; 
				}
				
			}
			
		}
		
		public int numberOfTrips() {
			
			return count;
		}
		
		public int[] purposeSplit() {
			
			return purposeSplit;
		}
		

		@Override
		public void handleEvent(PersonArrivalEvent event) {

			if (event.getLegMode().equals("walk_ff")) {
				
				mapFix.put(event.getPersonId(), true);
				
			}
			else
				mapFix.put(event.getPersonId(), false);
				
		}

		@Override
		public void handleEvent(ActivityEndEvent event) {
			// TODO Auto-generated method stub
			
			if (startTimes.get(event.getPersonId()) != null) {
				
				time += (event.getTime() - startTimes.get(event.getPersonId()));
				number++;
				startTimes.remove(event.getPersonId());
				
			}
			
		}
		
		public double averageWorkTime() {
			
			return time/(double)number;
		}
		
		
	}
	
	public static void main(String[] args) {

		PurposeSplitFFFromEvents cp = new PurposeSplitFFFromEvents();
		
		String eventsFilePath = args[0]; 
		
		
		cp.run(eventsFilePath);
		
	}

}
