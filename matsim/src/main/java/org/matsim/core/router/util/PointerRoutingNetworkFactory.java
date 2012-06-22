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

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;

public class PointerRoutingNetworkFactory extends AbstractRoutingNetworkFactory {

	public PointerRoutingNetworkFactory(PreProcessDijkstra preProcessData) {
		super(preProcessData);
	}
	
	@Override
	public PointerRoutingNetwork createRoutingNetwork(Network network) {
		PointerRoutingNetwork routingNetwork = new PointerRoutingNetwork(network, this);
		routingNetwork.setPreProcessDijkstra(preProcessData);
		return routingNetwork;
	}

	@Override
	public PointerRoutingNetworkNode createRoutingNetworkNode(Node node, int numOutLinks) {
		return new PointerRoutingNetworkNode(node, numOutLinks);
	}
	
	@Override
	public PointerRoutingNetworkLink createRoutingNetworkLink(Link link,
			RoutingNetworkNode fromNode, RoutingNetworkNode toNode) {
		return new PointerRoutingNetworkLink(link, fromNode, toNode);
	}
}