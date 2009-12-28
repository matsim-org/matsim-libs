/* *********************************************************************** *
 * project: org.matsim.*
 * RawLegImpl.java
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
package playground.johannes.plans.plain.impl;

import org.matsim.api.core.v01.TransportMode;

import playground.johannes.plans.plain.PlainLeg;
import playground.johannes.plans.plain.PlainRoute;

/**
 * @author illenberger
 *
 */
public class PlainLegImpl extends PlainPlanElementImpl implements PlainLeg{

	private PlainRouteImpl route;

	private TransportMode mode;
	
	public PlainRouteImpl getRoute() {
		return route;
	}

	public void setRoute(PlainRoute route) {
		this.route = (PlainRouteImpl) route;
		modified();
	}

	public TransportMode getMode() {
		return mode;
	}

	public void setMode(TransportMode mode) {
		this.mode = mode;
		modified();
	}

}
