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
import org.matsim.api.core.v01.Customizable;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.scenario.CustomizableUtils;
import org.matsim.pt.transitSchedule.api.TransitStopArea;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.utils.objectattributes.attributable.Attributes;
import org.matsim.utils.objectattributes.attributable.AttributesImpl;

/**
 * A facility (infrastructure) describing a public transport stop.
 *
 * @author mrieser
 */
public class TransitStopFacilityImpl implements TransitStopFacility {

	private final Id<TransitStopFacility> id;
	private Id<TransitStopArea> stopAreaId;
	private Coord coord;
	private Id<Link> linkId = null;
	private final boolean isBlockingLane;
	private String name = null;
	private Customizable customizableDelegate;
	private final Attributes attributes = new AttributesImpl();

	protected TransitStopFacilityImpl(final Id<TransitStopFacility> id, final Coord coord, final boolean isBlockingLane) {
		this.id = id;
		this.coord = coord;
		this.isBlockingLane = isBlockingLane;
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
	public void setCoord(Coord coord) {
		this.coord = coord;
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
		StringBuilder strb = new StringBuilder(  ) ;
		strb.append( "[ facility id=" ).append( id ) ;
		if (name != null) {
			strb.append( " | name=" ).append( name ) ;
		}
		strb.append ( " | coord=").append( coord ) ;
		strb.append( " | linkId=" ).append( linkId ) ;

		strb.append(" ] ") ;
		return strb.toString() ;
	}

	@Override
	public Id<TransitStopArea> getStopAreaId() {
		return this.stopAreaId;
	}

	@Override
	public void setStopAreaId(Id<TransitStopArea> stopAreaId) {
		this.stopAreaId = stopAreaId;
	}

	@Override
	public Map<String, Object> getCustomAttributes() {
		if (this.customizableDelegate == null) {
			this.customizableDelegate = CustomizableUtils.createCustomizable();
		}
		return this.customizableDelegate.getCustomAttributes();
	}

	@Override
	public Attributes getAttributes() {
		return this.attributes;
	}
}
