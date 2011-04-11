package playground.wrashid.lib.tools.events;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.AgentMoneyEvent;
import org.matsim.core.api.experimental.events.AgentStuckEvent;
import org.matsim.core.api.experimental.events.AgentWait2LinkEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.PersonEvent;
import org.matsim.core.api.experimental.events.handler.ActivityEndEventHandler;
import org.matsim.core.api.experimental.events.handler.ActivityStartEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentMoneyEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentStuckEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentWait2LinkEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.EventsReaderTXTv1;
import org.matsim.core.events.EventsUtils;

public class FilterEventsOfSingleAgent {

	public static void main(String[] args) {
		String eventsFile="H:/data/experiments/ARTEMIS/output/run10/ITERS/it.50/50.events.txt.gz";
		EventsManager events = (EventsManager) EventsUtils.createEventsManager();

		SingleAgentEventsPrinter singleAgentEventsPrinter = new SingleAgentEventsPrinter(new IdImpl("2791187"));
		
		events.addHandler(singleAgentEventsPrinter);
		
		EventsReaderTXTv1 reader = new EventsReaderTXTv1(events);
		
		reader.readFile(eventsFile);
		
	}
	
	private static class SingleAgentEventsPrinter implements ActivityEndEventHandler, ActivityStartEventHandler, AgentArrivalEventHandler, 
	AgentDepartureEventHandler, AgentStuckEventHandler, AgentMoneyEventHandler, 
	AgentWait2LinkEventHandler, LinkEnterEventHandler, LinkLeaveEventHandler{

		private final Id filterEventsForAgentId;

		public SingleAgentEventsPrinter(Id agentId){
			this.filterEventsForAgentId = agentId;
		}
		
		@Override
		public void reset(int iteration) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void handleEvent(LinkLeaveEvent event) {
			printEvent(event);
		}


		@Override
		public void handleEvent(LinkEnterEvent event) {
			printEvent(event);
		}

		@Override
		public void handleEvent(AgentWait2LinkEvent event) {
			printEvent(event);
		}

		@Override
		public void handleEvent(AgentMoneyEvent event) {
			printEvent(event);			
		}

		@Override
		public void handleEvent(AgentStuckEvent event) {
			printEvent(event);			
		}

		@Override
		public void handleEvent(AgentDepartureEvent event) {
			printEvent(event);			
		}

		@Override
		public void handleEvent(AgentArrivalEvent event) {
			printEvent(event);			
		}

		@Override
		public void handleEvent(ActivityStartEvent event) {
			printEvent(event);			
		}

		@Override
		public void handleEvent(ActivityEndEvent event) {
			printEvent(event);			
		}
		
		private void printEvent(PersonEvent event) {
			if (event.getPersonId().equals(filterEventsForAgentId)){
				System.out.println(event.toString());
			}
		}
	}
	
}
