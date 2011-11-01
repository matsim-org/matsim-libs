/* *********************************************************************** *
 * project: org.matsim.*
 * HouseholdActivityEventImpl.java
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
public abstract class HouseholdActivityEventImpl extends HouseholdEventImpl implements HouseholdActivityEvent {

	public static final String ATTRIBUTE_LINK = "link";
	public static final String ATTRIBUTE_FACILITY = "facility";
	public static final String ATTRIBUTE_ACTTYPE = "actType";

	private final Id linkId;
	private final Id facilityId;
	private final String acttype;
	
	HouseholdActivityEventImpl(final double time, final Id householdId, final Id linkId, final Id facilityId, final String acttype) {
		super(time, householdId);
		this.linkId = linkId;
		this.facilityId = facilityId;
		this.acttype = acttype == null ? "" : acttype;
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();

		attr.put(ATTRIBUTE_LINK, this.linkId.toString());
		if (this.facilityId != null) {
			attr.put(ATTRIBUTE_FACILITY, this.facilityId.toString());
		}
		attr.put(ATTRIBUTE_ACTTYPE, this.acttype);
		return attr;
	}

	@Override
	public String getActType() {
		return this.acttype;
	}

	@Override
	public Id getLinkId() {
		return this.linkId;
	}

	@Override
	public Id getFacilityId() {
		return this.facilityId;
	}
}