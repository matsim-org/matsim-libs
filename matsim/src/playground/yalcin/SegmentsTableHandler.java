/* *********************************************************************** *
 * project: org.matsim.*
 * SegmentsTableReader.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.yalcin;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

import org.matsim.utils.geometry.shared.Coord;
import org.matsim.utils.io.IOUtils;
import org.matsim.utils.io.tabularFileParser.TabularFileHandlerI;

import playground.marcel.visum.VisumNetwork;

public class SegmentsTableHandler implements TabularFileHandlerI {

	// the visum network to do the lookups
	private final VisumNetwork vNetwork;

	// the search radius
	private final double searchRadius;

	private final BufferedWriter writer;

	// some variables to maintain state
	private Coord startCoord = null;
	private Coord endCoord = null;
	private String personId = null;
	private String tripId = null;
	private String startDate = null;
	private String startTime = null;
	private String endDate = null;
	private String endTime = null;
	private int cntSegments = 0;
	private int cntPtSegments = 0;

	private int line =  0;

	/**
	 * @param vNetwork VisumNetwork for looking up stops
	 * @param searchRadius the search radius for stops in kilometers
	 * @param filename name of a file where to write the results into
	 */
	public SegmentsTableHandler(final VisumNetwork vNetwork, final double searchRadius, final String filename) {
		this.vNetwork = vNetwork;
		this.searchRadius = searchRadius;
		try {
			this.writer = IOUtils.getBufferedWriter(filename);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void startRow(final String[] row) {
		this.line++;
		if (this.line == 1) {
			// header
			try {
				this.writer.write("PersonID\tTripID\tSegments\tPtSegments\tXStartingPoint\tYStartingPoint\tStartingDate\tStartingTime\tXEndingPoint\tYEndingPoint\tEndingDate\tEndingTime\tStartingStops\tEndingStops\n");
			} catch (IOException e) {
				e.printStackTrace();
			}
			return;
		}

		if (!row[0].equals(this.personId) || !row[1].equals(this.tripId)) {
			// either personId or tripId has changed: process the last trip
			if (this.cntPtSegments > 0) {
				try {
					handleTrip();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			// initialize the data for the new person
			this.personId = row[0];
			this.tripId = row[1];
			this.startDate = row[6];
			this.startTime = row[7];
			this.startCoord = new Coord(Double.parseDouble(row[3])/1000.0, Double.parseDouble(row[4])/1000.0);
			this.cntSegments = 0;
			this.cntPtSegments = 0;
		}

		this.endDate = row[11];
		this.endTime = row[12];
		this.endCoord = new Coord(Double.parseDouble(row[8])/1000.0, Double.parseDouble(row[9])/1000.0);
		this.cntSegments++;
		if (Double.parseDouble(row[18]) >= 0.5 || Double.parseDouble(row[19]) >= 0.5) {
			this.cntPtSegments++;
		}
	}

	private void handleTrip() throws IOException {
		if (this.cntPtSegments > 0) {
			// the last trip had at least one public transport segment, so write the person out
			final Collection<VisumNetwork.Stop> startStops = this.vNetwork.findStops(this.startCoord, this.searchRadius);
			final Collection<VisumNetwork.Stop> endStops = this.vNetwork.findStops(this.endCoord, this.searchRadius);
			// write basic information
			this.writer.write(this.personId + "\t" + this.tripId + "\t" + this.cntSegments + "\t" + this.cntPtSegments
					+ "\t" + this.startCoord.getX() * 1000.0 + "\t" + this.startCoord.getY()*1000.0 + "\t" + this.startDate+ "\t" + this.startTime
					+ "\t" + this.endCoord.getX() * 1000.0 + "\t" + this.endCoord.getY() * 1000.0 + "\t" + this.endDate+ "\t" + this.endTime);

			// write possible starting stops
			this.writer.write("\t");
			Iterator<VisumNetwork.Stop> stopIterator = startStops.iterator();
			if (stopIterator.hasNext()) {
				this.writer.write(stopIterator.next().id.toString());
			} else {
				this.writer.write("-");
			}
			while (stopIterator.hasNext()) {
				this.writer.write("," + stopIterator.next().id.toString());
			}

			// write possible ending stops
			this.writer.write("\t");
			stopIterator = endStops.iterator();
			if (stopIterator.hasNext()) {
				this.writer.write(stopIterator.next().id.toString());
			} else {
				this.writer.write("-");
			}
			while (stopIterator.hasNext()) {
				this.writer.write("," + stopIterator.next().id.toString());
			}
			this.writer.write("\n");
		}
	}

	public void finish() {
		try {
			handleTrip();
			this.writer.flush();
			this.writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
