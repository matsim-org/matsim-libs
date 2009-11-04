/* *********************************************************************** *
 * project: org.matsim.*
 * EventsHandling.java
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

package playground.balmermi;

import org.matsim.analysis.LegHistogram;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.gbl.Gbl;

public class EventsHandling {

	//////////////////////////////////////////////////////////////////////
	// test run 01
	//////////////////////////////////////////////////////////////////////

	public static void handlingEvents(final int binSize) {

		System.out.println("MATSim-ANALYSIS: handling events....");

		System.out.println("  reading events and analyzing departure times... ");
		final EventsManagerImpl events = new EventsManagerImpl();
		final LegHistogram analysis = new LegHistogram(binSize);
		events.addHandler(analysis);
		new MatsimEventsReader(events).readFile(Gbl.getConfig().events().getInputFile());
		System.out.println("  done.");

		analysis.write(System.out);
	}

	//////////////////////////////////////////////////////////////////////
	// main
	//////////////////////////////////////////////////////////////////////

	public static void main(String[] args) {
		Gbl.startMeasurement();
		Gbl.printElapsedTime();

		Gbl.createConfig(args);
		Gbl.createWorld();

		handlingEvents(300);

		Gbl.printElapsedTime();
	}
}
