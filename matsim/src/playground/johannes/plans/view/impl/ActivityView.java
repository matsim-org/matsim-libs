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

import org.matsim.api.core.v01.network.Link;

import playground.johannes.plans.plain.impl.PlainActivityImpl;
import playground.johannes.plans.view.Activity;
import playground.johannes.plans.view.Facility;

/**
 * @author illenberger
 *
 */
public class ActivityView extends AbstractView<PlainActivityImpl> implements Activity {

	private Map<String, Link> linkMapping;
	
	private Map<String, Facility> facilityMapping;
	
	public ActivityView(PlainActivityImpl rawAct) {
		super(rawAct);
	}
	
	public Link getLink() {
		return linkMapping.get(getLinkId());
	}

	public String getLinkId() {
		return delegate.getLinkId();
	}

	public Facility getFacility() {
		return facilityMapping.get(getFacilityId());
	}

	public String getFacilityId() {
		return delegate.getFacilityId();
	}

	/* (non-Javadoc)
	 * @see playground.johannes.plans.view.impl.AbstractView#update()
	 */
	@Override
	protected void update() {
		// TODO Auto-generated method stub
		
	}

}
