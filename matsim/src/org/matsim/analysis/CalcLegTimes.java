/* *********************************************************************** *
 * project: org.matsim.*
 * CalcLegTimes.java
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

package org.matsim.analysis;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.TreeMap;

import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.core.api.population.Activity;
import org.matsim.core.api.population.Person;
import org.matsim.core.api.population.Plan;
import org.matsim.core.api.population.Population;
import org.matsim.core.events.AgentArrivalEvent;
import org.matsim.core.events.AgentDepartureEvent;
import org.matsim.core.events.handler.AgentArrivalEventHandler;
import org.matsim.core.events.handler.AgentDepartureEventHandler;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;

/**
 * @author mrieser
 *
 * Calculates the distribution of legs-durations, e.g. how many legs took at
 * most 5 minutes, how many between 5 and 10 minutes, and so on.
 * Also calculates the average trip duration.
 * Trips ended because of vehicles being stuck are not counted.
 */
public class CalcLegTimes implements AgentDepartureEventHandler, AgentArrivalEventHandler {

	private static final int SLOT_SIZE = 300;	// 5-min slots
	private static final int MAXINDEX = 12; // slots 0..11 are regular slots, slot 12 is anything above

	private Population population = null;
	private final TreeMap<Id, Double> agentDepartures = new TreeMap<Id, Double>();
	private final TreeMap<Id, Integer> agentLegs = new TreeMap<Id, Integer>();
	private final TreeMap<String, int[]> legStats = new TreeMap<String, int[]>();
	private TreeMap<TransportMode, Double> sumTripDurationsByMode = new TreeMap<TransportMode, Double>();
	private TreeMap<TransportMode, Integer> sumTripsByMode = new TreeMap<TransportMode, Integer>();
	private double sumTripDurations = 0;
	private int sumTrips = 0;

	public CalcLegTimes(final Population population) {
		this.population = population;
	}

	public void handleEvent(final AgentDepartureEvent event) {
		this.agentDepartures.put(event.getPersonId(), event.getTime());
		Integer cnt = this.agentLegs.get(event.getPersonId());
		if (cnt == null) cnt = 0;
		this.agentLegs.put(event.getPersonId(), cnt+1);
	}

	public void handleEvent(final AgentArrivalEvent event) {
		Double depTime = this.agentDepartures.remove(event.getPersonId());
		Person agent = this.population.getPerson(event.getPersonId());
		if (depTime != null && agent != null) {
			double travTime = event.getTime() - depTime;
			int legNr = this.agentLegs.get(event.getPersonId());
			Plan plan = agent.getSelectedPlan();
			int index = (legNr - 1) * 2;
			String fromActType = ((Activity)plan.getPlanElements().get(index)).getType();
			String toActType = ((Activity)plan.getPlanElements().get(index + 2)).getType();
			String legType = fromActType + "---" + toActType;
			int[] stats = this.legStats.get(legType);
			if (stats == null) {
				stats = new int[MAXINDEX+1];
				for (int i = 0; i <= MAXINDEX; i++) {
					stats[i] = 0;
				}
				this.legStats.put(legType, stats);
			}
			stats[getTimeslotIndex(travTime)]++;

			Double oldSumTripDuration = this.sumTripDurationsByMode.get(event.getLeg().getMode());
			if (oldSumTripDuration == null) oldSumTripDuration = 0.0;
			this.sumTripDurationsByMode.put(event.getLeg().getMode(), oldSumTripDuration + travTime);
			Integer oldSumTripsByMode = this.sumTripsByMode.get(event.getLeg().getMode());
			if (oldSumTripsByMode == null) oldSumTripsByMode = 0;
			this.sumTripsByMode.put(event.getLeg().getMode(), oldSumTripsByMode + 1);
			
			this.sumTripDurations += travTime;
			this.sumTrips++;
		}
	}

	public void reset(final int iteration) {
		this.agentDepartures.clear();
		this.agentLegs.clear();
		this.legStats.clear();
		this.sumTripDurationsByMode.clear();
		this.sumTripsByMode.clear();
		this.sumTripDurations = 0;
		this.sumTrips = 0;
	}

	public TreeMap<String, int[]> getLegStats() {
		return this.legStats;
	}

	private int getTimeslotIndex(final double time_s) {
		int idx = (int)(time_s / SLOT_SIZE);
		if (idx > MAXINDEX) idx = MAXINDEX;
		return idx;
	}

	public double getAverageTripDuration() {
		return (this.sumTripDurations / this.sumTrips);
	}

	public TreeMap<TransportMode, Double> getAverageTripDurationsByMode() {
		TreeMap<TransportMode, Double> averageTripDurations = new TreeMap<TransportMode, Double>();
		for (TransportMode mode : this.sumTripDurationsByMode.keySet()) {
			averageTripDurations.put(mode, this.sumTripDurationsByMode.get(mode) / this.sumTripsByMode.get(mode));
		}
		return averageTripDurations;
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

	public void writeStats(final java.io.Writer out) throws IOException {
		boolean first = true;
		for (String key : this.legStats.keySet()) {
			int[] counts = this.legStats.get(key);
			if (first) {
				first = false;
				out.write("pattern");
				for (int i = 0; i < counts.length; i++) {
					out.write("\t" + (i*SLOT_SIZE/60) + "+");
				}
				out.write("\n");
			}
			out.write(key);
			for (int i = 0; i < counts.length; i++) {
				out.write("\t" + counts[i]);
			}
			out.write("\n");
		}
		out.write("\n");
		for (TransportMode mode : this.sumTripDurationsByMode.keySet()) {
			double avgTripDuration = this.sumTripDurationsByMode.get(mode) / this.sumTripsByMode.get(mode);
			out.write(
					"average trip duration for mode " + mode.toString() + ": " + 
					Double.toString(avgTripDuration) + " seconds = " + 
					Time.writeTime(avgTripDuration));
			out.write(System.getProperty("line.separator"));
		}
		if (this.sumTrips == 0) {
			out.write("average trip duration: no trips!");
		} else {
			out.write("average trip duration: "
					+ (this.sumTripDurations / this.sumTrips) + " seconds = "
					+ Time.writeTime(((int)(this.sumTripDurations / this.sumTrips))));
		}
		out.write("\n");
		out.flush();
	}

}
