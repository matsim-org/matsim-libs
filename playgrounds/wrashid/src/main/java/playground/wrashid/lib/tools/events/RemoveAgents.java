package playground.wrashid.lib.tools.events;

import java.util.HashSet;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.AgentMoneyEvent;
import org.matsim.core.api.experimental.events.AgentStuckEvent;
import org.matsim.core.api.experimental.events.AgentWait2LinkEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsReaderTXTv1;
import org.matsim.core.events.algorithms.EventWriterTXT;

public class RemoveAgents {

	public static void main(String[] args) {
		
	}
	
	public static void removeAgents(HashSet<Id> agentIds, String inputEventsFilePath, String outputEventsFilePath){
		EventsManagerImpl events = new EventsManagerImpl();

		EventsFilter eventsFilter = new EventsFilter(outputEventsFilePath, agentIds);

		events.addHandler(eventsFilter);

		EventsReaderTXTv1 reader = new EventsReaderTXTv1(events);

		reader.readFile(inputEventsFilePath);

		eventsFilter.closeFile();
	}
	
	private static class EventsFilter extends EventWriterTXT {
		private HashSet<Id> ignorePersonsSet;

		public EventsFilter(String filename, HashSet<Id> filterAgentIds) {
			super(filename);
			this.ignorePersonsSet=filterAgentIds;
		}



		public void handleEvent(ActivityEndEvent event) {
			Id personId = event.getPersonId();

			if (ignorePersonsSet.contains(personId)) {
				return;
			}
			
			super.handleEvent(event);
		}

		public void handleEvent(ActivityStartEvent event) {
			Id personId = event.getPersonId();

			if (ignorePersonsSet.contains(personId)) {
				return;
			}
			
			super.handleEvent(event);
		}

		public void handleEvent(AgentArrivalEvent event) {
			Id personId = event.getPersonId();

			if (ignorePersonsSet.contains(personId)) {
				return;
			}
			
			super.handleEvent(event);
		}

		public void handleEvent(AgentDepartureEvent event) {
			Id personId = event.getPersonId();

			if (ignorePersonsSet.contains(personId)) {
				return;
			}
			
			super.handleEvent(event);
		}

		public void handleEvent(AgentStuckEvent event) {
			Id personId = event.getPersonId();

			if (ignorePersonsSet.contains(personId)) {
				return;
			}
			
			super.handleEvent(event);
		}

		public void handleEvent(AgentMoneyEvent event) {
			Id personId = event.getPersonId();

			if (ignorePersonsSet.contains(personId)) {
				return;
			}
			
			super.handleEvent(event);
		}

		public void handleEvent(AgentWait2LinkEvent event) {
			Id personId = event.getPersonId();

			if (ignorePersonsSet.contains(personId)) {
				return;
			}
			
			super.handleEvent(event);
		}

		public void handleEvent(LinkEnterEvent event) {
			Id personId = event.getPersonId();

			if (ignorePersonsSet.contains(personId)) {
				return;
			}
			
			super.handleEvent(event);
		}

		public void handleEvent(LinkLeaveEvent event) {
			Id personId = event.getPersonId();

			if (ignorePersonsSet.contains(personId)) {
				return;
			}
			
			super.handleEvent(event);
		}

	}
	
}
