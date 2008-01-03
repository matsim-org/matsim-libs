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

import org.matsim.events.Events;
import org.matsim.events.MatsimEventsReader;
import org.matsim.events.algorithms.AnalyzeLegTimes;
import org.matsim.gbl.Gbl;
import org.matsim.utils.misc.Time;

public class EventsHandling {

	//////////////////////////////////////////////////////////////////////
	// test run 01
	//////////////////////////////////////////////////////////////////////

	public static void handlingEvents(final int binSize) {

		System.out.println("MATSim-ANALYSIS: handling events....");

		System.out.println("  reading events and analyzing departure times... ");
		final Events events = new Events();
		final AnalyzeLegTimes analysis = new AnalyzeLegTimes(binSize,null);
		events.addHandler(analysis);
		new MatsimEventsReader(events).readFile(Gbl.getConfig().events().getInputFile());
		System.out.println("  done.");

		final int[][] countsDep = analysis.getLegDepCounts();
		final int[][] countsArr = analysis.getLegArrCounts();
		final int[][] countsStuck = analysis.getStuckCounts();

		System.out.print("RESULTS:\n time\ttime");
		for (int i = 0; i < countsDep.length; i++) {
			System.out.print("\tdepartures_" + i + "\tarrivals_" + i + "\tstuck_" + i);
		}
		System.out.println();
		for (int j = 0; j < countsDep[0].length; j++) {
			System.out.print(j*binSize + "\t" + Time.writeTime(j*binSize));
			for (int i = 0; i < countsDep.length; i++) {
				System.out.print("\t" + countsDep[i][j] + "\t" + countsArr[i][j] + "\t" + countsStuck[i][j]);
			}
			System.out.println();
		}
		System.out.println();

		System.out.println("done.");
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
