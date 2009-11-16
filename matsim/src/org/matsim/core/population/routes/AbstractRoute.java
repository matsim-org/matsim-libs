/* *********************************************************************** *
 * project: org.matsim.*
 * AbstractRoute.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.core.population.routes;

import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.population.BasicRoute;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.misc.Time;

/**
 * Default, abstract implementation of the {@link RouteWRefs}-interface.
 *
 * @author mrieser
 */
public abstract class AbstractRoute implements BasicRoute, RouteWRefs {

	private double dist = Double.NaN;

	private double travTime = Time.UNDEFINED_TIME;
	
	private Id startLinkId = null;
	private Id endLinkId = null;
	
	private Link startLink = null;
	private Link endLink = null;

	/**
	 * This constructor is only needed for backwards compatibility reasons and thus is
	 * set to deprecated. New code should make use of the constructor which sets the
	 * start and the end link of a Route correctly.
	 */
	@Deprecated
	protected AbstractRoute(){
	}

	public AbstractRoute(final Link startLink, final Link endLink) {
		this.startLinkId = (startLink == null ? null : startLink.getId());
		this.endLinkId = (endLink == null ? null : endLink.getId());
		this.startLink = startLink;
		this.endLink = endLink;
	}

	public double getDistance() {
		return dist;
	}

	public final void setDistance(final double dist) {
		this.dist = dist;
	}

	public final double getTravelTime() {
		return this.travTime;
	}
	
	public final void setTravelTime(final double travTime) {
		this.travTime = travTime;
	}
//	
//	public Id getStartLinkId() {
//		return this.startLinkId;
//	}
//	
//	public Id getEndLinkId() {
//		return this.endLinkId;
//	}
	
	public Link getEndLink() {
		return this.endLink;
	}

	public Link getStartLink() {
		return this.startLink;
	}

	public void setEndLink(final Link link) {
		this.endLink = link;
	}

	public void setStartLink(final Link link) {
		this.startLink = link;
	}

	public Id getStartLinkId() {
		return (this.startLink == null ? null : this.startLink.getId());
	}

	public Id getEndLinkId() {
		return (this.endLink == null ? null : this.endLink.getId());
	}

	@Override
	public AbstractRoute clone() {
		try {
			return (AbstractRoute) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new AssertionError(e);
		}
	}
}
