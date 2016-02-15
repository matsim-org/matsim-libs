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
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.core.utils.misc.Time;

import javax.inject.Inject;

/**
 * @author mrieser
 *
 * Calculates the distribution of legs-durations, e.g. how many legs took at
 * most 5 minutes, how many between 5 and 10 minutes, and so on.
 * Also calculates the average trip duration.
 * Trips ended because of vehicles being stuck are not counted.
 */
public class CalcLegTimes implements PersonDepartureEventHandler, PersonArrivalEventHandler, 
	ActivityEndEventHandler, ActivityStartEventHandler {

	private final static Logger log = Logger.getLogger(CalcLegTimes.class);
	
	private static final int SLOT_SIZE = 300;	// 5-min slots
	private static final int MAXINDEX = 12; // slots 0..11 are regular slots, slot 12 is anything above

	private final Map<Id<Person>, Double> agentDepartures = new HashMap<>();
	private final Map<Id<Person>, Double> agentArrivals = new HashMap<>();
	private final Map<String, int[]> legStats = new TreeMap<>();
	private final Map<Id<Person>, String> previousActivityTypes = new HashMap<>();
	private double sumTripDurations = 0;
	private int sumTrips = 0;

	@Inject
	CalcLegTimes(EventsManager eventsManager) {
		eventsManager.addHandler(this);
	}

	public CalcLegTimes() {

	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		this.previousActivityTypes.put(event.getPersonId(), event.getActType());
	}
	
	@Override
	public void handleEvent(final PersonDepartureEvent event) {
		this.agentDepartures.put(event.getPersonId(), event.getTime());
	}

	@Override
	public void handleEvent(final PersonArrivalEvent event) {
		this.agentArrivals.put(event.getPersonId(), event.getTime());
	}


	@Override
	public void handleEvent(ActivityStartEvent event) {
		Double depTime = this.agentDepartures.remove(event.getPersonId());
		Double arrTime = this.agentArrivals.remove(event.getPersonId());
		if (depTime != null) {
			double travTime = arrTime - depTime;
			String fromActType = previousActivityTypes.remove(event.getPersonId());
			String toActType = event.getActType();
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

			this.sumTripDurations += travTime;
			this.sumTrips++;
		}
	}

	
	@Override
	public void reset(final int iteration) {
		this.previousActivityTypes.clear();
		this.agentDepartures.clear();
		this.legStats.clear();
		this.sumTripDurations = 0;
		this.sumTrips = 0;
	}

	public Map<String, int[]> getLegStats() {
		return this.legStats;
	}

	public static int getTimeslotIndex(final double time_s) {
		int idx = (int)(time_s / SLOT_SIZE);
		if (idx > MAXINDEX) idx = MAXINDEX;
		return idx;
	}

	public double getAverageTripDuration() {
		return (this.sumTripDurations / this.sumTrips);
	}

	public void writeStats(final String filename) {
		BufferedWriter legStatsFile = IOUtils.getBufferedWriter(filename);
		writeStats(legStatsFile);
		try {
			if (legStatsFile != null) {
				legStatsFile.close();
			}
		} catch (IOException e) {
			log.error(e);
		}
	}

	public void writeStats(final java.io.Writer out) throws UncheckedIOException {
		try {
		boolean first = true;
		for (Map.Entry<String, int[]> entry : this.legStats.entrySet()) {
			String key = entry.getKey();
			int[] counts = entry.getValue();
			if (first) {
				first = false;
				out.write("pattern");
				for (int i = 0; i < counts.length; i++) {
					out.write("\t" + (i*SLOT_SIZE/60) + "+");
				}
				out.write("\n");
			}
			out.write(key);
            for (int count : counts) {
                out.write("\t" + count);
            }
			out.write("\n");
		}
		out.write("\n");
		if (this.sumTrips == 0) {
			out.write("average trip duration: no trips!");
		} else {
			out.write("average trip duration: "
					+ (this.sumTripDurations / this.sumTrips) + " seconds = "
					+ Time.writeTime(((int)(this.sumTripDurations / this.sumTrips))));
		}
		out.write("\n");
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		} finally {
			try {
				out.flush();
			} catch (IOException e) {
				log.error(e);
			}
		}
	}
}