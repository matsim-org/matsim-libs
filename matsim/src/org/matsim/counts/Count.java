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

import org.matsim.interfaces.basic.v01.Id;
import org.matsim.utils.geometry.Coord;

public class Count {

	private final Id locId;
	private String csId;

	private final HashMap<Integer,Volume> volumes = new HashMap<Integer, Volume>();
	private Coord coord;


	protected Count(final Id locId, final String csId) {
		this.locId = locId;
		this.csId = csId;
	}

	public final Volume createVolume(final int h, final double val) {
		// overkill?
		Volume v = new Volume(h,val);
		this.volumes.put(Integer.valueOf(h), v);
		return v;
	}

	public final void setCsId(final String cs_id) {
		this.csId = cs_id;
	}

	public final Id getLocId() {
		return this.locId;
	}

	public final String getCsId() {
		return this.csId;
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
		return "[Loc_id=" + this.locId + "]" +
		"[cs_id=" + this.csId + "]" +
		"[nof_volumes=" + this.volumes.size() + "]";
	}

}
