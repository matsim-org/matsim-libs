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

import org.matsim.api.core.v01.Id;

import java.util.TreeMap;

public class Counts<T> {

    public static final String ELEMENT_NAME = "counts";

	private String name = null;
	private String desc = null;
	private int year = 0;
	private final TreeMap<Id<T>, Count<T>> counts = new TreeMap<>();

	/**
	 * @param linkId the link to which the counting station is assigned, must be unique
	 * @param stationName some additional identifier for humans, e.g. the original name/id of the counting station
 	 * @return the created Count object, or {@linkplain RuntimeException} if it could not be created because it already exists
	 */
	public final Count<T> createAndAddCount(final Id<T> linkId, final String stationName) {
		// check id string for uniqueness
		if (this.counts.containsKey(linkId)) {
			throw new RuntimeException("There is already a counts object for location " + linkId);
		}
		Count<T> c = new Count<>(linkId, stationName);
		this.counts.put(linkId, c);
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

	public final String getName() {
		return this.name;
	}

	public final String getDescription() {
		return this.desc;
	}

	public final int getYear() {
		return this.year;
	}

	public final TreeMap<Id<T>, Count<T>> getCounts() {
		return this.counts;
	}

	public final Count<T> getCount(final Id<T> locId) {
		return this.counts.get(locId);
	}

	@Override
	public final String toString() {
		return "[name=" + this.name + "]" + "[nof_counts=" + this.counts.size() + "]";
	}
}
