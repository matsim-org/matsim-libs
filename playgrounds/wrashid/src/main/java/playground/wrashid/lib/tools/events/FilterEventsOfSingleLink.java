package playground.wrashid.lib.tools.events;

import org.matsim.api.core.v01.Id;
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
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
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
		EventsFilter eventsFilter = new EventsFilter(outputEventsFilePath, Id.create("17560001228443FT", Link.class));
		EventsManager events = EventsUtils.createEventsManager();


		events.addHandler(eventsFilter);

		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(inputEventsFilePath);

		eventsFilter.closeFile();

	}
	
	private static class EventsFilter implements PersonDepartureEventHandler, PersonArrivalEventHandler,VehicleEntersTrafficEventHandler,LinkEnterEventHandler,LinkLeaveEventHandler {
		private Id<Link> filterLinkId;
		private EventWriterXML eventWriter;

		public EventsFilter(String outputFileName, Id<Link> filterLinkId) {
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
		public void handleEvent(PersonDepartureEvent event) {
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
		public void handleEvent(VehicleEntersTrafficEvent event) {
			if (event.getLinkId().toString().equalsIgnoreCase(filterLinkId.toString())){
				eventWriter.handleEvent(event);
			}			
		}

		@Override
		public void handleEvent(PersonArrivalEvent event) {
			if (event.getLinkId().toString().equalsIgnoreCase(filterLinkId.toString())){
				eventWriter.handleEvent(event);
			}			
		}


	}

}
