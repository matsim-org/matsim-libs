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


public class WorkLengthAfterMode {
	
	EventsManager events = (EventsManager) EventsUtils.createEventsManager();
    EventsReaderXMLv1 reader = new EventsReaderXMLv1(events);


	public void run(String s){
    	
		Purpose purpose = new Purpose();
    	
    	events.addHandler(purpose);
    	reader.parse(s);
    	int[] purposeSplit = purpose.purposeSplitCar();
    	
    	for (int i = 0; i < 5; i++) { 
    		System.out.println((double)purposeSplit[i]/(double)purpose.numberOfTripsCar() * 100.0);
						
    	}
    	System.out.println(purpose.numberOfTripsCar());
    	
    	System.out.println(purpose.averageWorkTimeCar());
    	
System.out.println(purpose.numberOfTripsBike());
    	
    	System.out.println(purpose.averageWorkTimeBike());
    	
System.out.println(purpose.numberOfTripsWalk());
    	
    	System.out.println(purpose.averageWorkTimeWalk());
    	
System.out.println(purpose.numberOfTripsPt());
    	
    	System.out.println(purpose.averageWorkTimePt());
    }
	private static class Purpose implements ActivityStartEventHandler, PersonArrivalEventHandler, ActivityEndEventHandler {

		int[] purposeSplitCar = new int[5];
		int[] purposeSplitBike = new int[5];
		int[] purposeSplitWalk = new int[5];
		int[] purposeSplitPt = new int[5];
		int countCar = 0;
		int countWalk = 0;
		int countBike = 0;
		int countPt = 0;
		HashMap<Id, Boolean> mapFixCar = new HashMap<Id, Boolean>();
		HashMap<Id, Boolean> mapFixBike = new HashMap<Id, Boolean>();
		HashMap<Id, Boolean> mapFixWalk = new HashMap<Id, Boolean>();
		HashMap<Id, Boolean> mapFixPt = new HashMap<Id, Boolean>();
		HashMap<Id, Double> startTimesCar = new HashMap<Id, Double>();
		HashMap<Id, Double> startTimesBike = new HashMap<Id, Double>();
		HashMap<Id, Double> startTimesWalk = new HashMap<Id, Double>();
		HashMap<Id, Double> startTimesPt = new HashMap<Id, Double>();
		double timeCar = 0.0;
		int numberCar = 0;
		double timeBike = 0.0;
		int numberBike = 0;
		double timeWalk = 0.0;
		int numberWalk = 0;
		double timePt = 0.0;
		int numberPt = 0;
		@Override
		public void reset(int iteration) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void handleEvent(ActivityStartEvent event) {
			
			if (mapFixCar.get(event.getPersonId())) {
				
				if (event.getActType().startsWith("work")){
					purposeSplitCar[0]++;
					startTimesCar.put(event.getPersonId(), event.getTime());
					countCar++; 
					}
				else if (event.getActType().startsWith("shop")){
						purposeSplitCar[1]++;
						countCar++; 
						}
				else if (event.getActType().startsWith("leisure")){
						purposeSplitCar[2]++;
						countCar++; 
						}
				else if (event.getActType().startsWith("education")){
						purposeSplitCar[3]++;
						countCar++; 
						}
				else if (event.getActType().startsWith("home")) {
						purposeSplitCar[4]++;
				
						countCar++; 
				}
				
			}
			
			if (mapFixBike.get(event.getPersonId())) {
				
				if (event.getActType().startsWith("work")){
					purposeSplitBike[0]++;
					startTimesBike.put(event.getPersonId(), event.getTime());
					countBike++; 
					}
				else if (event.getActType().startsWith("shop")){
						purposeSplitBike[1]++;
						countBike++; 
						}
				else if (event.getActType().startsWith("leisure")){
						purposeSplitBike[2]++;
						countBike++; 
						}
				else if (event.getActType().startsWith("education")){
						purposeSplitBike[3]++;
						countBike++; 
						}
				else if (event.getActType().startsWith("home")) {
						purposeSplitBike[4]++;
				
						countBike++; 
				}
				
			}

			if (mapFixWalk.get(event.getPersonId())) {
	
	if (event.getActType().startsWith("work")){
		purposeSplitWalk[0]++;
		startTimesWalk.put(event.getPersonId(), event.getTime());
		countWalk++; 
		}
	else if (event.getActType().startsWith("shop")){
			purposeSplitWalk[1]++;
			countWalk++; 
			}
	else if (event.getActType().startsWith("leisure")){
			purposeSplitWalk[2]++;
			countWalk++; 
			}
	else if (event.getActType().startsWith("education")){
			purposeSplitWalk[3]++;
			countWalk++; 
			}
	else if (event.getActType().startsWith("home")) {
			purposeSplitWalk[4]++;
	
	countWalk++; 
	}
	
}

if (mapFixPt.get(event.getPersonId())) {
	
	if (event.getActType().startsWith("work")){
		purposeSplitPt[0]++;
		startTimesPt.put(event.getPersonId(), event.getTime());
		countPt++; 
		}
	else if (event.getActType().startsWith("shop")){
			purposeSplitPt[1]++;
			countPt++; 
			}
	else if (event.getActType().startsWith("leisure")){
			purposeSplitPt[2]++;
			countPt++; 
			}
	else if (event.getActType().startsWith("education")){
			purposeSplitPt[3]++;
			countPt++; 
			}
	else if (event.getActType().startsWith("home")) {
			purposeSplitPt[4]++;
	
	countPt++; 
	}
	
}
			
		}
		
