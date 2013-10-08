/* *********************************************************************** *
 * project: org.matsim.*
 * PersonInformationEventImpl.java
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

package playground.christoph.evacuation.events;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;

/**
 * @author cdobler
 */
public class HouseholdInformationEventImpl extends Event implements HouseholdInformationEvent {

	private final Id householdId;

	public HouseholdInformationEventImpl(final double time, final Id householdId) {
		super(time);
		this.householdId = householdId;
	}
	
	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		attr.put(ATTRIBUTE_HOUSEHOLD, this.householdId.toString());
		return attr;
	}

	@Override
	public Id getHouseholdId() {
		return this.householdId;
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}
}