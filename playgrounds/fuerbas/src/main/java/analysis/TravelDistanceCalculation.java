package analysis;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;

import tutorial.programming.example06EventsHandling.MyEventHandler1;

public class TravelDistanceCalculation {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		//path to events file
		String inputFile = "...";
		
		//create an event object
		EventsManager events = new EventsManagerImpl();
		
		//create the handler
		TravelDistanceHandler handler = new TravelDistanceHandler();
		
		//add the handler
		events.addHandler(handler);
		
		//create the reader and read the file
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(inputFile);
		System.out.println("Events file read!");
		
//		...

	}

}
