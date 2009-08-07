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

import org.matsim.api.basic.v01.Id;

import playground.johannes.plans.plain.PlainActivity;

/**
 * @author illenberger
 *
 */
public class PlainActivityImpl extends PlainPlanElementImpl implements PlainActivity {

	private Id facilityId;

	public Id getFacilityId() {
		return facilityId;
	}

	public void setFacilityId(Id id) {
		facilityId = id;
		modified();
	}


}
