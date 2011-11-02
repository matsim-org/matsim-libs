/* *********************************************************************** *
 * project: org.matsim.*
 * HouseholdSetMeetingPointEventImpl.java
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

import java.util.Map;

import org.matsim.api.core.v01.Id;

/**
 * @author cdobler
 */
public class HouseholdSetMeetingPointEventImpl extends HouseholdEventImpl implements HouseholdSetMeetingPointEvent {

	public static final String ATTRIBUTE_LINK = "link";
	public static final String ATTRIBUTE_FACILITY = "facility";
	public static final String EVENT_TYPE = "householdsetmeetingpoint";
	
	private final Id facilityId;
	
	public HouseholdSetMeetingPointEventImpl(final double time, final Id householdId, final Id facilityId) {
		super(time, householdId);
		this.facilityId = facilityId;
	}
	
	@Override
	public Id getFacilityId() {
		return this.facilityId;
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		
		attr.put(ATTRIBUTE_FACILITY, this.facilityId.toString());
		return attr;
	}
	
	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}

}
