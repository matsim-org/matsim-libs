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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.IdMap;
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
import org.matsim.core.utils.misc.Time;

import jakarta.inject.Inject;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author mrieser
 *
 * Calculates the distribution of legs-durations, e.g. how many legs took at
 * most 5 minutes, how many between 5 and 10 minutes, and so on.
 * Also calculates the average leg duration.
 * Legs ended because of vehicles being stuck are not counted.
 */
public class CalcLegTimes implements PersonDepartureEventHandler, PersonArrivalEventHandler,
	ActivityEndEventHandler, ActivityStartEventHandler {

	private final static Logger log = LogManager.getLogger(CalcLegTimes.class);

	private static final int SLOT_SIZE = 300;    // 5-min slots
	private static final int MAXINDEX = 12; // slots 0..11 are regular slots, slot 12 is anything above

	private final IdMap<Person, Double> agentDepartures = new IdMap<>(Person.class);
	private final IdMap<Person, Double> agentArrivals = new IdMap<>(Person.class);
	private final Map<String, int[]> legStats = new TreeMap<>();
	private final IdMap<Person, String> previousActivityTypes = new IdMap<>(Person.class);
	private double sumLegDurations = 0;
	private int sumLegs = 0;

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
				stats = new int[MAXINDEX + 1];
				for (int i = 0; i <= MAXINDEX; i++) {
					stats[i] = 0;
				}
				this.legStats.put(legType, stats);
			}
			stats[getTimeslotIndex(travTime)]++;

			this.sumLegDurations += travTime;
			this.sumLegs++;
		}
	}


	@Override
	public void reset(final int iteration) {
		this.previousActivityTypes.clear();
		this.agentDepartures.clear();
		this.legStats.clear();
		this.sumLegDurations = 0;
		this.sumLegs = 0;
	}

	public Map<String, int[]> getLegStats() {
		return this.legStats;
	}

	public static int getTimeslotIndex(final double time_s) {
		int idx = (int) (time_s / SLOT_SIZE);
		if (idx > MAXINDEX) idx = MAXINDEX;
		return idx;
	}

	public double getAverageLegDuration() {
		return (this.sumLegDurations / this.sumLegs);
	}

	public void writeStats(final String filename) {
		try (BufferedWriter legStatsFile = IOUtils.getBufferedWriter(filename)) {
			writeStats(legStatsFile);
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
						out.write("\t" + (i * SLOT_SIZE / 60) + "+");
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
			if (this.sumLegs == 0) {
				out.write("average legs duration: no legs!");
			} else {
				out.write("average leg duration: "
						+ (this.sumLegDurations / this.sumLegs)
						+ " seconds = "
						+ Time.writeTime(((int) (this.sumLegDurations / this.sumLegs))));
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
