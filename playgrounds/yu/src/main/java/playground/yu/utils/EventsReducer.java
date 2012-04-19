/* *********************************************************************** *
 * project: org.matsim.*
 * EventsReducer.java
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

import playground.yu.utils.io.SimpleReader;
import playground.yu.utils.io.SimpleWriter;

/**
 * @author yu
 * 
 */
public class EventsReducer {
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		final String eventsFilename = "../matsimTests/scoringTest/output/ITERS/it.100/100.events.txt.gz";

		SimpleReader sr = new SimpleReader(eventsFilename);
		SimpleWriter sw = new SimpleWriter(eventsFilename.replaceFirst(
				"events", "events4mvi"));

		String line = sr.readLine();
		sw.writeln(line);
		// after filehead
		double time = 0;
		while (line != null && time < 86400.0) {
			line = sr.readLine();
			if (line != null) {
				sw.writeln(line);
				time = Double.parseDouble(line.split("\t")[0]);
			}
		}
		try {
			sr.close();
			sw.close();
		} catch (Exception e) {
			System.err.println(e);
		}
	}

}
