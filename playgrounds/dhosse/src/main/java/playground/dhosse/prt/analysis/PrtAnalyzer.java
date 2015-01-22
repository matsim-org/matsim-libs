package playground.dhosse.prt.analysis;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;

public class PrtAnalyzer {
	
	public static void main(String args[]){
		
		EventsManager events = EventsUtils.createEventsManager();
		PrtEventsHandler handler = new PrtEventsHandler();
		events.addHandler(handler);
		
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile("C:/Users/Daniel/Desktop/dvrp/events.xml");
		
		double meanWaitTime = handler.diff / handler.counter;
		
		System.out.println("max: " + handler.max + "\tmin: " + handler.min + "\tmean: " + meanWaitTime + " [s]");
		
	}

}
