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

package playground.marcel.pt.transitSchedule;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.basic.v01.Id;
import org.matsim.transitSchedule.TransitStopFacility;

/**
 * 
 * 
 * @author mrieser
 */
public class TransitScheduleImpl {

	private final Map<Id, TransitLineImpl> transitLines = new TreeMap<Id, TransitLineImpl>();
	private final Map<Id, TransitStopFacility> stopFacilities = new TreeMap<Id, TransitStopFacility>();

	public TransitScheduleImpl() {
	}

	public void addTransitLine(final TransitLineImpl line) {
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

	public Map<Id, TransitLineImpl> getTransitLines() {
		return Collections.unmodifiableMap(this.transitLines);
	}
	
	public Map<Id, TransitStopFacility> getFacilities() {
		return Collections.unmodifiableMap(this.stopFacilities);
	}

}
