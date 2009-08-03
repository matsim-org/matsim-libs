/* *********************************************************************** *
 * project: org.matsim.*
 * LeastCostPathCalculatorInvertedNetProxy
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkLayer;


/**
 * Proxy for the LeastCostPathCalculator to make it
 * work on an inverted network considering LinkToLinkTravelTimes
 * 
 * @author dgrether
 *
 */
public class LeastCostPathCalculatorInvertedNetProxy implements
		LeastCostPathCalculator {

	private NetworkLayer invertedNetwork;
	private NetworkInverter netInverter;
	private LeastCostPathCalculator leastCostPathCalculator;
	
	public LeastCostPathCalculatorInvertedNetProxy(NetworkInverter networkInverter, LeastCostPathCalculator lcpc) {
		this.netInverter = networkInverter;
		this.invertedNetwork = this.netInverter.getInvertedNetwork();
		this.leastCostPathCalculator = lcpc;
	}
	
	/**
	 * @see org.matsim.core.router.util.LeastCostPathCalculator#calcLeastCostPath(org.matsim.core.network.NodeImpl, org.matsim.core.network.NodeImpl, double)
	 */
	public Path calcLeastCostPath(Node fromNode, Node toNode, double starttime) {
		//we start at the toNode of the link representing the fromNode of the original network
		Link startLink = this.invertedNetwork.getLink(fromNode.getId());
		Node startNode = startLink.getToNode();
		//we stop at the
		Link endLink = this.invertedNetwork.getLink(toNode.getId());
		Node endNode = endLink.getToNode();
		
		Path invertedPath = this.leastCostPathCalculator.calcLeastCostPath(startNode, endNode, starttime);
		
		Path path = new Path(this.netInverter.convertInvertedLinksToNodes(invertedPath.links), 
				this.netInverter.convertInvertedNodesToLinks(invertedPath.nodes), invertedPath.travelTime, invertedPath.travelCost);
		return path;
	}

	

}
