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

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.Facility;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a park and ride facility, linking a network link to transit stops
 * @author thibautd
 */
public class ParkAndRideFacility implements Facility {
	private final Map<String, Object> customAttributes = new HashMap<String, Object>();
	private final Coord coord;
	private final Id<Link> linkId;
	private final Id<ActivityFacility> id;
	private final List<Id<TransitStopFacility>> stopsIds;

	public ParkAndRideFacility(
			final Id<ActivityFacility> id,
			final Coord coord,
			final Id<Link> linkId,
			final List<Id<TransitStopFacility>> stopsIds) {
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
	public Id<ActivityFacility> getId() {
		return id;
	}

	@Override
	public Map<String, Object> getCustomAttributes() {
		return customAttributes;
	}

	@Override
	public Id<Link> getLinkId() {
		return linkId;
	}

	/**
	 * Gives access to the associated stops. The cost of walking from the parking to
	 * the stop is considered as being the same for all stops. Thus, they should correspond
	 * to the same "physical" station. It should even be wise to have only one stop
	 * associated.
	 * Possibility to walk to another stop is handled by the router itself, using the
	 * "transfer" links of the routing network.
	 *
	 * @return the list of associated stops Ids, as they are referenced in the schedule
	 * @deprecated this is not actually used to determine the pt stops asociated
	 * with a PNR facility! The PT router walk distance is used instead!
	 */
	@Deprecated 
	public List<Id<TransitStopFacility>> getStopsFacilitiesIds() {
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