		public int numberOfTripsCar() {
			
			return numberCar;
		}
public int numberOfTripsPt() {
			
			return numberPt;
		}
public int numberOfTripsWalk() {
	
	return numberWalk;
}
public int numberOfTripsBike() {
	
	return numberBike;
}
		
		public int[] purposeSplitCar() {
			
			return purposeSplitCar;
		}
public int[] purposeSplitBike() {
			
			return purposeSplitBike;
		}
public int[] purposeSplitPt() {
	
	return purposeSplitPt;
}
public int[] purposeSplitWalk() {
	
	return purposeSplitWalk;
}

		@Override
		public void handleEvent(PersonArrivalEvent event) {

			if (event.getLegMode().equals("car")) {
				
				mapFixCar.put(event.getPersonId(), true);
				
			}
			else
				mapFixCar.put(event.getPersonId(), false);
			
			if (event.getLegMode().equals("bike")) {
				
				mapFixBike.put(event.getPersonId(), true);
				
			}
			else
				mapFixBike.put(event.getPersonId(), false);
			
			if (event.getLegMode().equals("walk")) {
				
				mapFixWalk.put(event.getPersonId(), true);
				
			}
			else
				mapFixWalk.put(event.getPersonId(), false);
			
			if (event.getLegMode().equals("transit_walk" ) || event.getLegMode().equals("pt" )) {
	
				mapFixPt.put(event.getPersonId(), true);
	
			}
			else
				mapFixPt.put(event.getPersonId(), false);
				
		}

		@Override
		public void handleEvent(ActivityEndEvent event) {
			// TODO Auto-generated method stub
			
			if (startTimesCar.get(event.getPersonId()) != null) {
				
				timeCar += (event.getTime() - startTimesCar.get(event.getPersonId()));
				numberCar++;
				startTimesCar.remove(event.getPersonId());
				
			}
			
			if (startTimesBike.get(event.getPersonId()) != null) {
				
				timeBike += (event.getTime() - startTimesBike.get(event.getPersonId()));
				numberBike++;
				startTimesBike.remove(event.getPersonId());
				
			}
			if (startTimesWalk.get(event.getPersonId()) != null) {
	
				timeWalk += (event.getTime() - startTimesWalk.get(event.getPersonId()));
				numberWalk++;
				startTimesWalk.remove(event.getPersonId());
	
			}
			if (startTimesPt.get(event.getPersonId()) != null) {
	
				timePt += (event.getTime() - startTimesPt.get(event.getPersonId()));
				numberPt++;
				startTimesPt.remove(event.getPersonId());
	
			}
			
		}
		
		public double averageWorkTimeCar() {
			
			return timeCar/(double)numberCar;
		}
public double averageWorkTimeBike() {
			
			return timeBike/(double)numberBike;
		}
public double averageWorkTimeWalk() {
	
	return timeWalk/(double)numberWalk;
}
public double averageWorkTimePt() {
	
	return timePt/(double)numberPt;
}
		
	}
	
	public static void main(String[] args) {

		WorkLengthAfterMode cp = new WorkLengthAfterMode();
		
		String eventsFilePath = args[0]; 
		
		
		cp.run(eventsFilePath);
		
	}

}
