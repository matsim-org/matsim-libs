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

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;

public class Count<T> {

	private final Id<T> linkId;
	private String stationName;

	private final HashMap<Integer,Volume> volumes = new HashMap<Integer, Volume>();
	private Coord coord;


	protected Count(final Id<T> linkId2, final String stationName) {
		this.linkId = linkId2;
		this.stationName = stationName;
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
		// overkill?
		Volume v = new Volume(h,val);
		this.volumes.put(Integer.valueOf(h), v);
		return v;
	}

	public final void setCsId(final String cs_id) {
		this.stationName = cs_id;
	}

	public final Id<T> getLocId() {
		return this.linkId;
	}

	public final String getCsId() {
		return this.stationName;
	}

	public final Volume getMaxVolume() {
		Volume v_max = null;
		double max = -1.0;
		for (Volume v : this.volumes.values()) {
			if (v.getValue() > max) { max = v.getValue(); v_max = v; }
		}
		return v_max;
	}

	public final Volume getVolume(final int h) {
		return this.volumes.get(Integer.valueOf(h));
	}

	public final HashMap<Integer,Volume> getVolumes() {
		return this.volumes;
	}

	public void setCoord(final Coord coord) {
		this.coord = coord;
	}

	/** @return Returns the exact coordinate, where this counting station is
	 * located, or null if no exact location is available.
	 **/
	public Coord getCoord() {
		return this.coord;
	}

	@Override
	public final String toString() {
		return "[Loc_id=" + this.linkId + "]" +
		"[cs_id=" + this.stationName + "]" +
		"[nof_volumes=" + this.volumes.size() + "]";
	}

}
