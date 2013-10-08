package playground.wrashid.lib.tools.events;

import java.util.HashSet;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.AgentMoneyEvent;
import org.matsim.core.api.experimental.events.AgentStuckEvent;
import org.matsim.core.api.experimental.events.Wait2LinkEvent;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.Wait2LinkEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.EventWriterXML;


public class FilterEventsOfSingleLink {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String inputEventsFilePath="\\\\kosrae.ethz.ch\\ivt-home\\simonimi\\thesis\\output_no_pricing_v5_subtours_JDEQSim_squeeze60\\ITERS\\it.50\\50.events.xml.gz";
		String outputEventsFilePath="C:/data/workspace3/playgrounds/wrashid/events.xml/events_filtered2.xml";
		EventsFilter eventsFilter = new EventsFilter(outputEventsFilePath, new IdImpl("17560001228443FT"));
		EventsManager events = EventsUtils.createEventsManager();


		events.addHandler(eventsFilter);

		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(inputEventsFilePath);

		eventsFilter.closeFile();

	}
	
	private static class EventsFilter implements AgentDepartureEventHandler, AgentArrivalEventHandler,Wait2LinkEventHandler,LinkEnterEventHandler,LinkLeaveEventHandler {
		private Id filterLinkId;
		private EventWriterXML eventWriter;

		public EventsFilter(String outputFileName, Id filterLinkId) {
			eventWriter = new EventWriterXML(outputFileName);
			this.filterLinkId = filterLinkId;
		}
		
		public void closeFile(){
			eventWriter.closeFile();
		}

		@Override
		public void reset(int iteration) {
			
		}

		@Override
		public void handleEvent(AgentDepartureEvent event) {
			if (event.getLinkId().toString().equalsIgnoreCase(filterLinkId.toString())){
				eventWriter.handleEvent(event);
			}
		}

		@Override
		public void handleEvent(LinkLeaveEvent event) {
			if (event.getLinkId().toString().equalsIgnoreCase(filterLinkId.toString())){
				eventWriter.handleEvent(event);
			}			
		}

		@Override
		public void handleEvent(LinkEnterEvent event) {
			if (event.getLinkId().toString().equalsIgnoreCase(filterLinkId.toString())){
				eventWriter.handleEvent(event);
			}			
		}

		@Override
		public void handleEvent(Wait2LinkEvent event) {
			if (event.getLinkId().toString().equalsIgnoreCase(filterLinkId.toString())){
				eventWriter.handleEvent(event);
			}			
		}

		@Override
		public void handleEvent(AgentArrivalEvent event) {
			if (event.getLinkId().toString().equalsIgnoreCase(filterLinkId.toString())){
				eventWriter.handleEvent(event);
			}			
		}


	}

}
