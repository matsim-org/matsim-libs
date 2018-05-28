/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * LegRerouter.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2018 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package org.matsim.pt.withinday;

import java.util.function.Consumer;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Route;

@FunctionalInterface
public interface LegRerouter extends Consumer<Leg> {
	
	static final Logger log = Logger.getLogger(LegRerouter.class);
	
	default public void safeReroute(Leg leg) {
		// Obtain the start and end links of the old route
		Route oldRoute = leg.getRoute();
		Id<Link> startLink = oldRoute.getStartLinkId();
		Id<Link> endLink = oldRoute.getEndLinkId();
		
		// Apply the rerouting
		this.accept(leg);
		
		// Obtain the rerouted route
		Route newRoute = leg.getRoute();
		if (   !startLink.equals(newRoute.getStartLinkId())
			|| !endLink.equals(newRoute.getEndLinkId())) {
			throw new LegReroutingException("The new route has a different start"
					+ " or end link than the old route. This is not allowed.");
		}
	}
	
}
