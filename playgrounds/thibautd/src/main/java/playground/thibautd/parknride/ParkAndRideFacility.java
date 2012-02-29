/* *********************************************************************** *
 * project: org.matsim.*
 * ParkAndRideFacility.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.thibautd.parknride;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.facilities.Facility;

/**
 * Represents a park and ride facility, linking a network link to transit stops
 * @author thibautd
 */
public class ParkAndRideFacility implements Facility {
	private final Map<String, Object> customAttributes = new HashMap<String, Object>();
	private final Coord coord;
	private final Id linkId;
	private final Id id;
	private final List<Id> stopsIds;

	public ParkAndRideFacility(
			final Id id,
			final Coord coord,
			final Id linkId,
			final List<Id> stopsIds) {
		this.coord = coord;
		this.id = id;
		this.linkId = linkId;
		this.stopsIds = Collections.unmodifiableList( stopsIds );
	}

	@Override
	public Coord getCoord() {
		return coord;
	}

	@Override
	public Id getId() {
		return id;
	}

	@Override
	public Map<String, Object> getCustomAttributes() {
		return customAttributes;
	}

	@Override
	public Id getLinkId() {
		return linkId;
	}

	public List<Id> getStopsFacilitiesIds() {
		return stopsIds;
	}

	@Override
	public boolean equals(final Object other) {
		if (other instanceof ParkAndRideFacility) {
			ParkAndRideFacility facility = (ParkAndRideFacility) other;

			return id.equals( facility.id ) &&
				coord.equals( facility.coord ) &&
				linkId.equals( facility.linkId ) &&
				stopsIds.size() == facility.stopsIds.size() &&
				stopsIds.containsAll( facility.stopsIds );
		}

		return false;
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}
}

