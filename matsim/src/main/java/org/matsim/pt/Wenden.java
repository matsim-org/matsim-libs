
/* *********************************************************************** *
 * project: org.matsim.*
 * Wenden.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

 package org.matsim.pt;

import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;

public class Wenden implements UmlaufStueckI {

	private NetworkRoute route;
	
	public Wenden(NetworkRoute route) {
		this.route = route;
	}

	@Override
	public Departure getDeparture() {
		return null;
	}

	@Override
	public TransitLine getLine() {
		return null;
	}

	@Override
	public TransitRoute getRoute() {
		return null;
	}

	@Override
	public NetworkRoute getCarRoute() {
		return route;
	}

	@Override
	public boolean isFahrt() {
		return false;
	}

}
