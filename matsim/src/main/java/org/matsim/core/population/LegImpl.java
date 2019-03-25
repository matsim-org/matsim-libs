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
import org.matsim.utils.objectattributes.attributable.Attributes;

/* deliberately package */  final class LegImpl  implements Leg {

	private Route route = null;

	private double depTime = Time.getUndefinedTime();
	private double travTime = Time.getUndefinedTime();
	private String mode;

	private final Attributes attributes = new Attributes();

	/* deliberately package */  LegImpl(final String transportMode) {
		this.mode = transportMode;
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
				"[arrTime=" + Time.writeTime(this.getDepartureTime() + this.getTravelTime()) + "]" +
				"[route=" + this.route + "]";
	}

	@Override
	public Attributes getAttributes() {
		return attributes;
	}

	//	private boolean locked;
//
//	public void setLocked() {
//		this.locked = true ;
//	}
//	private void testForLocked() {
//		if ( this.locked ) {
//			throw new RuntimeException("too late to change this") ;
//		}
//	}

}
