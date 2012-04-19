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

package org.matsim.core.router.util;

import java.util.List;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;

public interface LeastCostPathCalculator {

	public Path calcLeastCostPath(Node fromNode, Node toNode, double starttime, final Person person, final Vehicle vehicle);
	
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
