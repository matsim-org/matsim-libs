
/* *********************************************************************** *
 * project: org.matsim.*
 * LeastCostPathCalculator.java
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

package playground.sergioo.bestTravelTimeRouter2012;
//package org.matsim.core.router.util;

import java.util.List;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;

public interface BestTravelTimePathCalculator {

	public Path calcBestTravelTimePath(Node fromNode, Node toNode, double travelTime, double starttime);
	
	public class Path {
		public final List<Node> nodes;
		public final List<Link> links;
		public final double travelTime;
		
		public Path(final List<Node> nodes, final List<Link> links, final double travelTime) {
			this.nodes = nodes;
			this.links = links;
			this.travelTime = travelTime;
		}
		@Override
		public String toString() {
			String res = "|";
			for(Link link:links)
				res+=link.getId()+"|";
			return res;
		}
	}
}
