/* *********************************************************************** *
 * project: org.matsim.*
 * Count.java
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

package org.matsim.counts;

import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.TransportMode;

import java.util.HashMap;
import java.util.OptionalDouble;

/**
 * This class is a wrapper around the newer {@link MeasurementLocation}.
 *
 * @param <T>
 */
@Deprecated
public class Count<T> implements Identifiable<T> {


	private static final Logger log = LogManager.getLogger(Counts.class);

	private final MeasurementLocation<T> location;
	private final Measurable volume;

	protected Count(MeasurementLocation<T> v) {
		this.location = v;

		String mode;
		if (location.hasMeasurableForMode(Measurable.VOLUMES, Measurable.ANY_MODE))
			mode = Measurable.ANY_MODE;
		else
			mode = TransportMode.car;

		// Counts only support hourly volumes
		this.volume = location.createVolume(mode, Measurable.HOURLY);

		if (!this.volume.supportsHourlyAggregate()) {
			log.warn("Unsupported counts interval '" + this.volume.getInterval() + "' for analysis functionality. " +
				"Interval should be able to aggregate hourly, otherwise counts will be empty");
		}
	}

	/**
	 * Creates and adds a {@link Volume} to the {@link Count}ing station.
	 *
	 * @param h   indicating the hour-of-day. <b><i>Note: the hours for a counting
	 *            station must be from 1-24, and <b><i>not</i></b> from 0-23,
	 *            otherwise the {@link MatsimCountsReader} will throw an error.
	 *            </i></b>
	 * @param val the total number of vehicles counted during hour <code>h</code>.
	 * @return the {@link Count}ing station's {@link Volume}.
	 */
	public final Volume createVolume(final int h, final double val) {
		if (h < 1) {
			throw new RuntimeException("counts start at 1, not at 0.  If you have a use case where you need to go below one, "
				+ "let us know and we think about it, but so far we had numerous debugging sessions because someone inserted counts at 0.");
		}

		Volume v = new Volume(h, val);

		// Only hourly volumes can be set
		if (this.volume.getInterval() == Measurable.HOURLY)
			this.volume.setAtHour(h - 1, val);
		else
			log.warn("Unsupported counts '" + this.volume.getInterval() + "' for setting values. Resolution must be hourly.");

		return v;
	}

	public final void setCsId(final String cs_id) {
		location.setStationName(cs_id);
	}

	@Override
	public final Id<T> getId() {
		return location.getRefId();
	}

	public final String getCsLabel() {
		return location.getStationName();
	}

	public final Volume getMaxVolume() {
		int hour = -1;
		double max = -1.0;
		for (Int2DoubleMap.Entry e : this.volume) {
			if (e.getDoubleValue() > max) {
				max = e.getDoubleValue();
				hour = e.getIntKey() / Measurable.HOURLY;
			}
		}
		return hour >= 0 ? new Volume(hour + 1, max) : null;
	}

	public final Volume getVolume(final int h) {
		if (this.volume.supportsHourlyAggregate()) {
			// Uses old format where hours starts at 1
			OptionalDouble v = this.volume.aggregateAtHour(h - 1);
			return v.isPresent() ? new Volume(h, v.getAsDouble()) : null;
		}

		return null;
	}

	public final HashMap<Integer, Volume> getVolumes() {
		HashMap<Integer, Volume> res = new HashMap<>();
		for (Int2DoubleMap.Entry e : volume) {
			// Offset for old API
			int h = (e.getIntKey() / Measurable.HOURLY) + 1;
			res.put(h, new Volume(h, e.getDoubleValue()));
		}

		return res;
	}

	/**
	 * @return Returns the exact coordinate, where this counting station is
	 * located, or null if no exact location is available.
	 **/
	public Coord getCoord() {
		return location.getCoordinates();
	}

	public void setCoord(final Coord coord) {
		location.setCoordinates(coord);
	}

	@Override
	public final String toString() {
		return "[Loc_id=" + this.location.getId() + "]" +
			"[cs_id=" + this.location.getStationName() + "]" +
			"[nof_volumes=" + this.volume.size() + "]";
	}

}
