/* *********************************************************************** *
 * project: org.matsim.*
 * ActivityImpl.java
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
package playground.johannes.plans.view.impl;

import java.util.Map;

import org.matsim.api.basic.v01.Id;

import playground.johannes.plans.plain.impl.PlainActivityImpl;
import playground.johannes.plans.view.Activity;
import playground.johannes.plans.view.Facility;

/**
 * @author illenberger
 *
 */
public class ActivityView extends PlanElementView<PlainActivityImpl> implements Activity {

	private Map<Id, Facility> facilityMapping;
	
	public ActivityView(PlainActivityImpl rawAct) {
		super(rawAct);
	}

	public Facility getFacility() {
		return facilityMapping.get(delegate.getFacilityId());
	}

	public void setFacility(Facility facility) {
		delegate.setFacilityId(facility.getId());
	}

	@Override
	protected void update() {
	}

}
