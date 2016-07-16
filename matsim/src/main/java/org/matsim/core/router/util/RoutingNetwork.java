/* *********************************************************************** *
 * project: org.matsim.*
 * RoutingNetwork.java
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

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;

/**
 * A network that is used by FastDijkstra, FastAStarEuclidean and FastAStarLandmarks.
 * Instead of storing the node data in a map, the data is attached directly to the nodes
 * which is faster but also consumes more memory.
 * 
 * @see org.matsim.core.router.FastDijkstra
 * @see org.matsim.core.router.FastAStarEuclidean
 * @see org.matsim.core.router.FastAStarLandmarks
 * @author cdobler
 */
public interface RoutingNetwork extends Network {
	
	public void initialize();
	
	@Override
	public Map<Id<Node>, RoutingNetworkNode> getNodes();
}