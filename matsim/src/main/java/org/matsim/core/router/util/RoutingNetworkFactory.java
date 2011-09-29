/* *********************************************************************** *
 * project: org.matsim.*
 * FastRoutingNetworkFactory.java
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

package org.matsim.core.router.util;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;

public class RoutingNetworkFactory implements NetworkFactory {
	
	public RoutingNetwork createRoutingNetwork(Network network) {
		
		RoutingNetwork routingNetwork = new RoutingNetwork(network);				
		return routingNetwork;
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
	public RoutingNetworkNode createNode(Id id, Coord coord) {
		throw new RuntimeException("Not supported operation!");
	}

}
