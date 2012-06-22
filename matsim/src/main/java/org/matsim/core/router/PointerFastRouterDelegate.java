/* *********************************************************************** *
 * project: org.matsim.*
 * PointerFastRouterDelegate.java
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
import org.matsim.core.router.util.NodeData;
import org.matsim.core.router.util.NodeDataFactory;
import org.matsim.core.router.util.PointerRoutingNetworkNode;

/*package*/ class PointerFastRouterDelegate extends AbstractFastRouterDelegate {

	/*package*/ PointerFastRouterDelegate(final Dijkstra dijkstra, final NodeDataFactory nodeDataFactory) {
		super(dijkstra, nodeDataFactory);
	}
	
	@Override
	public final void initialize() {
		// nothing to do here
	}
	
	@Override
	public NodeData getData(final Node n) {
		PointerRoutingNetworkNode routingNetworkNode = (PointerRoutingNetworkNode) n;
		NodeData data;
		data = routingNetworkNode.getNodeData();
		
		if (data == null) {
			data = nodeDataFactory.createNodeData();
			routingNetworkNode.setNodeData(data);
		}
		return data;
	}

}