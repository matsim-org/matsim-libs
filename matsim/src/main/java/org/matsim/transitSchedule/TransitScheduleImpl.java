/* *********************************************************************** *
 * project: org.matsim.*
 * TransitSchedule.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package org.matsim.transitSchedule;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.transitSchedule.api.TransitLine;
import org.matsim.transitSchedule.api.TransitSchedule;
import org.matsim.transitSchedule.api.TransitScheduleFactory;
import org.matsim.transitSchedule.api.TransitStopFacility;


/**
 * Default implementation of {@link TransitSchedule}.
 * 
 * {@inheritDoc}
 * 
 * @author mrieser
 */
public class TransitScheduleImpl implements TransitSchedule {

	private final Map<Id, TransitLine> transitLines = new TreeMap<Id, TransitLine>();
	private final Map<Id, TransitStopFacility> stopFacilities = new TreeMap<Id, TransitStopFacility>();
	private final TransitScheduleFactory factory;
	
	protected TransitScheduleImpl(final TransitScheduleFactory builder) {
		this.factory = builder;
	}

	public void addTransitLine(final TransitLine line) {
		final Id id = line.getId();
		if (this.transitLines.containsKey(id)) {
			throw new IllegalArgumentException("There is already a transit line with id " + id.toString());
		}
		this.transitLines.put(id, line);
	}
	
	public void addStopFacility(final TransitStopFacility stop) {
		final Id id = stop.getId();
		if (this.stopFacilities.containsKey(id)) {
			throw new IllegalArgumentException("There is already a stop facility with id " + id.toString());
		}
		this.stopFacilities.put(id, stop);
	}

	public Map<Id, TransitLine> getTransitLines() {
		return Collections.unmodifiableMap(this.transitLines);
	}
	
	public Map<Id, TransitStopFacility> getFacilities() {
		return Collections.unmodifiableMap(this.stopFacilities);
	}
	
	public TransitScheduleFactory getFactory() {
		return this.factory;
	}

}
