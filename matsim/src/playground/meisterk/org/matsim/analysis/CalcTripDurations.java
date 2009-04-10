/* *********************************************************************** *
 * project: org.matsim.*
 * CalcTripDurations.java
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

package playground.meisterk.org.matsim.analysis;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.TreeMap;

import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.population.BasicLeg;
import org.matsim.core.events.AgentArrivalEvent;
import org.matsim.core.events.AgentDepartureEvent;
import org.matsim.core.events.handler.AgentArrivalEventHandler;
import org.matsim.core.events.handler.AgentDepartureEventHandler;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;

/**
 * Calculates trip durations differentiated by mode and time-of-day.
 * 
 * @author meisterk
 *
 */
public class CalcTripDurations implements AgentArrivalEventHandler, AgentDepartureEventHandler {

	private static final int BIN_SIZE = Gbl.getConfig().travelTimeCalculator().getTraveltimeBinSize();
	private static final int TRIP_DURATIONS_ARRAY_SIZE = 30 * 3600 / CalcTripDurations.BIN_SIZE + 1;

	private final TreeMap<Id, Double> agentDepartures = new TreeMap<Id, Double>();
	private final TreeMap<BasicLeg.Mode, int[]> tripDurations = new TreeMap<BasicLeg.Mode, int[]>();
	private final TreeMap<BasicLeg.Mode, int[]> numTrips = new TreeMap<BasicLeg.Mode, int[]>();
	private final TreeMap<BasicLeg.Mode, Double> overallTripDurations = new TreeMap<BasicLeg.Mode, Double>();
	private final TreeMap<BasicLeg.Mode, Integer> overallNumTrips = new TreeMap<BasicLeg.Mode, Integer>();

	private int[] timeOfDayData;
	private BasicLeg.Mode mode;
	private Double depTime;
	
	public CalcTripDurations() {
	}

	public void handleEvent(AgentDepartureEvent event) {
		this.agentDepartures.put(event.getPersonId(), event.getTime());
	}

	public void handleEvent(AgentArrivalEvent event) {
		depTime = this.agentDepartures.remove(event.getPersonId());
		if (depTime != null) {
			mode = event.getLeg().getMode();
			double tripDuration = event.getTime() - depTime;
			
			// log trip duration for time slot
			timeOfDayData = this.tripDurations.get(mode);
			if (timeOfDayData == null) {
				timeOfDayData = new int[TRIP_DURATIONS_ARRAY_SIZE];
				java.util.Arrays.fill(timeOfDayData, 0);
				this.tripDurations.put(mode, timeOfDayData);
			}
			timeOfDayData[this.getTimeslotIndex(depTime)] += tripDuration;

			// log trip duration for overall average
			double oldOverallTripDuration = ( (this.overallTripDurations.get(mode) == null) ? 0.0 : this.overallTripDurations.get(mode) );
			this.overallTripDurations.put(mode, oldOverallTripDuration + tripDuration);
			
			// increase trip counter for time slot
			timeOfDayData = this.numTrips.get(mode);
			if (timeOfDayData == null) {
				// mode has to be initialized
				timeOfDayData = new int[TRIP_DURATIONS_ARRAY_SIZE];
				java.util.Arrays.fill(timeOfDayData, 0);
				this.numTrips.put(mode, timeOfDayData);
			}
			timeOfDayData[this.getTimeslotIndex(depTime)]++;
			
			// increase overall trip counter
			int oldNumTrips = ( (this.overallNumTrips.get(mode) == null) ? 0 : this.overallNumTrips.get(mode));
			this.overallNumTrips.put(mode, ++oldNumTrips);
		}
	}

	public void reset(int iteration) {
		this.agentDepartures.clear();
		this.tripDurations.clear();
		this.numTrips.clear();
		this.overallNumTrips.clear();
		this.overallTripDurations.clear();
	}

	public void writeStats(final String filename) {
		BufferedWriter legStatsFile = null;
		try {
			legStatsFile = IOUtils.getBufferedWriter(filename);
			writeStats(legStatsFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			if (legStatsFile != null) {
				legStatsFile.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected void writeStats(final java.io.Writer out) throws IOException {
		
		double avgTripDuration;
		
		// header
		out.write("time\ttime\t");
		for (BasicLeg.Mode mode : this.tripDurations.keySet()) {
			out.write(mode.toString() + "\t");
		}
		out.write(System.getProperty("line.separator"));

		// time-of-day data
		for (int ii=0; ii < CalcTripDurations.TRIP_DURATIONS_ARRAY_SIZE; ii++) {
			out.write(Integer.toString(ii * BIN_SIZE) + "\t" + Time.writeTime(ii * BIN_SIZE));
			for (BasicLeg.Mode mode : this.tripDurations.keySet()) {
				avgTripDuration = ( (this.numTrips.get(mode)[ii] == 0) ? Double.NaN : (this.tripDurations.get(mode)[ii] / this.numTrips.get(mode)[ii]) );
				out.write("\t" + Double.toString(avgTripDuration));
			}
			out.write(System.getProperty("line.separator"));
		}
		out.write(System.getProperty("line.separator"));

		// average data by mode
		for (BasicLeg.Mode mode : overallTripDurations.keySet()) {
			out.write(
					"average trip duration for mode " + mode.toString() + ": " +
					Double.toString(this.overallTripDurations.get(mode) / this.overallNumTrips.get(mode)) + " seconds = " +
					Time.writeTime(this.overallTripDurations.get(mode) / this.overallNumTrips.get(mode)));
			out.write(System.getProperty("line.separator"));
		}
		
		// flush
		out.flush();
	}
	
	private int getTimeslotIndex(double time_s) {
		int idx = (int)(time_s / CalcTripDurations.BIN_SIZE);
		if (idx >= CalcTripDurations.TRIP_DURATIONS_ARRAY_SIZE) idx = CalcTripDurations.TRIP_DURATIONS_ARRAY_SIZE - 1;
		return idx;
	}

}
