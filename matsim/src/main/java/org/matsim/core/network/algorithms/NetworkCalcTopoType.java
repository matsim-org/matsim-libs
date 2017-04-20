/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkCalcTopoType.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.core.network.algorithms;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.internal.NetworkRunnable;
import org.matsim.core.utils.misc.Counter;

/** See "http://www.ivt.ethz.ch/vpl/publications/reports/ab283.pdf"
 * for a description of node types. It's the graph matching paper.
 *
 * @author balmermi
 **/
public final class NetworkCalcTopoType implements NetworkRunnable {

	public final static Integer EMPTY        = 0;
	public final static Integer SOURCE       = 1;
	public final static Integer SINK         = 2;
	public final static Integer DEADEND      = 3;
	public final static Integer PASS1WAY     = 4;
	public final static Integer PASS2WAY     = 5;
	public final static Integer START1WAY    = 6;
	public final static Integer END1WAY      = 7;
	public final static Integer INTERSECTION = 8;

	private final Map<Node, Integer> topoTypePerNode = new IdentityHashMap<>(100000);

	@Override
	public void run(final Network network) {
		System.out.println("    running " + this.getClass().getName() + " algorithm...");
		Counter ctr = new Counter("node #");

		for (Node n : network.getNodes().values()) {
			ctr.incCounter();
			int nOfInLinks = n.getInLinks().size();
			int nOfOutLinks = n.getOutLinks().size();
			if ((nOfInLinks + nOfOutLinks) == 0) { setTopoType(n, EMPTY); }
			else if (nOfInLinks == 0) { setTopoType(n, SOURCE); }
			else if (nOfOutLinks == 0) {setTopoType(n, SINK); }
			else if (getNOfIncidentNodes(n) == 1) { setTopoType(n, DEADEND); }
			else if (getNOfIncidentNodes(n) == 2) {
				if ((nOfOutLinks == 1) && (nOfInLinks == 1)) { setTopoType(n, PASS1WAY); }
				else if ((nOfOutLinks == 2) && (nOfInLinks == 2)) { setTopoType(n, PASS2WAY); }
				else if ((nOfOutLinks == 2) && (nOfInLinks == 1)) { setTopoType(n, START1WAY); }
				else if ((nOfOutLinks == 1) && (nOfInLinks == 2)) { setTopoType(n, END1WAY); }
				// The following case is not covered by the paper, but quite common, e.g. parallel roads connecting the same nodes.
				else if ((nOfOutLinks >= 1) && (nOfInLinks >= 1)) { setTopoType(n, INTERSECTION); }
				else { throw new RuntimeException("Node=" + n.toString() + " cannot be assigned to a topo type!"); }
			}
			else { // more than two neighbors nodes and no sink or source
				setTopoType(n, INTERSECTION);
			}
		}

		int [] cnt = {0,0,0,0,0,0,0,0,0};
		for (Node n : network.getNodes().values()) {
			cnt[getTopoType(n)]++;
		}

		System.out.println("      #nodes        = " + network.getNodes().size());
		System.out.println("      #EMTPY        = " + cnt[EMPTY]);
		System.out.println("      #SOURCE       = " + cnt[SOURCE]);
		System.out.println("      #SINK         = " + cnt[SINK]);
		System.out.println("      #DEADEND      = " + cnt[DEADEND]);
		System.out.println("      #PASS1WAY     = " + cnt[PASS1WAY]);
		System.out.println("      #PASS2WAY     = " + cnt[PASS2WAY]);
		System.out.println("      #START1WAY    = " + cnt[START1WAY]);
		System.out.println("      #END1WAY      = " + cnt[END1WAY]);
		System.out.println("      #INTERSECTION = " + cnt[INTERSECTION]);

		System.out.println("    done.");
	}

	private void setTopoType(final Node node, final Integer topoType) {
		this.topoTypePerNode.put(node, topoType);
	}

	public int getTopoType(final Node node) {
		Integer i = this.topoTypePerNode.get(node);
		if (i == null) {
			return Integer.MIN_VALUE;
		}
		return i;
	}

	private int getNOfIncidentNodes(final Node node) {
		HashMap<Id<Node>, Node> nodes = new HashMap<>();
		for (Link link : node.getInLinks().values()) {
			nodes.put(link.getFromNode().getId(), link.getFromNode());
		}
		for (Link link : node.getOutLinks().values()) {
			nodes.put(link.getToNode().getId(), link.getToNode());
		}
		return nodes.size();
	}
}
