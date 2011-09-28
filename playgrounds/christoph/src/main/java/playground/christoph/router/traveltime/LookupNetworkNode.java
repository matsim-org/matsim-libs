/* *********************************************************************** *
 * project: org.matsim.*
 * RoutingNetworkNode.java
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

package playground.christoph.router.traveltime;

import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;

public class LookupNetworkNode implements Node {

	private final Node node;
		
	/*package*/ LookupNetworkNode(Node node) {
		this.node = node;
	}
	
	public Node getNode() {
		return node;
	}
	
	public Id getId() {
		return node.getId();
	}

	@Override
	public Coord getCoord() {
		return node.getCoord();
	}

	@Override
	public boolean addInLink(Link link) {
		throw new RuntimeException("Not supported operation!");
	}

	@Override
	public boolean addOutLink(Link link) {
		throw new RuntimeException("Not supported operation!");
	}

	@Override
	public Map<Id, ? extends Link> getInLinks() {
		return node.getInLinks();
	}


	@Override
	public Map<Id, ? extends Link> getOutLinks() {
		return node.getOutLinks();
	}
	
}