/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.wrashid.lib.tools.events;

import java.util.HashSet;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.EventWriterXML;

public class FilterAgents {

	public static void main(String[] args) {
		String inputEventsFile="H:/data/experiments/TRBAug2011/runs/ktiRun22/output/ITERS/it.50/50.events.xml.gz";
		
		String outputEventsFile="c:/tmp/output-events1.xml.gz";
		
		HashSet<Id<Person>> agentIds = new HashSet<>();
		
		agentIds.add(Id.create("7223866", Person.class));
		agentIds.add(Id.create("2781839", Person.class));
		agentIds.add(Id.create("3220050", Person.class));
		agentIds.add(Id.create("3463142", Person.class));
		agentIds.add(Id.create("3702380", Person.class));
		agentIds.add(Id.create("1691781", Person.class));
		agentIds.add(Id.create("5023315", Person.class));
		
		
		FilterAgents.keepAgents(agentIds, inputEventsFile, outputEventsFile);
	}

	public static void keepAgents(HashSet<Id<Person>> agentIds, String inputEventsFilePath, String outputEventsFilePath) {
		boolean keepAgentsInFilter = true;

		startFiltering(agentIds, inputEventsFilePath, outputEventsFilePath, keepAgentsInFilter);
	}

	public static void removeAgents(HashSet<Id<Person>> agentIds, String inputEventsFilePath, String outputEventsFilePath) {
		boolean keepAgentsInFilter = false;

		startFiltering(agentIds, inputEventsFilePath, outputEventsFilePath, keepAgentsInFilter);
	}

	private static void startFiltering(HashSet<Id<Person>> agentIds, String inputEventsFilePath, String outputEventsFilePath,
			boolean keepAgentsInFilter) {
		EventsManager events = EventsUtils.createEventsManager();

		EventsFilter eventsFilter = new EventsFilter(outputEventsFilePath, agentIds, keepAgentsInFilter);

		events.addHandler(eventsFilter);

		// TODO: make switch between txt/xml general.
		//EventsReaderTXTv1 reader = new EventsReaderTXTv1(events);
		//reader.readFile(inputEventsFilePath);
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(inputEventsFilePath);

		eventsFilter.closeFile();
	}

	// TODO: make switch between txt/xml general.
	//private static class EventsFilter extends EventWriterTXT {
	private static class EventsFilter extends EventWriterXML {
		private HashSet<Id<Person>> filterPersonsSet;
		boolean keepAgentsInFilter;

		// true: keep only agents provided,
		// false: remove agents provided in filter set

		public EventsFilter(String filename, HashSet<Id<Person>> filterAgentIds, boolean keepAgentsInFilter) {
			super(filename);
			this.filterPersonsSet = filterAgentIds;
			this.keepAgentsInFilter = keepAgentsInFilter;
		}

		// this method is for the xml events writer, the methods below for the txt case
		@Override
		public void handleEvent(final Event event) {
			//System.out.println(event.toString());
			Id<Person> personId=null;
			if (event.toString().split("driverId=\"").length<2){
				if (event.toString().split("person=\"").length>=2){
					personId = Id.create(event.toString().split("person=\"")[1].split("\"")[0], Person.class);
				}
			} else {
				personId = Id.create(event.toString().split("driverId=\"")[1].split("\"")[0], Person.class);
			}

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
		
		
		public void handleEvent(ActivityEndEvent event) {
			Id<Person> personId = event.getPersonId();

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
			Id<Person> personId = event.getPersonId();

			if (filterPersonsSet.contains(personId)) {
				return;
			}

			super.handleEvent(event);
		}

		public void handleEvent(PersonArrivalEvent event) {
			Id<Person> personId = event.getPersonId();

			if (filterPersonsSet.contains(personId)) {
				return;
			}

			super.handleEvent(event);
		}

		public void handleEvent(PersonDepartureEvent event) {
			Id<Person> personId = event.getPersonId();

			if (filterPersonsSet.contains(personId)) {
				return;
			}

			super.handleEvent(event);
		}

		public void handleEvent(PersonStuckEvent event) {
			Id<Person> personId = event.getPersonId();

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

		public void handleEvent(PersonMoneyEvent event) {
			Id<Person> personId = event.getPersonId();

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

		public void handleEvent(VehicleEntersTrafficEvent event) {
			Id<Person> personId = event.getPersonId();

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
			Id<Person> personId = event.getDriverId();

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
			Id<Person> personId = event.getDriverId();

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
