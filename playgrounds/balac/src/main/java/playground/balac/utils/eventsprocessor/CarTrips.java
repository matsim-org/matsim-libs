package playground.balac.utils.eventsprocessor;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;


public class CarTrips implements ActivityEndEventHandler, ActivityStartEventHandler, PersonEntersVehicleEventHandler,
								PersonLeavesVehicleEventHandler{

	Set<String> vehicles = new TreeSet<String>();
		
	int trips = 0;
	double times = 0.0;
	Map<String,Double> enterTimes = new HashMap<String, Double>();
	
	@Override
	public void reset(int iteration) {
		
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {		
		
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {		
		
	}
	
	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
				
		if (!event.getVehicleId().toString().startsWith("FF")) {
			enterTimes.put(event.getVehicleId().toString(), event.getTime());
			
		}
	}
	
	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		if (!event.getVehicleId().toString().startsWith("FF")) {

			times += event.getTime() - enterTimes.get(event.getVehicleId().toString());
			vehicles.add(event.getVehicleId().toString());
			trips++;
		}
	}
	
	public double avgTripLength() {
		
		return times/trips;
	}
	
	public double avgTripsPerCar() {
		
		return (double)trips/(double)vehicles.size();
	}
	
	public static void main(String[] args) throws IOException {
		//final BufferedReader readLink = IOUtils.getBufferedReader("C:/Users/balacm/Desktop/garageParkingIds.txt");

		EventsManager events = EventsUtils.createEventsManager();
		
	
		CarTrips occ = new CarTrips();
		events.addHandler(occ); 

		EventsReaderXMLv1 reader = new EventsReaderXMLv1(events);
		reader.parse(args[0]);
		
		System.out.println(occ.avgTripLength());
		System.out.println(occ.avgTripsPerCar());
		
	}

	
}
