package playground.wrashid.lib.tools.events;

import java.util.HashSet;
import java.util.Random;

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
import org.matsim.contrib.parking.lib.DebugLib;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.EventWriterTXT;


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

		EventsManager events = EventsUtils.createEventsManager();

		EventsFilter eventsFilter = new EventsFilter(outputEventsFile, sampleFraction);

		events.addHandler(eventsFilter);

		MatsimEventsReader reader = new MatsimEventsReader(events);

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

		public EventsFilter(String filename, double sampleFraction) {
			super(filename);
			this.sampleFraction = sampleFraction;
			random = new Random();
			writeOutPersonsSet=new HashSet<Id>();
			ignorePersonsSet=new HashSet<Id>(); 
		}

		@Override
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

		@Override
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

		@Override
	public void handleEvent(PersonArrivalEvent event) {
			Id personId = event.getPersonId();

			if (ignorePersonsSet.contains(personId)) {
				return;
			}

			updateFilterList(personId);

			if (writeOutPersonsSet.contains(personId)) {
				super.handleEvent(event);
			}
		}

		@Override
	public void handleEvent(PersonDepartureEvent event) {
			Id personId = event.getPersonId();

			if (ignorePersonsSet.contains(personId)) {
				return;
			}

			updateFilterList(personId);

			if (writeOutPersonsSet.contains(personId)) {
				super.handleEvent(event);
			}
		}

		@Override
	public void handleEvent(PersonStuckEvent event) {
			Id personId = event.getPersonId();

			if (ignorePersonsSet.contains(personId)) {
				return;
			}

			updateFilterList(personId);

			if (writeOutPersonsSet.contains(personId)) {
				super.handleEvent(event);
			}
		}

		@Override
	public void handleEvent(PersonMoneyEvent event) {
			Id personId = event.getPersonId();

			if (ignorePersonsSet.contains(personId)) {
				return;
			}

			updateFilterList(personId);

			if (writeOutPersonsSet.contains(personId)) {
				super.handleEvent(event);
			}
		}

		@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
			Id personId = event.getPersonId();

			if (ignorePersonsSet.contains(personId)) {
				return;
			}

			updateFilterList(personId);

			if (writeOutPersonsSet.contains(personId)) {
				super.handleEvent(event);
			}
		}

		@Override
	public void handleEvent(LinkEnterEvent event) {
			Id personId = event.getDriverId();

			if (ignorePersonsSet.contains(personId)) {
				return;
			}

			updateFilterList(personId);

			if (writeOutPersonsSet.contains(personId)) {
				super.handleEvent(event);
			}
		}

		@Override
	public void handleEvent(LinkLeaveEvent event) {
			Id personId = event.getDriverId();

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
