/* *********************************************************************** *
 * project: org.matsim.*
 * ShortOTFEvents2MviXML.java
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
package playground.yu.utils;

import java.io.File;

import org.matsim.api.core.v01.events.Event;
import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.EventWriterXML;

/**
 * quote from org.matsim.utils.vis.otfvis.executables.OTFEvent2MVI of David
 * Strippgen
 * 
 * @author yu
 * 
 */
public class ShortOTFEvents2MviXML {
	private static class ShortEventsWriterXML extends EventWriterXML {
		private final double endTime;

		/**
		 * @param filename
		 * @param endTime
		 *            the time of the last {@code Event} in output eventsfile
		 */
		public ShortEventsWriterXML(String filename, double endTime) {
			super(filename);
			this.endTime = endTime;
		}

		@Override
		public void handleEvent(Event event) {
			if (event.getTime() <= endTime) {
				super.handleEvent(event);
			}
		}

	}

	private static void printUsage() {
		System.out.println();
		System.out.println("MyOTFEvents2Mvi:");
		System.out.println("----------------");
		System.out
				.println("Create a Converter from eventsfile to .mvi-file with 4 parameters");
		System.out.println();
		System.out.println("usage: MyOTFEvents2Mvi args");
		System.out.println(" args[0]: netFilename incl. path (.xml)(required)");
		System.out
				.println(" arg[1]: eventsFilename incl. path (without xml[.gz])(required), the .mvi file path = args[1] + mvi");

		System.out
				.println(" arg[2]: time-interval[s](optional, default value = 300 [s])");
		System.out.println("----------------");
	}

	/**
	 * @param args
	 *            [0] - netFilename;
	 * @param args
	 *            [1] - eventsFilename with out xml(.gz);
	 * @param args
	 *            [2] - time-interval_s;
	 */
	public static void main(String[] args) {
		if (args.length < 2) {
			printUsage();
			System.exit(0);
		}
		// -----------WRITES A SHORT EVENTSFILE-----------------
		String eventsFilename = args[1] + "xml";
		if (!new File(eventsFilename).exists()) {
			eventsFilename += ".gz";
		}

		EventsManager events = EventsUtils.createEventsManager();

		String eventsOutputFilename = eventsFilename.replaceAll("events",
				"events_short");
		EventWriterXML writer = new ShortEventsWriterXML(eventsOutputFilename,
				108000d);
		events.addHandler(writer);

		new MatsimEventsReader(events).readFile(eventsFilename);

		writer.closeFile();

		// ----------------------------------------------------
		OTFVis.main(new String[] { "-convert", eventsOutputFilename,
				args[0]/* networkFilename */, args[1] + "mvi"/* mviFilename */,
				args.length < 3 ? "300" : args[2] /* snapshotPeriod */});

		System.out.println("done.");
	}
}
