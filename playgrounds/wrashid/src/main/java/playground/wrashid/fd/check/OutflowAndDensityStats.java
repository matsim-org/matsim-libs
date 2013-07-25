package playground.wrashid.fd.check;

import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.AgentWait2LinkEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentWait2LinkEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.basic.v01.IdImpl;
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
	LinkEnterEventHandler, AgentArrivalEventHandler,
	AgentWait2LinkEventHandler, AgentDepartureEventHandler {
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
		public void handleEvent(AgentWait2LinkEvent event) {
			wait2Link++;			
		}

		@Override
		public void handleEvent(AgentArrivalEvent event) {
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
		public void handleEvent(AgentDepartureEvent event) {
			departure++;			
		}
		
	}
	
}
