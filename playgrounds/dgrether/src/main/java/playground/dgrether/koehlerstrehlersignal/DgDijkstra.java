/* *********************************************************************** *
 * project: org.matsim.*
 * DgDijkstra
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.dgrether.koehlerstrehlersignal;

import java.util.PriorityQueue;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;


/**
 * @author dgrether
 *
 */
public class DgDijkstra {

	private Network network;

	public DgDijkstra(Network network) {
		this.network = network;
	}
	
	public void calcLeastCostPath(Id fromNodeId, Id toNodeId, double startTime, TravelDisutility travelDisutility, TravelTime travelTime) {
		Node fromNode = this.network.getNodes().get(fromNodeId);
		PriorityQueue<Node> queue = new PriorityQueue<Node>();
		
	}

}
