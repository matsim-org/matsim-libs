/* *********************************************************************** *
 * project: org.matsim.*
 * HouseholdSeparatedEventImpl.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.christoph.evacuation.events;

import org.matsim.api.core.v01.Id;

/**
 * @author cdobler
 */
public class HouseholdSeparatedEventImpl extends HouseholdActivityEventImpl implements HouseholdJoinedEvent {

	public static final String EVENT_TYPE = "householdseparated";
	
	public HouseholdSeparatedEventImpl(final double time, final Id householdId, final Id linkId, final Id facilityId, final String acttype) {
		super(time, householdId, linkId, facilityId, acttype);
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}
}
