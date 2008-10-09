/* *********************************************************************** *
 * project: org.matsim.*
 * PtRouteImpl.java
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

package playground.marcel.pt.implementations.routes;

import org.matsim.basic.v01.Id;

import playground.marcel.pt.interfaces.routes.PtRoute;

public class PtRouteImpl implements PtRoute {

	private final Id line;
	private final Id enterStop;
	private final Id exitStop;

	private double totalTravelTime;

	public PtRouteImpl(final Id line, final Id enterStop, final Id exitStop, final double travelTime) {
		this.line = line;
		this.enterStop = enterStop;
		this.exitStop = exitStop;
		this.totalTravelTime = travelTime;
	}

	public Id getEnterStop() {
		return this.enterStop;
	}

	public Id getExitStop() {
		return this.exitStop;
	}

	public Id getLine() {
		return this.line;
	}

	public double getTravelTime() {
		return this.totalTravelTime;
	}

	public void setTravelTime(final double travelTime) {
		this.totalTravelTime = travelTime;
	}

}
