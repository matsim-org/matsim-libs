package playground.wrashid.lib.tools.events;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.AgentMoneyEvent;
import org.matsim.core.api.experimental.events.AgentStuckEvent;
import org.matsim.core.api.experimental.events.Wait2LinkEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.ActivityEndEventHandler;
import org.matsim.core.api.experimental.events.handler.ActivityStartEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentMoneyEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentStuckEventHandler;
import org.matsim.core.api.experimental.events.handler.Wait2LinkEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;

public class PrintEventsOfSingleAgent {

	public static void main(String[] args) {
		String eventsFile="H:/data/experiments/TRBAug2011/runs/ktiRun22/output/ITERS/it.50/50.events.xml.gz";
		EventsManager events = (EventsManager) EventsUtils.createEventsManager();

		SingleAgentEventsPrinter singleAgentEventsPrinter = new SingleAgentEventsPrinter(new IdImpl("1470986"));
		
		events.addHandler(singleAgentEventsPrinter);
		
		//EventsReaderTXTv1 reader = new EventsReaderTXTv1(events);
		//reader.readFile(eventsFile);
		
		EventsReaderXMLv1 reader = new EventsReaderXMLv1(events);
		reader.parse(eventsFile);
		
	}
	
	private static class SingleAgentEventsPrinter implements ActivityEndEventHandler, ActivityStartEventHandler, AgentArrivalEventHandler, 
	AgentDepartureEventHandler, AgentStuckEventHandler, AgentMoneyEventHandler, 
	Wait2LinkEventHandler, LinkEnterEventHandler, LinkLeaveEventHandler{

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
			if (event.getPersonId().equals(filterEventsForAgentId)){
				System.out.println(event.toString());
			}
		}


		@Override
		public void handleEvent(LinkEnterEvent event) {
			if (event.getPersonId().equals(filterEventsForAgentId)){
				System.out.println(event.toString());
			}
		}

		@Override
		public void handleEvent(Wait2LinkEvent event) {
			if (event.getPersonId().equals(filterEventsForAgentId)){
				System.out.println(event.toString());
			}
		}

		@Override
		public void handleEvent(AgentMoneyEvent event) {
			if (event.getPersonId().equals(filterEventsForAgentId)){
				System.out.println(event.toString());
			}			
		}

		@Override
		public void handleEvent(AgentStuckEvent event) {
			if (event.getPersonId().equals(filterEventsForAgentId)){
				System.out.println(event.toString());
			}			
		}

		@Override
		public void handleEvent(AgentDepartureEvent event) {
			if (event.getPersonId().equals(filterEventsForAgentId)){
				System.out.println(event.toString());
			}			
		}

		@Override
		public void handleEvent(AgentArrivalEvent event) {
			if (event.getPersonId().equals(filterEventsForAgentId)){
				System.out.println(event.toString());
			}			
		}

		@Override
		public void handleEvent(ActivityStartEvent event) {
			if (event.getPersonId().equals(filterEventsForAgentId)){
				System.out.println(event.toString());
			}		
		}

		@Override
		public void handleEvent(ActivityEndEvent event) {
			if (event.getPersonId().equals(filterEventsForAgentId)){
				System.out.println(event.toString());
			}		
		}
		
	}
	
}
