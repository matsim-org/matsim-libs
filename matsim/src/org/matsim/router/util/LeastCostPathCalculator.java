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

package org.matsim.router.util;

import java.util.List;

import org.matsim.network.Link;
import org.matsim.network.Node;

public interface LeastCostPathCalculator {
	public Path calcLeastCostPath(Node fromNode, Node toNode, double starttime);
	
	public class Path {
		public final List<Node> nodes;
		public final List<Link> links;
		public final double travelTime;
		public final double travelCost;
		
		public Path(final List<Node> nodes, final List<Link> links, final double travelTime, final double travelCost) {
			this.nodes = nodes;
			this.links = links;
			this.travelTime = travelTime;
			this.travelCost = travelCost;
		}
	}
}
