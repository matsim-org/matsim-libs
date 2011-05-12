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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;

public class PuT_trip_finder_handler implements TabularFileHandler {

	// the search radius
	private final double searchRadius;

	private final BufferedWriter writer;

	// some variables to maintain state
	private CoordImpl startCoord = null;
	private CoordImpl endCoord = null;
	private String personId = null;
	private String tripId = null;
	private List<String> segmentIds = new ArrayList<String>();
	private String startDate = null;
	private String startTime = null;
	private String endDate = null;
	private String endTime = null;
	private int cntSegments = 0;
	private int cntPuTSegments = 0;
	private int cntRailSegments = 0;
	private List<Double> Probability_Walks = new ArrayList<Double>(),
			Probability_Bikes = new ArrayList<Double>(),
			Probability_Cars = new ArrayList<Double>(),
			Probability_UrbanPuTs = new ArrayList<Double>(),
			Probability_Rails = new ArrayList<Double>();

	private int line = 0;

	/**
	 * @param vNetwork
	 *            VisumNetwork for looking up stops
	 * @param searchRadius
	 *            the search radius for stops in kilometers
	 * @param filename
	 *            name of a file where to write the results into
	 */
	public PuT_trip_finder_handler(final double searchRadius,
			final String filename) {
		this.searchRadius = searchRadius;
		this.writer = IOUtils.getBufferedWriter(filename);
	}

	@Override
	public void startRow(final String[] row) {
		this.line++;
		if (this.line == 1) {
			// header
			try {
				this.writer
						.write("PersonID\tTripID\tsegmentId\tSegments\tPutSegments\tRailSegments\tXStartingPoint\tYStartingPoint\tStartingDate\tStartingTime\tXEndingPoint\tYEndingPoint\tEndingDate\tEndingTime\tProbability_Walk\tProbability_Bike\tProbability_Car\tProbability_UrbanPuT\tProbability_Rail\n");
			} catch (IOException e) {
				e.printStackTrace();
			}
			return;
		}

		if (!row[0].equals(this.personId) || !row[1].equals(this.tripId)) {
			// either personId or tripId has changed: process the last trip
			if (this.cntPuTSegments > 0 || this.cntRailSegments > 0) {
//				if (this.tripId.equals("53") && this.personId.equals("45121"))
//					System.out.println("personid: " + this.personId
//							+ "\tcntRailSegments:" + this.cntRailSegments);
				try {
					handleTrip();
				} catch (IOException e) {
					e.printStackTrace();
				}

			}
			this.segmentIds.clear();
			this.Probability_Bikes.clear();
			this.Probability_Cars.clear();
			this.Probability_Rails.clear();
			this.Probability_UrbanPuTs.clear();
			this.Probability_Walks.clear();
			// initialize the data for the new person
			this.personId = row[0];
			this.tripId = row[1];
			this.startDate = row[6];
			this.startTime = row[7];
			this.startCoord = new CoordImpl(Double.parseDouble(row[3]) / 1000.0,
					Double.parseDouble(row[4]) / 1000.0);
			this.cntSegments = 0;
			this.cntPuTSegments = 0;
			this.cntRailSegments = 0;
		}
		this.segmentIds.add(row[2]);
		this.endDate = row[11];
		this.endTime = row[12];
		this.endCoord = new CoordImpl(Double.parseDouble(row[8]) / 1000.0, Double
				.parseDouble(row[9]) / 1000.0);
		this.cntSegments++;
		this.Probability_Walks.add(Double.valueOf(row[15]));
		this.Probability_Bikes.add(Double.valueOf(row[16]));
		this.Probability_Cars.add(Double.valueOf(row[17]));
		this.Probability_UrbanPuTs.add(Double.valueOf(row[18]));
		this.Probability_Rails.add(Double.valueOf(row[19]));
		if (Double.parseDouble(row[18]) >= 0.5) {
			this.cntPuTSegments++;
		}
		if (Double.parseDouble(row[19]) >= 0.5) {
			this.cntRailSegments++;
		}
	}

	private void handleTrip() throws IOException {
		if (this.cntPuTSegments > 0 || this.cntRailSegments > 0) {
			// the last trip had at least one public transport segment, so write
			// the person out
			// write basic information
			for (int i = 0; i < this.segmentIds.size(); i++)
				this.writer.write(this.personId + "\t" + this.tripId + "\t"
						+ this.segmentIds.get(i) + "\t" + this.cntSegments
						+ "\t" + this.cntPuTSegments + "\t"
						+ this.cntRailSegments + "\t"
						+ this.startCoord.getX() * 1000.0 + "\t"
						+ this.startCoord.getY() * 1000.0 + "\t"
						+ this.startDate + "\t" + this.startTime + "\t"
						+ this.endCoord.getX() * 1000.0 + "\t"
						+ this.endCoord.getY() * 1000.0 + "\t" + this.endDate
						+ "\t" + this.endTime + "\t"
						+ this.Probability_Walks.get(i) + "\t"
						+ this.Probability_Bikes.get(i) + "\t"
						+ this.Probability_Cars.get(i) + "\t"
						+ this.Probability_UrbanPuTs.get(i) + "\t"
						+ this.Probability_Rails.get(i) + "\n");
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
