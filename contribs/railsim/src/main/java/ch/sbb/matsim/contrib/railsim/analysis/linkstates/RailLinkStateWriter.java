/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2023 by the members listed in the COPYING,        *
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

package ch.sbb.matsim.contrib.railsim.analysis.linkstates;

import ch.sbb.matsim.contrib.railsim.events.RailsimLinkStateChangeEvent;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.matsim.core.utils.io.IOUtils;

import java.io.IOException;
import java.io.UncheckedIOException;

public class RailLinkStateWriter {

	public static void writeCsv(RailLinkStateAnalysis analysis, String filename) throws UncheckedIOException {
		String[] header = {"link", "time", "state", "vehicle", "track"};

		try (CSVPrinter csv = new CSVPrinter(IOUtils.getBufferedWriter(filename), CSVFormat.DEFAULT.builder().setHeader(header).build())) {
			for (RailsimLinkStateChangeEvent event : analysis.events) {
				csv.print(event.getLinkId().toString());
				csv.print(event.getTime());
				csv.print(event.getState().toString());
				csv.print(event.getVehicleId() != null ? event.getVehicleId().toString() : "");
				csv.print(event.getTrack());
				csv.println();
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

	}
}
