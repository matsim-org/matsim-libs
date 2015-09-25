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

package org.matsim.contrib.cadyts.measurement;

import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

// like Counts, but not based on Id<Measurement>
public class Measurements {

    public static final String ELEMENT_NAME = "counts";

//	private String name = null;
//	private String desc = null;
//	private int year = 0;
//	private final TreeMap<Id<Measurement>, Count> counts = new TreeMap<Id<Measurement>, Count>();
	private final TreeMap<Id<Measurement>, Measurement> measurements = new TreeMap<>();

	/**
	 * @param measurementId the link to which the counting station is assigned, must be unique
	 * @param stationName some additional identifier for humans, e.g. the original name/id of the counting station
	 * @return the created Count object, or null if it could not be created (maybe because it already exists)
	 */
	public final Measurement createAndAddMeasurement(final Id<Measurement> measurementId) {
//	public final Measurement createAndAddCount(final Integer linkId, final String stationName) {
		// check id string for uniqueness
		if (this.measurements.containsKey(measurementId)) {
			return null;
		}
//		Measurement measurment = new Measurement(linkId, stationName);
		Measurement measurement = new Measurement(measurementId);
		this.measurements.put(measurementId, measurement);
		return measurement;
	}

//	public final void setName(final String name) {
//		this.name = name;
//	}
//
//	public final void setDescription(final String desc) {
//		this.desc = desc;
//	}
//
//	public final void setYear(final int year) {
//		this.year = year;
//	}
//
//	public final String getName() {
//		return this.name;
//	}
//
//	public final String getDescription() {
//		return this.desc;
//	}
//
//	public final int getYear() {
//		return this.year;
//	}

//	public final TreeMap<Id<Measurement>, Measurement> getCounts() {
	public final TreeMap<Id<Measurement>, Measurement> getMeasurements() {
		return this.measurements;
	}

//	public final Measurement getCount(final Id<Measurement> locId) {
	public final Measurement getMeasurment(final Id<Measurement> measurementId) {
		return this.measurements.get(measurementId);
	}

	@Override
	public final String toString() {
//		return "[name=" + this.name + "]" + "[nof_counts=" + this.measurements.size() + "]";
		return "[n_of_measurements=" + this.measurements.size() + "]";
	}
}
