/* *********************************************************************** *
 * project: org.matsim.*
 * CalcLegTimes.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.christoph.events.implementations;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.core.utils.misc.Time;

import playground.christoph.events.EventHandlerInstance;
import playground.christoph.events.MasterEventHandler;

/**
 * @author mrieser
 * @author cdobler
 *
 * Calculates the distribution of legs-durations, e.g. how many legs took at
 * most 5 minutes, how many between 5 and 10 minutes, and so on.
 * Also calculates the average trip duration.
 * Trips ended because of vehicles being stuck are not counted.
 */
public class CalcLegTimes implements MasterEventHandler {

	private final static Logger log = Logger.getLogger(CalcLegTimes.class);
	
	/*package*/ static final int SLOT_SIZE = 300;	// 5-min slots
	/*package*/ static final int MAXINDEX = 12; // slots 0..11 are regular slots, slot 12 is anything above

	private final ConcurrentMap<Id, Double> agentDepartures = new ConcurrentHashMap<Id, Double>();
	private final ConcurrentMap<Id, Double> agentArrivals = new ConcurrentHashMap<Id, Double>();
	private final ConcurrentMap<String, int[]> legStats = new ConcurrentHashMap<String, int[]>();
	private final ConcurrentMap<Id, String> previousActivityTypes = new ConcurrentHashMap<Id, String>();
	private double sumTripDurations = 0;
	private int sumTrips = 0;
	
	private final Set<CalcLegTimesInstance> instances = new LinkedHashSet<CalcLegTimesInstance>();
	
	@Override
	public void reset(final int iteration) {
		this.previousActivityTypes.clear();
		this.agentDepartures.clear();
		this.legStats.clear();
		this.sumTripDurations = 0;
		this.sumTrips = 0;
	}

	// Seems not be called anywhere in the original class - therefore remove it??
//	public Map<String, int[]> getLegStats() {
//		return this.legStats;
//	}

	// TODO: ensure that finishEventsHandling() has been called!
	public double getAverageTripDuration() {
		return (this.sumTripDurations / this.sumTrips);
	}

	public void writeStats(final String filename) {
		BufferedWriter legStatsFile = null;
		legStatsFile = IOUtils.getBufferedWriter(filename);
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
		
		Set<String> orderedKeySet = new TreeSet<String>();
		orderedKeySet.addAll(this.legStats.keySet());
		for (String key : orderedKeySet) {
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

	@Override
	public EventHandlerInstance createInstance() {
		CalcLegTimesInstance instance = new CalcLegTimesInstance(this.agentDepartures, this.agentArrivals,
				this.legStats, this.previousActivityTypes);
		instances.add(instance);
		return instance;
	}

	@Override
	public void finishEventsHandling() {
		for (CalcLegTimesInstance instance : instances) {
			this.sumTripDurations += instance.getSumTripDurations();
			this.sumTrips += instance.getSumTrips();
		}
	}

	@Override
	public void synchronize(double time) {
		for (CalcLegTimesInstance instance : instances) instance.synchronize(time);
	}
}