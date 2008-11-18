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

package org.matsim.population;

import org.matsim.basic.v01.BasicLeg;
import org.matsim.basic.v01.BasicLegImpl;
import org.matsim.utils.misc.Time;

public class Leg extends BasicLegImpl {

	public Leg(final BasicLeg.Mode mode) {
		super(mode);
	}

	/**
	 * Makes a deep copy of this leg, however only when the Leg has a route which is
	 * instance of Route or BasicRoute. Other route instances are not considered.
	 * @param leg
	 */
	public Leg(final Leg leg) {
		super(leg.getMode());
		this.num = leg.num;
		this.setDepartureTime(leg.getDepartureTime());
		this.setTravelTime(leg.getTravelTime());
		this.setArrivalTime(leg.getArrivalTime());
		if (leg.route instanceof RouteImpl) {
			this.route = new RouteImpl((RouteImpl) leg.route);
		} else {
			this.route = new RouteImpl();
			((Route)this.route).setRoute(leg.getRoute().getRoute());
		}

	}

	//////////////////////////////////////////////////////////////////////
	// create methods
	//////////////////////////////////////////////////////////////////////

	public final Route createRoute(final String dist, final String time) {
		this.route = new RouteImpl();
		if (dist != null) {
			this.route.setDist(Double.parseDouble(dist));
		}
		if (time != null) {
			this.route.setTravTime(Time.parseTime(time));
		}
		return getRoute();
	}

	//////////////////////////////////////////////////////////////////////
	// remove methods
	//////////////////////////////////////////////////////////////////////

	protected final void removeRoute() {
		this.route = null;
	}

	@Override
	public Route getRoute() {
		return (Route) this.route;
	}

	@Override
	public final String toString() {
		return "[num=" + this.num + "]" +
				"[mode=" + this.getMode().toString()  + "]" +
				"[depTime=" + Time.writeTime(this.getDepartureTime()) + "]" +
				"[travTime=" + Time.writeTime(this.getTravelTime()) + "]" +
				"[arrTime=" + Time.writeTime(this.getArrivalTime()) + "]" +
				"[route=" + this.route + "]";
	}

}
