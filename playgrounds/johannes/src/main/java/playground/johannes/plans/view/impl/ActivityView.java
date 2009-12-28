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

import org.matsim.api.core.v01.network.Link;

import playground.johannes.plans.plain.impl.PlainActivityImpl;
import playground.johannes.plans.view.Activity;
import playground.johannes.plans.view.Facility;

/**
 * @author illenberger
 *
 */
public class ActivityView extends PlanElementView<PlainActivityImpl> implements Activity {
	
	public ActivityView(PlainActivityImpl rawAct) {
		super(rawAct);
	}

	public Facility getFacility() {
		return IdMapping.getFacility(delegate.getFacilityId());
	}

	public void setFacility(Facility facility) {
		delegate.setFacilityId(facility.getId());
	}

	@Override
	protected void update() {
	}

	public Link getLink() {
		Facility f = getFacility();
		if(f != null) {
			return f.getLink();
		} else
			return IdMapping.getLink(delegate.getLinkId());
	}

	public String getType() {
		return delegate.getType();
	}

	public void setLink(Link link) {
		if(getFacility() == null)
			delegate.setLinkId(link.getId());
		else
			throw new UnsupportedOperationException("Link can only be modified via the facility.");
	}

	public void setType(String type) {
		delegate.setType(type);
	}

}
