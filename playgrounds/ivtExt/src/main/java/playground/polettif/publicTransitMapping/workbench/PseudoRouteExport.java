/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.polettif.publicTransitMapping.workbench;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import playground.polettif.publicTransitMapping.mapping.pseudoPTRouter.PseudoGraph;
import playground.polettif.publicTransitMapping.mapping.pseudoPTRouter.PseudoRoutePath;
import playground.polettif.publicTransitMapping.mapping.pseudoPTRouter.PseudoRouteStop;
import playground.polettif.publicTransitMapping.tools.NetworkTools;

public class PseudoRouteExport {
	
	public static void main(final String[] args) {
		
	}

	public static void run(Network network, PseudoGraph pseudoGraph, String filePath) {
		Network pseudoNetwork = NetworkTools.createNetwork();
		NetworkFactory f = network.getFactory();

		for(PseudoRoutePath path : pseudoGraph.getEdges()) {
			if(!path.isDummy()) {
				PseudoRouteStop fromStop = path.getFromPseudoStop();
				PseudoRouteStop toStop = path.getToPseudoStop();
				Id<Node> fromNodeId = Id.createNodeId(fromStop.getId());
				Id<Node> toNodeId = Id.createNodeId(toStop.getId());

				if(!pseudoNetwork.getNodes().containsKey(fromNodeId)) {
					Link link = network.getLinks().get(Id.createLinkId(fromStop.getLinkIdStr()));
					pseudoNetwork.addNode(f.createNode(fromNodeId, link.getCoord()));
				}
				if(!pseudoNetwork.getNodes().containsKey(toNodeId)) {
					Link link = network.getLinks().get(Id.createLinkId(fromStop.getLinkIdStr()));
					pseudoNetwork.addNode(f.createNode(toNodeId, link.getCoord()));
				}

				Id<Link> newLinkId = Id.createLinkId(path.getId());

				Link newLink = f.createLink(newLinkId, pseudoNetwork.getNodes().get(fromNodeId), pseudoNetwork.getNodes().get(toNodeId));

				pseudoNetwork.addLink(newLink);
			}
		}

		NetworkTools.writeNetwork(pseudoNetwork, filePath);
	}
	
}