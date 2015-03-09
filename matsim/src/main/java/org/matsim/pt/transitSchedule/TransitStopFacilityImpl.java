/* *********************************************************************** *
 * project: org.matsim.*
 * TransitStopFacility.java
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

package org.matsim.pt.transitSchedule;

import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.utils.customize.Customizable;
import org.matsim.utils.customize.CustomizableUtils;

/**
 * A facility (infrastructure) describing a public transport stop.
 *
 * @author mrieser
 */
public class TransitStopFacilityImpl implements TransitStopFacility {
	
	private final Id<TransitStopFacility> id;
	private String stopPostAreaId;
	private final Coord coord;
	private Id<Link> linkId = null;
	private final boolean isBlockingLane;
	private String name = null;
	private Customizable customizableDelegate;

	protected TransitStopFacilityImpl(final Id<TransitStopFacility> id, final Coord coord, final boolean isBlockingLane) {
		this.id = id;
		this.coord = coord;
		this.isBlockingLane = isBlockingLane;
		this.stopPostAreaId = id.toString();
	}

	@Override
	public void setLinkId(final Id<Link> linkId) {
		this.linkId = linkId;
	}

	@Override
	public Id<Link> getLinkId() {
		return this.linkId;
	}

	@Override
	public Coord getCoord() {
		return this.coord;
	}

	@Override
	public Id<TransitStopFacility> getId() {
		return this.id;
	}

	@Override
	public boolean getIsBlockingLane() {
		return this.isBlockingLane;
	}

	@Override
	public void setName(final String name) {
		this.name = name;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public String toString() {
		return "TransitStopFacilityImpl_" + this.id;
	}

	@Override
	public String getStopPostAreaId() {
		return stopPostAreaId;
	}

	@Override
	public void setStopPostAreaId(String stopPostAreaId) {
		this.stopPostAreaId = stopPostAreaId;
	}
	
	@Override
	public Map<String, Object> getCustomAttributes() {
		if (this.customizableDelegate == null) {
			this.customizableDelegate = CustomizableUtils.createCustomizable();
		}
		return this.customizableDelegate.getCustomAttributes();
	}


}
