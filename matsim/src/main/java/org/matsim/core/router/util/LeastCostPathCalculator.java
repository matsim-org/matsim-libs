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

	/**
	 * Despite most routing algorithms operating at the node level we deliberately decided to change this interface
	 * to query from link to link. MATSim usually thinks in "links" instead of "nodes". The underlying implementations
	 * may still operate on the links' from and to nodes. Using links has the advantage that the start link may be considered
	 * in existing turn restriction sequences that may start at that link.
	 * Please use the link-based method from now on.
	 * nkuehnel, after discussions at the code sprint March '25
	 */
	@Deprecated
	Path calcLeastCostPath(Node fromNode, Node toNode, double startTime, final Person person, final Vehicle vehicle);

	default Path calcLeastCostPath(Link fromLink, Link toLink, double startTime, final Person person, final Vehicle vehicle) {
		return calcLeastCostPath(fromLink.getToNode(), toLink.getFromNode(), startTime, person, vehicle);
	}

	class Path {
		public List<Node> nodes;
		public final List<Link> links;
		public final double travelTime;
		public final double travelCost;

		public Path(final List<Node> nodes, final List<Link> links, final double travelTime, final double travelCost) {
			this.nodes = nodes;
			this.links = links;
			this.travelTime = travelTime;
			this.travelCost = travelCost;
		}

		public Node getFromNode() {
			return nodes.get(0);
		}

		public Node getToNode() {
			return nodes.get(nodes.size() - 1);
		}
	}
}
