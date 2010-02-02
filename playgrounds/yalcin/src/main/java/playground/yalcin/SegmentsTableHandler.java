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

import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;

import playground.yalcin.visum.VisumNetwork;
import playground.yalcin.visum.VisumNetwork.Stop;

public class SegmentsTableHandler implements TabularFileHandler {

	private static final String TAB = "\t";

	// the visum network to do the lookups
	private final VisumNetwork vNetwork;

	// the search radius
	private final double searchRadius;

	private final BufferedWriter writer;

	// some variables to maintain state
	private CoordImpl startCoord = null;
	private CoordImpl endCoord = null;
	private String personId = null;
	private String tripId = null;
	private String startDate = null;
	private String startTime = null;
	private String endDate = null;
	private String endTime = null;
	private int cntSegments = 0;
	private int cntPuTSegments = 0;
	private int cntRailSegments = 0;
	private double sumSegmentDistance = 0.0;
	private final CodesTableReader codes;
	private boolean onlyWriteCodedTrips = false;

	private int line =  0;

	/**
	 * @param vNetwork VisumNetwork for looking up stops
	 * @param searchRadius the search radius for stops in kilometers
	 * @param codesTable the lookupTable for codes
	 * @param filename name of a file where to write the results into
	 */
	public SegmentsTableHandler(final VisumNetwork vNetwork, final double searchRadius, final CodesTableReader codesTable, final String filename) {
		this.vNetwork = vNetwork;
		this.searchRadius = searchRadius;
		this.codes = codesTable;
		try {
			this.writer = IOUtils.getBufferedWriter(filename);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void setOnlyWriteCodedTrips(final boolean onlyWriteCodedTrips) {
		this.onlyWriteCodedTrips = onlyWriteCodedTrips;
	}

	public void startRow(final String[] row) {
		this.line++;
		if (this.line == 1) {
			// header
			try {
				this.writer.write("Code\tPersonID\tTripID\tSegments\tPutSegments\tRailSegments\tXStartingPoint\tYStartingPoint\tStartingDate\tStartingTime\tXEndingPoint\tYEndingPoint\tEndingDate\tEndingTime\tTotalDistance\tStartStopId\tStartStopDistance\tEndStopID\tEndStopDistance\tFoundStartStops\tFoundEndStops\tNearestStartStop\tNearestStartStopDistance\tNearestEndStop\tNearestEndStopDistance\n");
			} catch (IOException e) {
				e.printStackTrace();
			}
			return;
		}

		if (!row[0].equals(this.personId) || !row[1].equals(this.tripId)) {
			// either personId or tripId has changed: process the last trip
			if (this.cntPuTSegments > 0 || this.cntRailSegments > 0) {
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
			this.startCoord = new CoordImpl(Double.parseDouble(row[3])/1000.0, Double.parseDouble(row[4])/1000.0);
			this.cntSegments = 0;
			this.cntPuTSegments = 0;
			this.cntRailSegments = 0;
			this.sumSegmentDistance = Double.parseDouble(row[13]);
		}

		this.endDate = row[11];
		this.endTime = row[12];
		this.endCoord = new CoordImpl(Double.parseDouble(row[8])/1000.0, Double.parseDouble(row[9])/1000.0);
		this.cntSegments++;
		if (Double.parseDouble(row[18]) >= 0.5) {
			this.cntPuTSegments++;
		}
		if (Double.parseDouble(row[19]) >= 0.5) {
			this.cntRailSegments++;
		}
		this.sumSegmentDistance += Double.parseDouble(row[13]);
	}

	private void handleTrip() throws IOException {
		if (this.cntPuTSegments > 0 || this.cntRailSegments > 0) {
			// the last trip had at least one public transport segment, so write the person out
			final Collection<VisumNetwork.Stop> startStops = this.vNetwork.findStops(this.startCoord, this.searchRadius);
			final Collection<VisumNetwork.Stop> endStops = this.vNetwork.findStops(this.endCoord, this.searchRadius);
			// write code if available
			String code = this.codes.getCode(this.personId, this.tripId);
			if (code != null) {
				this.writer.write(code);
			} else if (onlyWriteCodedTrips) {
				// this line has no code, and we should only write lines that contain a code...
				return;
			}

			// write basic information
			this.writer.write(TAB + this.personId + TAB + this.tripId + TAB + this.cntSegments + TAB + this.cntPuTSegments + TAB + this.cntRailSegments
					+ TAB + this.startCoord.getX() * 1000.0 + TAB + this.startCoord.getY()*1000.0 + TAB + this.startDate+ TAB + this.startTime
					+ TAB + this.endCoord.getX() * 1000.0 + TAB + this.endCoord.getY() * 1000.0 + TAB + this.endDate+ TAB + this.endTime
					+ TAB + this.sumSegmentDistance);
			this.writer.write("\tSTART-ID\tstart-distance\tEND-ID\tend-distance");
			// FoundStartStops
			this.writer.write(TAB);
			if (startStops.size() > 0) {
				this.writer.write("1");
			} else {
				this.writer.write("0");
			}
			//FoundEndStops
			this.writer.write(TAB);
			if (endStops.size() > 0) {
				this.writer.write("1");
			} else {
				this.writer.write("0");
			}
			//NearestStartStop
			Stop nearestStartStop = this.vNetwork.findNearestStop(this.startCoord);
			this.writer.write(TAB);
			this.writer.write(nearestStartStop.id.toString());

			//NearestStartStopDistance
			this.writer.write(TAB);
			this.writer.write(Double.toString(CoordUtils.calcDistance(this.startCoord, nearestStartStop.coord)));

			//NearestEndStop
			Stop nearestEndStop = this.vNetwork.findNearestStop(this.endCoord);
			this.writer.write(TAB);
			this.writer.write(nearestEndStop.id.toString());

			//NearestEndStopDistance
			this.writer.write(TAB);
			this.writer.write(Double.toString(CoordUtils.calcDistance(this.endCoord, nearestEndStop.coord)));

			this.writer.write('\n');
			// write possible starting stops
			for (VisumNetwork.Stop stop : startStops) {
				this.writer.write("\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t");
				this.writer.write(stop.id.toString() + "\t");
				this.writer.write(Double.toString(CoordUtils.calcDistance(this.startCoord, stop.coord)));
				this.writer.write("\n");
			}
			// write possible ending stops
			for (VisumNetwork.Stop stop : endStops) {
				this.writer.write("\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t");
				this.writer.write(stop.id.toString() + "\t");
				this.writer.write(Double.toString(CoordUtils.calcDistance(this.endCoord, stop.coord)));
				this.writer.write("\n");
			}
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
