/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.mrieser.routedistance;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

public class ExperimentalTransitRouteDistanceCalculator implements RouteDistanceCalculator {

	private final TransitSchedule schedule;
	private final Network network;

	public ExperimentalTransitRouteDistanceCalculator(final TransitSchedule schedule, final Network network) {
		this.schedule = schedule;
		this.network = network;
	}

	@Override
	public double calcDistance(Route route) {
		if (!(route instanceof ExperimentalTransitRoute)) {
			throw new IllegalArgumentException("wrong type of route, expected ExperimentalTransitRoute, but was " + (route != null ? route.getClass() : "null"));
		}
		ExperimentalTransitRoute r = (ExperimentalTransitRoute) route;
		Id lineId = r.getLineId();
		Id routeId = r.getRouteId();
		Id enterStopId = r.getAccessStopId();
		Id exitStopId = r.getEgressStopId();

		TransitLine line = this.schedule.getTransitLines().get(lineId);
		TransitRoute tr = line.getRoutes().get(routeId);

		Id enterLinkId = this.schedule.getFacilities().get(enterStopId).getLinkId();
		Id exitLinkId = this.schedule.getFacilities().get(exitStopId).getLinkId();

		NetworkRoute nr = tr.getRoute();
		double dist = 0;
		boolean count = false;
		if (enterLinkId.equals(nr.getStartLinkId())) {
			count = true;
		}
		for (Id linkId : nr.getLinkIds()) {
			if (count) {
				Link l = this.network.getLinks().get(linkId);
				dist += l.getLength();
			}
			if (enterLinkId.equals(linkId)) {
				count = true;
			}
			if (exitLinkId.equals(linkId)) {
				count = false;
				break;
			}
		}
		if (count) {
			Link l = this.network.getLinks().get(nr.getEndLinkId());
			dist += l.getLength();
		}
		route.setDistance(dist);
		return dist;
	}

}
