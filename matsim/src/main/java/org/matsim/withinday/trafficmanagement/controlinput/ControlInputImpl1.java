/* *********************************************************************** *
 * project: org.matsim.*
 * ControlInputImpl1.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.withinday.trafficmanagement.controlinput;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.population.routes.NetworkRouteWRefs;

/**
 * Measures the travel time difference between route 1 and 2 and returns that as
 * the control signal. ("Reactive control")
 * 
 * @author a.bergsten, d.zetterberg
 */
public class ControlInputImpl1 extends AbstractControlInputImpl {

	public ControlInputImpl1(final Network network) {
		super(network);
	}
	
	@Override
	public double getPredictedNashTime(NetworkRouteWRefs route) {
		if (route.equals(this.mainRoute)) {
			return this.lastTimeMainRoute;
		}
		return this.lastTimeAlternativeRoute;
	}

	@Override
	public double getNashTime() {
		super.getNashTime();
		return this.timeDifference;
	}

}
