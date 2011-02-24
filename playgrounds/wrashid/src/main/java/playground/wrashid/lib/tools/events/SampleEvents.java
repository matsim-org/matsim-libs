package playground.wrashid.lib.tools.events;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Random;

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
import org.matsim.core.controler.Controler;
import org.matsim.core.events.ActivityEndEventImpl;
import org.matsim.core.events.AgentArrivalEventImpl;
import org.matsim.core.events.AgentDepartureEventImpl;
import org.matsim.core.events.AgentStuckEventImpl;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsReaderTXTv1;
import org.matsim.core.events.algorithms.EventWriterTXT;
import org.matsim.core.events.handler.EventHandler;

import playground.wrashid.lib.DebugLib;

public class SampleEvents {

	public static void main(String[] args) {
// to perform several parallel runs (use args)
		String inputEventsFile = args[0];
		String outputEventsFile = args[1];
		double sampleFraction = Double.parseDouble(args[2]);
		
		// e.g. only considert that percentage of agents at random (discard the
		// events of the other agents).
//		double sampleFraction = 0.05; // max:1
//		String inputEventsFile = "H:/data/experiments/ARTEMIS/output/run10/ITERS/it.50/50.events.txt.gz";
//		String outputEventsFile = "H:/data/experiments/ARTEMIS/output/run10/ITERS/it.50/events sampling/5pct-sample-2-events.txt.txt.gz";

		EventsManagerImpl events = new EventsManagerImpl();

		EventsFilter eventsFilter = new EventsFilter(outputEventsFile, sampleFraction);

		events.addHandler(eventsFilter);

		EventsReaderTXTv1 reader = new EventsReaderTXTv1(events);

		reader.readFile(inputEventsFile);

		eventsFilter.closeFile();
	}

	private static class EventsFilter extends EventWriterTXT {

		private double sampleFraction;
		private Random random;
		private HashSet<Id> writeOutPersonsSet;
		private HashSet<Id> ignorePersonsSet;

		public EventsFilter(String filename) {
			super(filename);
			
			DebugLib.stopSystemAndReportInconsistency("please use other constructor!!!");
		}

		// TODO: whenever new agent detected, decide if want to write events for
		// that agent (using random variable) or move agent to set of agents,
		// for which we do not want to
		// write events.

		public EventsFilter(String filename, double sampleFraction) {
			super(filename);
			this.sampleFraction = sampleFraction;
			random = new Random();
			writeOutPersonsSet=new HashSet<Id>();
			ignorePersonsSet=new HashSet<Id>(); 
		}

		public void handleEvent(ActivityEndEvent event) {
			Id personId = event.getPersonId();

			if (ignorePersonsSet.contains(personId)) {
				return;
			}

			updateFilterList(personId);

			if (writeOutPersonsSet.contains(personId)) {
				super.handleEvent(event);
			}
		}

		private void updateFilterList(Id personId) {
			if (!writeOutPersonsSet.contains(personId)) {
				if (random.nextDouble() < sampleFraction) {
					writeOutPersonsSet.add(personId);
				} else {
					ignorePersonsSet.add(personId);
				}
			}
		}

		public void handleEvent(ActivityStartEvent event) {
			Id personId = event.getPersonId();

			if (ignorePersonsSet.contains(personId)) {
				return;
			}

			updateFilterList(personId);

			if (writeOutPersonsSet.contains(personId)) {
				super.handleEvent(event);
			}
		}

		public void handleEvent(AgentArrivalEvent event) {
			Id personId = event.getPersonId();

			if (ignorePersonsSet.contains(personId)) {
				return;
			}

			updateFilterList(personId);

			if (writeOutPersonsSet.contains(personId)) {
				super.handleEvent(event);
			}
		}

		public void handleEvent(AgentDepartureEvent event) {
			Id personId = event.getPersonId();

			if (ignorePersonsSet.contains(personId)) {
				return;
			}

			updateFilterList(personId);

			if (writeOutPersonsSet.contains(personId)) {
				super.handleEvent(event);
			}
		}

		public void handleEvent(AgentStuckEvent event) {
			Id personId = event.getPersonId();

			if (ignorePersonsSet.contains(personId)) {
				return;
			}

			updateFilterList(personId);

			if (writeOutPersonsSet.contains(personId)) {
				super.handleEvent(event);
			}
		}

		public void handleEvent(AgentMoneyEvent event) {
			Id personId = event.getPersonId();

			if (ignorePersonsSet.contains(personId)) {
				return;
			}

			updateFilterList(personId);

			if (writeOutPersonsSet.contains(personId)) {
				super.handleEvent(event);
			}
		}

		public void handleEvent(AgentWait2LinkEvent event) {
			Id personId = event.getPersonId();

			if (ignorePersonsSet.contains(personId)) {
				return;
			}

			updateFilterList(personId);

			if (writeOutPersonsSet.contains(personId)) {
				super.handleEvent(event);
			}
		}

		public void handleEvent(LinkEnterEvent event) {
			Id personId = event.getPersonId();

			if (ignorePersonsSet.contains(personId)) {
				return;
			}

			updateFilterList(personId);

			if (writeOutPersonsSet.contains(personId)) {
				super.handleEvent(event);
			}
		}

		public void handleEvent(LinkLeaveEvent event) {
			Id personId = event.getPersonId();

			if (ignorePersonsSet.contains(personId)) {
				return;
			}

			updateFilterList(personId);

			if (writeOutPersonsSet.contains(personId)) {
				super.handleEvent(event);
			}
		}

	}

}
