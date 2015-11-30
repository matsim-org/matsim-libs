/* *********************************************************************** *
 * project: org.matsim.*
 * Leg.java
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

package org.matsim.core.population;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.utils.misc.Time;

public final class LegImpl implements Leg {

	protected Route route = null;

	private double depTime = Time.UNDEFINED_TIME;
	private double travTime = Time.UNDEFINED_TIME;
	private String mode;

	private double arrTime = Time.UNDEFINED_TIME;

	public LegImpl(final String transportMode) {
		this.mode = transportMode;
	}

	/**
	 * Makes a deep copy of this leg, however only when the Leg has a route which is
	 * instance of Route or BasicRoute. Other route instances are not considered.
	 * @param leg
	 */
	public LegImpl(final LegImpl leg) {
		this(leg.getMode());
		this.setDepartureTime(leg.getDepartureTime());
		this.setTravelTime(leg.getTravelTime());
		this.setArrivalTime(leg.getArrivalTime());
		if (leg.getRoute() != null) {
			this.setRoute(leg.getRoute().clone());
		}
	}

	@Override
	public final String getMode() {
		return this.mode;
	}

	@Override
	public final void setMode(String transportMode) {
		this.mode = transportMode;
	}

	@Override
	public final double getDepartureTime() {
		return this.depTime;
	}

	@Override
	public final void setDepartureTime(final double depTime) {
		this.depTime = depTime;
	}

	@Override
	public final double getTravelTime() {
		return this.travTime;
	}

	@Override
	public final void setTravelTime(final double travTime) {
		this.travTime = travTime;
	}

	public final double getArrivalTime() {
		return this.arrTime;
	}

	public final void setArrivalTime(final double arrTime) {
		this.arrTime = arrTime;
	}

	@Override
	public Route getRoute() {
		return this.route;
	}

	@Override
	public final void setRoute(Route route) {
		this.route = route;
	}

	@Override
	public final String toString() {
		return "[mode=" + this.getMode() + "]" +
				"[depTime=" + Time.writeTime(this.getDepartureTime()) + "]" +
				"[travTime=" + Time.writeTime(this.getTravelTime()) + "]" +
				"[arrTime=" + Time.writeTime(this.getArrivalTime()) + "]" +
				"[route=" + this.route + "]";
	}

}
