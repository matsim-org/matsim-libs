/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.andreas.osmBB;

import java.util.HashMap;

import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.misc.NetworkUtils;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;

public class NetworkCalcTopoType extends org.matsim.core.network.algorithms.NetworkCalcTopoType{

	public int run(final Node node){
		if (NetworkUtils.getIncidentLinks(node).size() == 0) { return EMPTY.intValue(); }
		else if (node.getInLinks().size() == 0) { return SOURCE.intValue(); }
		else if (node.getOutLinks().size() == 0) { return SINK.intValue(); }
		else if (getIncidentNodes(node).size() == 1) { return DEADEND.intValue(); }
		else if (getIncidentNodes(node).size() == 2) {
			if ((node.getOutLinks().size() == 1) && (node.getInLinks().size() == 1)) { return PASS1WAY.intValue(); }
			else if ((node.getOutLinks().size() == 2) && (node.getInLinks().size() == 2)) { return PASS2WAY.intValue(); }
			else if ((node.getOutLinks().size() == 2) && (node.getInLinks().size() == 1)) { return START1WAY.intValue(); }
			else if ((node.getOutLinks().size() == 1) && (node.getInLinks().size() == 2)) { return END1WAY.intValue(); }
			// The following case is not covered by the paper, but quite common, e.g. parallel roads connecting the same nodes.
			else if ((node.getOutLinks().size() >= 1) && (node.getInLinks().size() >= 1)) { return INTERSECTION.intValue(); }
			else { Gbl.errorMsg("Node=" + node.toString() + " cannot be assigned to a topo type!"); return -1;}
		}
		else { // more than two neighbour nodes and no sink or source
			return INTERSECTION.intValue();
		}
	}
	
	private HashMap<Id, Node> getIncidentNodes(final Node node) {
		HashMap<Id, Node> nodes = new HashMap<Id, Node>();
		for (Link link : node.getInLinks().values()) {
			nodes.put(link.getFromNode().getId(), link.getFromNode());
		}
		for (Link link : node.getOutLinks().values()) {
			nodes.put(link.getToNode().getId(), link.getToNode());
		}
		return nodes;
	}	
}
