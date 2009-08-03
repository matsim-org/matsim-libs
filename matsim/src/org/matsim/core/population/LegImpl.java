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

import org.matsim.api.basic.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.basic.v01.BasicLegImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteWRefs;
import org.matsim.core.utils.misc.Time;

public class LegImpl extends BasicLegImpl implements Leg {

	private double arrTime = Time.UNDEFINED_TIME;

	
	public LegImpl(final TransportMode mode) {
		super(mode);
	}

	/**
	 * Makes a deep copy of this leg, however only when the Leg has a route which is
	 * instance of Route or BasicRoute. Other route instances are not considered.
	 * @param leg
	 */
	public LegImpl(final LegImpl leg) {
		super(leg.getMode());
		this.setDepartureTime(leg.getDepartureTime());
		this.setTravelTime(leg.getTravelTime());
		this.setArrivalTime(leg.getArrivalTime());
		if (leg.getRoute() instanceof NetworkRoute) {
			NetworkRoute route2 = (NetworkRoute) leg.getRoute();
			NetworkLayer net = (NetworkLayer) route2.getStartLink().getLayer();
			this.route = net.getFactory().createRoute(TransportMode.car, route2.getStartLink(), route2.getEndLink());
			((NetworkRoute) this.route).setLinks(route2.getStartLink(), route2.getLinks(), route2.getEndLink());
			this.route.setDistance(route.getDistance());
			this.route.setTravelTime(route.getTravelTime());
		} else {
			this.route = new GenericRouteImpl(leg.getRoute().getStartLink(), leg.getRoute().getEndLink());
		}
	}
	
	public final double getArrivalTime() {
		return this.arrTime;
	}

	public final void setArrivalTime(final double arrTime) {
		this.arrTime = arrTime;
	}


	@Override
	public RouteWRefs getRoute() {
		return (RouteWRefs) this.route;
	}

	@Override
	public final String toString() {
		return "[mode=" + this.getMode().toString()  + "]" +
				"[depTime=" + Time.writeTime(this.getDepartureTime()) + "]" +
				"[travTime=" + Time.writeTime(this.getTravelTime()) + "]" +
				"[arrTime=" + Time.writeTime(this.getArrivalTime()) + "]" +
				"[route=" + this.route + "]";
	}

}
