/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.agarwalamit.munich.utils;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;
import org.matsim.core.scenario.MutableScenario;

import playground.agarwalamit.utils.LoadMyScenarios;
import playground.vsp.congestion.handlers.CongestionHandlerImplV3;
import playground.vsp.congestion.handlers.MarginalCongestionPricingHandler;

/**
 * Read existing events file (with almost all events + congestion events + money events from congestion and emission pricing)
 * and then rewrite them.
 * @author amit
 */

public class EventsFilterAndWriter {

	public static void main(String[] args) {
		new EventsFilterAndWriter().run();
	}

	private void run() {
		String eventsDir = "../../../../repos/runs-svn/detEval/emissionCongestionInternalization/iatbr/output/eci/ITERS/it.1500/";
		String existingEventsFile = eventsDir+"/1500.events.xml.gz";
		String outEventsFile = eventsDir+"/1500.events_excludeMoneyAndCongestionEvents.xml.gz";
		String outputFilesDir = "../../../../repos/runs-svn/detEval/emissionCongestionInternalization/iatbr/output/eci/";

		List<Event> listOfEvents = readAndReturnEventsAsList(existingEventsFile);
		writeEventsFromList(listOfEvents, outEventsFile);

		// now process this events file and get congestion and money events.
		Scenario sc = LoadMyScenarios.loadScenarioFromOutputDir(outputFilesDir);
		EventsManager events = EventsUtils.createEventsManager();
		MatsimEventsReader  reader = new MatsimEventsReader(events);
		events.addHandler(new CongestionHandlerImplV3(events, sc));
		events.addHandler(new MarginalCongestionPricingHandler(events, (MutableScenario) sc));
		EventWriterXML ewxml = new EventWriterXML(eventsDir+"/1500.events_congestionAndMoneyEvent.xml.gz");
		events.addHandler(ewxml);
		reader.readFile(outEventsFile);
		ewxml.closeFile();
	}


	private List<Event> readAndReturnEventsAsList(String existingEventsFile){
		EventsManager events = EventsUtils.createEventsManager();
		MatsimEventsReader  reader = new MatsimEventsReader(events);
		EventsAsListHandler efw = new EventsAsListHandler();
		events.addHandler(efw);
		reader.readFile(existingEventsFile);
		return efw.getListOfEvents();
	}

	private void writeEventsFromList(List<Event> listOfEvents, String outEventsFile){
		EventWriterXML eventsWriter = new EventWriterXML(outEventsFile);
		for(Event e : listOfEvents){
			eventsWriter.handleEvent(e);
		}
		eventsWriter.closeFile();
	}

	private class EventsAsListHandler implements LinkEnterEventHandler,
	LinkLeaveEventHandler,
	TransitDriverStartsEventHandler,
	PersonDepartureEventHandler, 
	PersonStuckEventHandler,
	VehicleEntersTrafficEventHandler,
	PersonArrivalEventHandler {

		private final List<Event> listOfEvents = new ArrayList<>();
		private final Vehicle2DriverEventHandler delegate = new Vehicle2DriverEventHandler();

		@Override
		public void reset(int iteration) {
			this.delegate.reset(iteration);

		}

		@Override
		public void handleEvent(PersonArrivalEvent event) {
			listOfEvents.add(event);
		}

		@Override
		public void handleEvent(VehicleEntersTrafficEvent event) {
			listOfEvents.add(event);
		}

		@Override
		public void handleEvent(PersonStuckEvent event) {
			listOfEvents.add(event);
		}

		@Override
		public void handleEvent(PersonDepartureEvent event) {
			listOfEvents.add(event);
		}

		@Override
		public void handleEvent(TransitDriverStartsEvent event) {
			listOfEvents.add(event);
		}

		@Override
		public void handleEvent(LinkLeaveEvent event) {
			listOfEvents.add(event);
		}

		@Override
		public void handleEvent(LinkEnterEvent event) {
			listOfEvents.add(event);
		}

		public List<Event> getListOfEvents() {
			return listOfEvents;
		}
	}
}
