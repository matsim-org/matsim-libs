package playground.wrashid.tryouts.events;

import java.util.HashMap;
import java.util.Iterator;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;


public class EventStatistics implements PersonArrivalEventHandler, PersonDepartureEventHandler  {

	HashMap<Id,Double> legStartTime=new HashMap<Id,Double>();
	HashMap<Id,Double> totalTravelTime=new HashMap<Id,Double>();
	
	

	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}

	
	
	public void printTotalTraveTimes(){
		Iterator<Id> iter=totalTravelTime.keySet().iterator();
		while (iter.hasNext()){
			Id personId=iter.next();
			if (personId!=null){
				System.out.println(personId + " - " + totalTravelTime.get(personId));
			}
		}
	}
	
	public static void printTotalTraveTimesAll(EventStatistics e1, EventStatistics e2){
		Iterator<Id> iter=e1.getTotalTravelTimeKeyIterator();
		while (iter.hasNext()){
			Id personId=iter.next();
			if (personId!=null){
				System.out.println(personId + " - " + e1.getTotalTravelTime(personId) + " - "  +  e2.getTotalTravelTime(personId));
			}
		}
	}

	public void handleEvent(PersonDepartureEvent event) {
		// TODO Auto-generated method stub
		legStartTime.put(event.getPersonId(), event.getTime());
	}

	public void handleEvent(PersonArrivalEvent event) {
		// TODO Auto-generated method stub
		double tripTravelTime=event.getTime() -  legStartTime.get(event.getPersonId());
		double travelTimeSum=0;
		if (totalTravelTime.get(event.getPersonId())!=null){
			travelTimeSum=totalTravelTime.get(event.getPersonId());
		}
		travelTimeSum+=tripTravelTime;
		totalTravelTime.put(event.getPersonId(), travelTimeSum);
		
	}
	
	public double getTotalTravelTime(Id personId){
		if (totalTravelTime.get(personId)==null){
			System.out.println(personId);
		}
		return totalTravelTime.get(personId);
	}
	
	public Iterator<Id> getTotalTravelTimeKeyIterator(){
		return totalTravelTime.keySet().iterator();
	}
	
	public static double compareAgentTripDuration(Id personId, EventStatistics e1, EventStatistics e2){
		return e1.getTotalTravelTime(personId) - e2.getTotalTravelTime(personId);
	}

}
