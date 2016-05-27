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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import playground.polettif.publicTransitMapping.mapping.pseudoPTRouter.PseudoGraph;
import playground.polettif.publicTransitMapping.mapping.pseudoPTRouter.PseudoRoutePath;
import playground.polettif.publicTransitMapping.mapping.pseudoPTRouter.PseudoRouteStop;
import playground.polettif.publicTransitMapping.tools.NetworkTools;

import java.util.List;

public class PseudoRouteExport {

	protected static Logger log = Logger.getLogger(PseudoRouteExport.class);


	public static void main(final String[] args) {

	}

	public synchronized static void run(Network network, PseudoGraph pseudoGraph, String filePath) {
		Network pseudoNetwork = NetworkTools.createNetwork();
		NetworkFactory f = pseudoNetwork.getFactory();

		List<PseudoRoutePath> edges = pseudoGraph.getEdges();

		for(PseudoRoutePath path : edges) {
			if(!path.isDummy()) {
				PseudoRouteStop fromStop = path.getFromPseudoStop();
				PseudoRouteStop toStop = path.getToPseudoStop();

				Id<Node> fromNodeId = Id.createNodeId(fromStop.getId());
				Coord fromCoord = network.getLinks().get(Id.createLinkId(fromStop.getLinkIdStr())).getCoord();
				Node fromNode = f.createNode(fromNodeId, fromCoord);
				if(!pseudoNetwork.getNodes().containsKey(fromNodeId)) {
					pseudoNetwork.addNode(fromNode);
				}

				Id<Node> toNodeId = Id.createNodeId(toStop.getId());
				Coord toCoord = network.getLinks().get(Id.createLinkId(toStop.getLinkIdStr())).getCoord();
				Node toNode = f.createNode(toNodeId, toCoord);
				if(!pseudoNetwork.getNodes().containsKey(toNodeId)) {
					pseudoNetwork.addNode(toNode);
				}

				Id<Link> newLinkId = Id.createLinkId(path.getId());
				Link newLink = f.createLink(newLinkId, fromNode, toNode);
				pseudoNetwork.addLink(newLink);
			}
		}

		NetworkTools.writeNetwork(pseudoNetwork, filePath);
	}

}