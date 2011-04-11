/* *********************************************************************** *
 * project: org.matsim.*
 * TvehHomeTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.yu.analysis;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.matsim.core.api.experimental.events.AgentWait2LinkEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.handler.AgentWait2LinkEventHandler;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.ConfigUtils;

public class EventsWait2LinkTest {
	public static class EventsWait2Link implements AgentWait2LinkEventHandler {
		private BufferedWriter writer = null;

		public EventsWait2Link(final String outputFilename) {
			try {
				writer = IOUtils.getBufferedWriter(outputFilename);
				writer.write("personId\tlinkId\n");
				writer.flush();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public void handleEvent(final AgentWait2LinkEvent event) {
			try {
				writer.write(event.getPersonId().toString() + "\t"
						+ event.getLinkId().toString() + "\n");
				writer.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public void end() {
			try {
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public void reset(final int iteration) {
			writer = null;
		}
	}

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		Gbl.startMeasurement();

		final String netFilename = "./test/yu/ivtch/input/network.xml";
		// final String eventsFilename =
		// "./test/yu/test/input/run265opt100.events.txt.gz";
		final String eventsFilename = "../runs/run266/100.events.txt.gz";
		final String outputFilename = "../runs/run266/Wait2Link.events.txt.gz";

		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario).readFile(netFilename);

		EventsManager events = (EventsManager) EventsUtils.createEventsManager();

		EventsWait2Link ew2l = new EventsWait2Link(outputFilename);
		events.addHandler(ew2l);

		new MatsimEventsReader(events).readFile(eventsFilename);

		ew2l.end();

		System.out.println("-> Done!");
		Gbl.printElapsedTime();
		System.exit(0);
	}
}
