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

import org.matsim.basic.v01.BasicLegImpl;
import org.matsim.interfaces.basic.v01.BasicLeg;
import org.matsim.population.routes.CarRoute;
import org.matsim.population.routes.NodeCarRoute;
import org.matsim.population.routes.Route;
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
		//FIXME the copy of a leg should contain the same subtype
		// of CarRoute as the original
		if (leg.route instanceof NodeCarRoute) {
			this.route = new NodeCarRoute((NodeCarRoute) leg.route);
		} else {
			this.route = new NodeCarRoute(leg.getRoute().getStartLink(), leg.getRoute().getEndLink());
			((CarRoute)this.route).setNodes(leg.getRoute().getStartLink(), ((CarRoute) (leg.getRoute())).getNodes(), leg.getRoute().getEndLink());
		}
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
