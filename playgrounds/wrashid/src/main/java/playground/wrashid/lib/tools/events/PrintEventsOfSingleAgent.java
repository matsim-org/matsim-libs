package playground.wrashid.lib.tools.events;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonMoneyEventHandler;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;

public class PrintEventsOfSingleAgent {

	public static void main(String[] args) {
		//String eventsFile="C:/data/parkingSearch/psim/berlin/ITERS/it.50/50.events.xml.gz";
		//String eventsFile="C:/data/parkingSearch/psim/output/all/events.xml.gz";
		
		//String eventsFile="H:/data/experiments/TRBAug2011/runs/ktiRun24/output/ITERS/it.50/50.events.xml.gz";
		String eventsFile="C:/data/parkingSearch/psim/zurich/output/basic output with 300 sec bins/events.xml.gz";
		
		
		EventsManager events = EventsUtils.createEventsManager();

		SingleAgentEventsPrinter singleAgentEventsPrinter = new SingleAgentEventsPrinter(Id.create("65802", Person.class));
		
		events.addHandler(singleAgentEventsPrinter);
		
		//EventsReaderTXTv1 reader = new EventsReaderTXTv1(events);
		//reader.readFile(eventsFile);
		
		EventsReaderXMLv1 reader = new EventsReaderXMLv1(events);
		reader.parse(eventsFile);
		
	}
	
	
	
}
