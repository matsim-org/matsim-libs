package playground.wrashid.fd.check;

import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;

public class OutflowAndDensityStats {

	
		public static void main(String[] args) {
			String inputEventsFilePath="C:/data/workspace3/playgrounds/wrashid/events.xml/events_filtered2.xml";
			CountNumberOfAgents countNumberOfAgents = new CountNumberOfAgents();
			EventsManager events = EventsUtils.createEventsManager();


			events.addHandler(countNumberOfAgents);

			MatsimEventsReader reader = new MatsimEventsReader(events);
			reader.readFile(inputEventsFilePath);
			countNumberOfAgents.printStats();

	}

	static class CountNumberOfAgents implements LinkLeaveEventHandler,
	LinkEnterEventHandler, PersonArrivalEventHandler,
	VehicleEntersTrafficEventHandler, PersonDepartureEventHandler {
		int wait2Link;
		int arrival;
		int departure;
		int enter;
		int leave;
		
		public void printStats(){
			System.out.println("wait2Link:" + wait2Link);
			System.out.println("arrival:" + arrival);
			System.out.println("departure:" + departure);
			System.out.println("enter:" + enter);
			System.out.println("leave:" + leave);
		}

		@Override
		public void reset(int iteration) {
			
		}

		@Override
		public void handleEvent(VehicleEntersTrafficEvent event) {
			wait2Link++;			
		}

		@Override
		public void handleEvent(PersonArrivalEvent event) {
			arrival++;
		}

		@Override
		public void handleEvent(LinkEnterEvent event) {
			enter++;
		}

		@Override
		public void handleEvent(LinkLeaveEvent event) {
			leave++;
		}

		@Override
		public void handleEvent(PersonDepartureEvent event) {
			departure++;			
		}
		
	}
	
}
