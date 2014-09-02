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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.utils.misc.Time;

/**
 * Default, abstract implementation of the {@link Route}-interface.
 *
 * @author mrieser
 */
public abstract class AbstractRoute implements Route {

	private double dist = Double.NaN;

	private double travTime = Time.UNDEFINED_TIME;

	private Id<Link> startLinkId = null;
	private Id<Link> endLinkId = null;

	public AbstractRoute(final Id<Link> startLinkId, final Id<Link> endLinkId) {
		this.startLinkId = startLinkId;
		this.endLinkId = endLinkId;
	}

	@Override
	public double getDistance() {
		return dist;
	}

	@Override
	public final void setDistance(final double dist) {
		this.dist = dist;
	}

	@Override
	public final double getTravelTime() {
		return this.travTime;
	}

	@Override
	public final void setTravelTime(final double travTime) {
		this.travTime = travTime;
	}

	@Override
	public void setEndLinkId(final Id<Link> linkId) {
		this.endLinkId = linkId;
	}

	@Override
	public void setStartLinkId(final Id<Link> linkId) {
		this.startLinkId = linkId;
	}

	@Override
	public Id<Link> getStartLinkId() {
		return this.startLinkId;
	}

	@Override
	public Id<Link> getEndLinkId() {
		return this.endLinkId;
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
