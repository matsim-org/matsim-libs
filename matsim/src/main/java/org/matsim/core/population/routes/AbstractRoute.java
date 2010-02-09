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
import org.matsim.core.utils.misc.Time;

/**
 * Default, abstract implementation of the {@link RouteWRefs}-interface.
 *
 * @author mrieser
 */
public abstract class AbstractRoute implements RouteWRefs {

	private double dist = Double.NaN;

	private double travTime = Time.UNDEFINED_TIME;

	private Id startLinkId = null;
	private Id endLinkId = null;

	public AbstractRoute(final Id startLinkId, final Id endLinkId) {
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
	public void setEndLinkId(final Id linkId) {
		this.endLinkId = linkId;
	}

	@Override
	public void setStartLinkId(final Id linkId) {
		this.startLinkId = linkId;
	}

	@Override
	public Id getStartLinkId() {
		return this.startLinkId;
	}

	@Override
	public Id getEndLinkId() {
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
