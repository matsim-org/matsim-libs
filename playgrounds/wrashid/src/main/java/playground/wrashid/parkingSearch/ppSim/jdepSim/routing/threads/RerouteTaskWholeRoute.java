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

public class RerouteTaskWholeRoute implements RerouteTask {

	private Id endLinkId;
	public int legIndex;
	public Id personId;
	public double getTime() {
		return time;
	}
	public Id getStartLinkId() {
		return startLinkId;
	}
	public Leg getLeg() {
		return leg;
	}
	public RerouteTaskWholeRoute(double time, Id startLinkId, Id endLinkId, int legIndex, Id personId) {
		super();
		this.time = time;
		this.startLinkId = startLinkId;
		this.endLinkId = endLinkId;
		this.legIndex = legIndex;
		this.personId = personId;
	}
	private double time;
	private Id startLinkId;
	private Leg leg;
	public LinkNetworkRouteImpl route;
	
	public void perform(EditRoute editRoute) {
		synchronized (this) {
			route = editRoute.getRoute(time, startLinkId, endLinkId);
		}
	}

}

