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

import java.util.HashMap;
import java.util.OptionalDouble;

import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.TransportMode;

/**
 * This class is a wrapper around the newer {@link MeasurementLocation}.
 * @param <T>
 */
public class Count<T> implements Identifiable<T> {
	private final MeasurementLocation<T> location;
	private final Measurable volume;

	protected Count(MeasurementLocation<T> v) {
		this.location = v;
		// Counts only support hourly volumes
		this.volume = location.createVolume(TransportMode.car, Measurable.HOURLY);
	}

	/**
	 * Creates and adds a {@link Volume} to the {@link Count}ing station.
	 * @param h indicating the hour-of-day. <b><i>Note: the hours for a counting
	 * 		station must be from 1-24, and <b><i>not</i></b> from 0-23,
	 * 		otherwise the {@link MatsimCountsReader} will throw an error.
	 * 		</i></b>
	 * @param val the total number of vehicles counted during hour <code>h</code>.
	 * @return the {@link Count}ing station's {@link Volume}.
	 */
	public final Volume createVolume(final int h, final double val) {
		if ( h < 1 ) {
			throw new RuntimeException( "counts start at 1, not at 0.  If you have a use case where you need to go below one, "
					+ "let us know and we think about it, but so far we had numerous debugging sessions because someone inserted counts at 0.") ;
		}

		Volume v = new Volume(h,val);
		this.volume.setAtHour(h, val);
		return v;
	}

	public final void setCsId(final String cs_id) {
		location.setStationName(cs_id);
	}

	@Override
	public final Id<T> getId() {
		return location.getId();
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
				hour = e.getIntKey() / 60;
			}
		}
		return hour >= 0 ? new Volume(hour, max) : null;
	}

	public final Volume getVolume(final int h) {
		OptionalDouble v = this.volume.getAtHour(h);
		return v.isPresent() ? new Volume(h, v.getAsDouble()) : null;
	}

	public final HashMap<Integer, Volume> getVolumes() {
		HashMap<Integer, Volume> res = new HashMap<>();
		for (Int2DoubleMap.Entry e : volume) {
			int h = e.getIntKey() / 60;
			res.put(h, new Volume(h, e.getDoubleValue()));
		}

		return res;
	}

	public void setCoord(final Coord coord) {
		location.setCoordinates(coord);
	}

	/** @return Returns the exact coordinate, where this counting station is
	 * located, or null if no exact location is available.
	 **/
	public Coord getCoord() {
		return location.getCoordinates();
	}

	@Override
	public final String toString() {
		return "[Loc_id=" + this.location.getId() + "]" +
		"[cs_id=" + this.location.getStationName() + "]" +
		"[nof_volumes=" + this.volume.size() + "]";
	}

}
