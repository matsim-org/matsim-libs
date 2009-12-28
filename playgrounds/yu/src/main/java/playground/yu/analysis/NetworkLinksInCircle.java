/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkLinksInCircle.java
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

/**
 *
 */
package playground.yu.analysis;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordUtils;

/**
 * this class is based on
 * playground.marcel.MyRuns.filterPlansWithRouteInArea(String[] args, double x,
 * double y, double radius)
 *
 * @author ychen
 *
 */
public class NetworkLinksInCircle {
	final private HashSet<Link> areaOfInterest = new HashSet<Link>();
	final private Network network;

	public NetworkLinksInCircle(Network network) {
		System.out.println("RUN: extract links out of Network with a circle");
		this.network = network;
	}

	public Set<Link> getLinks(double x, double y, double radius) {
		CoordImpl center = new CoordImpl(x, y);
		System.out.println("--> extracting area of interest... at "
				+ (new Date()));
		for (Link link : this.network.getLinks().values()) {
			final Node from = link.getFromNode();
			final Node to = link.getToNode();
			if ((CoordUtils.calcDistance(from.getCoord(), center) <= radius)
					|| (CoordUtils.calcDistance(to.getCoord(), center) <= radius)) {
				System.out.println("    link " + link.getId().toString());
				this.areaOfInterest.add(link);
			}
		}
		System.out.println("--> area of interest contains: "
				+ this.areaOfInterest.size() + " links.");
		System.out.println("--> extractiong ... done. ");
		return this.areaOfInterest;
	}
}
