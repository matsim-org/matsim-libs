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

import org.matsim.utils.identifiers.IdI;

public class Count {

	private final IdI locId;
	private String csId;

	private HashMap<Integer,Volume> volumes_=new HashMap<Integer, Volume>();


	protected Count(final IdI locId, final String csId) {
		this.locId = locId;
		this.csId = csId;
	}

	public final Volume createVolume(final int h, final double val) {
		// overkill?
		Volume v = new Volume(h,val);
		this.volumes_.put(Integer.valueOf(h), v);
		return v;
	}

	public final void setCsId(final String cs_id) {
		this.csId = cs_id;
	}

	public final IdI getLocId() {
		return this.locId;
	}

	public final String getCsId() {
		return this.csId;
	}


	public final Volume getVolume(final int h) {
		return this.volumes_.get(Integer.valueOf(h));
	}

	public final HashMap<Integer,Volume> getVolumes() {
		return this.volumes_;
	}

	@Override
	public final String toString() {
		return "[Loc_id=" + this.locId + "]" +
				"[cs_id=" + this.csId + "]" +
				"[nof_volumes=" + this.volumes_.size() + "]";
	}
}
