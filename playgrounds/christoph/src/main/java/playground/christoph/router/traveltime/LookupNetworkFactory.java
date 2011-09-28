/* *********************************************************************** *
 * project: org.matsim.*
 * LookupNetworkFactory.java
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

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;

public class LookupNetworkFactory implements NetworkFactory {
		
	public LookupNetwork createLookupNetwork(Network network) {
		
		LookupNetwork lookupNetwork = new LookupNetwork(network);
		
		for (Node node : network.getNodes().values()) {
			lookupNetwork.addNode(new LookupNetworkNode(node));
		}
		
		for (Link link : network.getLinks().values()) {
			lookupNetwork.addLink(new LookupNetworkLink(link));
		}
					
		return lookupNetwork;
	}

	@Override
	public Link createLink(Id id, Id fromNodeId, Id toNodeId) {
		throw new RuntimeException("Not supported operation!");
	}

	@Override
	public Link createLink(Id id, Node fromNode, Node toNode) {
		throw new RuntimeException("Not supported operation!");
	}

	@Override
	public LookupNetworkNode createNode(Id id, Coord coord) {
		throw new RuntimeException("Not supported operation!");
	}

}
