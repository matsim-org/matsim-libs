/* *********************************************************************** *
 * project: org.matsim.*
 * BinaryEvents2TextEvents.java
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

package org.matsim.mobsim.cppdeqsim;

import java.io.File;

import org.matsim.events.Events;
import org.matsim.events.algorithms.CalcLegNumber;
import org.matsim.events.algorithms.EventWriterTXT;

public class BinaryEvents2TextEvents {

	public static void convert(final String fromFilename, final String toFilename) {
		final Events events = new Events();

		events.addHandler(new CalcLegNumber());

		EventWriterTXT ew_txt = new EventWriterTXT(toFilename);
		events.addHandler(ew_txt);

		final EventsReaderDEQv1 eventsReader = new EventsReaderDEQv1(events);
		eventsReader.readFile(fromFilename);
		events.printEventsCount();

		ew_txt.closefile();
	}

	private static void usage() {
		System.out.println();
		System.out.println("BinaryEvents2TextEvents");
		System.out.println("Converts one or more binary events file from DEQSim to a text events file.");
		System.out.println();
		System.out.println("usage: BinaryEvents2TextEvents infile [tofile]");
		System.out.println("       If no tofile is given, a file will be generated at the same location");
		System.out.println("       as the infile but with different file-ending.");
		System.out.println("       If there are multiple binary events file (named *.0, *.1, etc), only");
		System.out.println("       the first one has to be given as fromfile to convert all binary event");
		System.out.println("       files into one text event file.");
		System.out.println();
		System.out.println("Options:");
		System.out.println("-h, --help:     Displays this message.");
		System.out.println();
		System.out.println("----------------");
		System.out.println("2007, matsim.org");
		System.out.println();
	}

	public static void main(final String[] args) {
		String fromFilename;
		String toFilename;
		if (args.length < 1 || args.length > 2) {
			usage();
			return;
		}
		if (args.length == 1 && (args[0].equals("-h") || args[0].equals("--help"))) {
			usage();
			return;
		}
		if (args.length == 1) {
			fromFilename = args[0];
			if (fromFilename.endsWith(".0")) {
				fromFilename = fromFilename.substring(0, fromFilename.length() - 2);
			}
			toFilename = fromFilename.replaceFirst("\\.dat", "\\.txt");
		} else {
			fromFilename = args[0];
			if (fromFilename.endsWith(".0")) {
				fromFilename = fromFilename.substring(0, fromFilename.length() - 2);
			}
			toFilename = args[1];
		}
		if (new File(toFilename).exists()) {
			System.err.println("File exists already: " + toFilename);
			return;
		}
		convert(fromFilename, toFilename);
	}
}
