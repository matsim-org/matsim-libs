/* *********************************************************************** *
 * project: org.matsim.*
 * CalcRouteDistance.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.yu.analysis;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.population.routes.GenericRoute;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.misc.RouteUtils;

/**
 * small tool to calculate Route distances accoring to different Route
 * Implementations (LinkNetworkRoute is not be included)
 * 
 * @author yu
 * 
 */
public class CalcRouteDistance {
	public static double getRouteDistance(Route route, Network network) {
		double distance = 0.0;
		if (route instanceof GenericRoute)
			distance = route.getDistance();
		else if (route instanceof NetworkRoute) {
			distance = RouteUtils.calcDistance((NetworkRoute) route, network);
			Id endLinkId = route.getEndLinkId();
			if (endLinkId != null && route.getStartLinkId() != endLinkId)
				distance += network.getLinks().get(endLinkId).getLength();
		}
		return distance;
	}
}
