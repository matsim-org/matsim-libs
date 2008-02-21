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

import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.utils.geometry.shared.Coord;

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
	final private NetworkLayer network;

	public NetworkLinksInCircle(NetworkLayer network) {
		System.out.println("RUN: extract links out of Network with a circle");
		this.network = network;
	}

	public Set<Link> getLinks(double x, double y, double radius) {
		Coord center = new Coord(x, y);
		System.out.println("--> extracting area of interest... at "
				+ (new Date()));
		for (Link link : network.getLinks().values()) {
			final Node from = link.getFromNode();
			final Node to = link.getToNode();
			if ((from.getCoord().calcDistance(center) <= radius)
					|| (to.getCoord().calcDistance(center) <= radius)) {
				System.out.println("    link " + link.getId().toString());
				areaOfInterest.add(link);
			}
		}
		System.out.println("--> area of interest contains: "
				+ areaOfInterest.size() + " links.");
		System.out.println("--> extractiong ... done. ");
		return areaOfInterest;
	}
}
