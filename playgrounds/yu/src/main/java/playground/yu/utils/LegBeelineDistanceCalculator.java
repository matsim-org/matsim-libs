/* *********************************************************************** *
 * project: org.matsim.*
 * CalculateLetgBeelineDistance.java
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

/**
 *
 */
package playground.yu.utils;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.utils.geometry.CoordUtils;

/**
 * @author yu
 * 
 */
public class LegBeelineDistanceCalculator {
	public static double getBeelineDistance_link(Network network, Leg leg) {
		Route route = leg.getRoute();
		Id startLinkId = route.getStartLinkId();
		Id endLinkId = route.getEndLinkId();
		Map<Id<Link>, ? extends Link> links = network.getLinks();
		return CoordUtils.calcDistance(links.get(startLinkId).getToNode()
				.getCoord(), links.get(endLinkId).getToNode().getCoord());
	}

	public static double getBeelineDistance_coord(Network network,
			Activity preAct, Activity nextAct) {
		double geoDist = CoordUtils.calcDistance(preAct.getCoord(),
				nextAct.getCoord());

		if (Double.isNaN(geoDist)) {
			throw new RuntimeException("geoDist=NaN, " + "\npreAct coord\t"
					+ preAct.getCoord() + "\tnextAct coord\t"
					+ nextAct.getCoord());
		}
		return geoDist;
	}
}
