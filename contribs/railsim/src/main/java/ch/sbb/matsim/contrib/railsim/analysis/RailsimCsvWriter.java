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

package ch.sbb.matsim.contrib.railsim.analysis;

import ch.sbb.matsim.contrib.railsim.RailsimUtils;
import ch.sbb.matsim.contrib.railsim.events.RailsimLinkStateChangeEvent;
import ch.sbb.matsim.contrib.railsim.events.RailsimTrainStateEvent;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.io.IOUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * Helper class to write railsim related csv files.
 */
public final class RailsimCsvWriter {

	private RailsimCsvWriter() {
	}

	/**
	 * Write {@link RailsimLinkStateChangeEvent} to a csv file.
	 */
	public static void writeLinkStatesCsv(List<RailsimLinkStateChangeEvent> events, String filename) throws UncheckedIOException {
		String[] header = {"link", "time", "state", "vehicle"};

		try (CSVPrinter csv = new CSVPrinter(IOUtils.getBufferedWriter(filename), CSVFormat.DEFAULT.builder().setHeader(header).build())) {
			for (RailsimLinkStateChangeEvent event : events) {
				csv.print(event.getLinkId().toString());
				csv.print(event.getTime());
				csv.print(event.getState().toString());
				csv.print(event.getVehicleId() != null ? event.getVehicleId().toString() : "");
				csv.println();
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

	}

	/**
	 * Write {@link RailsimTrainStateEvent} to a csv file.
	 */
	public static void writeTrainStatesCsv(List<RailsimTrainStateEvent> events, Network network, String filename) throws UncheckedIOException {
		String[] header = {"vehicle", "time", "acceleration", "speed", "targetSpeed", "headLink", "headPosition", "headX", "headY", "tailLink", "tailPosition", "tailX", "tailY"};

		try (CSVPrinter csv = new CSVPrinter(IOUtils.getBufferedWriter(filename), CSVFormat.DEFAULT.builder().setHeader(header).build())) {
			for (RailsimTrainStateEvent event : events) {
				csv.print(event.getVehicleId().toString());
				csv.print(event.getExactTime());
				csv.print(event.getAcceleration());
				csv.print(RailsimUtils.round(event.getSpeed()));
				csv.print(RailsimUtils.round(event.getTargetSpeed()));

				csv.print(event.getHeadLink().toString());
				csv.print(RailsimUtils.round(event.getHeadPosition()));
				if (network != null) {
					Link link = network.getLinks().get(event.getHeadLink());
					if (link != null) {
						double fraction = event.getHeadPosition() / link.getLength();
						Coord from = link.getFromNode().getCoord();
						Coord to = link.getToNode().getCoord();
						csv.print(from.getX() + (to.getX() - from.getX()) * fraction);
						csv.print(from.getY() + (to.getY() - from.getY()) * fraction);
					}
				} else {
					csv.print("");
					csv.print("");
				}

				csv.print(event.getTailLink().toString());
				csv.print(RailsimUtils.round(event.getTailPosition()));
				if (network != null) {
					Link link = network.getLinks().get(event.getTailLink());
					if (link != null) {
						double fraction = event.getTailPosition() / link.getLength();
						Coord from = link.getFromNode().getCoord();
						Coord to = link.getToNode().getCoord();
						csv.print(from.getX() + (to.getX() - from.getX()) * fraction);
						csv.print(from.getY() + (to.getY() - from.getY()) * fraction);
					}
				} else {
					csv.print("");
					csv.print("");
				}

				csv.println();
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

	}

}
