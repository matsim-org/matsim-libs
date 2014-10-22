package playground.balac.utils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

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
import org.matsim.core.utils.io.IOUtils;



public class PTTravelTimesFromEvents {

	EventsManager events = (EventsManager) EventsUtils.createEventsManager();
    EventsReaderXMLv1 reader = new EventsReaderXMLv1(events);
    public void run(String s) throws IOException{
    	
		Purpose purpose = new Purpose();
    	
    	events.addHandler(purpose);
    	reader.parse(s);
    	
    	
    	HashMap<Id, ArrayList<playground.balac.utils.PTTravelTimesFromEvents.Purpose.TripInfo>> results = purpose.results();
		final BufferedWriter outLink = IOUtils.getBufferedWriter("C:/Users/balacm/Desktop/InputPt/StatisticsPt.txt");
		
		
		for(ArrayList<playground.balac.utils.PTTravelTimesFromEvents.Purpose.TripInfo> a: results.values()) {
			int count = 0;
			for (int i = 0; i < a.size(); i++)
				if (a.get(i).description.equals("pt"))
					count++;
			int numberOfTransfers = -1 + count;
			
			outLink.write(a.get(0).personId.toString() + " ");
			outLink.write(Integer.toString(numberOfTransfers) + " ");
			double firstWaitingTIme = 0.0;
			if (numberOfTransfers != -1) {
				
				firstWaitingTIme = a.get(1).startTime -  a.get(0).endTime;
				
				outLink.write(Double.toString(a.get(1).startTime -  a.get(0).endTime) + " ");
				
				outLink.write(Double.toString(a.get(a.size() - 1).endTime - a.get(0).startTime) + " ");
				
				outLink.write(Double.toString(a.get(0).endTime - a.get(0).startTime) + " ");
				
				outLink.write(Double.toString(a.get(a.size() - 1).endTime - a.get(a.size() - 1).startTime) + " ");
				
			}
			else {
				
				firstWaitingTIme = 0.0;
				
				outLink.write(Double.toString(0.0) + " ");
				outLink.write(Double.toString(a.get(0).endTime - a.get(0).startTime) + " ");
				
				outLink.write(Double.toString(0.0) + " ");
				
				outLink.write(Double.toString(0.0) + " ");
			}
			
			
			if (a.size() >= 3) {
				double time1 = 0.0;
				double transferTime = 0.0;
				for (playground.balac.utils.PTTravelTimesFromEvents.Purpose.TripInfo t: a) {
				
				
					if (t.description.equals("pt")) {
						
						if (time1 == 0.0) {
							
							time1 = t.endTime;
						}
						else {
							transferTime += t.startTime - time1;
							time1 = t.endTime;
						}
						
					}
					
					
				}
				outLink.write(Double.toString(transferTime + firstWaitingTIme));
			}
			else {
				outLink.write(Double.toString(firstWaitingTIme));
			}
			outLink.newLine();
			
		}
		outLink.flush();
		outLink.close();

    }
    
    

	private static class Purpose implements ActivityStartEventHandler, PersonArrivalEventHandler, ActivityEndEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler, PersonDepartureEventHandler {

		HashMap<Id, Boolean> mapFix = new HashMap<Id, Boolean>();
		HashMap<Id, Double> travelTImes = new HashMap<Id, Double>();
		HashMap<Id, ArrayList<TripInfo>> mapa = new HashMap<Id, ArrayList<TripInfo>>();
		
		public HashMap<Id, ArrayList<TripInfo>> results () {
			
			return mapa;
		}
		
		@Override
		public void reset(int iteration) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void handleEvent(ActivityStartEvent event) {
			if (event.getPersonId().toString().equals("14344"))
				System.out.print("");
			 if (event.getActType().startsWith("leisure")){
				 mapFix.put(event.getPersonId(), true);
				 travelTImes.put(event.getPersonId(), event.getTime() - travelTImes.get(event.getPersonId())); 
			}
				
		}

		@Override
		public void handleEvent(PersonLeavesVehicleEvent event) {
			// TODO Auto-generated method stub
			if (mapFix.get(event.getPersonId()) != null && mapFix.get(event.getPersonId()) == false)
				mapa.get(event.getPersonId()).get(mapa.get(event.getPersonId()).size() - 1).endTime = event.getTime();
		}

		@Override
		public void handleEvent(PersonEntersVehicleEvent event) {
			// TODO Auto-generated method stub
			if (event.getPersonId().toString().equals("14344"))
				System.out.print("");
			if (mapFix.get(event.getPersonId()) != null && mapFix.get(event.getPersonId()) == false) {
			
				TripInfo ti = new TripInfo();
				ti.startTime = event.getTime();
				ti.description = "pt";
				ti.personId = event.getPersonId();
				if (mapa.get(event.getPersonId()) == null) {
					ArrayList<TripInfo> a = new ArrayList<TripInfo>();
					a.add(ti);
					mapa.put(event.getPersonId(), a);
				}
				else {
					mapa.get(event.getPersonId()).add(ti);
				}
			
			}
		}

		@Override
		public void handleEvent(ActivityEndEvent event) {
			// TODO Auto-generated method stub
			if (event.getPersonId().toString().equals("14344"))
				System.out.print("");
			if (travelTImes.get(event.getPersonId()) == null) {
				
				if (event.getActType().equals("home")) {
					mapFix.put(event.getPersonId(), false);
					travelTImes.put(event.getPersonId(), event.getTime());
				}
			}
			
		}			
		

		@Override
		public void handleEvent(PersonArrivalEvent event) {

			if (mapFix.get(event.getPersonId()) != null && mapFix.get(event.getPersonId()) == false && event.getLegMode().equals("transit_walk")) {
				
				mapa.get(event.getPersonId()).get(mapa.get(event.getPersonId()).size() - 1).endTime = event.getTime();
				
			}
				
		}
		
		@Override
		public void handleEvent(PersonDepartureEvent event) {
			// TODO Auto-generated method stub
			
			if (mapFix.get(event.getPersonId()) != null && mapFix.get(event.getPersonId()) == false && event.getLegMode().equals("transit_walk")) {
				
				TripInfo ti = new TripInfo();
				ti.startTime = event.getTime();
				ti.description = event.getLegMode();
				ti.personId = event.getPersonId();
				if (mapa.get(event.getPersonId()) == null) {
					ArrayList<TripInfo> a = new ArrayList<TripInfo>();
					a.add(ti);
					mapa.put(event.getPersonId(), a);
				}
				else {
					mapa.get(event.getPersonId()).add(ti);
				}
				
			}
			
		}
		
		public class TripInfo {
			private Id personId = null;
			private double startTime = 0.0;
			private double endTime = 0.0;
			private String description = null;
			public String toString() {
				
				return personId + " " + Double.toString(startTime) + " " + Double.toString(endTime) + " " +
				description;
			}
		}
	}
    
	
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		
		PTTravelTimesFromEvents cp = new PTTravelTimesFromEvents();
		
		String eventsFilePath = args[0]; 		
		
		cp.run(eventsFilePath);		
		

	}

}
