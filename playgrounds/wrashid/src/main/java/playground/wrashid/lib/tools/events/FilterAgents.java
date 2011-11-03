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
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.EventsReaderTXTv1;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.algorithms.EventWriterTXT;
import org.matsim.core.events.algorithms.EventWriterXML;

public class FilterAgents {

	public static void main(String[] args) {
		String inputEventsFile="H:/data/experiments/TRBAug2011/runs/ktiRun22/output/ITERS/it.50/50.events.xml.gz";
		
		String outputEventsFile="c:/tmp/output-events.xml.gz";
		
		HashSet<Id> agentIds = new HashSet<Id>();
		
		agentIds.add(new IdImpl("7223866"));
		agentIds.add(new IdImpl("2781839"));
		agentIds.add(new IdImpl("3220050"));
		agentIds.add(new IdImpl("3463142"));
		agentIds.add(new IdImpl("3702380"));
		agentIds.add(new IdImpl("1691781"));
		agentIds.add(new IdImpl("5023315"));
		
		
		FilterAgents.keepAgents(agentIds, inputEventsFile, outputEventsFile);
	}

	public static void keepAgents(HashSet<Id> agentIds, String inputEventsFilePath, String outputEventsFilePath) {
		boolean keepAgentsInFilter = true;

		startFiltering(agentIds, inputEventsFilePath, outputEventsFilePath, keepAgentsInFilter);
	}

	public static void removeAgents(HashSet<Id> agentIds, String inputEventsFilePath, String outputEventsFilePath) {
		boolean keepAgentsInFilter = false;

		startFiltering(agentIds, inputEventsFilePath, outputEventsFilePath, keepAgentsInFilter);
	}

	private static void startFiltering(HashSet<Id> agentIds, String inputEventsFilePath, String outputEventsFilePath,
			boolean keepAgentsInFilter) {
		EventsManager events = (EventsManager) EventsUtils.createEventsManager();

		EventsFilter eventsFilter = new EventsFilter(outputEventsFilePath, agentIds, keepAgentsInFilter);

		events.addHandler(eventsFilter);

		// TODO: make switch between txt/xml general.
		//EventsReaderTXTv1 reader = new EventsReaderTXTv1(events);
		//reader.readFile(inputEventsFilePath);
		EventsReaderXMLv1 reader = new EventsReaderXMLv1(events);
		reader.parse(inputEventsFilePath);

		eventsFilter.closeFile();
	}

	// TODO: make switch between txt/xml general.
	//private static class EventsFilter extends EventWriterTXT {
	private static class EventsFilter extends EventWriterXML {
		private HashSet<Id> filterPersonsSet;
		boolean keepAgentsInFilter;

		// true: keep only agents provided,
		// false: remove agents provided in filter set

		public EventsFilter(String filename, HashSet<Id> filterAgentIds, boolean keepAgentsInFilter) {
			super(filename);
			this.filterPersonsSet = filterAgentIds;
			this.keepAgentsInFilter = keepAgentsInFilter;
		}

		public void handleEvent(ActivityEndEvent event) {
			Id personId = event.getPersonId();

			if (keepAgentsInFilter) {
				if (filterPersonsSet.contains(personId)) {
					super.handleEvent(event);
				}
			} else {
				if (!filterPersonsSet.contains(personId)) {
					super.handleEvent(event);
				}
			}

		}

		public void handleEvent(ActivityStartEvent event) {
			Id personId = event.getPersonId();

			if (filterPersonsSet.contains(personId)) {
				return;
			}

			super.handleEvent(event);
		}

		public void handleEvent(AgentArrivalEvent event) {
			Id personId = event.getPersonId();

			if (filterPersonsSet.contains(personId)) {
				return;
			}

			super.handleEvent(event);
		}

		public void handleEvent(AgentDepartureEvent event) {
			Id personId = event.getPersonId();

			if (filterPersonsSet.contains(personId)) {
				return;
			}

			super.handleEvent(event);
		}

		public void handleEvent(AgentStuckEvent event) {
			Id personId = event.getPersonId();

			if (keepAgentsInFilter) {
				if (filterPersonsSet.contains(personId)) {
					super.handleEvent(event);
				}
			} else {
				if (!filterPersonsSet.contains(personId)) {
					super.handleEvent(event);
				}
			}
		}

		public void handleEvent(AgentMoneyEvent event) {
			Id personId = event.getPersonId();

			if (keepAgentsInFilter) {
				if (filterPersonsSet.contains(personId)) {
					super.handleEvent(event);
				}
			} else {
				if (!filterPersonsSet.contains(personId)) {
					super.handleEvent(event);
				}
			}
		}

		public void handleEvent(AgentWait2LinkEvent event) {
			Id personId = event.getPersonId();

			if (keepAgentsInFilter) {
				if (filterPersonsSet.contains(personId)) {
					super.handleEvent(event);
				}
			} else {
				if (!filterPersonsSet.contains(personId)) {
					super.handleEvent(event);
				}
			}
		}

		public void handleEvent(LinkEnterEvent event) {
			Id personId = event.getPersonId();

			if (keepAgentsInFilter) {
				if (filterPersonsSet.contains(personId)) {
					super.handleEvent(event);
				}
			} else {
				if (!filterPersonsSet.contains(personId)) {
					super.handleEvent(event);
				}
			}
		}

		public void handleEvent(LinkLeaveEvent event) {
			Id personId = event.getPersonId();

			if (keepAgentsInFilter) {
				if (filterPersonsSet.contains(personId)) {
					super.handleEvent(event);
				}
			} else {
				if (!filterPersonsSet.contains(personId)) {
					super.handleEvent(event);
				}
			}
		}

	}

}
