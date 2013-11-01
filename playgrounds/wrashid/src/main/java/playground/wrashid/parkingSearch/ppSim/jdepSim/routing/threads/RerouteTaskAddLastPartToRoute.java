/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.wrashid.parkingSearch.ppSim.jdepSim.routing.threads;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;

import playground.wrashid.parkingSearch.ppSim.jdepSim.routing.EditRoute;

public class RerouteTaskAddLastPartToRoute implements RerouteTask {

	private Id newEndLinkId;

	public double getTime() {
		return time;
	}
	public Id getStartLinkId() {
		return startLinkId;
	}
	public Leg getLeg() {
		return leg;
	}
	public RerouteTaskAddLastPartToRoute(double time, Leg leg, Id newEndLinkId) {
		super();
		this.time = time;
		this.leg = leg;
		this.newEndLinkId = newEndLinkId;
	}
	private double time;
	private Id startLinkId;
	private Leg leg;
	
	public void perform(EditRoute editRoute) {
		LinkNetworkRouteImpl route = editRoute.addLastPartToRoute(time, leg, newEndLinkId);
		
		synchronized (this) {
			leg.setRoute(route);
		}
	}

}

