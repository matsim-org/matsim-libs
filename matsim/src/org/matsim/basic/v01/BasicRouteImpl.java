/* *********************************************************************** *
 * project: org.matsim.*
 * BasicRouteImpl.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007, 2008 by the members listed in the COPYING,  *
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

package org.matsim.basic.v01;

import java.util.List;

import org.matsim.utils.misc.Time;

public class BasicRouteImpl implements BasicRoute {
	
	private double dist = Double.NaN;

	private double travTime = Time.UNDEFINED_TIME;

	private List<Id> linkIds = null;
	
	private Id startLinkId = null;
	private Id endLinkId = null;

	/**
	 * This constructor is only needed for backwards compatibility reasons and thus is
	 * set to deprecated. New code should make use of the constructor which sets the Ids of the
	 * start and the end link of a Route correctly.
	 */
	@Deprecated
	public BasicRouteImpl() {}
	
	public BasicRouteImpl(Id startLinkId, Id endLinkId){
		this.startLinkId = startLinkId;
		this.endLinkId = endLinkId;
	}
	
	
	public double getDist() {
		return dist;
	}

	public final void setDist(final double dist) {
		this.dist = dist;
	}

	public final double getTravelTime() {
		return this.travTime;
	}
	
	public final void setTravelTime(final double travTime) {
		this.travTime = travTime;
	}

	public void setLinkIds(List<Id> linkids) {
		this.linkIds = linkids;
	}
	
	public List<Id> getLinkIds() {
		return this.linkIds;
	}

	/**
	 * Deprecated: Use constructor instead.
	 * @param linkId
	 */
	@Deprecated 
	public void setStartLinkId(final Id linkId) {
		this.startLinkId = linkId;
	}
	
	public Id getStartLinkId() {
		return this.startLinkId;
	}
	/**
	 * Deprecated: Use constructor instead.
	 * @param linkId
	 */
	@Deprecated 	
	public void setEndLinkId(final Id linkId) {
		this.endLinkId = linkId;
	}
	
	public Id getEndLinkId() {
		return this.endLinkId;
	}

}
