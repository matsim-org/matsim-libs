/* *********************************************************************** *
 * project: org.matsim.*
 * ArrayFastRouterDelegate.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.core.router;

import org.matsim.api.core.v01.network.Node;
import org.matsim.core.router.util.ArrayRoutingNetwork;
import org.matsim.core.router.util.ArrayRoutingNetworkNode;
import org.matsim.core.router.util.NodeData;
import org.matsim.core.router.util.NodeDataFactory;

/*package*/ class ArrayFastRouterDelegate extends AbstractFastRouterDelegate {

	private final ArrayRoutingNetwork network;
	private final NodeData[] nodeData;
	private boolean isInitialized = false;
	
	/*package*/ ArrayFastRouterDelegate(final Dijkstra dijkstra, final NodeDataFactory nodeDataFactory,
			final ArrayRoutingNetwork network) {
		super(dijkstra, nodeDataFactory);
		this.network = network;
		this.nodeData = new NodeData[network.getNodes().size()];
	}

	@Override
	public final void initialize() {
		// lazy initialization
		if (!isInitialized) {
			for (Node node : this.network.getNodes().values()) {
				int index = ((ArrayRoutingNetworkNode) node).getArrayIndex();
				this.nodeData[index] = nodeDataFactory.createNodeData();
			}
			
			this.isInitialized = true;
		}
	}
	
	/*
	 * The NodeData is taken from the array.
	 */
	public NodeData getData(final Node n) {
		ArrayRoutingNetworkNode routingNetworkNode = (ArrayRoutingNetworkNode) n;
		return this.nodeData[routingNetworkNode.getArrayIndex()];
	}
}