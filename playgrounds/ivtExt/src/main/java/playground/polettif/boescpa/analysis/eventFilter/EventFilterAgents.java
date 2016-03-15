/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
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
 * *********************************************************************** *
 */

package playground.polettif.boescpa.analysis.eventFilter;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.internal.HasPersonId;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Given a list of agents, this class filters all events related to those agents from a given events file.
 *
 * @author boescpa
 */
public class EventFilterAgents {

	private static Logger log = Logger.getLogger(EventFilterAgents.class);

	public static void main(String[] args) {
		// args[0]: Path to an events-File, e.g. "run.combined.150.events.xml.gz"
		String eventsFile = args[0];
		// args[1]: Path to the network-File used for the simulation resulting in the above events-File, e.g. "multimodalNetwork2030final.xml.gz"
		String networkFile = args[1];
		// args[2]: Path to target-Eventfile, e.g. "Events" (IMPORTANT: No file-ending needed, will be added...);
		String newEventFile = args[2] + ".xml.gz";
		// args[3]: Path to File with interesting Agents, e.g. "agents.txt";
		String interestingAgents = args[3];

		EventsManager events = EventsUtils.createEventsManager();

		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		scenario.getConfig().transit().setUseTransit(true);

		log.info("Reading network xml file...");
		MatsimNetworkReader NetworkReader = new MatsimNetworkReader(scenario.getNetwork());
		NetworkReader.readFile(networkFile);
		log.info("Reading network xml file...done.");

		log.info("Initializing event handler...");
		ECEventHandler eventCutter = new ECEventHandler(newEventFile, interestingAgents);
		events.addHandler(eventCutter);
		log.info("Initializing event handler...done.");

		log.info("Reading events file...");
		if (eventsFile.endsWith(".xml.gz")) { // if events-File is in the newer xml-format
			EventsReaderXMLv1 reader = new EventsReaderXMLv1(events);
			reader.parse(eventsFile);
		}
		else {
			throw new IllegalArgumentException("Given events-file not of known format.");
		}
		eventCutter.reset(0);
		log.info("Reading events file...done.");
	}

	private static class ECEventHandler implements BasicEventHandler {

		private final EventWriterXML eventsWriter;
		private final Set<Id> interestingAgents = new HashSet<Id>();

		public ECEventHandler(String newEventFile, String interestingAgents) {
			this.eventsWriter = new EventWriterXML(newEventFile);
			agentReader(interestingAgents);
		}

		private void agentReader(String interestingAgents) {
			try {
				BufferedReader in = IOUtils.getBufferedReader(interestingAgents);
				String newLine = in.readLine();
				while (newLine != null) {
					this.interestingAgents.add(Id.create(newLine, Person.class));
					newLine = in.readLine();
				}
				in.close();
			} catch (IOException e) {
				e.getStackTrace();
			}
		}

		@Override
		public void reset(int iteration) {
			eventsWriter.closeFile();
		}

		@Override
		public void handleEvent(Event event) {
            if (event instanceof HasPersonId) {
                if (this.interestingAgents.contains(((HasPersonId) event).getPersonId())) {
                    eventsWriter.handleEvent(event);
                }
            }
		}
	}
}
