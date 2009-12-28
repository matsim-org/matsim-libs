/* *********************************************************************** *
 * project: org.matsim.*
 * RawActivityImpl.java
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
package playground.johannes.plans.plain.impl;

import org.matsim.api.core.v01.Id;

import playground.johannes.plans.plain.PlainActivity;

/**
 * @author illenberger
 *
 */
public class PlainActivityImpl extends PlainPlanElementImpl implements PlainActivity {

//	public static final List<String> ACTIVITY_TYPES = new ArrayList<String>();
	
	private Id linkId;
	
	private Id facilityId;
	
	private String type;

	public Id getLinkId() {
		return linkId;
	}
	
	public void setLinkId(Id id) {
		linkId = id;
		modified();
	}
	
	public Id getFacilityId() {
		return facilityId;
	}

	public void setFacilityId(Id id) {
		facilityId = id;
		modified();
	}

	public String getType() {
		return type;
	}
	
	public void setType(String type) {
		this.type = type;
		modified();
	}

}
