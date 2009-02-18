/* *********************************************************************** *
 * project: org.matsim.*
 * Counts.java
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

import java.util.TreeMap;

import org.matsim.interfaces.basic.v01.Id;

public class Counts {

	private String name = null;
	private String desc = null;
	private int year = 0;
	private String layer = null;
	private final TreeMap<Id, Count> counts = new TreeMap<Id, Count>();

	/**
	 * @param locId
	 * @param csId
	 * @return the created Count object, or null if it could not be created (maybe because it already exists)
	 */
	public final Count createCount(final Id locId, final String csId) {
		// check id string for uniqueness
		if (this.counts.containsKey(locId)) {
			return null;
		}
		Count c = new Count(locId, csId);
		this.counts.put(locId, c);
		return c;
	}

	public final void setName(final String name) {
		this.name = name;
	}

	public final void setDescription(final String desc) {
		this.desc = desc;
	}

	public final void setYear(final int year) {
		this.year = year;
	}

	public final void setLayer(final String layer) {
		this.layer = layer;
	}

	public final String getName() {
		return this.name;
	}

	public final String getDescription() {
		return this.desc;
	}

	public final int getYear() {
		return this.year;
	}

	public final String getLayer() {
		return this.layer;
	}

	public final TreeMap<Id, Count> getCounts() {
		return this.counts;
	}

	public final Count getCount(final Id locId) {
		return this.counts.get(locId);
	}

	@Override
	public final String toString() {
		return "[name=" + this.name + "]" + "[nof_counts=" + this.counts.size() + "]";
	}
}
